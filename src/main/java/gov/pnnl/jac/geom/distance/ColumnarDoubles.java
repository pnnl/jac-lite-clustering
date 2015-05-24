package gov.pnnl.jac.geom.distance;

/**
 * <p>Defines an object containing fixed-length columns of doubles.</p>
 *
 * @author d3j923
 *
 */
public interface ColumnarDoubles {

    /**
     * Fetch the values in the specified column and place them
     * in the passed buffer if the buffer is non-null.  If the buffer
     * is null, create a new buffer to hold the values.
     * 
     * @param column
     * @param buffer - if non-null, used to hold the returned values.
     * @return - array containing the values.  Same as buffer if buffer
     *   is non-null, o/w a newly-created array.
     *   
     * @throws IndexOutOfBoundsException if column is out of range.
     * @throws IllegalArgumentException if buffer is non-null, but of incorrect length.
     */
    public double[] getColumnValues(int column, double[] buffer);
    
    /**
     * Returns the number of columns.
     * 
     * @return the number of columns.
     */
    public int getColumnCount();
    
    /**
     * Get the number of values or rows in each column.
     * 
     * @return the number of rows, which is the same for each column.
     */
    public int getRowCount();
    
}
