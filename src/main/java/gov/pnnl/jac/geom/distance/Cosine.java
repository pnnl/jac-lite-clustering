package gov.pnnl.jac.geom.distance;

import gov.pnnl.jac.geom.*;

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
public final class Cosine extends AbstractDistanceFunc {

    public Cosine() {
    }

    public String methodName() {
        return BasicDistanceMethod.COSINE.toString();
    }
    
    public double distanceBetween(double[] coord1, double[] coord2) {

        // The maximum of the absolute vals of the coords.
        double maxA = Math.max(
                CoordinateMath.absMax(coord1),
                CoordinateMath.absMax(coord2)); 
        
        final int dim = coord1.length;

        double cos = 1.0;
        double sx = 0.0, sy = 0.0, sxy = 0.0;

        if (maxA > 0.0) {
            for (int i=0; i<dim; i++) {
                double dx = coord1[i];
                double dy = coord2[i];
                if (!Double.isNaN(dx) && !Double.isNaN(dy)) {
                    dx /= maxA;
                    dy /= maxA;
                    sx += dx*dx;
                    sy += dy*dy;
                    sxy += dx*dy;
                }
            }
            if (sxy != 0.0) {
                cos = sxy/Math.sqrt(sx*sy);
            }
        }

        return 1.0 - Math.abs(cos);
    }

    public int hashCode() {
    	return BasicDistanceMethod.COSINE.name().hashCode();
    }
}
