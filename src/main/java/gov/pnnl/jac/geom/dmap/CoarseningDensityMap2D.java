package gov.pnnl.jac.geom.dmap;

import java.awt.geom.Point2D.Float;

/**
 * CoarseningDensityMap2D is an implementation of DensityMap2D
 * used to wrap another DensityMap2D to make it appear to have fewer
 * rows and columns.
 * 
 * @author d3j923
 *
 */
public class CoarseningDensityMap2D implements DensityMap2D {

    private DensityMap2D mInnerMap;

    private int mGridX, mGridY;

    private int mCoarseningLevel = 2;

    public CoarseningDensityMap2D(DensityMap2D innerMap, int coarseningLevel) {
        if (coarseningLevel <= 0) {
            throw new IllegalArgumentException("coarsening level <= 0: "
                    + coarseningLevel);
        }
        mCoarseningLevel = coarseningLevel;
        setInnerMap(innerMap);
    }

    public void setInnerMap(DensityMap2D innerMap) {
        if (innerMap == null) {
            throw new NullPointerException();
        }
        mInnerMap = innerMap;
        mGridX = mInnerMap.getGridLengthX() / mCoarseningLevel;
        mGridY = mInnerMap.getGridLengthY() / mCoarseningLevel;
    }

    public int getGridLengthX() {
        return mGridX;
    }

    public int getGridLengthY() {
        return mGridY;
    }

    public float getUnitWidth() {
        return mInnerMap.getUnitWidth() * mCoarseningLevel;
    }

    public float getUnitHeight() {
        return mInnerMap.getUnitHeight() * mCoarseningLevel;
    }

    public float getOriginX() {
        return mInnerMap.getOriginX();
    }

    public float getOriginY() {
        return mInnerMap.getOriginY();
    }

    public int getXIndex(Float point) {
        return mInnerMap.getXIndex(point) / mCoarseningLevel;
    }

    public int getYIndex(Float point) {
        return mInnerMap.getYIndex(point) / mCoarseningLevel;
    }

    public int getDensity(int x, int y) {
        int xstart = x * mCoarseningLevel;
        int xend = xstart + mCoarseningLevel;
        int ystart = y * mCoarseningLevel;
        int yend = ystart + mCoarseningLevel;
        int density = 0;
        for (int i = xstart; i < xend; i++) {
            for (int j = ystart; j < yend; j++) {
                density += mInnerMap.getDensity(i, j);
            }
        }
        return density;
    }

    public int getMinDensity() {
        int min = 0;
        if (mGridX > 0 && mGridY > 0) {
            min = Integer.MAX_VALUE;
            for (int x = 0; x < mGridX; x++) {
                for (int y = 0; y < mGridY; y++) {
                    int d = getDensity(x, y);
                    if (d < min) {
                        min = d;
                    }
                }
            }
        }
        return min;
    }

    public int getMaxDensity() {
        int max = 0;
        for (int x = 0; x < mGridX; x++) {
            for (int y = 0; y < mGridY; y++) {
                int d = getDensity(x, y);
                if (d > max) {
                    max = d;
                }
            }
        }
        return max;
    }

}
