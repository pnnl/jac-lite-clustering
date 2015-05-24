package gov.pnnl.jac.projection;

import gov.pnnl.jac.cluster.ClusterList;
import gov.pnnl.jac.geom.CoordinateList;
import gov.pnnl.jac.geom.RealMatrixCoordinateList;
import gov.pnnl.jac.geom.SimpleCoordinateList;
import gov.pnnl.jac.math.linalg.PCA;
import gov.pnnl.jac.task.AbstractTask;
import gov.pnnl.jac.task.ProgressHandler;

import java.util.Arrays;
import java.util.Random;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealMatrixChangingVisitor;
import org.apache.commons.math3.linear.RealVector;

public class ProjectionTask extends AbstractProjectionTask {

    // The inputs.
    private CoordinateList mCoordinates;

    private ClusterList mClusters;

    private ProjectionParams mParams;

    // The outputs or products of doTask().
    private ProjectionData mClusterProjection;

    private ProjectionData mPointProjection;

    public ProjectionTask(CoordinateList coordinates, ClusterList clusters,
            ProjectionParams params) {
        if (coordinates == null || clusters == null || params == null) {
            throw new NullPointerException();
        }
        mCoordinates = coordinates;
        mClusters = clusters;
        mParams = params;
    }

    public ProjectionTask(CoordinateList coordinates, ClusterList clusters) {
        this(coordinates, clusters, new ProjectionParams());
    }

    public String taskName() {
        return "coordinate and cluster projection";
    }

    public ClusterList getClusterList() {
        return mClusters;
    }

    public ProjectionData getClusterProjection() {
        return mClusterProjection;
    }

    public ProjectionData getPointProjection() {
        return mPointProjection;
    }

    protected ProjectionData doTask() throws Exception {

        int steps = 4;
        if (mParams.getNormalizeDimensions())
            steps++;
        if (mParams.getNormalizeCoordinates())
            steps++;
        if (mParams.getGravityFactor() < 1.0)
            steps++;

        ProgressHandler ph = new ProgressHandler(this, steps);
        ph.postBegin();

        int numClusters = mClusters.getClusterCount();
        int numCoords = mCoordinates.getCoordinateCount();
        int numDim = mCoordinates.getDimensionCount();
        int projectionDim = mParams.getNumDimensions();

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
        
        // Make a matrix from the cluster centers. Will be numClusters X numDim
        RealMatrix pcaCentroidMatrix = mClusters.createCentroidMatrix();
        // Eliminate NaNs, if any.
        pcaCentroidMatrix.walkInColumnOrder(nanRemover);
        
        if (mParams.getNormalizeDimensions()) {
            // Compute the mins and maxes.  This was originally done from the coordinates,
        	// not the cluster centers, but Dave Gillen changed it to this to get
        	// the result to more closely match IN-SPIRE's.
            RealMatrix dimMinMax = computeMinMax(pcaCentroidMatrix);
            // Normalize the cluster centers.
            normalizeDimensions(
                    new RealMatrixCoordinateList(pcaCentroidMatrix), dimMinMax);
            // Normalize the coordinates.  Since this changes the coordinates,
            // projection should be done on a COPY of the coordinates if the original coordinates
            // need to be preserved.
            normalizeDimensions(mCoordinates, dimMinMax);
            ph.postStep();
        }

        // Compute the projection matrix.
        PCA pca = new PCA(pcaCentroidMatrix, PCA.CovarianceType.COVARIANCE, projectionDim);
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
        SimpleProjectionData clusterProjection = new SimpleProjectionData(
                numClusters, projectionDim, minAllowed, maxAllowed);

        float[] projectionBuf = new float[projectionDim];

        for (int i = 0; i < numClusters; i++) {
            RealVector centroid = pcaCentroidMatrix.getRowVector(i);
            for (int j = 0; j < projectionDim; j++) {
                RealVector column = projMatrix.getColumnVector(j);
                projectionBuf[j] = (float) centroid.dotProduct(column);
            }
            clusterProjection.setProjection(i, projectionBuf);
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
        if (mParams.getNormalizeCoordinates()) {
            RealMatrix coordMinMax = computeMinMax(clusterProjection);
            normalizeCoordinates(clusterProjection, coordMinMax);
            normalizeCoordinates(pointProjection, coordMinMax);
            ph.postStep();
        }

        // Apply the gravity transform.
        if (mParams.getGravityFactor() < 1.0) {
            applyGravityTransform(clusterProjection, pointProjection, mParams
                    .getGravityFactor());
            ph.postStep();
        }

        // Do the final normalization.
        normalizeGlobally(clusterProjection, pointProjection);
        ph.postStep();

        mClusterProjection = clusterProjection;
        mPointProjection = pointProjection;

        ph.postEnd();

        return mPointProjection;
    }


    private void applyGravityTransform(ProjectionData clusterProjection,
            ProjectionData pointProjection, double factor) {

        int numClusters = mClusters.getClusterCount();
        int projectionDim = clusterProjection.getDimensionCount();

        double factorComp = 1.0 - factor;
        double[] clustFactors = new double[projectionDim];

        float[] clusterPt = new float[projectionDim];
        float[] coordPt = new float[projectionDim];

        // For each cluster
        for (int c = 0; c < numClusters; c++) {
            clusterProjection.getProjection(c, clusterPt);
            for (int d = 0; d < projectionDim; d++) {
                clustFactors[d] = factorComp * clusterPt[d];
            }
            int[] ids = mClusters.getCluster(c).getMembership();
            int n = ids.length;
            for (int i = 0; i < n; i++) {
                pointProjection.getProjection(ids[i], coordPt);
                for (int d = 0; d < projectionDim; d++) {
                    coordPt[d] = (float) (factor * coordPt[d] + clustFactors[d]);
                }
                pointProjection.setProjection(ids[i], coordPt);
            }
        }
    }

}
