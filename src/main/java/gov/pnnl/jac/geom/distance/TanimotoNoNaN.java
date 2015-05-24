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
public class TanimotoNoNaN extends AbstractDistanceFunc {

    public TanimotoNoNaN() {
    }

    public String methodName() {
        return BasicDistanceMethod.TANIMOTO_NO_NAN.toString();
    }
    
    public double distanceBetween(double[] coord1, double[] coord2) {
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
    	return BasicDistanceMethod.TANIMOTO_NO_NAN.name().hashCode();
    }
}
