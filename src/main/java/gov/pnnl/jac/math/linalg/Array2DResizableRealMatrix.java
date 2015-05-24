package gov.pnnl.jac.math.linalg;

import gov.pnnl.jac.util.ExceptionUtil;

import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.NoDataException;
import org.apache.commons.math3.exception.NullArgumentException;
import org.apache.commons.math3.exception.util.LocalizedFormats;
import org.apache.commons.math3.linear.AbstractRealMatrix;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

public class Array2DResizableRealMatrix extends AbstractRealMatrix implements
		ResizableRealMatrix {

	// The data
	private double[][] data;
	
	public Array2DResizableRealMatrix() {}
	
	public Array2DResizableRealMatrix(final int rowDimension, final int columnDimension) {
		super(rowDimension, columnDimension);
		data = new double[rowDimension][columnDimension];
	}
	
	public Array2DResizableRealMatrix(double[][] data, boolean copyData) {
		if (copyData) {
			copyDataIn(data);
		} else {
            if (data == null) {
                throw new NullArgumentException();
            }
            final int rows = data.length;
            if (rows == 0) {
                throw new NoDataException(LocalizedFormats.AT_LEAST_ONE_ROW);
            }
            final int cols = data[0].length;
            if (cols == 0) {
                throw new NoDataException(LocalizedFormats.AT_LEAST_ONE_COLUMN);
            }
            for (int r = 1; r < rows; r++) {
                if (data[r].length != cols) {
                    throw new DimensionMismatchException(data[r].length, cols);
                }
            }
            this.data = data;
		}
	}
	
    /**
     * Create a new (column) RealMatrix using {@code v} as the
     * data for the unique column of the created matrix.
     * The input array is copied.
     *
     * @param v Column vector holding data for new matrix.
     */
    public Array2DResizableRealMatrix(final double[] v) {
        final int rows = v.length;
        data = new double[rows][1];
        for (int row = 0; row < rows; row++) {
            data[row][0] = v[row];
        }
    }

    @Override
	public void insertRow(int row) {
		final int rows = getRowDimension();
		final int cols = getColumnDimension();
		ExceptionUtil.checkInBounds(row, 0, rows);
		double[][] newData = new double[rows + 1][];
		if (row > 0) {
			System.arraycopy(data, 0, newData, 0, row);
		}
		if (row < data.length) {
			System.arraycopy(data, row, newData, row+1, rows - row);
		}
		newData[row] = new double[cols];
		this.data = newData;
	}

	@Override
	public void removeRow(int row) {
		final int rows = getRowDimension();
		ExceptionUtil.checkInBounds(row, 0, rows-1);
		double[][] newData = new double[rows-1][];
		if (row > 0) {
			System.arraycopy(data, 0, newData, 0, row);
		}
		if (row < data.length - 1) {
			System.arraycopy(data, row+1, newData, row, rows - row - 1);
		}
		this.data = newData;
	}

	@Override
	public void insertColumn(int column) {
		final int rows = getRowDimension();
		final int cols = getColumnDimension();
		ExceptionUtil.checkInBounds(column, 0, cols);
		final int colsAbove = cols - column;
		for (int i=0; i<rows; i++) {
			double[] oldRow = data[i];
			double[] newRow = new double[cols+1];
			if (column > 0) {
				System.arraycopy(oldRow, 0, newRow, 0, column);
			}
			if (colsAbove > 0) {
				System.arraycopy(oldRow, column, newRow, column+1, colsAbove);
			}
			data[i] = newRow;
		}
	}

	@Override
	public void removeColumn(int column) {
		final int rows = getRowDimension();
		final int cols = getColumnDimension();
		ExceptionUtil.checkInBounds(column, 0, cols-1);
		final int colsAbove = cols - column - 1;
		for (int i=0; i<rows; i++) {
			double[] oldRow = data[i];
			double[] newRow = new double[cols-1];
			if (column > 0) {
				System.arraycopy(oldRow, 0, newRow, 0, column);
			}
			if (colsAbove > 0) {
				System.arraycopy(oldRow, column+1, newRow, column, colsAbove);
			}
			data[i] = newRow;
		}
	}

	@Override
	public RealMatrix createMatrix(int rowDimension, int columnDimension) {
		return new Array2DResizableRealMatrix(rowDimension, columnDimension);
	}

	@Override
	public RealMatrix copy() {
		return new Array2DResizableRealMatrix(cloneData(), false);
	}

	@Override
	public double getEntry(int row, int column) {
        MatrixUtils.checkMatrixIndex(this, row, column);
        return data[row][column];
	}

	@Override
	public void setEntry(int row, int column, double value) {
        MatrixUtils.checkMatrixIndex(this, row, column);
        data[row][column] = value;
	}

	@Override
	public int getRowDimension() {
		return data != null ? data.length : 0;
	}

	@Override
	public int getColumnDimension() {
		return data != null && data.length > 0 ? data[0].length : 0;
	}

	private double[][] cloneData() {
		final int rows = getRowDimension();
		final int cols = getColumnDimension();
		double[][] copy = new double[rows][cols];
		for (int i=0; i<rows; i++) {
			System.arraycopy(data[i], 0, copy[i], 0, cols);
		}
		return copy;
	}
	
	private void copyDataIn(double[][] dataIn) {
		final int rows = dataIn.length;
	    if (rows == 0) {
	        throw new NoDataException(LocalizedFormats.AT_LEAST_ONE_ROW);
	    }
		final int cols = rows > 0 ? dataIn[0].length : 0;
	    if (cols == 0) {
	        throw new NoDataException(LocalizedFormats.AT_LEAST_ONE_COLUMN);
	    }
	    for (int i=1; i<rows; i++) {
	        if (dataIn[i].length != cols) {
	            throw new DimensionMismatchException(dataIn[i].length, cols);
	        }
	    }
		this.data = new double[rows][cols];
		for (int i=0; i<rows; i++) {
			System.arraycopy(dataIn[i], 0, this.data[i], 0, cols);
		}
	}
}
