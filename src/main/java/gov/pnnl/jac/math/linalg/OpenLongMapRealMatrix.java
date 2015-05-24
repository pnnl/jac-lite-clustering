package gov.pnnl.jac.math.linalg;

import gov.pnnl.jac.collections.LongCollectionIterator;
import gov.pnnl.jac.collections.LongDoubleHashMap;
import gov.pnnl.jac.collections.LongDoubleMap;
import gov.pnnl.jac.math.linalg.ResizableRealMatrix;
import gov.pnnl.jac.util.DataConverter;
import gov.pnnl.jac.util.ExceptionUtil;

import java.io.Serializable;

import org.apache.commons.math3.linear.AbstractRealMatrix;
import org.apache.commons.math3.linear.BlockRealMatrix;
import org.apache.commons.math3.linear.MatrixDimensionMismatchException;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SparseRealMatrix;

public class OpenLongMapRealMatrix extends AbstractRealMatrix 
	implements ResizableRealMatrix, SparseRealMatrix, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1923305097332922771L;
	
	/** Number of rows */
	private int rows;
	/** Number of columns */
	private int columns;
	/** Storage for the non-zero matrix elements. */
	private LongDoubleMap nonZeroEntries;
	
	public OpenLongMapRealMatrix(int rows, int columns) {
		super(rows, columns);
		this.rows = rows;
		this.columns = columns;
		this.nonZeroEntries = new LongDoubleHashMap();
		// So it'll return 0s instead of NaNs for the missing value.
		this.nonZeroEntries.setMissingValue(0.0);
	}
	
	public OpenLongMapRealMatrix(OpenLongMapRealMatrix matrix) {
		this(matrix.rows, matrix.columns);
		this.nonZeroEntries.putAll(matrix.nonZeroEntries);
	}
	
	/**
	 * Inserts a new row at the specified row index. Values in existing rows equal to or
	 * greater than the insertion point are shifted up. All values for the new row are
	 * zero.
	 * 
	 * @param row
	 */
	public void insertRow(int row) {
		
		ExceptionUtil.checkInBounds(row, 0, rows);
		
		if (row < rows) {
		
			LongDoubleMap newMap = new LongDoubleHashMap(nonZeroEntries.size() * 2);
			newMap.setMissingValue(0.0);
			
			LongCollectionIterator it = nonZeroEntries.keyIterator();
			
			while(it.hasNext()) {
				long key = it.next();
				double value = nonZeroEntries.get(key);
				int[] rowCol = DataConverter.intsFromLong(key);
				if (rowCol[0] >= row) {
					// Increment the row and update the key.
					rowCol[0]++;
					key = DataConverter.longFromInts(rowCol[0], rowCol[1]);
				}
				newMap.put(key, value);
			}
			
			this.nonZeroEntries = newMap;
			
		}
		
		rows++;
	}
	
	/**
	 * Removes an entire row from the matrix, shifting values in all higher rows down
	 * by one row.
	 * 
	 * @param row
	 */
	public void removeRow(int row) {
		
		ExceptionUtil.checkInBounds(row, 0, rows-1);
	
		LongDoubleMap newMap = new LongDoubleHashMap(nonZeroEntries.size() * 2);
		newMap.setMissingValue(0.0);
		
		LongCollectionIterator it = nonZeroEntries.keyIterator();
		
		while(it.hasNext()) {
			long key = it.next();
			double value = nonZeroEntries.get(key);
			int[] rowCol = DataConverter.intsFromLong(key);
			// Ignore the matching row, since it's being deleted.
			if (rowCol[0] == row) continue;
			if (rowCol[0] > row) {
				// Decrement the row and update the key.
				rowCol[0]--;
				key = DataConverter.longFromInts(rowCol[0], rowCol[1]);
			}
			newMap.put(key, value);
		}
		
		this.nonZeroEntries = newMap;
	
		rows--;
	}
	
	/**
	 * Inserts a new column at the specified column index. Values in existing columns equal to or
	 * greater than the insertion point are shifted up. All values for the new column are
	 * zero.
	 * 
	 * @param col
	 */
	public void insertColumn(int col) {

		ExceptionUtil.checkInBounds(col, 0, columns);
		
		if (col < columns) {
		
			LongDoubleMap newMap = new LongDoubleHashMap(nonZeroEntries.size() * 2);
			newMap.setMissingValue(0.0);
			
			LongCollectionIterator it = nonZeroEntries.keyIterator();
			
			while(it.hasNext()) {
				long key = it.next();
				double value = nonZeroEntries.get(key);
				int[] rowCol = DataConverter.intsFromLong(key);
				if (rowCol[1] >= col) {
					// Increment the column and update the key.
					rowCol[1]++;
					key = DataConverter.longFromInts(rowCol[0], rowCol[1]);
				}
				newMap.put(key, value);
			}
			
			this.nonZeroEntries = newMap;
			
		}
		
		columns++;
	}
	
	/**
	 * Removes an entire column from the matrix, shifting values in all higher columns down
	 * by one column.
	 * 
	 * @param col
	 */
	public void removeColumn(int col) {
		
		ExceptionUtil.checkInBounds(col, 0, columns-1);
	
		LongDoubleMap newMap = new LongDoubleHashMap(nonZeroEntries.size() * 2);
		newMap.setMissingValue(0.0);
		
		LongCollectionIterator it = nonZeroEntries.keyIterator();
		
		while(it.hasNext()) {
			long key = it.next();
			double value = nonZeroEntries.get(key);
			int[] rowCol = DataConverter.intsFromLong(key);
			// Ignore the matching column, since it's being deleted.
			if (rowCol[1] == col) continue;
			if (rowCol[1] > col) {
				// Decrement the column and update the key.
				rowCol[1]--;
				key = DataConverter.longFromInts(rowCol[0], rowCol[1]);
			}
			newMap.put(key, value);
		}
		
		this.nonZeroEntries = newMap;
	
		columns--;
	}

	@Override
	public RealMatrix createMatrix(int rowDimension, int columnDimension) {
		return new OpenLongMapRealMatrix(rowDimension, columnDimension);
	}

	@Override
	public RealMatrix copy() {
		return new OpenLongMapRealMatrix(this);
	}

	@Override
	public double getEntry(int row, int column) {
        MatrixUtils.checkRowIndex(this, row);
        MatrixUtils.checkColumnIndex(this, column);
		long key = DataConverter.longFromInts(row, column);
		return nonZeroEntries.get(key);
	}

	@Override
	public void setEntry(int row, int column, double value) {
        MatrixUtils.checkRowIndex(this, row);
        MatrixUtils.checkColumnIndex(this, column);
		long key = DataConverter.longFromInts(row, column);
		if (value == 0.0) {
			nonZeroEntries.remove(key);
		} else {
			nonZeroEntries.put(key, value);
		}
	}

	@Override
	public int getRowDimension() {
		return rows;
	}

	@Override
	public int getColumnDimension() {
		return columns;
	}

    /**
     * Compute the sum of this matrix and {@code m}.
     *
     * @param m Matrix to be added.
     * @return {@code this} + {@code m}.
     * @throws org.apache.commons.math3.exception.DimensionMismatchException
     * if {@code m} is not the same size as this matrix.
     */
    public OpenLongMapRealMatrix add(OpenLongMapRealMatrix m) {

        // safety check
        MatrixUtils.checkAdditionCompatible(this, m);

        final OpenLongMapRealMatrix out = new OpenLongMapRealMatrix(this.rows, this.columns);
        
        LongCollectionIterator it = m.nonZeroEntries.keyIterator();

        while(it.hasNext()) {
        	long key = it.next();
        	int[] rowCol = DataConverter.intsFromLong(key);
        	int row = rowCol[0];
        	int col = rowCol[1];
        	out.setEntry(row, col, this.getEntry(row, col) + m.nonZeroEntries.get(key));
        }

        return out;

    }
    
    /** {@inheritDoc} */
    @Override
    public OpenLongMapRealMatrix subtract(final RealMatrix m) {
        if (m instanceof OpenLongMapRealMatrix) {
            return subtract((OpenLongMapRealMatrix) m);
        }    
        return (OpenLongMapRealMatrix) super.subtract(m);
    }

    /**
     * Subtract {@code m} from this matrix.
     *
     * @param m Matrix to be subtracted.
     * @return {@code this} - {@code m}.
     * @throws org.apache.commons.math3.exception.DimensionMismatchException
     * if {@code m} is not the same size as this matrix.
     */
    public OpenLongMapRealMatrix subtract(OpenLongMapRealMatrix m) {
        // Safety check.
        MatrixUtils.checkAdditionCompatible(this, m);

        final OpenLongMapRealMatrix out = new OpenLongMapRealMatrix(this.rows, this.columns);
        
        LongCollectionIterator it = m.nonZeroEntries.keyIterator();

        while(it.hasNext()) {
        	long key = it.next();
        	int[] rowCol = DataConverter.intsFromLong(key);
        	int row = rowCol[0];
        	int col = rowCol[1];
        	out.setEntry(row, col, this.getEntry(row, col) - m.nonZeroEntries.get(key));
        }

        return out;

    }
    
    /**
     * Returns an iterator over all keys associated with non-zero entries. Each key
     * can be converted to the row and column using <code>DataConverter.intsFromLong(long key)</code>.
     * @return
     */
    public LongCollectionIterator nonZeroKeyIterator() {
    	return nonZeroEntries.keyIterator();
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix multiply(final RealMatrix m) {
        
    	if (m instanceof OpenLongMapRealMatrix) {
            return multiply((OpenLongMapRealMatrix) m);
        }

        // safety check
        MatrixUtils.checkMultiplicationCompatible(this, m);

        final int outCols = m.getColumnDimension();
        final BlockRealMatrix out = new BlockRealMatrix(rows, outCols);
        
        LongCollectionIterator it = nonZeroEntries.keyIterator();
        while(it.hasNext()) {
        	long key = it.next();
        	double value = nonZeroEntries.get(key);
        	int[] rowCol = DataConverter.intsFromLong(key);
        	int row = rowCol[0];
        	int col = rowCol[1];
        	for (int i=0; i<outCols; i++) {
        		out.addToEntry(row, col, value * m.getEntry(col, i));
        	}
        }
        
        return out;
    }

    /**
     * Postmultiply this matrix by {@code m}.
     *
     * @param m Matrix to postmultiply by.
     * @return {@code this} * {@code m}.
     * @throws MatrixDimensionMismatchException
     * if the number of rows of {@code m} differ from the number of columns
     * of this matrix.
     */
    public OpenLongMapRealMatrix multiply(OpenLongMapRealMatrix m) {
        // Safety check.
        MatrixUtils.checkMultiplicationCompatible(this, m);

        final int outCols = m.getColumnDimension();
        OpenLongMapRealMatrix out = new OpenLongMapRealMatrix(rows, outCols);
        
        LongCollectionIterator it = nonZeroEntries.keyIterator();
        while(it.hasNext())	{
        	final long key = it.next();
        	final double value = nonZeroEntries.get(key);
        	final int[] rowCol = DataConverter.intsFromLong(key);
        	final int row = rowCol[0];
        	int col = rowCol[1];
        	for (int j=0; j<outCols; j++) {
        		long rightKey = DataConverter.longFromInts(col, j);
        		if (m.nonZeroEntries.containsKey(rightKey)) {
        			long outKey = DataConverter.longFromInts(row, j);
        			double outValue = out.nonZeroEntries.get(outKey) + value + m.nonZeroEntries.get(rightKey);
        			if (outValue == 0.0) {
        				out.nonZeroEntries.remove(outKey);
        			} else {
        				out.nonZeroEntries.put(outKey, outValue);
        			}
        		}
        	}
        }

        return out;
    }
    
    /** {@inheritDoc} */
    @Override
    public void addToEntry(int row, int column, double increment) {
        MatrixUtils.checkRowIndex(this, row);
        MatrixUtils.checkColumnIndex(this, column);
        final long key = DataConverter.longFromInts(row, column);
        final double value = nonZeroEntries.get(key) + increment;
        if (value == 0.0) {
            nonZeroEntries.remove(key);
        } else {
            nonZeroEntries.put(key, value);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void multiplyEntry(int row, int column, double factor) {
        MatrixUtils.checkRowIndex(this, row);
        MatrixUtils.checkColumnIndex(this, column);
        final long key = DataConverter.longFromInts(row, column);
        final double value = nonZeroEntries.get(key) * factor;
        if (value == 0.0) {
            nonZeroEntries.remove(key);
        } else {
            nonZeroEntries.put(key, value);
        }
    }


}
