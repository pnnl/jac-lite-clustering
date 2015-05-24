package gov.pnnl.jac.collections;

import gov.pnnl.jac.util.DataConverter;
import cern.colt.map.AbstractIntObjectMap;
import cern.colt.map.OpenIntObjectHashMap;

/**
 * <p>ZGrid is a data structure for associating object with
 * x, y coordinates in a grid. For both the x and y dimensions,
 * the constructor takes the range of values (xMin, xMax, yMin, yMax)
 * and the grid width and height (width, height).  Object values may
 * be associated with each grid location using either x, y coordinates
 * expressed as doubles in the ranges <tt>([xMin - xMax], [yMin, yMax])</tt> or as
 * integers in the ranges <tt>([0 - width-1], [0 - height-1])</tt>.</p>
 * 
 * @author D3J923
 *
 */
public class ZGrid {

	protected int mWidth, mHeight;
	protected double mXMin, mXMax, mYMin, mYMax;
	
	protected double mCellWidth, mCellHeight;
	
	private AbstractIntObjectMap mGridMap = new OpenIntObjectHashMap();
	
	/**
	 * Constructor.  The first two parameters, <tt>xMin</tt> and <tt>xMax</tt>,
	 * defines the range in the x-dimension.  They cannot be equal.  The next
	 * two parameters similarly define the range of values for the y-dimension.
	 * The width and height parameters define the the number of units in the x and
	 * y dimensions, respectively of the grid.  These values must be positive and
	 * less than or equal to <tt>32,767</tt>.
	 * 
	 * @param xMin the lower value for the x dimension.
	 * @param xMax the upper value for the x dimension.
	 * @param yMin the lower value for the y dimension.
	 * @param yMax the upper value for the y dimension.
	 * @param width the number of units for the grid in the x dimension.
	 * @param height the number of units for the grid in the y dimension.
	 * 
	 * @throws IllegalArgumentException if any arguments fail to satisfy the constraints 
	 *   mentioned above.
	 */
	public ZGrid(double xMin, double xMax, 
			double yMin, double yMax, int width, int height) {
		
		if (width <= 0 || height <= 0 || 
				width > Short.MAX_VALUE || 
				height > Short.MAX_VALUE) {
			throw new IllegalArgumentException (
					"width and height must both be in range [1 - " + 
					Short.MAX_VALUE + "]");
		} 
		
		mXMin = Math.min(xMin, xMax);
		mXMax = Math.max(xMin, xMax);
		mYMin = Math.min(yMin, yMax);
		mYMax = Math.max(yMin, yMax);
		
		mWidth = width;
		mHeight = height;
		
		mCellWidth = (mXMax - mXMin)/mWidth;
		mCellHeight = (mYMax - mYMin)/mHeight;
		
		if (Double.isNaN(mCellWidth) || mCellWidth == 0 || 
			Double.isNaN(mCellHeight) || mCellHeight == 0) {
			throw new IllegalArgumentException("invalid xMin, xMax, yMin, or yMax: (" +
					xMin + ", " + xMax + ", " + yMin + ", " + yMax + ")");
		}
	}
	
	public int getGridWidth() {
		return mWidth;
	}
	
	public int getGridHeight() {
		return mHeight;
	}
	
	public double getXMin() {
		return mXMin;
	}
	
	public double getXMax() {
		return mXMax;
	}
	
	public double getYMin() {
		return mYMin;
	}
	
	public double getYMax() {
		return mYMax;
	}
	
	/**
	 * Returns the width of each cell in the grid.
	 * @return
	 */
	public double getCellWidth() {
		return mCellWidth;
	}
	
	/**
	 * Returns the height of each cell in the grid.
	 * @return
	 */
	public double getCellHeight() {
		return mCellHeight;
	}
	
	/**
	 * Compute the grid x-coordinate from a double x value.
	 * @param x
	 * @return
	 */
	public int xGridCoordinate(double x) {
		if (x < mXMin || x > mXMax) {
			throw new IllegalArgumentException("x (" + x + ") not in [" + mXMin + " - " + mXMax + "]");
		}
		return (int) ((x - mXMin)/mCellWidth);
	}
	
	public int yGridCoordinate(double y) {
		if (y < mYMin || y > mYMax) {
			throw new IllegalArgumentException("y (" + y + ") not in [" + mYMin + " - " + mYMax + "]");
		}
		return (int) ((y - mYMin)/mCellHeight);
	}

	public void put(double xCoord, double yCoord, Object o) {
		int xy = DataConverter.intFromShorts(
				(short)xGridCoordinate(xCoord), (short)yGridCoordinate(yCoord));
		mGridMap.put(xy, o);
	}
	
	public Object get(double xCoord, double yCoord) {
		int xy = DataConverter.intFromShorts(
				(short)xGridCoordinate(xCoord), (short)yGridCoordinate(yCoord));
		return mGridMap.get(xy);
	}
	
	public Object remove(double xCoord, double yCoord) {
		int xy = DataConverter.intFromShorts(
				(short)xGridCoordinate(xCoord), (short)yGridCoordinate(yCoord));
		Object o = null;
		if (mGridMap.containsKey(xy)) {
			o = mGridMap.get(xy);
			mGridMap.removeKey(xy);
		}
		return o;
	}
	
	public boolean containsValueFor(double xCoord, double yCoord) {
		int xy = DataConverter.intFromShorts(
				(short)xGridCoordinate(xCoord), (short)yGridCoordinate(yCoord));
		return mGridMap.containsKey(xy);
	}

	private void checkCoordinates(int x, int y) {
		if (x < 0 || x > Short.MAX_VALUE || y < 0 || y > Short.MAX_VALUE) {
			throw new IllegalArgumentException("invalid x,y coordinates (" + 
					x + ", " + y + "): must be in [0 - " + Short.MAX_VALUE + "]");
		}
	}
	
	public void put(int x, int y, Object o) {
		checkCoordinates(x, y);
		int xy = DataConverter.intFromShorts((short) x, (short) y);
		mGridMap.put(xy, o);
	}
	
	public Object get(int x, int y) {
		checkCoordinates(x, y);
		int xy = DataConverter.intFromShorts((short) x, (short) y);
		return mGridMap.get(xy);
	}
	
	public Object remove(int x, int y) {
		checkCoordinates(x, y);
		int xy = DataConverter.intFromShorts((short) x, (short) y);
		Object o = null;
		if (mGridMap.containsKey(xy)) {
			o = mGridMap.get(xy);
			mGridMap.removeKey(xy);
		}
		return o;
	}
	
	public boolean containsValueFor(int x, int y) {
		checkCoordinates(x, y);
		int xy = DataConverter.intFromShorts((short) x, (short) y);
		return mGridMap.containsKey(xy);
	}
	
	public void clear() {
		mGridMap.clear();
	}
}
