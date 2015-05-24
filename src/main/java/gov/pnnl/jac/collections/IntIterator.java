package gov.pnnl.jac.collections;

/**
 * <p>Interface that defines iterators over a 
 * group of integer primitives. Do not confuse this 
 * interface with <tt>IntCollectionIterator</tt> and
 * <tt>IntListIterator</tt> which are used with
 * the <tt>IntCollection</tt>s and <tt>IntList</tt> 
 * collection interfaces and their implementations.</p>
 * 
 * @author R. Scarberry
 *
 */
public interface IntIterator extends Cloneable {

	/**
	 * Positions the iterator to the start of the iteration.
	 */
	public void gotoFirst();
	
	/**
	 * Positions the iterator to the end of the iteration.
	 */
	public void gotoLast();
	
	/**
	 * Positions the iterator to the start of the iteration and 
	 * returns the first element.  This method should only be
	 * used when certain that the iterator contains at least one
	 * element.
	 * 
	 * @return - the first int element.
	 * 
	 * @throws IndexOutOfBoundsException - if the iterator contains
	 *   no elements.
	 */
	public int getFirst();
	
	/**
	 * Positions the iterator to the end of the iteration and 
	 * returns the last element.  This method should only be
	 * used when certain that the iterator contains at least one
	 * element.
	 * 
	 * @return - the last int element.
	 * 
	 * @throws IndexOutOfBoundsException - if the iterator contains
	 *   no elements.
	 */
	public int getLast();
	
	/**
	 * Get the next element in the iteration.  Only call this method
	 * when <tt>hasNext()</tt> returns true.
	 * 
	 * @return
	 */
	public int getNext();
	
	/**
	 * Get the previous element in the iteration.  Only call this method
	 * when <tt>hasPrev()</tt> returns true.
	 * 
	 * @return
	 */
	public int getPrev();
	
	/**
	 * Does the iteration contain another element?
	 * @return
	 */
	public boolean hasNext();
	
	/**
	 * Does the iteration contain an element previous to its current position?
	 * @return
	 */
	public boolean hasPrev();
	
	/**
	 * Returns the number of elements in the iteration.
	 * @return
	 */
	public int size();
	
	/**
	 * Returns an array containing all element in the iteration.
	 * @return
	 */
	public int[] toArray();
	
	/**
	 * Returns a deep clone of the iterator.
	 * @return
	 */
	public Object clone();
}
