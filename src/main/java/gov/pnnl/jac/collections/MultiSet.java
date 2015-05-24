package gov.pnnl.jac.collections;

import java.util.Collection;

public interface MultiSet<E> extends ObjectIntMap<E> {

	/**
	 * Returns the number of elements (keys) in the set
	 * which have counts greater than zero.
	 */
	int size();
	
	/**
	 * Increments the count associated with the given 
	 * element by 1.
	 * 
	 * @param e
	 * 
	 * @return the updated count.
	 */
	int add(E e);
	
	/**
	 * Increments the count associated with the given element 
	 * by the specified amount
	 * 
	 * @param e
	 * @param howMany
	 * 
	 * @return the new count after the addition.
	 * 
	 * @IllegalArgumentException if howMany is negative.
	 */
	int add(E e, int howMany);
	
	/**
	 * Sets the count for the specified element to the specified amount
	 * 
	 * @param e
	 * @param count
	 * 
	 * @return the new count, which should always be count.
	 * 
	 * @IllegalArgumentException if count is negative.
	 */
	int setCount(E e, int count);
	
	/**
	 * Sets the count for the specified elements to newCount, but
	 * only if the current count equals expectedCount.
	 * 
	 * @param e
	 * @param newCount
	 * @param expectedCount
	 * 
	 * @return true if the count was set to newCount.
	 * 
	 * @IllegalArgumentException if newCount is negative.
	 */
	boolean setCount(E e, int newCount, int expectedCount);
	
	/**
	 * Returns the count associated with the element.
	 * 
	 * @param e
	 * 
	 * @return the count, or zero if the element is not in the multiset.
	 */
	int count(E e);
	
	/**
	 * Returns the cumulative counts of all elements in the multiset.
	 * @return
	 */
	int totalCount();
	
	/**
	 * Removes one instance of the element.
	 * 
	 * @return the count after the removal. A return of zero could mean either 
	 *   that the count before the call was 1 or that the element was
	 *   not in the multiset.
	 */
	int remove (E e);
	
	/**
	 * Subtracts the specified number of occurrences for the specified element.
	 * If howMany is greater than the number associated with the element, the
	 * element is removed altogether.
	 * 
	 * @param e
	 * @param howMany
	 *
	 * @return the updated count.
	 */
	int remove(E e, int howMany);
	
	/**
	 * Removes the element from the multiset completely.
	 * 
	 * @param e
	 * 
	 * @return true if the element was in the multiset before the call.
	 */
	boolean removeCompletely(E e);
	
	/**
	 * Reduces the count by one for each element in the specified collection also
	 * present in the multiset.
	 * 
	 * @param c
	 * 
	 * @return true if the multiset is modified.
	 */
	boolean removeAll(Collection<E> c);
	
	/**
	 * Completely removes the elements in the specified collection from
	 * the multiset.
	 * 
	 * @param c
	 * 
	 * @return true if the multiset is modified.
	 */
	boolean removeAllCompletely(Collection<E> c);
	
	/**
	 * Checks whether or not all elements of the specified collection are
	 * contained in the multiset.
	 * 
	 * @param c
	 * 
	 * @return
	 */
	boolean containsAll(Collection<E> c);

}
