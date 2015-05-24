package gov.pnnl.jac.geom.distance;

/**
 * <p>A basic implementation of <tt>DistanceFuncFactory</tt> which uses
 * instances of the enumeration <tt>BasicDistanceMethod</tt> to identify
 * the <tt>DistanceFunc</tt>s to produce.  The <tt>getDistanceFunc()</tt>
 * method also takes <tt>String</tt> arguments if the strings can 
 * successfully be parsed into <tt>BasicDistanceMethod</tt>s.</p>
 *
 * @author R. Scarberry
 * @version 1.0
 */
public class BasicDistanceFuncFactory implements DistanceFuncFactory {

	// The data source, which may be null.
	private ColumnarDoubles mDataSource;
	
	/**
	 * Constructor.
	 */
    public BasicDistanceFuncFactory(ColumnarDoubles dataSource) {
        mDataSource = dataSource;
    }
    
    public BasicDistanceFuncFactory() {
    	this(null);
    }
    
    /**
     * Attempts to return a <tt>DistanceFunc</tt> for the 
     * specified method.  If method is not an instance of 
     * <tt>BasicDistanceFuncMethod</tt>, a <tt>BasicDistanceMethod</tt> is
     * parsed from <tt>method.toString()</tt>.  Then, the distance function
     * is interpreted from the instance of <tt>BasicDistanceMethod</tt>
     * @return - a <tt>DistanceFunc</tt> instance.
     * @throws IllegalArgumentException - if no <tt>DistanceFunc</tt> is defined by
     *   the method.
     */
    public DistanceFunc getDistanceFunc(Object method) {
    	BasicDistanceMethod methodObj = null;
    	if (method instanceof BasicDistanceMethod) {
    		methodObj = (BasicDistanceMethod) method;
    	} else {
    		String methodName = method.toString();
    		try {
    			methodObj = BasicDistanceMethod.valueOf(methodName);
    		} catch (IllegalArgumentException iae) {
    			// Ignore
    		}
    		if (methodObj == null) {
    			// Try the friendly names.
    			BasicDistanceMethod[] methods = BasicDistanceMethod.values();
    			for (int i=0; i<methods.length; i++) {
    				if (methodName.equals(methods[i].toString())) {
    					methodObj = methods[i];
    					break;
    				}
    			}
    		}
    	}
    	if (methodObj != null) {
    		switch (methodObj) {
        		case EUCLIDEAN_NO_NAN:
        			return new EuclideanNoNaN();	
        		case MANHATTAN_NO_NAN:
        			return new ManhattanNoNaN();
        		case COSINE:
        			return new Cosine();
        		case TANIMOTO_NO_NAN:
        			return new TanimotoNoNaN();
        		case BRAYCURTIS_NO_NAN:
        			return new BrayCurtisNoNaN();
        		case CANBERRA_NO_NAN:
        			return new CanberraNoNaN();
        		case CHEBYSHEV_NO_NAN:
        			return new ChebyshevNoNaN();
        		case EUCLIDEAN:
        		        return new Euclidean();
        		case MANHATTAN:
        		        return new Manhattan();
        		case TANIMOTO:
        		        return new Tanimoto();
    		}
        }
        throw new IllegalArgumentException("no distance function defined for " + method);
    }
}
