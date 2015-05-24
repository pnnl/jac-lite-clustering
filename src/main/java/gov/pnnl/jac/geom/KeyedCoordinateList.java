package gov.pnnl.jac.geom;

public interface KeyedCoordinateList<E> extends CoordinateList {

    void setCoordinates(int ndx, Comparable<E> key, double[] coords);
    
    void setCoordinateKey(int ndx, Comparable<E> key);
    
    Comparable<E> getCoordinateKey(int ndx);
    
}
