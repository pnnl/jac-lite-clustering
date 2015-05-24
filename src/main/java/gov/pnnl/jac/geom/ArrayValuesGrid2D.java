package gov.pnnl.jac.geom;


public class ArrayValuesGrid2D implements ValuesGrid2D {

    private double[][] mValues;
    private int mRows, mCols;
    private double mRowHeight, mColWidth;

    public ArrayValuesGrid2D(double[][] values, double rowHeight, double colWidth) {
        setValues(values);
        setRowHeight(rowHeight);
        setColumnWidth(colWidth);
    }

    public ArrayValuesGrid2D() {
        this(null, 1.0, 1.0);
    }

    public void setValues(double[][] values) {
        if (values != null) {
            int rows = values.length;
            int cols = 0;
            if (rows > 0) {
                cols = values[0].length;
                for (int i=1; i<rows; i++) {
                    if (values[i].length != cols) {
                        throw new IllegalArgumentException("grid not rectangular");
                    }
                }
            }
            mValues = new double[rows][];
            for (int i=0; i<rows; i++) {
                mValues[i] = new double[cols];
                System.arraycopy(values[i], 0, mValues[i], 0, cols);
            }
            mRows = rows;
            mCols = cols;
        } else {
            mValues = null;
            mRows = mCols = 0;
        }
    }

    public int getColumns() {
        return mCols;
    }

    public int getRows() {
        return mRows;
    }

    public double getValue(int row, int column) {
        return mValues[row][column];
    }

    public double getRowHeight() {
        return mRowHeight;
    }

    public void setRowHeight(double rowHeight) {
        if (rowHeight < 0) {
            throw new IllegalArgumentException("negative row height: " + rowHeight);
        }
        mRowHeight = rowHeight;
    }

    public double getColumnWidth() {
        return mColWidth;
    }

    public void setColumnWidth(double columnWidth) {
        if (columnWidth < 0) {
            throw new IllegalArgumentException("negative column width: " + columnWidth);
        }
        mColWidth = columnWidth;
    }

	public void setValue(int row, int column, double value) {
		mValues[row][column] = value;
	}
}
