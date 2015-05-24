/*
 * Cluster.java
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

import gov.pnnl.jac.collections.ArrayIntIterator;
import gov.pnnl.jac.collections.IntIterator;
import gov.pnnl.jac.geom.CoordinateList;

import java.util.Arrays;

/**
 * <p><tt>Cluster</tt> represents a cluster of coordinates.
 * Each <tt>Cluster</tt> object contains the indexes of
 * the coordinates, but not the coordinate values themselves.
 * A <tt>Cluster</tt> object maintains a cluster center
 * (centroid) with the same dimensionality as its coordinates.</p>
 * <p><tt>Cluster</tt> objects are immutable.</p>
 *
 * @author R. Scarberry
 * @version 1.0
 */
public class Cluster {

    // Indexes of the coordinates, always kept in sorted order.
    private int[] mIndexes;
    // The cluster center.
    private double[] mCenter;

    /**
     * Constructor which takes the indexes of the coordinates
     * making up the cluster and the pre-computed center.
     * 
     * @param indexes the indexes of the 0-indexed
     *   members of a <code>gov.pnnl.jac.geom.CoordinateList</code>.
     * @param center
     */
    public Cluster(int[] indexes, double[] center) {
        int len = indexes.length;
        mIndexes = new int[len];
        System.arraycopy(indexes, 0, mIndexes, 0, len);
        Arrays.sort(mIndexes);
        len = center.length;
        mCenter = new double[len];
        System.arraycopy(center, 0, mCenter, 0, len);
    }

    /**
     * Constructor taking the indexes of the coordinates in the
     * cluster and the <tt>CoodinateList</tt> containing the 
     * coordinates.  This constructor computes the center itself.
     * 
     * @param indexes
     * @param cs
     * 
     * @throws IndexOutOfBoundsException - if any of the indexes
     *   are invalid for the <tt>CoordinateList</tt>.
     */
    public Cluster(int[] indexes, CoordinateList cs) {
        int len = indexes.length;
        mIndexes = new int[len];
        System.arraycopy(indexes, 0, mIndexes, 0, len);
        Arrays.sort(mIndexes);
        int dim = cs.getDimensionCount();
        mCenter = new double[dim];
        // Compute the cluster center.
        cs.computeAverage(indexes, mCenter);
    }

    /**
     * Returns true if this cluster contains the specified index.
     * @param index
     * @return
     */
    public boolean contains(int index) {
        // Uses a binary search -- mIDs must be sorted for this to work.
        int n = mIndexes.length;
        if (n > 0) {
            int low = 0;
            int high = n-1;
            while (low <= high) {
                int mid = (low + high) >> 1;
                int midVal = mIndexes[mid];
                if (midVal < index)
                    low = mid + 1;
                else if (midVal > index)
                    high = mid - 1;
                else
                    return true;
            }
        }
        return false;
    }

    /**
     * Returns an array containing a copy of the membership (coordinate indexes)
     * of the cluster.    
     * 
     * @return - an array containing the membership, sorted.
     */
    public int[] getMembership() {
     	int n = mIndexes.length;
        int[] ids = new int[n];
        System.arraycopy(mIndexes, 0, ids, 0, n);
        return ids;
    }
    
    /**
     * Returns an iterator over the cluster membership.
     * 
     * @return
     */
    public IntIterator getMembershipIterator() {
        return new ArrayIntIterator(mIndexes);
    }

    /**
     * Returns the number of members in the cluster.
     * @return
     */
    public int getSize() {
    	return mIndexes.length;
    }

    /**
     * Returns the index of the nth member of the cluster.
     * 
     * @param n
     * @return
     */
    public int getMember(int n) {
     	return mIndexes[n];
    }

    /**
     * Returns the number of dimensions of the coordinates making up
     * the cluster.
     * 
     * @return
     */
    public int getDimensions() {
     	return mCenter.length;
    }

    /**
     * Returns a reference to the cluster center, not
     * a protective copy.  This method has protected access,
     * so only subclasses and classes in the same package can
     * call it.
     * @return
     */
    protected double[] getCenterDirect() {
    	return mCenter;
    }

    /**
     * Returns a copy of the cluster center.
     * @return
     */
    public double[] getCenter() {
    	double[] rtn = new double[mCenter.length];
    	System.arraycopy(mCenter, 0, rtn, 0, mCenter.length);
    	return rtn;
    }

    /**
     * Returns the nth element of the cluster center, where
     * n must be in the range <code>[0 - cluster.getDimensions() - 1]</code>.
     * @param n
     * @return
     */
    public double getCenterElement(int n) {
     	return mCenter[n];
    }

}
