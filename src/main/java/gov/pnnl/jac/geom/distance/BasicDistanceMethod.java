package gov.pnnl.jac.geom.distance;

/**
 * <p>Enumeration which defines common distance function methods.  Used with
 * <tt>BasicDistanceFuncFactory</tt> to produce <tt>DistanceFunc</tt>s.</p>
 *
 * @author R. Scarberry
 * @version 1.0
 */
public enum BasicDistanceMethod {

	// !!! NOTE: !!!
	// If any new methods are added here, also modify BasicDistanceFuncFactory.getDistanceFunc().
    EUCLIDEAN_NO_NAN("Euclidean (NaN not supported)"),
    MANHATTAN_NO_NAN("Manhattan (NaN not supported)"),
    COSINE("Cosine"),
    TANIMOTO_NO_NAN("Tanimoto (NaN not supported)"),
    BRAYCURTIS_NO_NAN("Bray-Curtis (NaN not supported)"),
    CANBERRA_NO_NAN("Canberra (NaN not supported)"),
    CHEBYSHEV_NO_NAN("Chebyshev (NaN not supported)"),
    // Ones supporting NaN elements in the coordinates:
    EUCLIDEAN("Euclidean"),
    MANHATTAN("Manhattan"),
    TANIMOTO("Tanimoto");

    private String mFriendlyName;
    
    private BasicDistanceMethod(String friendlyName) {
    	mFriendlyName = friendlyName;
    }
    
    /**
     * Generates a <tt>DistanceFunc</tt> from this method using
     * a <tt>BasicDistanceFuncFactory</tt>.
     * @return - an instance of <tt>DistanceFunc</tt>.
     */
    public DistanceFunc newFunction() {
        return newFunction(null);
    }
    
    /**
     * Generates a <tt>DistanceFunc</tt> from this method using
     * a <tt>BasicDistanceFuncFactory</tt> and the given data source.
     * 
     * @return - an instance of <tt>DistanceFunc</tt>.
     */
    public DistanceFunc newFunction(ColumnarDoubles dataSource) {
        return new BasicDistanceFuncFactory(dataSource).getDistanceFunc(this);
    }
    
    public String toString() {
    	return mFriendlyName;
    }
}
