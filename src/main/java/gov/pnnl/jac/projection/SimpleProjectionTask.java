package gov.pnnl.jac.projection;

import gov.pnnl.jac.geom.CoordinateList;
import gov.pnnl.jac.geom.RealMatrixCoordinateList;
import gov.pnnl.jac.math.linalg.PCA;
import gov.pnnl.jac.task.ProgressHandler;
import gov.pnnl.jac.util.ExceptionUtil;

import java.util.Arrays;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealMatrixChangingVisitor;
import org.apache.commons.math3.linear.RealVector;

public class SimpleProjectionTask extends AbstractProjectionTask {


    // The inputs.
    private CoordinateList mCoordinates;
    private RealMatrix mPCAMatrix;
    private boolean mNormalizeDimensions;
    private boolean mNormalizeCoordinates;
    private int mDimensions;
    
    // The outputs or products of doTask().
    private ProjectionData mProjection;
    private ProjectionData mPCAProjection;

    public SimpleProjectionTask(CoordinateList coordinates, RealMatrix pcaMatrix,
            boolean normalizeDimensions,
            boolean normalizeCoordinates,
            int dimensions) {
        if (coordinates == null || pcaMatrix == null) {
            throw new NullPointerException();
        }
        mCoordinates = coordinates;
        mPCAMatrix = pcaMatrix;
        mNormalizeDimensions = normalizeDimensions;
        mNormalizeCoordinates = normalizeCoordinates;
        mDimensions = dimensions;
    }

    public String taskName() {
        return "projection";
    }

    public ProjectionData getProjection() {
        return mProjection;
    }
    
    public ProjectionData getPCAProjection() {
        return mPCAProjection;
    }

    protected ProjectionData doTask() throws Exception {

        int steps = 4;
        if (mNormalizeDimensions)
            steps++;
        if (mNormalizeCoordinates)
            steps++;

        ProgressHandler ph = new ProgressHandler(this, steps);
        ph.postBegin();

        int numPCARows = mPCAMatrix.getRowDimension();
        int numCoords = mCoordinates.getCoordinateCount();
        int numDim = mCoordinates.getDimensionCount();
        int projectionDim = mDimensions;
        
        ExceptionUtil.checkInBounds(projectionDim, 1, numDim - 1);

        RealMatrixChangingVisitor nanRemover = new RealMatrixChangingVisitor() {

            @Override
            public void start(int rows, int columns, int startRow, int endRow,
                    int startColumn, int endColumn) {
            }

            @Override
            public double visit(int row, int column, double value) {
                if (Double.isNaN(value)) return 0.0;
                return value;
            }

            @Override
            public double end() {
                return 0;
            }
            
        };
        
        // Eliminate NaNs, if any.
        mPCAMatrix.walkInColumnOrder(nanRemover);
        
        if (mNormalizeDimensions) {
            // Compute the mins and maxes.  This was originally done from the coordinates,
            // not the cluster centers, but Dave Gillen changed it to this to get
            // the result to more closely match IN-SPIRE's.
            RealMatrix dimMinMax = computeMinMax(mPCAMatrix);
            // Normalize the cluster centers.
            normalizeDimensions(
                    new RealMatrixCoordinateList(mPCAMatrix), dimMinMax);
            // Normalize the coordinates.  Since this changes the coordinates,
            // projection should be done on a COPY of the coordinates if the original coordinates
            // need to be preserved.            
            normalizeDimensions(mCoordinates, dimMinMax);
            ph.postStep();
        }

        // Compute the projection matrix.
        PCA pca = new PCA(mPCAMatrix, PCA.CovarianceType.COVARIANCE, projectionDim);
        RealMatrix projMatrix = pca.getPrincipalComponents();
        
        standardizeOrientation(projMatrix);

        ph.postStep();

        // Generate the projection data for the clusters.
        // Have to pass in minAllowed and maxAllowed with extreme values to constructor of
        // the project data objects, since they default to restricting values to [0 - 1].
        float[] minAllowed = new float[projectionDim];
        float[] maxAllowed = new float[projectionDim];
        Arrays.fill(minAllowed, -Float.MAX_VALUE);
        Arrays.fill(maxAllowed, Float.MAX_VALUE);

        // Compute the cluster projection.
        SimpleProjectionData pcaProjection = new SimpleProjectionData(
                numPCARows, projectionDim, minAllowed, maxAllowed);

        final float[] projectionBuf = new float[projectionDim];

        for (int i = 0; i < numPCARows; i++) {
            RealVector centroid = mPCAMatrix.getRowVector(i);
            for (int j = 0; j < projectionDim; j++) {
                RealVector column = projMatrix.getColumnVector(j);
                projectionBuf[j] = (float) centroid.dotProduct(column);
            }
            pcaProjection.setProjection(i, projectionBuf);
        }

        ph.postStep();

        // Compute the point projection.
        SimpleProjectionData pointProjection = new SimpleProjectionData(
                numCoords, projectionDim, minAllowed, maxAllowed);

        RealVector coordinate = new ArrayRealVector(numDim);
        double[] coordBuf = new double[numDim];

        for (int i = 0; i < numCoords; i++) {
            mCoordinates.getCoordinates(i, coordBuf);
            for (int j=0; j<numDim; j++) {
                double d = coordBuf[j];
                coordinate.setEntry(j, Double.isNaN(d) ? 0.0 : d);
            }
            for (int j = 0; j < projectionDim; j++) {
                RealVector column = projMatrix.getColumnVector(j);
                projectionBuf[j] = (float) coordinate.dotProduct(column);
            }
            pointProjection.setProjection(i, projectionBuf);
        }

        ph.postStep();

        // Normalize the coordinates, if asked to do so.
        if (mNormalizeCoordinates) {
            RealMatrix coordMinMax = computeMinMax(pcaProjection);
            normalizeCoordinates(pcaProjection, coordMinMax);
            normalizeCoordinates(pointProjection, coordMinMax);
            ph.postStep();
        }

        // Do the final normalization.
        normalizeGlobally(pcaProjection, pointProjection);
        ph.postStep();

        mPCAProjection = pcaProjection;
        mProjection = pointProjection;

        ph.postEnd();

        return mProjection;
    }
}
