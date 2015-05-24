package gov.pnnl.jac.geom;

import cern.colt.matrix.impl.SparseDoubleMatrix2D;
public class SparseValuesGrid2D implements ValuesGrid2D {
	
	private SparseDoubleMatrix2D mValues;
	private int mRows, mColumns;
	private double mColumnWidth, mRowHeight;

    public SparseValuesGrid2D(int rows, int columns, double rowHeight, double colWidth) {
    	mRows = rows;
    	mColumns = columns;
    	mValues = new SparseDoubleMatrix2D(rows, columns);
    	setRowHeight(rowHeight);
        setColumnWidth(colWidth);
    }

    public SparseValuesGrid2D(int rows, int columns) {
        this(rows, columns, 1.0, 1.0);
    }
    
	public double getColumnWidth() {
		return mColumnWidth;
	}

	public int getColumns() {
		return mColumns;
	}

	public double getRowHeight() {
		return mRowHeight;
	}

	public int getRows() {
		return mRows;
	}

	public double getValue(int row, int column) {
		return mValues.get(row, column);
	}

	public void setColumnWidth(double width) {
		// TODO Auto-generated method stub

	}

	public void setRowHeight(double height) {
		// TODO Auto-generated method stub

	}

	public void setValue(int row, int column, double value) {
		// TODO Auto-generated method stub

	}

}
