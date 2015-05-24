package gov.pnnl.jac.projection;

import gov.pnnl.jac.cluster.Cluster;
import gov.pnnl.jac.cluster.ClusterList;
import gov.pnnl.jac.geom.CoordinateList;
import gov.pnnl.jac.geom.DoubleMatrix2DCoordinateList;
import gov.pnnl.jac.geom.SimpleCoordinateList;
import gov.pnnl.jac.geom.distance.DistanceCache;
import gov.pnnl.jac.geom.distance.DistanceCacheFactory;
import gov.pnnl.jac.math.linalg.PCA;
import gov.pnnl.jac.task.AbstractTask;
import gov.pnnl.jac.task.ProgressHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import cern.colt.function.DoubleFunction;
import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;

public class MDSProjectionTask  {}
//extends AbstractTask<ProjectionData> {
//
//    // Threshold that determines the number of coordinates whose
//    // pairwise distances can be cached in RAM.  Defaulting to 128MB,
//    // this equates to 5793 coordinates.  If there are more coordinates
//    // than this, a file-based cache will have to be used.
//    private long mDistanceCacheMemThreshold = 128L * 1024L * 1024L;
//
//    // The file threshold limits the number of coordinates that can be
//    // hierarchically clustered.  By default, this threshold is 2GB, limiting
//    // the number of coordinates to 23,170.
//    private long mDistanceCacheFileThreshold = 2L * 1024L * 1024L * 1024L;
//
//    // The directory in which to store cache files temporarily during the
//    // construction of a new dendrogram.
//    private File mCacheFileLocation;
//
//    // The inputs.
//    private CoordinateList mCoordinates;
//
//    private ClusterList mClusters;
//
//    private ProjectionParams mParams;
//
//    // The outputs or products of doTask().
//    private ProjectionData mClusterProjection;
//
//    private ProjectionData mPointProjection;
//
//    public MDSProjectionTask(CoordinateList coordinates, ClusterList clusters,
//            ProjectionParams params) {
//        if (coordinates == null || clusters == null || params == null) {
//            throw new NullPointerException();
//        }
//        mCoordinates = coordinates;
//        mClusters = clusters;
//        mParams = params;
//    }
//
//    public MDSProjectionTask(CoordinateList coordinates, ClusterList clusters) {
//        this(coordinates, clusters, new ProjectionParams());
//    }
//
//    public String taskName() {
//        return "MDS projection";
//    }
//
//    public ClusterList getClusterList() {
//        return mClusters;
//    }
//
//    public ProjectionData getClusterProjection() {
//        return mClusterProjection;
//    }
//
//    public ProjectionData getPointProjection() {
//        return mPointProjection;
//    }
//
//    protected ProjectionData doTask() throws Exception {
//
//        int steps = 4;
//        if (mParams.getNormalizeDimensions())
//            steps++;
//        if (mParams.getNormalizeCoordinates())
//            steps++;
//        if (mParams.getGravityFactor() < 1.0)
//            steps++;
//
//        ProgressHandler ph = new ProgressHandler(this, steps);
//        ph.postBegin();
//
//        int numClusters = mClusters.getClusterCount();
//        int numCoords = mCoordinates.getCoordinateCount();
//        int numDim = mCoordinates.getDimensionCount();
//        int projectionDim = mParams.getNumDimensions();
//
//        // A function for ensuring that NaNs are removed from the data.
//        DoubleFunction nanRemover = new cern.colt.function.DoubleFunction() {
//            public double apply(double val) {
//                if (Double.isNaN(val)) {
//                    return 0;
//                }
//                return val;
//            }
//        };
//        
//        double meanSize = mClusters.averageClusterSize();
//        
//        int minSize = Math.max(1, (int)(0.25 * meanSize + 0.5));
//        int maxSize = Integer.MAX_VALUE;
//        
//        // Remove abnormally-sized clusters from computation.
//        List<Cluster> clist = new ArrayList<Cluster> ();
//        for (int i=0; i<numClusters; i++) {
//        	Cluster c = mClusters.getCluster(i);
//        	if(c.getSize() >= minSize && c.getSize() <= maxSize) {
//        		clist.add(c);
//        	}
//        }
//        
//        ClusterList listForComputation = mClusters;
//        
//        if (clist.size() >= 3) {
//        	listForComputation = new ClusterList((Cluster[]) clist.toArray(new Cluster[clist.size()]));
//        }
//        
//        // Make a matrix from the cluster centers. Will be numClusters X numDim
//        DoubleMatrix2D centroidMatrix = listForComputation.createCentroidMatrix();
//        
//        // This is to eliminate the possibility of the centroid matrix containing NaNs.
//        centroidMatrix.assign(nanRemover);
//
//        DoubleMatrix2D completeCentroidMatrix = mClusters.createCentroidMatrix();
//        completeCentroidMatrix.assign(nanRemover);
//        
//        if (mParams.getNormalizeDimensions()) {
//            // Compute the mins and maxes from the coordinates, not the
//            // cluster centers, since the coordinates generally are dispersed over
//            // a larger volume than the cluster centers.
//            DoubleMatrix2D dimMinMax = computeMinMax(mCoordinates);
//            // Normalize the cluster centers.
//            normalizeDimensions(
//                    new DoubleMatrix2DCoordinateList(centroidMatrix), dimMinMax);
//            normalizeDimensions(
//                    new DoubleMatrix2DCoordinateList(completeCentroidMatrix), dimMinMax);
//            // Normalize the coordinates.  Since this changes the coordinates,
//            // projection should be done on a COPY of the coordinates if the original coordinates
//            // need to be preserved.
//            normalizeDimensions(mCoordinates, dimMinMax);
//
//            ph.postStep();
//        }
//
//        // Compute the projection matrix.
//        PCA pca = new PCA(centroidMatrix, PCA.CovarianceType.COVARIANCE, projectionDim);
//        DoubleMatrix2D projMatrix = pca.getPrincipalComponents();
//        standardizeOrientation(projMatrix);
//
//        ph.postStep();
//
//        // Generate the projection data for the clusters.
//        // Have to pass in minAllowed and maxAllowed with extreme values to constructor of
//        // the project data objects, since they default to restricting values to [0 - 1].
//        float[] minAllowed = new float[projectionDim];
//        float[] maxAllowed = new float[projectionDim];
//        Arrays.fill(minAllowed, -Float.MAX_VALUE);
//        Arrays.fill(maxAllowed, Float.MAX_VALUE);
//
//        // Compute the cluster projection.
//        SimpleProjectionData clusterProjection = new SimpleProjectionData(
//                numClusters, projectionDim, minAllowed, maxAllowed);
//
//        Algebra alg = new Algebra();
//
//        float[] projectionBuf = new float[projectionDim];
//
//        for (int i = 0; i < numClusters; i++) {
//            DoubleMatrix1D centroid = completeCentroidMatrix.viewRow(i);
//            for (int j = 0; j < projectionDim; j++) {
//                DoubleMatrix1D column = projMatrix.viewColumn(j);
//                projectionBuf[j] = (float) alg.mult(centroid, column);
//            }
//            clusterProjection.setProjection(i, projectionBuf);
//        }
//
//        ph.postStep();
//
//        // Compute the point projection.
//        SimpleProjectionData pointProjection = new SimpleProjectionData(
//                numCoords, projectionDim, minAllowed, maxAllowed);
//
//        DoubleMatrix1D coordinate = DoubleFactory1D.dense.make(numDim);
//        double[] coordBuf = new double[numDim];
//
//        for (int i = 0; i < numCoords; i++) {
//            mCoordinates.getCoordinates(i, coordBuf);
//            coordinate.assign(coordBuf);
//            // Replace all NaNs in the coordinate matrix with 0s.
//            coordinate.assign(nanRemover);
//            for (int j = 0; j < projectionDim; j++) {
//                DoubleMatrix1D column = projMatrix.viewColumn(j);
//                projectionBuf[j] = (float) alg.mult(coordinate, column);
//            }
//            pointProjection.setProjection(i, projectionBuf);
//        }
//
//        ph.postStep();
//
//        // Normalize the coordinates, if asked to do so.
//        if (mParams.getNormalizeCoordinates()) {
//            DoubleMatrix2D coordMinMax = computeMinMax(pointProjection);
//            normalizeCoordinates(clusterProjection, coordMinMax);
//            normalizeCoordinates(pointProjection, coordMinMax);
//            ph.postStep();
//        }
//
//        // Apply the gravity transform.
//        if (mParams.getGravityFactor() < 1.0) {
//            applyGravityTransform(clusterProjection, pointProjection, mParams
//                    .getGravityFactor());
//            ph.postStep();
//        }
//
//        // Do the final normalization.
//        normalizeGlobally(clusterProjection, pointProjection);
//        ph.postStep();
//
//        mClusterProjection = clusterProjection;
//        mPointProjection = pointProjection;
//
//        ph.postEnd();
//
//        return mPointProjection;
//    }
//
//    private static DoubleMatrix2D computeMinMax(CoordinateList coordList) {
//
//        int rows = coordList.getCoordinateCount();
//        int cols = coordList.getDimensionCount();
//
//        double[] dmin = new double[cols];
//        double[] dmax = new double[cols];
//        Arrays.fill(dmin, Double.MAX_VALUE);
//        Arrays.fill(dmax, -Double.MAX_VALUE);
//
//        double[] coords = new double[cols];
//        for (int i = 0; i < rows; i++) {
//            coordList.getCoordinates(i, coords);
//            for (int j = 0; j < cols; j++) {
//                double d = coords[j];
//                if (Double.isNaN(d))
//                    d = 0.0;
//                if (d < dmin[j])
//                    dmin[j] = d;
//                if (d > dmax[j])
//                    dmax[j] = d;
//            }
//        }
//
//        return DoubleFactory2D.dense.make(new double[][] { dmin, dmax });
//    }
//
//    private static DoubleMatrix2D computeMinMax(ProjectionData projData) {
//
//        int rows = projData.getProjectionCount();
//        int cols = projData.getDimensionCount();
//
//        double[] dmin = new double[cols];
//        double[] dmax = new double[cols];
//        Arrays.fill(dmin, Double.MAX_VALUE);
//        Arrays.fill(dmax, -Double.MAX_VALUE);
//
//        float[] proj = new float[cols];
//        for (int i = 0; i < rows; i++) {
//            projData.getProjection(i, proj);
//            for (int j = 0; j < cols; j++) {
//                double d = proj[j];
//                if (d < dmin[j])
//                    dmin[j] = d;
//                if (d > dmax[j])
//                    dmax[j] = d;
//            }
//        }
//
//        return DoubleFactory2D.dense.make(new double[][] { dmin, dmax });
//    }
//
//    private static void normalizeDimensions(
//    		CoordinateList nspace,
//            DoubleMatrix2D minmax) {
//
//        int rows = nspace.getCoordinateCount();
//        int cols = nspace.getDimensionCount();
//
//        double[] dmin = new double[cols];
//        double[] drange = new double[cols];
//        for (int i = 0; i < cols; i++) {
//            dmin[i] = minmax.getQuick(0, i);
//            drange[i] = minmax.getQuick(1, i) - dmin[i];
//        }
//
//        double[] coords = new double[cols];
//        for (int i = 0; i < rows; i++) {
//            nspace.getCoordinates(i, coords);
//            for (int j = 0; j < cols; j++) {
//                double min = dmin[j];
//                double range = drange[j];
//                if (range == 0.0) {
//                    coords[j] = 0.0;
//                } else {
//                    coords[j] = (coords[j] - min) / range;
//                }
//            }
//            nspace.setCoordinates(i, coords);
//        }
//
//    }
//
//    private static void normalizeCoordinates(
//    		ProjectionData pts,
//            DoubleMatrix2D minmax) {
//
//        int numCoords = pts.getProjectionCount();
//        int dimensions = pts.getDimensionCount();
//
//        float[] dmin = new float[dimensions];
//        float[] drange = new float[dimensions];
//        for (int i = 0; i < dimensions; i++) {
//            dmin[i] = (float) minmax.viewRow(0).getQuick(i);
//            drange[i] = (float) minmax.viewRow(1).getQuick(i) - dmin[i];
//            pts.setMinAllowed(i, -Float.MAX_VALUE);
//            pts.setMaxAllowed(i, Float.MAX_VALUE);
//        }
//
//        float[] proj = new float[dimensions];
//        for (int i = 0; i < numCoords; i++) {
//            pts.getProjection(i, proj);
//            for (int j = 0; j < dimensions; j++) {
//                float r = drange[j];
//                if (r == 0.0f) {
//                    proj[j] = 0.5f;
//                } else {
//                    proj[j] = (proj[j] - dmin[j]) / r;
//                }
//            }
//            pts.setProjection(i, proj);
//        }
//
//        for (int i=0; i<dimensions; i++) {
//            pts.setMinAllowed(i, 0f);
//            pts.setMaxAllowed(i, 1f);
//        }
//    }
//
//    private void applyGravityTransform(ProjectionData clusterProjection,
//            ProjectionData pointProjection, double factor) {
//
//        int numClusters = mClusters.getClusterCount();
//        int projectionDim = clusterProjection.getDimensionCount();
//
//        double factorComp = 1.0 - factor;
//        double[] clustFactors = new double[projectionDim];
//
//        float[] clusterPt = new float[projectionDim];
//        float[] coordPt = new float[projectionDim];
//
//        // For each cluster
//        for (int c = 0; c < numClusters; c++) {
//            clusterProjection.getProjection(c, clusterPt);
//            for (int d = 0; d < projectionDim; d++) {
//                clustFactors[d] = factorComp * clusterPt[d];
//            }
//            int[] ids = mClusters.getCluster(c).getMembership();
//            int n = ids.length;
//            for (int i = 0; i < n; i++) {
//                pointProjection.getProjection(ids[i], coordPt);
//                for (int d = 0; d < projectionDim; d++) {
//                    coordPt[d] = (float) (factor * coordPt[d] + clustFactors[d]);
//                }
//                pointProjection.setProjection(ids[i], coordPt);
//            }
//        }
//    }
//
//    private void normalizeGlobally(
//    		ProjectionData clusterProjection,
//            ProjectionData pointProjection) {
//
//        float min = Float.MAX_VALUE;
//        float max = -Float.MAX_VALUE;
//        int projectionDim = clusterProjection.getDimensionCount();
//
//        for (int d = 0; d < projectionDim; d++) {
//            float m = Math.min(clusterProjection.getMin(d), pointProjection
//                    .getMin(d));
//            if (m < min) {
//                min = m;
//            }
//            m = Math.max(clusterProjection.getMax(d), pointProjection.getMax(d));
//            if (m > max) {
//                max = m;
//            }
//        }
//
//        // Since we're changing projection values, set min/max allowed to values
//        // that cannot cause problems.
//        for (int d = 0; d < projectionDim; d++) {
//        	clusterProjection.setMinAllowed(d, -Float.MAX_VALUE);
//        	pointProjection.setMinAllowed(d, -Float.MAX_VALUE);
//        	clusterProjection.setMaxAllowed(d, Float.MAX_VALUE);
//        	pointProjection.setMaxAllowed(d, Float.MAX_VALUE);
//        }
//
//        float range = max - min;
//        float[] buffer = new float[projectionDim];
//
//        // Trivial case -- all projections are [0.0, 0.0], so shift 'em to [0.5, 0.5]
//        if (range <= 0.0) {
//
//            Arrays.fill(buffer, 0.5f);
//            int numPoints = clusterProjection.getProjectionCount();
//            for (int i = 0; i < numPoints; i++) {
//                clusterProjection.setProjection(i, buffer);
//            }
//
//            numPoints = pointProjection.getProjectionCount();
//            for (int i = 0; i < numPoints; i++) {
//                pointProjection.setProjection(i, buffer);
//            }
//
//        } else { // Usual case
//
//            int numPoints = clusterProjection.getProjectionCount();
//            for (int i = 0; i < numPoints; i++) {
//                clusterProjection.getProjection(i, buffer);
//                for (int d = 0; d < projectionDim; d++) {
//                    buffer[d] = (buffer[d] - min) / range;
//                }
//                clusterProjection.setProjection(i, buffer);
//            }
//
//            numPoints = pointProjection.getProjectionCount();
//            for (int i = 0; i < numPoints; i++) {
//                pointProjection.getProjection(i, buffer);
//                for (int d = 0; d < projectionDim; d++) {
//                    buffer[d] = (buffer[d] - min) / range;
//                }
//                pointProjection.setProjection(i, buffer);
//            }
//        }
//
//        for (int d = 0; d < projectionDim; d++) {
//        	clusterProjection.setMinAllowed(d, 0.0f);
//        	pointProjection.setMinAllowed(d, 0.0f);
//        	clusterProjection.setMaxAllowed(d, 1.0f);
//        	pointProjection.setMaxAllowed(d, 1.0f);
//        }
//    }
//
//    /**
//     * Standardize the projection's orientation to eliminate reflections and
//     * rotations caused by minor perturbation of the data. This is done by
//     * making the eigenvectors (matrix columns) face in the same general
//     * direction as an arbitrary reference vector.
//     * 
//     * @param projection_matrix
//     *            DoubleMatrix2D
//     */
//    private static void standardizeOrientation(
//                    DoubleMatrix2D projection_matrix) {
//            int dimensions = projection_matrix.columns();
//
//            for (int d = 0; d < dimensions; d++) {
//                    // Compute dot product with the reference vector (1, 1, ..., 1),
//                    // which was chosen specifically to simplify this step
//                    DoubleMatrix1D column = projection_matrix.viewColumn(d);
//                    double dot_product = column.zSum();
//
//                    if (dot_product < 0) {
//                            // Eigenvector points the wrong way; reverse it
//                            int n = column.size();
//                            for (int i = 0; i < n; i++) {
//                                    column.setQuick(i, (-1.0 * column.getQuick(i)));
//                            }
//                    }
//            }
//    }
//    
//    /**
//     * Computes an N by N distance matrix where the distances are between pairs of
//     * rows of the input.  Since Dij = Dji, and Dii = 0, the matrix is symmetric about
//     * a diagonal of zeros.
//     * 
//     * @param input
//     * @return
//     */
//    private static DistanceCache computeDistances(DoubleMatrix2D input) {
//    	DistanceCache cache = DistanceCacheFactory.newDistanceCache(coordinateCount, memoryThreshold, fileThreshold, cacheFile)
//    	
//    }
//    
//    /*    
//        private void normalizeByColumn(ProjectionData clusterProjection, 
//                ProjectionData pointProjection) {
//            
//            float[] mins = new float[mProjectionDimensions];
//            float[] ranges = new float[mProjectionDimensions];
//            
//            for (int d=0; d<mProjectionDimensions; d++) {
//                
//                mins[d] = Math.min(clusterProjection.getMin(d), 
//                        pointProjection.getMin(d));
//                float max = Math.max(clusterProjection.getMax(d), 
//                        pointProjection.getMax(d));
//                
//                ranges[d] = max - mins[d];
//            }
//            
//            float[] buffer = new float[mProjectionDimensions];
//            int numPoints = clusterProjection.getProjectionCount();
//            for (int i=0; i<numPoints; i++) {
//                clusterProjection.getProjection(i, buffer);
//                for (int d=0; d<mProjectionDimensions; d++) {
//                    if (ranges[d] <= 0f) { // Trivial case
//                        buffer[d] = 0.5f;
//                    } else { // Usual case
//                        buffer[d] = (buffer[d] - mins[d])/ranges[d];
//                    }
//                }
//                clusterProjection.setProjection(i, buffer);
//            }
//
//            numPoints = pointProjection.getProjectionCount();
//            for (int i=0; i<numPoints; i++) {
//                pointProjection.getProjection(i, buffer);
//                for (int d=0; d<mProjectionDimensions; d++) {
//                    if (ranges[d] <= 0f) { // Trivial case
//                        buffer[d] = 0.5f;
//                    } else { // Usual case
//                        buffer[d] = (buffer[d] - mins[d])/ranges[d];
//                    }
//                }
//                pointProjection.setProjection(i, buffer);
//            }
//        }
//    */
//    
//    public static void main(String[] args) {
//        try {
//            int rows = 5, cols = 5;
//            CoordinateList coords = new SimpleCoordinateList(cols, rows);
//            Random random = new Random(1234L);
//            for (int i=0; i<rows; i++) {
//                for (int j=0; j<cols; j++) {
//                    coords.setCoordinateQuick(i, j, 0.5 + random.nextDouble()*0.5);
//                }
//            }
//            
//            DoubleMatrix2D minmax = computeMinMax(coords);
////            int minmaxrows = minmax.rows();
////            int minmaxcols = minmax.columns();
////            for (int i=0; i<minmaxrows; i++) {
////                for (int j=0; j<minmaxcols; j++) {
////                    if (j > 0) System.out.print(", ");
////                    System.out.print(minmax.get(i, j));
////                }
////                System.out.println();
////            }
//            
//            normalizeDimensions(coords, minmax);
//            
//            for (int i=0; i<rows; i++) {
//                for (int j=0; j<cols; j++) {
//                    if (j > 0) System.out.print(", ");
//                    System.out.print(coords.getCoordinateQuick(i, j));
//                }
//                System.out.println();
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//}
