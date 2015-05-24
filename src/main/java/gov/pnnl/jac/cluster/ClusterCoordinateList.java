/*
 * ClusterCoordinateList.java
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

import gov.pnnl.jac.geom.*;

/**
 * Wrapper class that makes a {@link ClusterList} appear to 
 * be a {@link gov.pnnl.jac.geom.CoordinateList}.  However, since
 * {@link Cluster}s and {@link ClusterList}s are both immutable,
 * the set method throw <tt>UnsupportedOperationException</tt>s.
 * 
 * @author R. Scarberry
 *
 */
public class ClusterCoordinateList extends AbstractCoordinateList {
    
    private ClusterList mClusters;
    
    public ClusterCoordinateList(ClusterList clusters) {
        if (clusters == null) {
            throw new NullPointerException();
        }
        mClusters = clusters;
        mCount = mClusters.getClusterCount();
        if (mCount > 0) {
            mDim = mClusters.getCluster(0).getDimensions();
        }
    }
    
    public ClusterList getClusterList() {
        return mClusters;
    }

    public double getCoordinateQuick(int ndx, int dim) {
        return mClusters.getCluster(ndx).getCenterElement(dim);
    }

    public double[] getCoordinates(int ndx, double[] coords) {
        double[] center = mClusters.getCluster(ndx).getCenterDirect();
        if (coords == null || coords.length < center.length) {
            coords = new double[center.length];
        }
        System.arraycopy(center, 0, coords, 0, center.length);
        return coords;
    }

    public void setCoordinateQuick(int ndx, int dim, double coord) {
        throw new UnsupportedOperationException();
    }

    public void setCoordinates(int ndx, double[] coords) {
        throw new UnsupportedOperationException();
    }
}
