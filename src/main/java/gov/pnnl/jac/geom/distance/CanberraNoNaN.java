package gov.pnnl.jac.geom.distance;

/**
 * <p>The Canberra distance is the sum of of the fractional differences between 
 * each pair of coordinate elements. Each fractional difference has value between 0 and 1. 
 * If one of the coordinate elements is zero, the term is 1.0 regardless of the corresponding
 * element from the other coordinate. If both coordinate elements are 0, the
 * fractional difference is regarded as 0. The Canberra distance is very sensitive to small changes 
 * when both coordinates are near zero.</p> 
 * 
 * @author R.Scarberry
 *
 */
public class CanberraNoNaN extends AbstractDistanceFunc {

    public CanberraNoNaN() {
    }

    public String methodName() {
        return BasicDistanceMethod.CANBERRA_NO_NAN.toString();
    }

    public double distanceBetween(double[] coord1, double[] coord2) {
        double dist = 0.0;
        int len = coord1.length;
        for (int i = 0; i < len; i++) {
            double c1 = coord1[i];
            double c2 = coord2[i];
            double num = Math.abs(c1 - c2);
            double denom = Math.abs(c1) + Math.abs(c2);
            if (denom > 0.0) {
                dist += num / denom;
            }
        }
        return dist;
    }

    public int hashCode() {
        return BasicDistanceMethod.CANBERRA_NO_NAN.name().hashCode();
    }
}
