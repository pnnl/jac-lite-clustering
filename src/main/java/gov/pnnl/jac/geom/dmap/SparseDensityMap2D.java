package gov.pnnl.jac.geom.dmap;

import gov.pnnl.jac.projection.ProjectionData;

import java.awt.geom.Point2D;

import cern.colt.list.IntArrayList;
import cern.colt.map.*;

public class SparseDensityMap2D implements DensityMap2D {

    private int mGridX, mGridY;
    private float mXMin, mXMax, mYMin, mYMax, mUnitWidth, mUnitHeight;
    private OpenIntObjectHashMap mDensityMaps;
    
    public SparseDensityMap2D(int gridX, int gridY, 
            float xmin, float xmax, float ymin, float ymax) {
        if (gridX <= 0) {
            throw new IllegalArgumentException("gridX <= 0: " + gridX);
        }
        if (gridY <= 0) {
            throw new IllegalArgumentException("gridY <= 0: " + gridY);
        }
        mGridX = gridX;
        mGridY = gridY;
        mXMin = Math.min(xmin, xmax);
        mXMax = Math.max(xmin, xmax);
        if (mGridX > 0) {
            mUnitWidth = (mXMax - mXMin)/mGridX;
        }
        mYMin = Math.min(ymin, ymax);
        mYMax = Math.max(ymin, ymax);
        if (mGridY > 0) {
            mUnitHeight = (mYMax - mYMin)/mGridY;
        }
        // There will be a separate OpenIntIntHashMap for each column (x-value).
        // The keys of this map are x-values [0 - (mGridX - 1)].  The values
        // are OpenIntIntHashMaps.  
        mDensityMaps = new OpenIntObjectHashMap();
    }
    
    public SparseDensityMap2D(int gridX, int gridY) {
        this(gridX, gridY, 0f, 1f, 0f, 1f);
    }
    
    public int getGridLengthX() {
        return mGridX;
    }

    public int getGridLengthY() {
        return mGridY;
    }

    public float getUnitWidth() {
        return mUnitWidth;
    }

    public float getUnitHeight() {
        return mUnitHeight;
    }

    public float getOriginX() {
        return mXMin;
    }

    public float getOriginY() {
        return mYMin;
    }

    /**
     * Translate the x component of the given point into
     * an integer in the range <tt>[0 - (getGridLengthX() - 1)]</tt>.
     * @param point a point whose x component should be in the
     *   range <tt>[getOriginX() - (getGridLengthX() * getUnitWidth()]</tt>. 
     *   If the point's x component is not in the proper range,
     *   -1 is returned.
     * @return integer x-index.
     */
    public int getXIndex(Point2D.Float point) {
        int x = -1;
        if (point.x >= mXMin && point.x <= mXMax) {
            x = (int) ((point.x - mXMin)/mUnitWidth);
            if (x == mGridX) {
                // x must be in range [0 - (mGridX - 1)]
                x--;
            }
        }
        return x;
    }

    /**
     * Translate the y component of the given point into
     * an integer in the range <tt>[0 - (getGridLengthY() - 1)]</tt>.
     * @param point a point whose y component should be in the
     *   range <tt>[getOriginY() - (getGridLengthY() * getUnitHeight()]</tt>. 
     *   If the point's y component is not in the proper range, -1 is returned.
     * @return integer y-index.
     */
    public int getYIndex(Point2D.Float point) {
        int y = -1;
        if (point.y >= mYMin && point.y <= mYMax) {
            y = (int) ((point.y - mYMin)/mUnitHeight);
            if (y == mGridY) {
                // y must be in range [0 - (mGridY - 1)]
                y--;
            }
        }
        return y;
    }

    public int getDensity(int x, int y) {
        OpenIntIntHashMap map = (OpenIntIntHashMap) mDensityMaps.get(x);
        int density = 0;
        if (map != null) {
            density = map.get(y);
        }
        return density;
    }
    
    public int getMinDensity() {
        int min = Integer.MAX_VALUE;
        for (int x=0; x<mGridX; x++) {
            OpenIntIntHashMap map = (OpenIntIntHashMap) mDensityMaps.get(x);
            if (map == null) {
                // All densities for this value of x are 0.
                return 0;
            } else {
                if (map.size() < mGridY) {
                    // Some y-value doesn't have an entry, because 
                    // the density there is 0.
                    return 0;
                }
                IntArrayList keys = map.keys();
                int sz = keys.size();
                for (int i=0; i<sz; i++) {
                    int density = map.get(keys.getQuick(i));
                    if (density < min) {
                        min = density;
                    }
                }
            }
        }
        // Can only fall through to here if the densities for
        // all values of x and y are > 0.  This is VERY unlikely.
        return min;
    }
    
    public int getMaxDensity() {
        int max = 0;
        for (int x=0; x<mGridX; x++) {
            OpenIntIntHashMap map = (OpenIntIntHashMap) mDensityMaps.get(x);
            if (map != null) {
                IntArrayList keys = map.keys();
                int sz = keys.size();
                for (int i=0; i<sz; i++) {
                    int density = map.get(keys.getQuick(i));
                    if (density > max) {
                        max = density;
                    }
                }
            }
        }
        return max;
    }
    
    public void setDensity(int x, int y, int density) {
        if (density < 0) {
            throw new IllegalArgumentException("density cannot be negative: " + density);
        }
        OpenIntIntHashMap map = (OpenIntIntHashMap) mDensityMaps.get(x);
        if (density == 0) {
            if (map != null && map.containsKey(y)) {
                map.removeKey(y);
                if (map.size() == 0) {
                    mDensityMaps.removeKey(x);
                }
            }
        } else { // density > 0
            if (map == null) {
                map = new OpenIntIntHashMap();
                mDensityMaps.put(x, map);
            }
            map.put(y, density);
        }
    }
    
    public void incrementDensity(int x, int y) {
        int density = 0;
        OpenIntIntHashMap map = (OpenIntIntHashMap) mDensityMaps.get(x);
        if (map == null) {
            map = new OpenIntIntHashMap();
            mDensityMaps.put(x, map);
        } else {
            density = map.get(y);
        }
        map.put(y, ++density);
    }
 
    public static DensityMap2D generate(ProjectionData pd, int gridx, int gridy) {
        int pdim = pd.getDimensionCount();
        if (pdim >= 2) {
            float xmin = pd.getMinAllowed(0);
            float xmax = pd.getMaxAllowed(0);
            float ymin = pd.getMinAllowed(1);
            float ymax = pd.getMaxAllowed(1);
            SparseDensityMap2D dm = new SparseDensityMap2D(gridx, gridy, xmin, xmax, ymin, ymax);
            int projectionCount = pd.getProjectionCount();
            float[] projectionBuffer = new float[pdim];
            Point2D.Float point = new Point2D.Float();
            for (int i=0; i<projectionCount; i++) {
                pd.getProjection(i, projectionBuffer);
                point.x = projectionBuffer[0];
                point.y = projectionBuffer[1];
                dm.incrementDensity(dm.getXIndex(point), dm.getYIndex(point));
            }
            return dm;
        } else {
            throw new IllegalArgumentException("ProjectionData has only " + 
                    pd.getDimensionCount() + " dimensions.");
        }
    }
}
