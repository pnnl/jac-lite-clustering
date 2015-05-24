package gov.pnnl.jac.collections;

/**
 * <p>An iterator over a list of double primitives (<tt>DoubleList</tt>)</p>.
 *
 * @author R. Scarberry, Vern Crow
 * @version 1.0
 * @see DoubleList
 * @see DoubleCollectionIterator
 */
public interface DoubleListIterator extends DoubleCollectionIterator {

    /**
     * Position to the start of the iteration.  If the backing <tt>DoubleList</tt>
     * has nonzero elements, an immediate to <tt>next()</tt> should return a
     * value but an immediate call to <tt>previous()</tt> will throw a
     * <tt>NoSuchElementException</tt>.
     */
    void gotoStart();

    /**
     * Position to the end of the iteration.  If the backing <tt>DoubleList</tt>
     * has nonzero elements, an immediate to <tt>previous()</tt> should return a
     * value but an immediate call to <tt>next()</tt> will throw a
     * <tt>NoSuchElementException</tt>.
     */
    void gotoEnd();

    /**
     * Returns <tt>true</tt> if a call to <tt>previous()</tt> will return
     * another value.
     *
     * @return <tt>true</tt> if the iterator has more elements to traverse
     *   through in the reverse direction.
     */
    boolean hasPrevious();

    /**
     * Returns the previous element in the list.  This method may be mixed with
     * calls to <tt>next()</tt> to iterate back and forth through the list.
     * (Alternate calls to <tt>next</tt> and <tt>previous</tt> or vice-versa
     * return the same element repeatedly.)
     *
     * @return the previous element in the list.
     *
     * @exception NoSuchElementException if the iteration has no previous
     *            element.
     */
    double previous();

    /**
     * Returns the index of the element that would be returned by a subsequent
     * call to <tt>next</tt>. (Returns list size if the list iterator is at the
     * end of the list.)
     *
     * @return the index of the element that would be returned by a subsequent
     *         call to <tt>next</tt>, or list size if list iterator is at end
     *         of list.
     */
    int nextIndex();

    /**
     * Returns the index of the element that would be returned by a subsequent
     * call to <tt>previous</tt>. (Returns -1 if the list iterator is at the
     * beginning of the list.)
     *
     * @return the index of the element that would be returned by a subsequent
     *         call to <tt>previous</tt>, or -1 if list iterator is at
     *         beginning of list.
     */
    int previousIndex();

    // Modification Operations
    /**
     * Replaces the last element returned by <tt>next</tt> or
     * <tt>previous</tt> with the specified element (optional operation).
     * This call can be made only if neither <tt>DoubleListIterator.remove</tt> nor
     * <tt>DoubleListIterator.add</tt> have been called after the last call to
     * <tt>next</tt> or <tt>previous</tt>.
     *
     * @param value the element with which to replace the last element returned by
     *          <tt>next</tt> or <tt>previous</tt>.
     * @exception UnsupportedOperationException if the <tt>set</tt> operation
     *            is not supported by this list iterator.
     * @exception IllegalArgumentException if some aspect of the specified
     *            element prevents it from being added to this list.
     * @exception IllegalStateException if neither <tt>next</tt> nor
     *            <tt>previous</tt> have been called, or <tt>remove</tt> or
     *            <tt>add</tt> have been called after the last call to
     *            <tt>next</tt> or <tt>previous</tt>.
     */
    void set(double value);

    /**
     * Inserts the specified element into the list (optional operation).  The
     * element is inserted immediately before the next element that would be
     * returned by <tt>next</tt>, if any, and after the next element that
     * would be returned by <tt>previous</tt>, if any.  (If the list contains
     * no elements, the new element becomes the sole element on the list.)
     * The new element is inserted before the implicit cursor: a subsequent
     * call to <tt>next</tt> would be unaffected, and a subsequent call to
     * <tt>previous</tt> would return the new element.  (This call increases
     * by one the value that would be returned by a call to <tt>nextIndex</tt>
     * or <tt>previousIndex</tt>.)
     *
     * @param value the element to insert.
     * @exception UnsupportedOperationException if the <tt>add</tt> method is
     *            not supported by this list iterator.
     *
     * @exception IllegalArgumentException if some aspect of this element
     *            prevents it from being added to this Collection.
     */
    void add(double value);
}
