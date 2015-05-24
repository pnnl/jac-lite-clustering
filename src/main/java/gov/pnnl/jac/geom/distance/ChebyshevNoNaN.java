package gov.pnnl.jac.geom.distance;

/**
 * The Chebyshev distance between two tuples is the maximum difference along
 * any dimension.  The Chebyshev distance is also called the Maximum Metric, or
 * the L-infinity metric.
 * 
 * @author R. Scarberry
 * @since 3.0.0
 *
 */
public class ChebyshevNoNaN extends AbstractDistanceFunc {

	public ChebyshevNoNaN() {}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public double distanceBetween(double[] coord1, double[] coord2) {
        double dist = 0.0;
        final int len = coord1.length;
        for (int i = 0; i < len; i++) {
            double c1 = coord1[i];
            double c2 = coord2[i];
            double diff = Math.abs(c1 - c2);
            if (diff > dist) {
            	dist = diff;
            }
        }
        return dist;
	}

	@Override
	public String methodName() {
		return BasicDistanceMethod.CHEBYSHEV_NO_NAN.toString();
	}
	
	@Override
    public int hashCode() {
        return BasicDistanceMethod.CHEBYSHEV_NO_NAN.name().hashCode();
    }
}
