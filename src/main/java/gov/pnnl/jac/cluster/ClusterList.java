/*
 * ClusterList.java
 * 
 * JAC: Java Analytic Components
 * 
 * For information contact Randall Scarberry, randall.scarberry@pnl.gov
 * 
 * Notice: This computer software was prepared by Battelle Memorial Institute, 
 * hereinafter the Contractor, under Contract No. DE-AC05-76RL0 1830 with the 
 * Department of Energy (DOE).  All rights in the computer software are 
 * reserved by DOE on behalf of the United States Government and the Contractor
 * as provided in the Contract.  You are authorized to use this computer 
 * software for Governmental purposes but it is not to be released or 
 * distributed to the public.  NEITHER THE GOVERNMENT NOR THE CONTRACTOR MAKES 
 * ANY WARRANTY, EXPRESS OR IMPLIED, OR ASSUMES ANY LIABILITY FOR THE USE OF 
 * THIS SOFTWARE.  This notice including this sentence must appear on any 
 * copies of this computer software.
 */
package gov.pnnl.jac.cluster;

import gov.pnnl.jac.geom.CoordinateList;
import gov.pnnl.jac.geom.CoordinateMath;
import gov.pnnl.jac.geom.SimpleCoordinateList;
import gov.pnnl.jac.math.linalg.OpenLongMapRealMatrix;
import gov.pnnl.jac.util.ArrayUtils;
import gov.pnnl.jac.util.ExceptionUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.Externalizable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.OpenMapRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;

/**
 * <p><tt>ClusterList</tt> is an immutable list of <tt>Cluster</tt> object
 * instances.</p>
 *
 * @author R. Scarberry
 * @version 1.0
 */
public class ClusterList implements Externalizable {

    private static final int SERIAL_VERSION = 1;
    
    // For large centroid matrices, this threshold determines whether to use
    // a sparse or dense implementation.
    private static double SPARSENESS_THRESHOLD = 0.6;
    // A 16,384 x 16,384 will exceed this threshold.
    private static int MATRIX_SIZE_SPARSENESS_THRESHOLD = Integer.MAX_VALUE/8; 
    
    // The cluster objects.
    private Cluster[] mClusters;

    /**
     * Constructor.
     * @param clusters - an array containing the cluster objects.
     */
    public ClusterList(Cluster[] clusters) {
        ExceptionUtil.checkNotNull((Object) clusters);
        ExceptionUtil.checkNotNull((Object []) clusters);
        int len = clusters.length;
        mClusters = new Cluster[len];
        System.arraycopy(clusters, 0, mClusters, 0, len);
    }

    /**
     * Returns the number of clusters in this <tt>ClusterList</tt>.
     * @return
     */
    public int getClusterCount() {
        return mClusters.length;
    }

    /**
     * Returns the <tt>Cluster</tt> with the specified index.
     * @param ndx
     * @return
     */
    public Cluster getCluster(int ndx) {
        return mClusters[ndx];
    }
    
    public double averageClusterSize() {
    	final int sz = this.getClusterCount();
    	double result = 0;
    	if (sz > 0) {
    		double[] values = new double[sz];
    		for (int i=0; i<sz; i++) {
    			values[i] = this.getCluster(i).getSize();
    		}
    		result = CoordinateMath.mean(values);
    	}
    	return result;
    }
    
    public double sizeStandardDeviation() {
    	final int sz = this.getClusterCount();
    	double result = 0;
    	if (sz > 0) {
    		double[] values = new double[sz];
    		for (int i=0; i<sz; i++) {
    			values[i] = this.getCluster(i).getSize();
    		}
    		double[] d = CoordinateMath.meanAndVariance(values);
    		result = Math.sqrt(d[1]);
    	}
    	return result;
    }

    /**
     * Creates a matrix 
     * with rows containing the cluster centers.
     * @return - an instance of <code>org.apache.commons.math3.linear.RealMatrix</code>.
     */
    public RealMatrix createCentroidMatrix() {

    	int rows = mClusters != null ? mClusters.length : 0;
    	int cols = 0;
    	
    	if (rows > 0) {
    		cols = mClusters[0].getDimensions();
    	}

    	boolean useSparse = false;
    	
    	if (((double) rows ) * cols > MATRIX_SIZE_SPARSENESS_THRESHOLD) {
    		
    		double sparseness = 0.0;
    		if (rows > 0) {
    			for (int i=0; i<rows; i++) {
    				sparseness += ArrayUtils.sparseness(mClusters[i].getCenterDirect());
    			}
    			sparseness /= rows;
    		}
    		
    		useSparse = sparseness >= SPARSENESS_THRESHOLD;
    	}
    	
    	RealMatrix centroids = null;
    	if (useSparse) {
    		centroids = new OpenLongMapRealMatrix(rows, cols);
    		for (int i=0; i<rows; i++) {
    			double[] center = mClusters[i].getCenterDirect();
    			for (int j=0; j<cols; j++) {
    				double d = center[j];
    				if (d != 0.0) {
    					centroids.setEntry(i, j, d);
    				}
    			}
    		}
    	} else { // Return a dense matrix.
    		double[][] data = new double[rows][cols];
    		for (int i=0; i<rows; i++) {
    			double[] rowData = data[i];
    			System.arraycopy(mClusters[i].getCenterDirect(), 0, rowData, 0, cols);
    		}
    		centroids = new Array2DRowRealMatrix(data);
    	}
    	
    	return centroids;
    }
    
    public void readExternal(ObjectInput in) throws IOException,
    ClassNotFoundException {
        ClusterList clusters = load(in);
        this.mClusters = clusters.mClusters;
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        save(this, out);
    }
    
    public static void save(ClusterList clusters, File f) throws IOException {
        DataOutputStream dos = null;
        try {
            dos = new DataOutputStream(
                    new BufferedOutputStream(new FileOutputStream(f)));
            save(clusters, dos);
            dos.flush();
            dos.close();
            dos = null;
        } finally {
            if (dos != null) {
                try {
                    dos.close();
                } catch (IOException e) {
                }
            }
        }
    }
    
    public static void save(ClusterList clusters, DataOutput out) throws IOException {
        out.writeInt(SERIAL_VERSION);
        int numClusters = clusters.getClusterCount();
        out.writeInt(numClusters);
        for (int i=0; i<numClusters; i++) {
            Cluster c = clusters.getCluster(i);
            double[] center = c.getCenterDirect();
            int[] members = c.getMembership();
            out.writeInt(center.length);
            for (int j=0; j<center.length; j++) {
                out.writeDouble(center[j]);
            }
            out.writeInt(members.length);
            for (int j=0; j<members.length; j++) {
                out.writeInt(members[j]);
            }
        }
    }
    
    public static ClusterList load (File f) throws IOException {
        DataInputStream dis = null;
        try {
            dis = new DataInputStream(new BufferedInputStream(new FileInputStream(f)));
            ClusterList clusters = load(dis);
            dis.close();
            dis = null;
            return clusters;
        } finally {
            if (dis != null) {
                try {
                    dis.close();
                } catch (IOException e) {
                }
            }
        }
    }
    
    public static ClusterList load(DataInput in) throws IOException {
        int serialVersion = in.readInt();
        if (serialVersion == 1) {
            try {
                int numClusters = in.readInt();
                Cluster[] clusters = new Cluster[numClusters];
                for (int i=0; i<numClusters; i++) {
                    int n = in.readInt();
                    double[] center = new double[n];
                    for (int j=0; j<n; j++) {
                        center[j] = in.readDouble();
                    }
                    n = in.readInt();
                    int[] members = new int[n];
                    for (int j=0; j<n; j++) {
                        members[j] = in.readInt();
                    }
                    clusters[i] = new Cluster(members, center);
                }
                return new ClusterList(clusters);
            } catch (RuntimeException e) {
                throw new IOException("invalid cluster data: " + e);
            }
        } else {
            throw new IOException("invalid serial version: " + serialVersion);
        }
    }
    
    /**
     * Returns a coordinate list containing the cluster centers as the coordinate
     * values.
     * 
     * @return
     */
    public CoordinateList centersAsCoordinateList() {
    	int clusterCount = getClusterCount();
    	int dimensionCount = 0;
    	if (clusterCount > 0) {
    		dimensionCount = getCluster(0).getDimensions();
    	}
    	CoordinateList coordList = new SimpleCoordinateList(dimensionCount, clusterCount);
    	for (int i=0; i<clusterCount; i++) {
    		Cluster c = getCluster(i);
    		coordList.setCoordinates(i, c.getCenterDirect());
    	}
    	return coordList;
    }
}
