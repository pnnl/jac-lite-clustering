package gov.pnnl.jac.geom;

import java.util.Arrays;

import cern.colt.list.DoubleArrayList;

/**
 * <p>A implementation of <tt>CoordinateList</tt> which maintains
 * the coordinate data in an dynamically-growing array in memory. 
 * Thus, the number of coordinates can grow in number.</p>
 * 
 * @author R. Scarberry
 * @version 1.0
 */
public class DynamicCoordinateList extends AbstractCoordinateList {

        private DoubleArrayList mCoordData = new DoubleArrayList();

        /**
         * Constructs a new <tt>DynamicCoordinateList</tt> with all values initialized to
         * zero.
         * @param dimensions the number of dimensions.
         * @param coordinateCount the number of coordinates.
         */
        public DynamicCoordinateList(int dimensions) {
                if (dimensions < 0) {
                        throw new IllegalArgumentException("dimensions < 0: " + dimensions);
                }
                mDim = dimensions;
        }
        
        public double[] getCoordinateData() {
            int sz = mCoordData.size();
            double[] data = new double[sz];
            System.arraycopy(mCoordData.elements(), 0, data, 0, sz);
            return data;
        }
        
        public void addCoordinates(double[] coords) {
            checkDimensions(coords.length);
            for (int i=0; i<coords.length; i++) {
                mCoordData.add(coords[i]);
            }
            mCount++;
        }
        
        public void setCoordinates(int ndx, double[] coords) {
                if (ndx >= mCount) {
                    double[] temp = new double[getDimensionCount()];
                    Arrays.fill(temp, Double.NaN);
                    while(ndx >= mCount) {
                        addCoordinates(temp);
                    }
                }
                checkIndex(ndx);
                checkDimensions(coords.length);
                int start = ndx*mDim;
                for (int i=0; i<mDim; i++) {
                    mCoordData.setQuick(start + i, coords[i]);
                }
        }

        public double[] getCoordinates(int ndx, double[] coords) {
                checkIndex(ndx);
                double[] c = null;
                if (coords != null) {
                        checkDimensions(coords.length);
                        c = coords;
                } else {
                        c = new double[mDim];
                }
                int start = ndx*mDim;
                for (int i=0; i<mDim; i++) {
                    c[i] = mCoordData.getQuick(start + i);
                }
                return c;
        }
    
        public void setCoordinateQuick(int ndx, int dim, double coord) {
            mCoordData.setQuick(ndx * mDim + dim, coord);
        }

        public double[] getDimensionValues(int dim, double[] values) {
                checkDimension(dim);
                double[] v = null;
                if (values != null) {
                        if (values.length != mCount) {
                                throw new IllegalArgumentException(String
                                                .valueOf(values.length)
                                                + " != " + mCount);
                        }
                        v = values;
                } else {
                        v = new double[mCount];
                }
                int ndx = dim;
                for (int i = 0; i < mCount; i++) {
                        v[i] = mCoordData.getQuick(ndx);
                        ndx += mDim;
                }
                return v;
        }

        public double getCoordinateQuick(int ndx, int dim) {
                return mCoordData.getQuick(ndx * mDim + dim);
        }

        public double[] computeAverage(int[] indices, double[] avg) {
                checkIndices(indices);
                double[] rtn = null;
                if (avg != null) {
                        checkDimensions(avg.length);
                        rtn = avg;
                } else {
                        rtn = new double[mDim];
                }
                java.util.Arrays.fill(rtn, 0.0);
                int[] counts = new int[mDim];
                int n = indices.length;
                for (int i = 0; i < n; i++) {
                        int ndx = indices[i];
                        int start = ndx * mDim;
                        for (int d = 0; d < mDim; d++) {
                                double dv = mCoordData.getQuick(start + d);
                                if (!Double.isNaN(dv)) {
                                        rtn[d] += dv;
                                        counts[d]++;
                                }
                        }
                }
                for (int d = 0; d < mDim; d++) {
                        int ct = counts[d];
                        if (ct >= 1) {
                                rtn[d] /= ct;
                        } else {
                                // No information in dimension d.
                                rtn[d] = Double.NaN;
                        }
                }
                return rtn;
        }
}
