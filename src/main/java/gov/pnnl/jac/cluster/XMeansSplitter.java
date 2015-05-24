/*
 * XMeansSplitter.java
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
import gov.pnnl.jac.geom.FilteredCoordinateList;
import gov.pnnl.jac.task.TaskOutcome;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class XMeansSplitter extends ClusterSplitter {

    private static final int[] FEW_SPLITS = new int[] { 2, 3 };
    private static final int[] MANY_SPLITS = new int[] { 2, 3, 5, 7, 11 };
    
    private CoordinateList mCoordinates;
    private ClusterList mClusterList;
    private XMeansClusterTaskParams mParams;
    
    private double mOverallBIC;
    
    public XMeansSplitter(CoordinateList coordinates, ClusterList clusterList,
            double overallBIC, XMeansClusterTaskParams params) {
        if (coordinates == null || clusterList == null || params == null) {
            throw new NullPointerException();
        }
        mCoordinates = coordinates;
        mClusterList = clusterList;
        mParams = params;
        mOverallBIC = overallBIC;
    }
    
    @Override
    protected boolean preferSplit(Cluster cluster, Collection<Cluster> children) {
        return children.size() > 0;
    }

    @Override
    protected Collection<Cluster> splitCluster(Cluster cluster) {

        boolean useOverallBIC = mParams.getUseOverallBIC();
        
        double bicThreshold = mOverallBIC;
        if (!useOverallBIC) {
            bicThreshold = ClusterStats.computeBIC(mCoordinates, cluster);
        }
        
        Collection<Cluster> result = null;
        int sz = cluster.getSize();
        int clusterCount = mClusterList.getClusterCount();
        int[] splitsToTry = clusterCount <= 3 ?
                MANY_SPLITS : FEW_SPLITS;
        
        for (int i=0; i<splitsToTry.length; i++) {
            int splitDiv = splitsToTry[i];
            if (sz >= splitDiv) {
                Collection<Cluster> children = split(cluster, splitDiv);
                int numChildren = children.size();
                if (numChildren > 1) {
                    
                    double bic = 0;
                    
                    if (useOverallBIC) {
                        bic = ClusterStats.computeBIC(mCoordinates, 
                                prepareClusterArray(mClusterList, children, cluster));
                    } else {
                        bic = ClusterStats.computeBIC(mCoordinates, 
                                children.toArray(new Cluster[children.size()]));
                    }
                
                    if (!Double.isNaN(bic) && bic > bicThreshold) {
                        result = children;
                        break;
                    }
                }
                
                if (numChildren < splitDiv) {
                    break;
                }
            }
        } // for
        
        if (result == null) {
            result = Arrays.asList(cluster);
        }
        
        return result;
    }

    private Collection<Cluster> split(Cluster cluster, int howMany) {
        
        int[] memberIndices = cluster.getMembership();
        FilteredCoordinateList fcs = new FilteredCoordinateList(memberIndices, mCoordinates);
        
        KMeansClusterTask kmeans = new KMeansClusterTask(fcs,
                new KMeansClusterTaskParams(howMany, Integer.MAX_VALUE, 0, 1, false,
                mParams.getDistanceFunc(), mParams.getClusterSeeder()));
        
        kmeans.run();
        
        if (kmeans.getTaskOutcome() != TaskOutcome.SUCCESS) {
        	System.err.printf("kmeans outcome = %s\n", kmeans.getTaskOutcome());
        	if (kmeans.getTaskOutcome() == TaskOutcome.ERROR) {
        		Throwable t = kmeans.getError(); 
        		if (t != null) {
        			t.printStackTrace();
        		}
        		System.err.printf("\tKMEANS ERROR: %s\n", kmeans.getErrorMessage());
        	}
        }
        
        ClusterList clusters = kmeans.getClusterList();
        int numClusters = clusters.getClusterCount();
        
        Collection<Cluster> rtn = new ArrayList<Cluster> (numClusters);
        
        for (int i=0; i<numClusters; i++) {
                Cluster c = clusters.getCluster(i);
                int[] indices = c.getMembership();
                int n = indices.length;
                for (int j=0; j<n; j++) {
                        indices[j] = fcs.getWrappedIndex(indices[j]);
                }
                rtn.add(new Cluster(indices, c.getCenter()));
        }
        
        return rtn;
    }
    
    private static Cluster[] prepareClusterArray(ClusterList clusterList,
            Collection<Cluster> splitClusters, Cluster original) {
        int numClusters = clusterList.getClusterCount();
        List<Cluster> clist = new ArrayList<Cluster> (numClusters + splitClusters.size() - 1);
        for (int i=0; i<numClusters; i++) {
            Cluster c = clusterList.getCluster(i);
            if (c != original) {
                clist.add(c);
            }
        }
        clist.addAll(splitClusters);
        int n = clist.size();
        Cluster[] result = new Cluster[n];
        clist.toArray(result);
        return result;
    }
}
