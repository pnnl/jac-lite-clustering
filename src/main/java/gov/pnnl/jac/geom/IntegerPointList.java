package gov.pnnl.jac.geom;

public interface IntegerPointList {

    public int getPointCount();
    
    public int getDimensionCount();
    
    public void setPointValue(int ndx, int dim, int value);
    
    public int getPointValue(int ndx, int dim);
    
}
