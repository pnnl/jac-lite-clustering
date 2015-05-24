package gov.pnnl.jac.geom.distance;

public abstract class AbstractDistanceFunc implements DistanceFunc {

    protected ColumnarDoubles mDataSource;
    
    public AbstractDistanceFunc(ColumnarDoubles dataSource) {
        setDataSource(dataSource);
    }
    
    public AbstractDistanceFunc() {
        this(null);
    }
    
    public void setDataSource(ColumnarDoubles dataSource) {
        mDataSource = dataSource;
    }
    
    public ColumnarDoubles getDataSource() {
        return mDataSource;
    }
    
    public DistanceFunc clone() {
        AbstractDistanceFunc clone = null;
        try {
            clone = (AbstractDistanceFunc) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
        return clone;
    }
    
    public boolean equals(Object o) {
    	return o != null && this.getClass() == o.getClass(); 
    }
}
