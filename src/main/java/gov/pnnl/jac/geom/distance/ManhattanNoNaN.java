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
public class ManhattanNoNaN extends AbstractDistanceFunc {

    public ManhattanNoNaN() {
    }

    public String methodName() {
        return BasicDistanceMethod.MANHATTAN_NO_NAN.toString();
    }
    
    public double distanceBetween(double[] coord1, double[] coord2) {
        double dist = 0.0;
        int dim = coord1.length;
        for (int i=0; i<dim; i++) {
            dist += Math.abs(coord2[i] - coord1[i]);
        }
        return dist;
    }

    public int hashCode() {
    	return BasicDistanceMethod.MANHATTAN_NO_NAN.name().hashCode();
    }
}
