package gov.pnnl.jac.projection;

/** 
 * Interface which defines a container for projection data,
 * which is usually 2 or 3 dimensions.  However, the number of 
 * projections dimensions may be any positive integer.
 * 
 * @author d3j923
 *
 */
public interface ProjectionData {
   
    /**
     * Returns the number of projection points maintained in this
     * object. 
     * @return
     */
    public int getProjectionCount();
    
    /**
     * Returns the dimensionality for the projection.
     * @return - a positive integer.
     */
    public int getDimensionCount();
    
    /**
     * Get the minimum permissible value for a projection
     * in the specified dimension.  All actual values in
     * this dimension must be greater than or equal to this
     * value.
     * @param dimension
     * @return - the minimum value.
     */
    public float getMinAllowed(int dimension);
    
    /**
     * Get the maximum permissible value for a projection
     * in the specified dimension.  All actual values in
     * this dimension must be less than or equal to this
     * value.
     * @param dimension
     * @return - the maximum value.
     */
    public float getMaxAllowed(int dimension);
    
    /**
     * Set the minimum permissible value for a projection
     * in the specified dimension.  All actual values in
     * this dimension must be greater than or equal to this
     * value.
     * @param dimension
     * @param value 
     */
    public void setMinAllowed(int dimension, float value);
    
    /**
     * Set the maximum permissible value for a projection
     * in the specified dimension.  All actual values in
     * this dimension must be less than or equal to this
     * value.
     * @param dimension
     * @para - the value.
     */
    public void setMaxAllowed(int dimension, float value);

    /**
     * Get the minimum actual value for a projection
     * in the specified dimension.  This should be 
     * greater than or equal to the value returned from
     * <code>getMinAllowed(dimension)</code>.
     * @param dimension
     * @return - the minimum value.
     */
    public float getMin(int dimension);
    
    /**
     * Get the maximum actual value for a projection
     * in the specified dimension.  This should be 
     * less than or equal to the value returned from
     * <code>getMaxAllowed(dimension)</code>.
     * @param dimension
     * @return - the maximum value.
     */
    public float getMax(int dimension);
    
    /**
     * Sets the projection for the point with the specified index.
     * @param ndx - the 0-based index of the point whose 
     *   projection is to be set.
     * @param projection - an array holding the projection values.
     * @exception IndexOutOfBoundsException - if ndx is not a valid
     *   point index.
     * @exception IllegalArgumentException - if the projection array's
     *   length is not equal to the number of projected dimensions.
     */
    public void setProjection(int ndx, float[] projection);

    public void setProjection(int ndx, int col, float projection);
    
    /**
     * Retrieves the projection for the point with the specified index.
     * @param ndx - the 0-based index of the point whose 
     *   projection is to be set.
     * @param projection - an array into which the projection values should
     *   be copied.  If this array is null or of incorrect length, a
     *   new array is allocated and returned.
     * @return an array containing the values.  This will be the passed in
     *   array if it's of the correct length.
     * @exception IndexOutOfBoundsException - if ndx is not a valid
     *   point index.
     */
    public float[] getProjection(int ndx, float[] projection);
    
    /**
     * Retrieves the projection component in the specified dimension 
     * for the point with the specified index. 
     * @param ndx the index of the point (zero-based).
     * @param dimension the index of the dimension (zero-based).
     * @return the projection value.
     * @throws IndexOutOfBoundsException if either of the arguments
     *   are out of range.
     */
    public float getProjection(int ndx, int dimension);
    
    public void normalize();
    
}
