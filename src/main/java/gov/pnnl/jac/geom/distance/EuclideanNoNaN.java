package gov.pnnl.jac.geom.distance;

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
public class EuclideanNoNaN extends AbstractDistanceFunc {

    public EuclideanNoNaN() {
    }

    public String methodName() {
        return BasicDistanceMethod.EUCLIDEAN_NO_NAN.toString();
    }
    
    public double distanceBetween(double[] coord1, double[] coord2) {
        double distSq = 0.0;
        int dim = coord1.length;
        for (int i = 0; i < dim; i++) {
            double d = coord2[i] - coord1[i];
            distSq += d*d;
        }
        return Math.sqrt(distSq);
    }
    
    public int hashCode() {
    	return BasicDistanceMethod.EUCLIDEAN_NO_NAN.name().hashCode();
    }
}
