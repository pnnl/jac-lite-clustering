package gov.pnnl.jac.geom.dmap;

import gov.pnnl.jac.geom.ValuesGrid2D;

/**
 * DensityMap2DValuesGrid wraps a DensityMap2D instance
 * to make it appear to be a ValuesGrid2D.
 * 
 * @author d3j923
 *
 */
public class DensityMap2DValuesGrid implements ValuesGrid2D {

    private DensityMap2D mDM;
    private int mCoarsening;
    private double mRowHeight, mColWidth;

    public DensityMap2DValuesGrid(DensityMap2D dm, double rowHeight,
            double colWidth, int coarsening) {
        if (dm == null) {
            throw new NullPointerException();
        }
        mDM = dm;
        mRowHeight = rowHeight;
        mColWidth = colWidth;
        mCoarsening = coarsening;
    }

    public int getColumns() {
        return mDM.getGridLengthX() / mCoarsening;
    }

    public int getRows() {
        return mDM.getGridLengthY() / mCoarsening;
    }

    public double getValue(int row, int column) {
        if (mCoarsening == 1) {
            return (double) mDM.getDensity(column, row);
        }
        int rowStart = row * mCoarsening;
        int rowEnd = rowStart + mCoarsening;
        int colStart = column * mCoarsening;
        int colEnd = colStart + mCoarsening;
        double sum = 0;
        for (int r = rowStart; r < rowEnd; r++) {
            for (int c = colStart; c < colEnd; c++) {
                sum += mDM.getDensity(c, r);
            }
        }
        return sum / (mCoarsening * mCoarsening);
    }

    public double getRowHeight() {
        return mRowHeight;
    }

    public double getColumnWidth() {
        return mColWidth;
    }

	public void setColumnWidth(double width) {
		mColWidth = width;
	}

	public void setRowHeight(double height) {
		mRowHeight = height;
	}

	/**
	 * Setting the value is unsupported for this class.
	 * 
	 * @throws UnSupportedOperationException
	 */
	public void setValue(int row, int column, double value) {
		throw new UnsupportedOperationException();
	}
}
