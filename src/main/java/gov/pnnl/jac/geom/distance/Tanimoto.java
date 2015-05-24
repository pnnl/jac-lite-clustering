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
public class Tanimoto extends AbstractDistanceFunc {

    private double[] mWorking1, mWorking2;
    private double[] mMinColumnValues, mMaxColumnValues;
    private boolean[] mColumnFlags;
    
    public Tanimoto() {
    }

    public String methodName() {
        return BasicDistanceMethod.TANIMOTO.toString();
    }
    
    public void setDataSource(ColumnarDoubles dataSource) {
        super.setDataSource(dataSource);
        if (mDataSource != null) {
            int dim = mDataSource.getColumnCount();
            mMinColumnValues = new double[dim];
            mMaxColumnValues = new double[dim];
            mColumnFlags = new boolean[dim];
        } else {
            mMinColumnValues = null;
            mMaxColumnValues = null;
            mColumnFlags = null;
        }
    }
    
    public double distanceBetween(double[] coord1, double[] coord2) {
        
        final int dim = coord1.length;

        if (mWorking1 == null || mWorking1.length != dim) {
            mWorking1 = (double[]) coord1.clone();
            mWorking2 = (double[]) coord2.clone();
        }
        
        if (mDataSource != null && dim == mDataSource.getColumnCount()) {
            for (int i=0; i<dim; i++) {
                double min = 0, max = 0;
                if (mColumnFlags[i]) {
                    min = mMinColumnValues[i];
                    max = mMaxColumnValues[i];
                } else {
                    double[] values = mDataSource.getColumnValues(i, null);
                    min = mMinColumnValues[i] = CoordinateMath.min(values);
                    max = mMaxColumnValues[i] = CoordinateMath.max(values);
                    mColumnFlags[i] = true;
                }
                double d = max - min;
                if (!Double.isNaN(d)) {
                   if (min != 0) {
                       mWorking1[i] -= min;
                       mWorking2[i] -= min;
                   }
                   if (d > 0.0) {
                       mWorking1[i] /= d;
                       mWorking2[i] /= d;
                   }
                }
            } // for
        }
        
        for (int i=0; i<dim; i++) {
            if (Double.isNaN(mWorking1[i])) mWorking1[i] = 0.0;
            if (Double.isNaN(mWorking2[i])) mWorking2[i] = 0.0;
        }
        
        return _distanceBetween(mWorking1, mWorking2);
    }
    
    private double _distanceBetween(double[] coord1, double[] coord2) {
        int dim = coord1.length;
        double snum = 0.0;
        double sdenom = 0.0;
        for (int i=0; i<dim; i++) {
            double x = coord1[i];
            double y = coord2[i];
            double xy = x*y;
            snum += xy;
            sdenom += (x*x + y*y - xy);
        }
        return sdenom != 0.0 ? 1.0 - snum/sdenom : 0.0;
    }

    public int hashCode() {
        int hc = BasicDistanceMethod.TANIMOTO.name().hashCode();
        return 31*hc + (mDataSource != null ? mDataSource.hashCode() : 0);
    }
    
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o != null && this.getClass() == o.getClass()) {
            ColumnarDoubles otherDataSource = ((Tanimoto) o).getDataSource();
            return (this.mDataSource == null && otherDataSource == null ||
                    this.mDataSource != null && this.mDataSource.equals(otherDataSource));
        }
        return false;
    }
}
