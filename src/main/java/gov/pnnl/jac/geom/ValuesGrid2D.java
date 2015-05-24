package gov.pnnl.jac.geom;

/**
 * ValuesGrid2D in an interface which represents a
 * rectangular grid of double values contained in
 * cells with the same width and the same height.
 * 
 * @author d3j923
 *
 */
public interface ValuesGrid2D {

    /**
     * Get the number of columns (the width) of the grid.
     * @return
     */
    public int getColumns();

    /**
     * Get the number of rows (the height) of the grid.
     * @return
     */
    public int getRows();

    /**
     * Get the value in the specified cell.
     * @param row
     * @param column
     * @return
     */
    public double getValue(int row, int column);
    
    /**
     * Sets the value at the specified row and column.
     * 
     * @param row
     * @param column
     * @param value
     */
    public void setValue(int row, int column, double value);

    /**
     * Get the height of the rows.
     * @return
     */
    public double getRowHeight();
    
    /**
     * Set the row height.
     * 
     * @param height
     */
    public void setRowHeight(double height);

    /**
     * Get the width of the columns.
     * @return
     */
    public double getColumnWidth();

    /**
     * Set the column width.
     * 
     * @param width
     */
    public void setColumnWidth(double width);
}
