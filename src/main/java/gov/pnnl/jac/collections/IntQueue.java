package gov.pnnl.jac.collections;

public interface IntQueue extends IntCollection {

    /**
     * Add the specified integer value to queue.  
     * Returns <tt>true</tt> if the add was successful. However, if no space is 
     * available this method should throw an IllegalStateException.  The offer method
     * behaves the same as this method, but it never throws an IllegalStateException.  It
     * simply returns false if the add could not be done. 
     * 
     * @param value element to be added.
     * @return <tt>true</tt> if this collection changed as a result of the
     *         call
     *
     * @throws IllegalStateException if the add was unsuccessful because of 
     *   capacity restrictions.
     */
	boolean add(int value);
	
	/**
	 * Retrieves the head of the queue but does not remove it.  Returns -1 if the queue is
	 * empty.  If values of -1 are legitimately contained in the queue, you should call size()
	 * prior to calling this method.
	 * 
	 * @return
	 */
	int element();
	
	/**
	 * Inserts the specified value into the queue if it is possible to do so without 
	 * violating capacity limits.
	 * 
	 * @param value
	 * 
	 * @return true if the value is successfully added, false otherwise.
	 */
	boolean offer(int value);
	
	/**
	 * Retrieves but does not remove the head of the queue.
	 * 
	 * @return the value at the head of the queue or -1 if the queue is empty.  If -1 may 
	 *   legitimately be a member in the queue, call size() or isEmpty() before calling this method.
	 */
	int peek();
	
	/**
	 * Retrieves and removes the head of the queue.
	 * 
	 * @return the value at the head of the queue or -1 if the queue is empty.  If -1 may 
	 *   legitimately be a member in the queue, call size() or isEmpty() before calling this method.
	 */
	int poll();
	
	/**
	 * Retrieves and removes the head of the queue.
	 * 
	 * @return the value at the head of the queue or -1 if the queue is empty.  If -1 may 
	 *   legitimately be a member in the queue, call size() or isEmpty() before calling this method.
	 */
	int remove();
	
}
