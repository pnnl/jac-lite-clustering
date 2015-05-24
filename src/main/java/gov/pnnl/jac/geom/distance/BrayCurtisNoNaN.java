package gov.pnnl.jac.geom.distance;

/**
 * Bray Curtis distance sometimes is also called Sorensen distance is a normalization 
 * method commonly used in the fields of botany, ecology and environmental scence. 
 * It views the space as grid similar to the city block distance. The Bray Curtis distance 
 * has a nice property that if all coordinates are positive, its value is between zero and 
 * one. Zero Bray Curtis distance represents coordinates that exactly match. 
 * However, if both coordinates are at the zero point, the Bray Curtis distance is 
 * technically undefined, but this implementation returns 0.0 for that case.
 * 
 * @author Stuart Rose
 *
 */
public class BrayCurtisNoNaN extends AbstractDistanceFunc {

    public BrayCurtisNoNaN() {}

    public double distanceBetween(double[] coord1, double[] coord2) {
        double sigma_dif = 0.0;
        double sigma_sum = 0.0;

        for (int i = 0; i < coord1.length; i++) {
            double samp1 = 0.0;
            double samp2 = 0.0;
            if (coord1[i] > 0.0) {
                samp1 = coord1[i];
            }
            if (coord2[i] > 0.0) {
                samp2 = coord2[i];
            }

            sigma_dif += Math.abs(samp1 - samp2);
            sigma_sum += samp1 + samp2;
        }

        if (sigma_sum <= 0.0)
            return 0;

        return sigma_dif / sigma_sum;
    }

    public String methodName() {
        return BasicDistanceMethod.BRAYCURTIS_NO_NAN.toString();
    }

    public int hashCode() {
        return BasicDistanceMethod.BRAYCURTIS_NO_NAN.name().hashCode();
    }
}
