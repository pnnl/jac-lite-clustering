package gov.pnnl.jac.geom;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;

/**
 * <p><tt>RealMatrixCoordinateList</tt> is a wrapper class
 * with the purpose of making a <tt>RealMatrix</tt>
 * instance into a <tt>CoordinateList</tt>.</p>
 * 
 * @author R. Scarberry
 *
 */
public class RealMatrixCoordinateList extends AbstractCoordinateList {

	// The matrix containing the data.
	private RealMatrix mMatrix;

	/**
	 * Constructor.
	 * @param matrix - the wrapped matrix containing the coordate data.
	 */
	public RealMatrixCoordinateList(RealMatrix matrix) {
		this.mCount = matrix.getRowDimension();
		this.mDim = matrix.getColumnDimension();
		this.mMatrix = matrix;
	}
	
	/**
	 * Constructor
	 * @param data from which a backing DoubleMatrix2D is populated.
	 */
	public RealMatrixCoordinateList(double[][] data) {
		this (new Array2DRowRealMatrix(data));
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
		return mMatrix.getEntry(ndx, dim);
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
		checkIndex(ndx);
		final int dim = this.mDim;
		double[] c = null;
		if (coords != null) {
			checkDimensions(coords.length);
			c = coords;
		} else {
			c = new double[dim];
		}
		for (int i = 0; i < dim; i++) {
			c[i] = mMatrix.getEntry(ndx, i);
		}
		return c;
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
		mMatrix.setEntry(ndx, dim, coord);
	}

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
	public void setCoordinates(int ndx, double[] coords) {
		checkIndex(ndx);
		checkDimensions(coords.length);
		final int dim = this.mDim;
		for (int i = 0; i < dim; i++) {
			mMatrix.setEntry(ndx, i, coords[i]);
		}
	}
}
