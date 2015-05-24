package gov.pnnl.jac.geom.distance;

import gov.pnnl.jac.geom.CoordinateMath;

/**
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: </p>
 *
 * @author not attributable
 * @version 1.0
 */
public class Manhattan extends AbstractDistanceFunc {

    private boolean[] mColumnFlags;
    private double[] mColumnReplacementValues;

    public Manhattan(ColumnarDoubles dataSource) {
        super(dataSource);
    }
    
    public Manhattan() {
        this(null);
    }

    public void setDataSource(ColumnarDoubles dataSource) {
        super.setDataSource(dataSource);
        if (mDataSource != null) {
            int n = mDataSource.getColumnCount();
            mColumnFlags = new boolean[n];
            mColumnReplacementValues = new double[n];
        } else {
            mColumnFlags =  null;
            mColumnReplacementValues = null;
        }
    }

    public String methodName() {
        return BasicDistanceMethod.MANHATTAN.toString();
    }
    
    public double distanceBetween(double[] coord1, double[] coord2) {
        // To count the number of columns with NaN in coord1 and/or coord2
        int nanDimensions = 0;
        
        double dist = 0.0;
        final int dim = coord1.length;
        
        for (int i=0; i<dim; i++) {
            double d = coord2[i] - coord1[i];
            if (!Double.isNaN(d)) {
                dist += Math.abs(d);
            } else {
                nanDimensions++;
            }
        }
        
        // d for every column was NaN; dist == 0
        //
        if (nanDimensions == dim) {

            // Make copies, since contents will be modified.
            double[] coord1Copy = (double[]) coord1.clone();
            double[] coord2Copy = (double[]) coord2.clone();

            for (int i=0; i<dim; i++) {

                double d1 = coord1Copy[i];
                double d2 = coord2Copy[i];

                if (Double.isNaN(d1)) {
                    coord1Copy[i] = 0.0;
                    d1 = getNaNReplacement(i);
                }

                if (Double.isNaN(d2)) {
                    coord2Copy[i] = 0.0;
                    d2 = getNaNReplacement(i);
                }

                double d = d1 - d2;
                if (!Double.isNaN(d)) {
                    // Both d1 and d2 were non-NaN.
                    nanDimensions--;
                    dist += Math.abs(d);
                }

            } // for
            
            // d1 and/or d2 were NaN, hence d was NaN for all columns.
            //
            if (nanDimensions == dim) {
                for (int i=0; i<dim; i++) {
                    // NaNs were been replaced by 0s, so can't get
                    // NaN for d.
                    dist += Math.abs(coord1Copy[i] - coord2Copy[i]);
                }
                nanDimensions = 0;
            }
        }
        
        if (nanDimensions > 0) {
            dist *= ((double) dim)/(dim - nanDimensions);
        }
        
        return dist;
    }

    private double getNaNReplacement(int column) {
        Double replacement = Double.NaN;
        if (mDataSource != null) {
            if (mColumnFlags[column]) {
                replacement = mColumnReplacementValues[column];
            } else {
                double[] columnValues = mDataSource.getColumnValues(column, null);
                replacement = CoordinateMath.median(columnValues, true);
                mColumnReplacementValues[column] = replacement;
                mColumnFlags[column] = true;
            }
        }
        return replacement;
    }
    
    public int hashCode() {
        int hc = BasicDistanceMethod.MANHATTAN.name().hashCode();
        return 31*hc + (mDataSource != null ? mDataSource.hashCode() : 0);
    }
    
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o != null && this.getClass() == o.getClass()) {
            ColumnarDoubles otherDataSource = ((Manhattan) o).getDataSource();
            return (this.mDataSource == null && otherDataSource == null ||
                    this.mDataSource != null && this.mDataSource.equals(otherDataSource));
        }
        return false;
    }
}
