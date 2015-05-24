package gov.pnnl.jac.projection;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Arrays;

public class SimpleProjectionData implements ProjectionData, Externalizable {
    
    private static final long MAX_BYTES_PER_ROW = 67108864L; // 64MB
    private float[][] mData;
    private transient int mPointsPerRow;
    private float[] mMinAllowed, mMaxAllowed;
    private int mProjectionCount;
    private int mDimensionCount;

    public SimpleProjectionData(int projectionCount, int dimensionCount, 
            float[] minAllowed, float[] maxAllowed) {
        if (projectionCount < 0) {
            throw new IllegalArgumentException("negative projectionCount: " + projectionCount);
        }
        if (dimensionCount <= 0) {
            throw new IllegalArgumentException("dimensionCount not positive: " + dimensionCount);
        }
        mProjectionCount = projectionCount;
        mDimensionCount = dimensionCount;
        if (minAllowed != null) {
            checkProjectionBuffer(minAllowed);
            mMinAllowed = (float[]) minAllowed.clone();
        } else {
            mMinAllowed = new float[mDimensionCount];
            Arrays.fill(mMinAllowed, -Float.MAX_VALUE);
        }
        if (maxAllowed != null) {
            checkProjectionBuffer(maxAllowed);
            mMaxAllowed = (float[]) maxAllowed.clone();
        } else {
            mMaxAllowed = new float[mDimensionCount];
            Arrays.fill(mMaxAllowed, Float.MAX_VALUE);
        }
        allocateData();
    }
    
    public SimpleProjectionData(int projectionCount, int dimensionCount) {
        this(projectionCount, dimensionCount, null, null);
    }
    
    private void allocateData() {
        mPointsPerRow = Math.min((int) (MAX_BYTES_PER_ROW/(4L*mDimensionCount)), mProjectionCount);
        int rows = mProjectionCount/mPointsPerRow;
        if (mProjectionCount%mPointsPerRow > 0) {
            rows++;
        }
        mData = new float[rows][mPointsPerRow*mDimensionCount];
    }
    
    private void checkIndex(int ndx) {
        if (ndx < 0 || ndx >= mProjectionCount) {
            throw new IndexOutOfBoundsException("ndx out of bounds: " + ndx);
        }
    }
    
    private void checkDimension(int dimension) {
        if (dimension < 0 || dimension >= mDimensionCount) {
            throw new IndexOutOfBoundsException("dimension out of bounds: " + dimension);
        }
    }
    
    private void checkProjectionBuffer(float[] projection) {
        if (projection.length != mDimensionCount) {
            throw new IllegalArgumentException("incorrect number of dimensions: " +
                    projection.length + " != " + mDimensionCount);
        }
    }
    
    /**
     * Returns the number of projection points maintained in this
     * object. 
     * @return
     */
    public int getProjectionCount() {
        return mProjectionCount;
    }
    
    /**
     * Returns the dimensionality for the projection.
     * @return - a positive integer.
     */
    public int getDimensionCount() {
        return mDimensionCount;
    }
    
    /**
     * Get the minimum permissible value for a projection
     * in the specified dimension.  All actual values in
     * this dimension must be greater than or equal to this
     * value.
     * @param dimension
     * @return - the minimum value.
     */
    public float getMinAllowed(int dimension) {
        return mMinAllowed[dimension];
    }
    
    /**
     * Get the maximum permissible value for a projection
     * in the specified dimension.  All actual values in
     * this dimension must be less than or equal to this
     * value.
     * @param dimension
     * @return - the maximum value.
     */
    public float getMaxAllowed(int dimension) {
        return mMaxAllowed[dimension];
    }
    
    public void setMinAllowed(int dimension, float value) {
    	mMinAllowed[dimension] = value;
    }
    
    public void setMaxAllowed(int dimension, float value) {
    	mMaxAllowed[dimension] = value;
    }
    
    /**
     * Get the minimum actual value for a projection
     * in the specified dimension.  This should be 
     * greater than or equal to the value returned from
     * <code>getMinAllowed(dimension)</code>.
     * @param dimension
     * @return - the minimum value.
     */
    public float getMin(int dimension) {
        float min = getMaxAllowed(dimension);
        int lastRow = mData.length - 1;
        for (int r=0; r<lastRow; r++) {
            float rowMin = getMin(mData[r], mPointsPerRow, dimension);
            if (rowMin < min) {
                min = rowMin;
            }
        }
        int pointsInLastRow = mProjectionCount%mPointsPerRow;
        if (pointsInLastRow == 0) {
            pointsInLastRow = mPointsPerRow;
        }
        float rowMin = getMin(mData[lastRow], pointsInLastRow, dimension);
        if (rowMin < min) {
            min = rowMin;
        }
        return min;
    }
    
    private float getMin(float[] data, int pointsInRow, int dimension) {
        int maxNdx = mDimensionCount * (pointsInRow - 1) + dimension;
        float min = getMaxAllowed(dimension);
        for (int ndx = dimension; ndx <= maxNdx; ndx += mDimensionCount) {
            float value = data[ndx];
            if (value < min) {
                min = value;
            }
        }
        return min;
    }
    
    /**
     * Get the maximum actual value for a projection
     * in the specified dimension.  This should be 
     * less than or equal to the value returned from
     * <code>getMaxAllowed(dimension)</code>.
     * @param dimension
     * @return - the maximum value.
     */
    public float getMax(int dimension) {
        float max = getMinAllowed(dimension);
        int lastRow = mData.length - 1;
        for (int r=0; r<lastRow; r++) {
            float rowMax = getMax(mData[r], mPointsPerRow, dimension);
            if (rowMax > max) {
                max = rowMax;
            }
        }
        int pointsInLastRow = mProjectionCount%mPointsPerRow;
        if (pointsInLastRow == 0) {
            pointsInLastRow = mPointsPerRow;
        }
        float rowMax = getMax(mData[lastRow], pointsInLastRow, dimension);
        if (rowMax > max) {
            max = rowMax;
        }
        return max;
    }
    
    private float getMax(float[] data, int pointsInRow, int dimension) {
        int maxNdx = mDimensionCount * (pointsInRow - 1) + dimension;
        float max = getMinAllowed(dimension);
        for (int ndx = dimension; ndx <= maxNdx; ndx += mDimensionCount) {
            float value = data[ndx];
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    /**
     * Sets the projection for the point with the specified index.
     * @param ndx - the 0-based index of the point whose 
     *   projection is to be set.
     * @param projection - an array holding the projection values.
     * @exception IndexOutOfBoundsException - if ndx is not a valid
     *   point index.
     * @exception IllegalArgumentException - if the projection array's
     *   length is not equal to the number of projected dimensions.
     */
    public void setProjection(int ndx, float[] projection) {
        checkIndex(ndx);
        checkProjectionBuffer(projection);
        int row = ndx/mPointsPerRow;
        int col = (ndx%mPointsPerRow) * mDimensionCount;
        System.arraycopy(projection, 0, mData[row], col, mDimensionCount);
    }

    public void setProjection(int ndx, int dimension, float projection) {
    	checkIndex(ndx);
    	checkDimension(dimension);
        int row = ndx/mPointsPerRow;
        int col = (ndx%mPointsPerRow) * mDimensionCount + dimension;
        mData[row][col] = projection;
    }
    
    /**
     * Retrieves the projection for the point with the specified index.
     * @param ndx - the 0-based index of the point whose 
     *   projection is to be set.
     * @param projection - an array into which the projection values should
     *   be copied.  If this array is null or of incorrect length, a
     *   new array is allocated and returned.
     * @return an array containing the values.  This will be the passed in
     *   array if it's of the correct length.
     * @exception IndexOutOfBoundsException - if ndx is not a valid
     *   point index.
     */
    public float[] getProjection(int ndx, float[] projection) {
        checkIndex(ndx);
        float[] rtn = projection;
        if (rtn == null || rtn.length < mDimensionCount) {
            rtn = new float[mDimensionCount];
        }
        int row = ndx/mPointsPerRow;
        int col = (ndx%mPointsPerRow) * mDimensionCount;
        System.arraycopy(mData[row], col, rtn, 0, mDimensionCount);
        return rtn;
    }
    
    public void normalize() {
        float[] mins = new float[mDimensionCount];
        float[] spreads = new float[mDimensionCount];
        for (int i=0; i<mDimensionCount; i++) {
            mins[i] = getMin(i);
            spreads[i] = getMax(i) - mins[i];
        }
        float[] projBuffer = new float[mDimensionCount];
        for (int i=0; i<mProjectionCount; i++) {
            getProjection(i, projBuffer);
            for (int d=0; d<mDimensionCount; d++) {
                float spread = spreads[d];
                if (spread > 0f) {
                    projBuffer[d] = (projBuffer[d] - mins[d])/spread;
                } else {
                    projBuffer[d] = 0.5f;
                }
            }
            setProjection(i, projBuffer);
        }
        Arrays.fill(mMinAllowed, 0f);
        Arrays.fill(mMaxAllowed, 1f);
    }

    public float getProjection(int ndx, int dimension) {
        checkIndex(ndx);
        checkDimension(dimension);
        int row = ndx/mPointsPerRow;
        int col = (ndx%mPointsPerRow) * mDimensionCount + dimension;
        return mData[row][col];
    }

    public void readExternal(ObjectInput in) throws IOException,
            ClassNotFoundException {
        ProjectionData pd = ProjectionDataSerializer.load(in);
        if (pd instanceof SimpleProjectionData) {
            SimpleProjectionData spd = (SimpleProjectionData) pd;
            this.mData = spd.mData;
            this.mPointsPerRow = spd.mPointsPerRow;
            this.mMinAllowed = spd.mMinAllowed;
            this.mMaxAllowed = spd.mMaxAllowed;
            this.mProjectionCount = spd.mProjectionCount;
            this.mDimensionCount = spd.mDimensionCount;
        } else {
            this.mProjectionCount = pd.getProjectionCount();
            this.mDimensionCount = pd.getDimensionCount();
            this.mMinAllowed = new float[this.mDimensionCount];
            this.mMaxAllowed = new float[this.mDimensionCount];
            for (int i=0; i<this.mDimensionCount; i++) {
                this.mMinAllowed[i] = pd.getMinAllowed(i);
                this.mMaxAllowed[i] = pd.getMaxAllowed(i);
            }
            allocateData();
            float[] buffer = new float[this.mDimensionCount];
            for (int i=0; i<this.mProjectionCount; i++) {
                this.setProjection(i, pd.getProjection(i, buffer));
            }
        }
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        ProjectionDataSerializer.save(this, out);
    }
}
