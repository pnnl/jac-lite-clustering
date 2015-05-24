package gov.pnnl.jac.geom.distance;

/**
 * <p>The interface <tt>DistanceFuncFactory</tt> defines 
 * factory entities for producing <tt>DistanceFunc</tt> objects
 * from an object describing the method.</p>
 *
 * @author R. Scarberry
 * @version 1.0
 */
public interface DistanceFuncFactory {

	/**
	 * Produces a <tt>DistanceFunc</tt> given an object
	 * identifying the method.
	 * 
	 * @param method - identifies the distance computation method.
	 * 
	 * @return an instance of a class that implements <tt>DistanceFunc</tt>.
	 * 
	 * @throws IllegalArgumentException - if no <tt>DistanceFunc</tt> is
	 *   defined by the method.
	 */
    DistanceFunc getDistanceFunc(Object method);
    
}
