package gov.pnnl.jac.geom;

import cern.colt.bitvector.BitVector;

/**
 * <p>The class <tt>FilteredCoordinateList</tt> provides a simple way to
 * make an existing <tt>CoordinateList</tt> appear to be a subset of itself.
 * The instance of <tt>FilteredCoordinateList</tt> simple wraps another
 * <tt>CoordinateList</tt> and is given the indexes of the wrapped coordinates
 * to make accessible.
 * </p>
 * <p>
 * The indexes provided to the constructor may also be a reordering of the indexes
 * of the wrapped coordinate list. Hence, <tt>FilteredCoordinateList</tt> may be
 * used for virtual subsetting, reordering, or both.
 * </p>
 * 
 * @author R. Scarberry
 *
 */
public class FilteredCoordinateList implements CoordinateList {

	// The wrapped coordinates.
	private CoordinateList mWrappedCS;
	// Indexes of the wrapped coordinates to make accessible.
	private int[] mIndices;
	
	/**
	 * Constructor.  
	 * @param indices - the indices of the coordinates 
	 * @param wrappedCS
	 */
	public FilteredCoordinateList(int[] indices, CoordinateList wrappedCS) {
		mWrappedCS = wrappedCS;
		int cc = mWrappedCS.getCoordinateCount();
		int n = indices.length;
		mIndices = new int[n];
		for (int i=0; i<n; i++) {
			int ndx = indices[i];
			if (ndx < 0 || ndx >= cc) {
				String s = null;
				if (ndx < 0) {
					s = "< 0: " + ndx;
		        } else {
		        	s = ">= " + cc + ": " + ndx;
		        }
				throw new IndexOutOfBoundsException(s);
			}
			mIndices[i] = ndx;
		} // for
	}
	
	public static FilteredCoordinateList fromInclusionBits(
			BitVector bitsToInclude, CoordinateList wrappedCS) {
		final int card = bitsToInclude.cardinality();
		int[] indices = new int[card];
		final int sz = bitsToInclude.size();
		int count = 0;
		for (int i=0; i<sz; i++) {
			if (bitsToInclude.getQuick(i)) {
				indices[count++] = i;
			}
		}
		return new FilteredCoordinateList(indices, wrappedCS);
	}
	
	public static FilteredCoordinateList fromOutlierBits(
			BitVector outlierBits, CoordinateList wrappedCS) {
		final int sz = outlierBits.size();
		final int numIndices = sz - outlierBits.cardinality();
		int[] indices = new int[numIndices];
		int count = 0;
		for (int i=0; i<sz; i++) {
			if (!outlierBits.getQuick(i)) {
				indices[count++] = i;
			}
		}
		return new FilteredCoordinateList(indices, wrappedCS);
	}
	
	public CoordinateList getWrappedCoordinates() {
	    return mWrappedCS;
	}
	
	/**
	 * Returns the index of the coordinate in the wrapped
	 * coordinate list corresponding to the specified index in
	 * this instance of <tt>FilteredCoordinateList</tt>.
	 * 
	 * @param ndx
	 * 
	 * @return
	 */
	public int getWrappedIndex(int ndx) {
		return mIndices[ndx];
	}
	
	/**
	 * Get the number of coordinates stored in the set.
	 * @return - the number of coordinates, never negative.
	 */
    public int getCoordinateCount() {
    	return mIndices.length;
    }

    public int getWrappedCoordinateCount() {
    	return mWrappedCS.getCoordinateCount();
    }
    
    /**
	 * Get the number of dimensions for each coordinate
	 * stored in the set.  All coordinates stored in the set
	 * must have the same number of dimensions.
	 * @return - the number of dimensions, never negative.
	 */
    public int getDimensionCount() {
    	return mWrappedCS.getDimensionCount();
    }

    /**
     * Set the coordinate values for the coordinate with the
     * specified index.
     * @param ndx - the coordinate index which must be in the range
     *   <code>[0 - getCoordinateCount()-1]</code>.
     * @param coords - the coordinate values.
     */
    public void setCoordinates(int ndx, double[] coords) {
    	mWrappedCS.setCoordinates(mIndices[ndx], coords);
    }

    /**
     * Retrieve the coordinate values for the coordinate with
     * the specified index.  
     * @param ndx - the coordinate index which must be in the range
     *   <code>[0 - getCoordinateCount()-1]</code>.
     * @param coords - an array to hold the returned coordinates. 
     *   If non-null, must be of length <code>getDimensions()</code>.
     *   If null, a new array is allocated and returned with the values.
     * @return - the array containing the values, which will be the
     *   same as the second argument if that argument is non-null.
     * @throws IndexOutOfBoundsException - if <code>ndx</code> 
     *   is not in the valid range.
     * @throws IllegalArgumentException - if the array passed in is
     *   non-null but of incorrect length.  
     */
    public double[] getCoordinates(int ndx, double[] coords) {
    	return mWrappedCS.getCoordinates(mIndices[ndx], coords);
    }
    
    /**
     * Retrieves a column of coordinate data for the specified dimension.
     * 
     * @param dim - the dimension of data to retrieve, which must be
     *   in the range <code>[0 - getDimensions() - 1]</code>.
     * @param values - an array to hold the values for the dimension.
     *   If non-null, must be of length <code>getCoordinateCount()</code>.
     *   If null, a new array is allocated and returned containing the values.
     * @return - the array containing the values, which will be the same
     *   as the second argument if that argument is non-null.
     * @throws IndexOutOfBoundsException - if dim is out of range.
     * @throws IllegalArgumentException - if values is
     *   non-null and of improper length.
     */
    public double[] getDimensionValues(int dim, double[] values) {
    	return mWrappedCS.getDimensionValues(dim, values);
    }

    /**
     * Retrieve the value for the given index and dimension.
     * @param ndx - the coordinate index which must be in the range
     *   <code>[0 - getCoordinateCount()-1]</code>.
     * @param dim - the dimension for the value, which must be
     *   in the range <code>[0 - getDimensions() - 1]</code>.
     * @return - the value.
     * @throws IndexOutOfBoundException - either argument if 
     *   out of range.
     */
    public double getCoordinate(int ndx, int dim) {
    	return mWrappedCS.getCoordinate(mIndices[ndx], dim);
    }
    
    /**
     * Identical to <code>getCoordinate(ndx, dim)</code>, but
     * bounds checking is not performed on the arguments.  This
     * method is mandated by the interface, so other methods  
     * can retrieve coordinates in loops without having 
     * redundant bounds checking performed on every iteration.  
     * Do not call this method directly unless you are sure the
     * arguments are in range.  If they are not, the behavior is
     * determined by the implementation class.  
     * @param ndx - the coordinate index which must be in the range
     *   <code>[0 - getCoordinateCount()-1]</code>.
     * @param dim - the dimension for the value, which must be
     *   in the range <code>[0 - getDimensions() - 1]</code>.
     * @return - the value.
     */
    public double getCoordinateQuick(int ndx, int dim) {
    	return mWrappedCS.getCoordinateQuick(mIndices[ndx], dim);
    }

    /**
     * Computes the average coordinate vector for a number of indices.
     * 
     * @param indices - an array containing the indices of the
     *   coordinates to be averaged.
     * @param avg - an array to hold the computed averages.  If 
     *   non-null, it must be of length <code>getDimensions()</code>.
     *   If null, a new array is allocated and returned with the 
     *   computed averages.
     * @return - an array containing the computed averages.
     */
    public double[] computeAverage(int[] indices, double[] avg) {
    	return mWrappedCS.computeAverage(wrappedIndices(indices), avg);
    }
    
    /**
     * Computes the average value for the specified dimension
     * from those coordinates with the specified indices.
     * 
     * @param indices - an array containing the indices of the
     *   coordinates to be averaged.
     * @param dim - the dimension of interest.
     * 
     * @return - the average.
     */
    public double computeAverage(int[] indices, int dim) {
    	return mWrappedCS.computeAverage(wrappedIndices(indices), dim);
    }

    /**
     * Obtains the minimum values in all dimensions considering the coordinates
     * with the specified indices.
     * 
     * @param indices - an array containing the indices of the
     *   coordinates of interest.
     * @param avg - an array to hold the minimum values.  If 
     *   non-null, it must be of length <code>getDimensions()</code>.
     *   If null, a new array is allocated and returned with the 
     *   computed averages.
     *   
     * @return - an array containing the minima.
     */
    public double[] computeMinimum(int[] indices, double[] min) {
    	return mWrappedCS.computeMinimum(wrappedIndices(indices), min);
    }
    
    /**
     * Obtains the minimum value in the specified dimension considering the coordinates
     * with the specified indices.
     * 
     * @param indices - an array containing the indices of the
     *   coordinates of interest.
     * @param dim - the dimension of interest.
     *   
     * @return - the minimum.
     */
    public double computeMinimum(int[] indices, int dim) {
    	return mWrappedCS.computeMinimum(wrappedIndices(indices), dim);
    }

    /**
     * Obtains the maximum values in all dimensions considering the coordinates
     * with the specified indices.
     * 
     * @param indices - an array containing the indices of the
     *   coordinates of interest.
     * @param avg - an array to hold the maximum values.  If 
     *   non-null, it must be of length <code>getDimensions()</code>.
     *   If null, a new array is allocated and returned with the 
     *   computed averages.
     *   
     * @return - an array containing the maxima.
     */
    public double[] computeMaximum(int[] indices, double[] max) {
    	return mWrappedCS.computeMaximum(wrappedIndices(indices), max);
    }
    
    /**
     * Obtains the maximum value in the specified dimension considering the coordinates
     * with the specified indices.
     * 
     * @param indices - an array containing the indices of the
     *   coordinates of interest.
     * @param dim - the dimension of interest.
     *   
     * @return - the maximum.
     */
    public double computeMaximum(int[] indices, int dim) {
    	return mWrappedCS.computeMaximum(wrappedIndices(indices), dim);
    }

    /**
     * Computes the median coordinate vector for a number of indices.
     * 
     * @param indices - an array containing the indices of the
     *   coordinates to be considered.
     * @param avg - an array to hold the computed medians.  If 
     *   non-null, it must be of length <code>getDimensions()</code>.
     *   If null, a new array is allocated and returned with the 
     *   computed averages.
     * @return - an array containing the computed medians.
     */
    public double[] computeMedian(int[] indices, double[] med) {
    	return mWrappedCS.computeMedian(wrappedIndices(indices), med);
    }
    
    /**
     * Computes the median value for the specified dimension
     * from those coordinates with the specified indices.
     * 
     * @param indices - an array containing the indices of the
     *   coordinates to be averaged.
     * @param dim - the dimension of interest.
     * 
     * @return - the median.
     */
    public double computeMedian(int[] indices, int dim) {
    	return mWrappedCS.computeMedian(wrappedIndices(indices), dim);
    }

    /**
     * Sets the value for the specified coordinate and dimension.
     * @param ndx the index of the coordinate.
     * @param dim the dimension to be set.
     * @param coord the value to be applied.
     * 
     * @throws IndexOutOfBoundsException if either ndx or dim is out of range.
     */
    public void setCoordinate(int ndx, int dim, double coord) {
    	mWrappedCS.setCoordinate(mIndices[ndx], dim, coord);
    }
    
    /**
     * Identical to <code>setCoordinate(ndx, dim, coord)</code>, but
     * bounds checking is not performed on the arguments.  This
     * method is mandated by the interface, so other methods  
     * can quickly set values in loops without having 
     * redundant bounds checking performed on every iteration.  
     * Do not call this method directly unless you are sure the
     * arguments are in range.  If they are not, the behavior is
     * determined by the implementation class.  
     * @param ndx the index of the coordinate.
     * @param dim the dimension to be set.
     * @param coord the value to be applied.
     * 
     * @throws IndexOutOfBoundsException if either ndx or dim is out of range.
     */
    public void setCoordinateQuick(int ndx, int dim, double coord) {
        mWrappedCS.setCoordinateQuick(mIndices[ndx], dim, coord);
    }

    // Returns the wrapped indices for the indices in the array.
    private int[] wrappedIndices(int[] indices) {
    	int n = indices.length;
    	int[] wrappedIndices = new int[n];
    	for (int i=0; i<n; i++) {
    		wrappedIndices[i] = mIndices[indices[i]];
    	}
    	return wrappedIndices;
    }
}
