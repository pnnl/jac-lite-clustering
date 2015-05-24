package gov.pnnl.jac.geom;

/**
 * <p>Abstract base class for <tt>CoordinateList</tt> implementations which
 * provides a number of the more common features.  Most importantly,
 * it defines the statistical methods so subclasses do not have to.</p>
 * 
 * @author R. Scarberry
 *
 */
public abstract class AbstractCoordinateList implements CoordinateList {

	// The number of dimensions and the number of coordinates,
	// which subclasses should always set in their 
	// constructors.
	protected int mDim, mCount;
		
	/**
	 * Get the number of coordinates stored in the list.
	 * @return - the number of coordinates, never negative.
	 */
	public int getCoordinateCount() {
		return mCount;
	}

    /**
	 * Get the number of dimensions for each coordinate
	 * stored in the list.  All coordinates stored in the list
	 * have the same number of dimensions.
	 * @return - the number of dimensions, never negative.
	 */
	public int getDimensionCount() {
		return mDim;
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
        checkIndex(ndx);
        checkDimension(dim);
        return getCoordinateQuick(ndx, dim);
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
        checkIndex(ndx);
        checkDimension(dim);
        setCoordinateQuick(ndx, dim, coord);
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
        checkDimension(dim);
        double[] v = null;
        if (values != null) {
            if (values.length != mCount) {
                throw new IllegalArgumentException(String
                        .valueOf(values.length)
                        + " != " + mCount);
            }
            v = values;
        } else {
            v = new double[mCount];
        }
        for (int i=0; i<mCount; i++) {
            v[i] = getCoordinateQuick(i, dim);
        }
        return v;
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
		
		checkIndices(indices);
		double[] rtn = null;
		if (avg != null) {
			checkDimensions(avg.length);
			rtn = avg;
		} else {
			rtn = new double[mDim];
		}
		java.util.Arrays.fill(rtn, 0.0);
		int[] counts = new int[mDim];
		double[] coord = new double[mDim];
		
		int n = indices.length;

		for (int i = 0; i < n; i++) {
			int ndx = indices[i];
			getCoordinates(ndx, coord);
			for (int d=0; d<mDim; d++) {
				double dv = coord[d];
				if (!Double.isNaN(dv)) {
					rtn[d] += dv;
					counts[d]++;
				}
			}
		}
		
		for (int d = 0; d < mDim; d++) {
			int ct = counts[d];
			if (ct >= 1) {
				rtn[d] /= ct;
			} else {
				// No information in dimension d.
				rtn[d] = Double.NaN;
			}
		}
		
		return rtn;
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
		checkIndices(indices);
		checkDimension(dim);
		double avg = 0.0;
		int n = indices.length;
		int count = 0;
		for (int i = 0; i < n; i++) {
			int ndx = indices[i];
			double dv = getCoordinateQuick(ndx, dim);
			if (!Double.isNaN(dv)) {
				avg += dv;
				count++;
			}
		}
		if (count >= 1) {
			avg /= count;
		} else {
			avg = Double.NaN;
		}
		return avg;
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
		checkIndices(indices);
		double[] rtn = null;
		if (min != null) {
			checkDimensions(min.length);
			rtn = min;
		} else {
			rtn = new double[mDim];
		}
		for (int d = 0; d < mDim; d++) {
			rtn[d] = computeMinimum(indices, d);
		}
		return rtn;
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
		checkIndices(indices);
		checkDimension(dim);
		double min = Double.NaN;
		int n = indices.length;
		for (int i = 0; i < n; i++) {
			double d = getCoordinateQuick(indices[i], dim);
			if (!Double.isNaN(d) && (d < min || Double.isNaN(min))) {
				min = d;
			}
		}
		return min;
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
		checkIndices(indices);
		double[] rtn = null;
		if (max != null) {
			checkDimensions(max.length);
			rtn = max;
		} else {
			rtn = new double[mDim];
		}
		for (int d = 0; d < mDim; d++) {
			rtn[d] = computeMaximum(indices, d);
		}
		return rtn;
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
		checkIndices(indices);
		checkDimension(dim);
		double max = Double.NaN;
		int n = indices.length;
		for (int i = 0; i < n; i++) {
			double d = getCoordinateQuick(indices[i], dim);
			if (!Double.isNaN(d) && (d > max || Double.isNaN(max))) {
				max = d;
			}
		}
		return max;
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
		checkIndices(indices);
		double[] rtn = null;
		if (med != null) {
			checkDimensions(med.length);
			rtn = med;
		} else {
			rtn = new double[mDim];
		}
		for (int i = 0; i < mDim; i++) {
			rtn[i] = _computeMedian(indices, i, false);
		}
		return rtn;
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
		return _computeMedian(indices, dim, true);
	}

	private double _computeMedian(int[] indices, int dim, boolean checkIndices) {
		if (checkIndices) {
			checkIndices(indices);
		}
		double med = Double.NaN;
		int n = indices.length;
		if (n > 0) {
			double[] values = new double[n];
			for (int i = 0; i < n; i++) {
				values[i] = getCoordinateQuick(indices[i], dim);
			}
			med = CoordinateMath.median(values, true);
		}
		return med;
	}

	protected void checkIndex(int ndx) {
		if (ndx < 0 || ndx >= mCount) {
			String s = null;
			if (ndx < 0) {
				s = "< 0: " + ndx;
			} else {
				s = ">= " + mCount + ": " + ndx;
			}
			throw new IndexOutOfBoundsException(s);
		}
	}

	protected void checkIndices(int[] indices) {
		int n = indices.length;
		for (int i = 0; i < n; i++) {
			int ndx = indices[i];
			if (ndx < 0 || ndx >= mCount) {
				String s = null;
				if (ndx < 0) {
					s = "< 0: " + ndx;
				} else {
					s = ">= " + mCount + ": " + ndx;
				}
				throw new IndexOutOfBoundsException(s);
			}
		}
	}

	protected void checkDimension(int dim) {
		if (dim < 0 || dim >= mDim) {
			String s = null;
			if (dim < 0) {
				s = "< 0: " + dim;
			} else {
				s = ">= " + mDim + ": " + dim;
			}
			throw new IndexOutOfBoundsException(s);
		}
	}

	protected void checkDimensions(int dim) {
		if (dim != mDim) {
			throw new IllegalArgumentException(String.valueOf(dim) + " != "
					+ mDim);
		}
	}
}
