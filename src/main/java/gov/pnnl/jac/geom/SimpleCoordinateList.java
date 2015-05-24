package gov.pnnl.jac.geom;

import java.io.*;
import java.util.*;

/**
 * <p>A simple implementation of <tt>CoordinateList</tt> which maintains
 * the coordinate data in an array in memory.</p>
 * 
 * @author R. Scarberry
 * @version 1.0
 */
public class SimpleCoordinateList extends AbstractCoordinateList {

    private double[] mCoords;

    /**
     * Constructs a new <tt>SimpleCoordinateList</tt> with all values initialized to
     * zero.
     * @param dimensions the number of dimensions.
     * @param coordinateCount the number of coordinates.
     */
    public SimpleCoordinateList(int dimensions, int coordinateCount) {
        if (dimensions < 0) {
            throw new IllegalArgumentException("dimensions < 0: " + dimensions);
        }
        if (coordinateCount < 0) {
            throw new IllegalArgumentException("coordinateCount < 0: "
                    + coordinateCount);
        }
        mDim = dimensions;
        mCount = coordinateCount;
        mCoords = new double[mDim * mCount];
    }

    /**
     * Constructs a new <tt>SimpleCoordinateList</tt> using the specified array of 
     * coordinate values.  The parameter <tt>allCoords</tt> is not copied, so any changes 
     * made directly to this array will affect the coordinate set. 
     * 
     * @param dimensions the number of dimensions.
     * @param coordinateCount the number of coordinates.
     * @param allCoords an array containing the coordinate values, which should be of length
     *   dimensions * coordinateCount.
     * 
     * @throws IllegalArgumentException if either dimensions or coordinateCount is negative, or 
     *   if allCoords.length is not equal to the product of the dimensions and the coordinates.
     */
    public SimpleCoordinateList(int dimensions, int coordinateCount,
            double[] allCoords) {
        if (dimensions < 0) {
            throw new IllegalArgumentException("dimensions < 0: " + dimensions);
        }
        if (coordinateCount < 0) {
            throw new IllegalArgumentException("coordinateCount < 0: "
                    + coordinateCount);
        }
        if (allCoords.length != dimensions * coordinateCount) {
            throw new IllegalArgumentException(
                    "invalid number of coordinate values: " + allCoords.length
                            + " != " + (dimensions * coordinateCount));
        }
        mDim = dimensions;
        mCount = coordinateCount;
        mCoords = allCoords;
    }

    /**
     * Creates and loads a new coordinate set from the specified input.
     * 
     * @param in
     * @return a new <tt>SimpleCoordinateList</tt> instance.
     * 
     * @throws IOException if an instance of the coordinate list cannot be
     *   successfully read from the input.
     */
    public static SimpleCoordinateList load(DataInput in) throws IOException {
        int dimensions = in.readInt();
        int count = in.readInt();
        if (dimensions < 0 || count < 0) {
            throw new IOException("invalid dimensions: " + count + " by "
                    + dimensions);
        }
        SimpleCoordinateList coords = new SimpleCoordinateList(dimensions,
                count);
        for (int coord = 0; coord < count; coord++) {
            for (int dim = 0; dim < dimensions; dim++) {
                coords.setCoordinateQuick(coord, dim, in.readDouble());
            }
        }
        return coords;
    }

    /**
     * Creates and loads a new coordinate set from the specified file.
     * 
     * @param in
     * @return a new <tt>SimpleCoordinateList</tt> instance.
     * 
     * @throws IOException if an instance of the coordinate list cannot be
     *   successfully read from the file.
     */
    public static SimpleCoordinateList load(File f) throws IOException {
        DataInputStream dis = null;
        try {
            dis = new DataInputStream(new BufferedInputStream(
                    new FileInputStream(f)));
            return load(dis);
        } finally {
            if (dis != null) {
                try {
                    dis.close();
                } catch (IOException ioe) {
                }
            }
        }
    }

    public void save(DataOutput out) throws IOException {
        out.writeInt(mDim);
        out.writeInt(mCount);
        final int numDoubles = mDim * mCount;
        for (int i=0; i<numDoubles; i++) {
            out.writeDouble(mCoords[i]);
        }
    }

    public void save(File f) throws IOException {
        DataOutputStream dos = null;
        try {
            dos = new DataOutputStream(new BufferedOutputStream(
                    new FileOutputStream(f)));
            save(dos);
            dos.flush();
        } finally {
            if (dos != null) {
                try {
                    dos.close();
                } catch (IOException ioe) {
                }
            }
        }
    }

    public void setCoordinates(int ndx, double[] coords) {
        checkIndex(ndx);
        checkDimensions(coords.length);
        System.arraycopy(coords, 0, mCoords, ndx * mDim, mDim);
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
        System.arraycopy(mCoords, ndx * mDim, c, 0, mDim);
        return c;
    }

    public void setCoordinateQuick(int ndx, int dim, double coord) {
        mCoords[ndx * mDim + dim] = coord;
    }
    
    /**
     * Returns a complete copy of the backing array.
     * 
     * @return
     */
    public double[] getAllCoordinateData() {
    	final int n = mCoords.length;
    	double[] rtn = new double[n];
    	System.arraycopy(mCoords, 0, rtn, 0, n);
    	return rtn;
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
            v[i] = mCoords[ndx];
            ndx += mDim;
        }
        return v;
    }

    public double getCoordinateQuick(int ndx, int dim) {
        return mCoords[ndx * mDim + dim];
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
                double dv = mCoords[start + d];
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
