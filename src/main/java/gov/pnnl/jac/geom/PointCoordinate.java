package gov.pnnl.jac.geom;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

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
public class PointCoordinate implements java.io.Serializable {

	private static final long serialVersionUID = -5160965538445803972L;
	
	public double[] mCoords;

    public PointCoordinate(double[] coords) {
        mCoords = new double[coords.length];
        System.arraycopy(coords, 0, mCoords, 0, coords.length);
    }

    public int getDimensions() {
        return mCoords.length;
    }

    public double getCoordinate(int dim) {
        return mCoords[dim];
    }

    public int hashCode() {
        int hc = 17;
        int dim = mCoords.length;
        for (int i=0; i<dim; i++) {
          long bits = Double.doubleToLongBits(mCoords[i]);
          hc = 37*hc + (int) (bits ^ (bits >>> 32));
        }
        return hc;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof PointCoordinate) {
            PointCoordinate other = (PointCoordinate) o;
            int dim = this.mCoords.length;
            if (other.mCoords.length == dim) {
                for (int i=0; i<dim; i++) {
                    if (Double.doubleToLongBits(this.mCoords[i]) !=
                        Double.doubleToLongBits(other.mCoords[i])) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }


}
