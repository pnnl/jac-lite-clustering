package gov.pnnl.jac.collections;

/**
 * An iterator over a collection of double primitives (<tt>DoubleCollection</tt>).
 *
 * @author R. Scarberry, Vern Crow
 * @version 1.0
 * @see DoubleCollection
 * @see DoubleListIterator
 */
public interface DoubleCollectionIterator extends Cloneable {

    /**
     * Returns <tt>true</tt> if there are more elements in this iteration.
     * If false is returned, a call to <tt>next()</tt> will throw an
     * exception.
     *
     * @return <tt>true</tt> if the iterator has more elements.
     */
    boolean hasNext();

    /**
     * Returns the next element in the iteration.
     *
     * @return the next element.
     * @exception NoSuchElementException if the iteration has no more elements.
     *   Protect against this exception by only calling this method if
     *   <tt>hasNext()</tt> returns true.
     */
    double next();

    /**
     *
     * Removes the last element returned by <tt>next()</tt> from the
     * underlying <tt>DoubleCollection</tt> (optional operation).
     * This method can be called only once per
     * call to <tt>next</tt>.
     *
     * @exception UnsupportedOperationException if the <tt>remove</tt>
     *            operation is not supported by this Iterator.

     * @exception IllegalStateException if the <tt>next</tt> method has not
     *            yet been called, or the <tt>remove</tt> method has already
     *            been called after the last call to the <tt>next</tt>
     *            method.
     */
    void remove();

}
