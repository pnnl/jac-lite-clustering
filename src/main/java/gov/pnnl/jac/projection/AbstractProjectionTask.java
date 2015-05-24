package gov.pnnl.jac.projection;

import java.util.Arrays;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import gov.pnnl.jac.geom.CoordinateList;
import gov.pnnl.jac.task.AbstractTask;

public abstract class AbstractProjectionTask extends AbstractTask<ProjectionData> {

    protected static RealMatrix computeMinMax(CoordinateList coordList) {

        int rows = coordList.getCoordinateCount();
        int cols = coordList.getDimensionCount();

        double[] dmin = new double[cols];
        double[] dmax = new double[cols];
        Arrays.fill(dmin, Double.MAX_VALUE);
        Arrays.fill(dmax, -Double.MAX_VALUE);

        double[] coords = new double[cols];
        for (int i = 0; i < rows; i++) {
            coordList.getCoordinates(i, coords);
            for (int j = 0; j < cols; j++) {
                double d = coords[j];
                if (Double.isNaN(d))
                    d = 0.0;
                if (d < dmin[j])
                    dmin[j] = d;
                if (d > dmax[j])
                    dmax[j] = d;
            }
        }

        return new Array2DRowRealMatrix(new double[][] { dmin, dmax});
    }

    protected static RealMatrix computeMinMax(final RealMatrix coordList) {

          final int rows = coordList.getRowDimension();
          final int cols = coordList.getColumnDimension();

          final double[] dmin = new double[cols];
          final double[] dmax = new double[cols];
          Arrays.fill(dmin, Double.MAX_VALUE);
          Arrays.fill(dmax, -Double.MAX_VALUE);

          for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
              double d = coordList.getEntry(i, j);
              if (Double.isNaN(d)) {
                d = 0.0;
              }
              if (d < dmin[j]) {
                dmin[j] = d;
              }
              if (d > dmax[j]) {
                dmax[j] = d;
              }
            }
          }

          return new Array2DRowRealMatrix(new double[][] { dmin, dmax});
        }

      protected static RealMatrix computeMinMax(ProjectionData projData) {

          int rows = projData.getProjectionCount();
          int cols = projData.getDimensionCount();

          double[] dmin = new double[cols];
          double[] dmax = new double[cols];
          Arrays.fill(dmin, Double.MAX_VALUE);
          Arrays.fill(dmax, -Double.MAX_VALUE);

          float[] proj = new float[cols];
          for (int i = 0; i < rows; i++) {
              projData.getProjection(i, proj);
              for (int j = 0; j < cols; j++) {
                  double d = proj[j];
                  if (d < dmin[j])
                      dmin[j] = d;
                  if (d > dmax[j])
                      dmax[j] = d;
              }
          }

          return new Array2DRowRealMatrix(new double[][] { dmin, dmax} );
      }

      protected static void normalizeDimensions(
              CoordinateList nspace,
              RealMatrix minmax) {

          int rows = nspace.getCoordinateCount();
          int cols = nspace.getDimensionCount();

          double[] dmin = new double[cols];
          double[] drange = new double[cols];
          for (int i = 0; i < cols; i++) {
              dmin[i] = minmax.getEntry(0, i);
              drange[i] = minmax.getEntry(1, i) - dmin[i];
          }

          double[] coords = new double[cols];
          for (int i = 0; i < rows; i++) {
              nspace.getCoordinates(i, coords);
              for (int j = 0; j < cols; j++) {
                  double min = dmin[j];
                  double range = drange[j];
                  if (range == 0.0) {
                      coords[j] = 0.0;
                  } else {
                      coords[j] = (coords[j] - min) / range;
                  }
              }
              nspace.setCoordinates(i, coords);
          }

      }

      protected static void normalizeCoordinates(
              ProjectionData pts,
              RealMatrix minmax) {

          int numCoords = pts.getProjectionCount();
          int dimensions = pts.getDimensionCount();

          float[] dmin = new float[dimensions];
          float[] drange = new float[dimensions];
          for (int i = 0; i < dimensions; i++) {
              dmin[i] = (float) minmax.getEntry(0, i);
              drange[i] = (float) minmax.getEntry(1, i) - dmin[i];
              pts.setMinAllowed(i, -Float.MAX_VALUE);
              pts.setMaxAllowed(i, Float.MAX_VALUE);
          }

          float[] proj = new float[dimensions];
          for (int i = 0; i < numCoords; i++) {
              pts.getProjection(i, proj);
              for (int j = 0; j < dimensions; j++) {
                  float r = drange[j];
                  if (r == 0.0f) {
                      proj[j] = 0.5f;
                  } else {
                      proj[j] = (proj[j] - dmin[j]) / r;
                  }
              }
              pts.setProjection(i, proj);
          }

          for (int i=0; i<dimensions; i++) {
              pts.setMinAllowed(i, 0f);
              pts.setMaxAllowed(i, 1f);
          }
      }

      protected static void normalizeGlobally(
              ProjectionData clusterProjection,
              ProjectionData pointProjection) {

          float min = Float.MAX_VALUE;
          float max = -Float.MAX_VALUE;
          int projectionDim = clusterProjection.getDimensionCount();

          for (int d = 0; d < projectionDim; d++) {
              float m = Math.min(clusterProjection.getMin(d), pointProjection
                      .getMin(d));
              if (m < min) {
                  min = m;
              }
              m = Math.max(clusterProjection.getMax(d), pointProjection.getMax(d));
              if (m > max) {
                  max = m;
              }
          }

          // Since we're changing projection values, set min/max allowed to values
          // that cannot cause problems.
          for (int d = 0; d < projectionDim; d++) {
              clusterProjection.setMinAllowed(d, -Float.MAX_VALUE);
              pointProjection.setMinAllowed(d, -Float.MAX_VALUE);
              clusterProjection.setMaxAllowed(d, Float.MAX_VALUE);
              pointProjection.setMaxAllowed(d, Float.MAX_VALUE);
          }

          float range = max - min;
          float[] buffer = new float[projectionDim];

          // Trivial case -- all projections are [0.0, 0.0], so shift 'em to [0.5, 0.5]
          if (range <= 0.0) {

              Arrays.fill(buffer, 0.5f);
              int numPoints = clusterProjection.getProjectionCount();
              for (int i = 0; i < numPoints; i++) {
                  clusterProjection.setProjection(i, buffer);
              }

              numPoints = pointProjection.getProjectionCount();
              for (int i = 0; i < numPoints; i++) {
                  pointProjection.setProjection(i, buffer);
              }

          } else { // Usual case

              int numPoints = clusterProjection.getProjectionCount();
              for (int i = 0; i < numPoints; i++) {
                  clusterProjection.getProjection(i, buffer);
                  for (int d = 0; d < projectionDim; d++) {
                      buffer[d] = (buffer[d] - min) / range;
                  }
                  clusterProjection.setProjection(i, buffer);
              }

              numPoints = pointProjection.getProjectionCount();
              for (int i = 0; i < numPoints; i++) {
                  pointProjection.getProjection(i, buffer);
                  for (int d = 0; d < projectionDim; d++) {
                      buffer[d] = (buffer[d] - min) / range;
                  }
                  pointProjection.setProjection(i, buffer);
              }
          }

          for (int d = 0; d < projectionDim; d++) {
              clusterProjection.setMinAllowed(d, 0.0f);
              pointProjection.setMinAllowed(d, 0.0f);
              clusterProjection.setMaxAllowed(d, 1.0f);
              pointProjection.setMaxAllowed(d, 1.0f);
          }
      }

      /**
       * Standardize the projection's orientation to eliminate reflections and
       * rotations caused by minor perturbation of the data. This is done by
       * making the eigenvectors (matrix columns) face in the same general
       * direction as an arbitrary reference vector.
       * 
       * @param projection_matrix
       *            DoubleMatrix2D
       */
      protected static void standardizeOrientation(
                      RealMatrix projection_matrix) {
              int dimensions = projection_matrix.getColumnDimension();

              for (int d = 0; d < dimensions; d++) {
                      // Compute dot product with the reference vector (1, 1, ..., 1),
                      // which was chosen specifically to simplify this step
                      RealVector column = projection_matrix.getColumnVector(d);
                      double dot_product = 0.0;
                      for (int i=0; i<column.getDimension(); i++) {
                          dot_product += column.getEntry(i);
                      }

                      if (dot_product < 0) {
                              // Eigenvector points the wrong way; reverse it
                              int n = column.getDimension();
                              for (int i = 0; i < n; i++) {
                                      projection_matrix.setEntry(i, d, (-1.0 * column.getEntry(i)));
                              }
                      }
              }
      }


}
