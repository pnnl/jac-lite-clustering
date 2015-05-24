package gov.pnnl.jac.collections;

/**
 * <p>Extension of <tt>IntIntMap</tt> which defines 2-way, 1:1 correspondence
 * maps between int values.</p>
 * 
 * @author R. Scarberry
 *
 */
public interface TwoWayIntIntMap extends IntIntMap {
    
	/**
	 * Returns the key, if any, associated with the given value.  
	 * If no key is associated with the value, -1 is returned.  If values of
	 * -1 are to be stored, call <tt>containsValue(value)</tt> before using this
	 * method.
	 * 
	 * @param value
	 * 
	 * @return the key associated with the value, or -1 if the value is not
	 *   contained in the map.
	 */
    public int getKey(int value);

    /**
     * Returns an iterator over the values of this map.
     * 
     * @return IntCollectionIterator
     */    
    public IntCollectionIterator valueIterator();
}
