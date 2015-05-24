package gov.pnnl.jac.geom.distance;

/**
 * <p>The <tt>DistanceFunc</tt> interface defines entities
 * that compute distances between pairs of coordinates in
 * n-dimensional space.</p>
 *
 * @author R. Scarberry
 * @version 1.0
 */
public interface DistanceFunc extends Cloneable {

    /**
     * Compute the distance between two coordinates.  The coordinates
     * should have equal lengths.
     * @param coord1
     * @param coord2
     * @return
     */
    public double distanceBetween(double[] coord1, double[] coord2);

    /**
     * Returns the name of the distance metric.
     * @return - a String with the name.
     */
    public String methodName();

    /**
     * Returns a clone of the object.
     * @return
     */
    public DistanceFunc clone();

}
