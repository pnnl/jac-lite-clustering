package gov.pnnl.jac.geom;

/**
 * <p>Class to represent a hyper-rectangle in N-space.</p>
 *
 * @author R. Scarberry
 * @version 1.0
 */
public class HyperRect implements Cloneable {

    private double[] mMinCorner;
    private double[] mMaxCorner;

    /**
     * Constructs a hyper-rectangle with zero volume in
     * hyper-space. Both corners are initialized to (0,0,...,0)
     * 
     * @param dim - the dimensionality of the hyper-rectangle.
     */
    public HyperRect(int dim) {
        mMinCorner = new double[dim];
        mMaxCorner = new double[dim];
    }

    /**
     * <p>Constructs a hyper-rectangle with the specified minimum and maximum 
     * corner points.  For example, if the dimensionality is 2, minCorner is the
     * lower-left corner and maxCorner is the upper-right.</p>
     * <p>The constructor rearranges the values in minCorner and maxCorner to
     * ensure that each element of minCorner is less than or equal to the
     * corresponding element of maxCorner.</p> 
     * 
     * @param minCorner
     * @param maxCorner
     * 
     * @throws IllegalArgumentException - if minCorner and maxCorner do not
     *   have the same length.
     */
    public HyperRect(double[] minCorner, double[] maxCorner) {
        int dim = minCorner.length;
        if (dim != maxCorner.length) {
            throw new IllegalArgumentException("inconsistent dimensions: "
                                               + dim
                                               + " != " + maxCorner.length);
        }
        mMinCorner = new double[dim];
        mMaxCorner = new double[dim];
        // Ensures corners really are the min/max corners.
        for (int i=0; i<dim; i++) {
            double d1 = minCorner[i];
            double d2 = maxCorner[i];
            if (d1 < d2) {
                mMinCorner[i] = d1;
                mMaxCorner[i] = d2;
            } else {
                mMinCorner[i] = d2;
                mMaxCorner[i] = d1;
            }
        }
    }

    /**
     * Is this hyper-rectangle actually a point?  That is, are the min and max
     * vertices the same?
     * @return boolean
     */
    public boolean isPoint() {
        int dim = mMinCorner.length;
        for (int i=0; i<dim; i++) {
            if (mMinCorner[i] != mMaxCorner[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the index of the dimension having the smallest difference
     * between the minimum vertex and maximum vertex point.
     * 
     * @return
     */
    public int dimensionOfMinWidth() {
        int dim = mMinCorner.length;
        int minDim = 0;
        if (dim > 0) {
            double minWidth = mMaxCorner[0] - mMinCorner[0];
            for (int d=1; d<dim; d++) {
                double width = mMaxCorner[d] - mMinCorner[d];
                if (width < minWidth) {
                    minWidth = width;
                    minDim = d;
                }
            }
        }
        return minDim;
    }

    /**
     * Returns the index of the dimension having the largest difference
     * between the minimum vertex and maximum vertex point.
     * 
     * @return
     */
    public int dimensionOfMaxWidth() {
        int dim = mMinCorner.length;
        int maxDim = 0;
        if (dim > 0) {
            double maxWidth = mMaxCorner[0] - mMinCorner[0];
            for (int d=1; d<dim; d++) {
                double width = mMaxCorner[d] - mMinCorner[d];
                if (width > maxWidth) {
                    maxWidth = width;
                    maxDim = d;
                }
            }
        }
        return maxDim;
    }

    /**
     * Sets one of the minimum vertex values.
     * 
     * @param n - the dimension index for the value to be changed.
     * @param value - the new value.
     * 
     * @throws IllegalArgumentException - if the value is greater than
     *   the corresponding maximum vertex element.
     */
    public void setMinCornerCoord(int n, double value) {
        if (value <= mMaxCorner[n]) {
            mMinCorner[n] = value;
        } else {
            throw new IllegalArgumentException("exceeds max corner coordinate: "
                                               + value + " > " + mMaxCorner[n]);
        }
    }

    /**
     * Returns the element of the minimum vertex point for the specified dimension.
     * @param n - the index of the dimension.
     * @return
     */
    public double getMinCornerCoord(int n) {
        return mMinCorner[n];
    }

    /**
     * Sets one of the maximum vertex values.
     * 
     * @param n - the dimension index for the value to be changed.
     * @param value - the new value.
     * 
     * @throws IllegalArgumentException - if the value is less than
     *   the corresponding maximum vertex element.
     */
    public void setMaxCornerCoord(int n, double value) {
        if (value >= mMinCorner[n]) {
            mMaxCorner[n] = value;
        } else {
            throw new IllegalArgumentException("less that min corner coordinate: "
                                               + value + " < " + mMinCorner[n]);
        }
    }

    /**
     * Returns the element of the maximum vertex point for the specified dimension.
     * @param n - the index of the dimension.
     * @return
     */
    public double getMaxCornerCoord(int n) {
        return mMaxCorner[n];
    }

    public Object clone() {
        return new HyperRect(mMinCorner, mMaxCorner);
    }

    /**
     * Returns the closest point on the surface or within
     * the hyper-rectangle to the specified point. If the point is
     * within the rectangle, a clone of the point itself is returned.
     * 
     * @param point
     * @return
     */
    public double[] closestPoint(double[] point) {
        int dim = point.length;
        checkDimension(dim);
        double[] closest = new double[dim];
        for (int i=0; i<dim; i++) {
            double d = point[i];
            if (d < mMinCorner[i]) {
                closest[i] = mMinCorner[i];
            } else if (d > mMaxCorner[i]) {
                closest[i] = mMaxCorner[i];
            } else {
                closest[i] = d;
            }
        }
        return closest;
    }

    /**
     * Returns true if the specified point is contained within or
     * on the surface of the hyper-rectangle.
     * 
     * @param point
     * @return
     */
    public boolean contains(double[] point) {
        int dim = point.length;
        checkDimension(dim);
        for (int i=0; i<dim; i++) {
            double d = point[i];
            if (d < mMinCorner[i] || d > mMaxCorner[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the dimensionality of the hyper-rectangle.
     * @return
     */
    public int getDimension() {
        return mMinCorner.length;
    }

    /**
     * Utility method which returns a hyper-rectangle of infinite volume. 
     * The elements of the minimum vertex are all set to Double.NEGATIVE_INFINITY
     * and all element of the maximum vertex are set to Double.POSITIVE_INFINITY.
     * 
     * @param dim
     * @return
     */
    public static HyperRect infiniteHyperRect(int dim) {
        HyperRect hrect = new HyperRect(dim);
        java.util.Arrays.fill(hrect.mMinCorner, Double.NEGATIVE_INFINITY);
        java.util.Arrays.fill(hrect.mMaxCorner, Double.POSITIVE_INFINITY);
        return hrect;
    }

    /**
     * Returns the intersection of this hyper-rectangle with another,
     * if the two hyper-rectangles intersect.
     * @param other 
     * @return - the intersection, or null if the hyper-rectangles do not intersect.
     */
    public HyperRect intersectionWith(HyperRect other) {
        int dim = other.getDimension();
        checkDimension(dim);
        HyperRect intersection = new HyperRect(dim);
        double[] minCorner = intersection.mMinCorner;
        double[] maxCorner = intersection.mMaxCorner;
        for (int i=0; i<dim; i++) {
            minCorner[i] = Math.max(this.mMinCorner[i], other.mMinCorner[i]);
            maxCorner[i] = Math.min(this.mMaxCorner[i], other.mMaxCorner[i]);
            if (minCorner[i] >= maxCorner[i]) {
                return null;
            }
        }
        return intersection;
    }

    /**
     * Returns true if this hyper-rectangle intersects with the other.
     * @param other
     * @return
     */
    public boolean intersectsWith(HyperRect other) {
        int dim = other.getDimension();
        checkDimension(dim);
        for (int i=0; i<dim; i++) {
            if(Math.max(this.mMinCorner[i], other.mMinCorner[i]) >=
               Math.min(this.mMaxCorner[i], other.mMaxCorner[i])) {
              return false;
            }
        }
        return true;
    }

    /**
     * Returns the volume of the hyper-rectangle.  The volume is the product
     * of all the dimension widths.
     * @return
     */
    public double volume() {
        double v = 0.0;
        int dim = mMinCorner.length;
        for (int i=0; i<dim; i++) {
            v *= mMaxCorner[i] - mMinCorner[i];
        }
        return v;
    }

    // Check that dim equals the dimension of this hyper-rectangle.
    private void checkDimension(int dim) {
        if (dim != mMinCorner.length) {
            throw new IllegalArgumentException("wrong number of dimensions: " +
                                               dim + " != " + mMinCorner.length);
        }
    }
}
