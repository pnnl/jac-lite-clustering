package gov.pnnl.jac.geom;

/**
 * <p>A <tt>CoordinateList</tt> represents a list of values for coordinates,
 * each having the same number of dimensions.  A <tt>CoordinateList</tt> 
 * is typically used to represent a list of point in N-space, when N is the
 * number of dimensions.</p>
 *
 * @author R. Scarberry
 * 
 * @version 1.0
 */
public interface CoordinateList {

	/**
	 * Get the number of coordinates stored in the list.
	 * @return - the number of coordinates, never negative.
	 */
    public int getCoordinateCount();

    /**
	 * Get the number of dimensions for each coordinate
	 * stored in the list.  All coordinates stored in the list
	 * have the same number of dimensions.
	 * @return - the number of dimensions, never negative.
	 */
    public int getDimensionCount();

    /**
     * Set the coordinate values for the coordinate with the
     * specified index.
     * @param ndx - the coordinate index which must be in the range
     *   <code>[0 - getCoordinateCount()-1]</code>.
     * @param coords - the coordinate values.
     * @throws IndexOutOfBoundsException - if <code>ndx</code> is out of range.
     * @throws IllegalArgumentException - if <code>coords</code> is not
     *   of length <code>getDimensions()</code>.
     */
    public void setCoordinates(int ndx, double[] coords);

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
    public double[] getCoordinates(int ndx, double[] coords);
    
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
    public double[] getDimensionValues(int dim, double[] values);

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
    public double getCoordinate(int ndx, int dim);

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
    public double getCoordinateQuick(int ndx, int dim);

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
    public double[] computeAverage(int[] indices, double[] avg);

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
    public double computeAverage(int[] indices, int dim);

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
    public double[] computeMinimum(int[] indices, double[] min);

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
    public double computeMinimum(int[] indices, int dim);

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
    public double[] computeMaximum(int[] indices, double[] max);

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
    public double computeMaximum(int[] indices, int dim);

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
    public double[] computeMedian(int[] indices, double[] med);

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
    public double computeMedian(int[] indices, int dim);

    /**
     * Sets the value for the specified coordinate and dimension.
     * @param ndx the index of the coordinate.
     * @param dim the dimension to be set.
     * @param coord the value to be applied.
     * 
     * @throws IndexOutOfBoundsException if either ndx or dim is out of range.
     */
    public void setCoordinate(int ndx, int dim, double coord);
    
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
    public void setCoordinateQuick(int ndx, int dim, double coord);
}
