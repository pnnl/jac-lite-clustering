// ** Notice:
// **     This computer software was prepared by Battelle Memorial Institute,
// **     hereinafter the Contractor, under Contract No. DE-AC06-76RL0 1830 with
// **     the Department of Energy (DOE).  All rights in the computer software
// **     are reserved by DOE on behalf of the United States Government and the
// **     Contractor as provided in the Contract.  You are authorized to use
// **     this computer software for Governmental purposes but it is not to be
// **     released or distributed to the public. NEITHER THE GOVERNMENT NOR THE
// **     CONTRACTOR MAKES ANY WARRANTY, EXPRESS OR IMPLIED, OR ASSUMES ANY
// **     LIABILITY FOR THE USE OF THIS SOFTWARE.  This notice including this
// **     sentence must appear on any copies of this computer software.
package gov.pnnl.jac.collections;

/**
 * <p>Defines a collection of float primitives.</p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: Battelle Memorial Institute</p>
 *
 * @author R. Scarberry, Vern Crow
 * @version 1.0
 */
public interface FloatCollection {

    /**
     * Returns the number of elements in this collection.
     *
     * @return the number of elements, never negative.
     */
    int size();

    /**
     * Returns <tt>true</tt> if this collection contains no elements.
     *
     * @return <tt>true</tt> if this collection contains no elements
     */
    boolean isEmpty();

    /**
     * Returns <tt>true</tt> if this collection contains the specified
     * value.
     *
     * @param value element whose presence in this collection is to be tested.
     * @return <tt>true</tt> if this collection contains the specified
     *         value.
     */
    boolean contains(float value);

    /**
     * <p>Returns an array containing all of the elements in this collection.
     * The order of the returned values is implementation dependent.</p>
     *
     * <p>The caller is thus free to modify the returned array without
     * affecting the collection.</p>
     *
     * @return an array containing all of the elements in this collection
     */
    float[] toArray();

    // Modification Operations

    /**
     * Add the specified float value to this collection (optional
     * operation).  Returns <tt>true</tt> if the collection changed as a
     * result of the call.  (Returns <tt>false</tt> if this collection does
     * not permit duplicates and already contains the specified value.)<p>
     *
     * Collections that support this operation may place limitations on what
     * values may be added to the collection.<p>
     *
     * If a collection refuses to add a particular element for any reason
     * other than that it already contains the element, it <i>must</i> throw
     * an exception (rather than returning <tt>false</tt>).  This preserves
     * the invariant that a collection always contains the specified element
     * after this call returns.
     *
     * @param value element to be added.
     * @return <tt>true</tt> if this collection changed as a result of the
     *         call
     *
     * @throws UnsupportedOperationException add is not supported by this
     *         collection.
     * @throws IllegalArgumentException some aspect of this element prevents
     *          it from being added to this collection.
     */
    boolean add(float value);

    /**
     * Removes a single instance of the specified value from this
     * collection, if it is present (optional operation).
     *
     * @param value element to be removed from this collection, if present.
     * @return <tt>true</tt> if this collection changed as a result of the
     *         call
     *
     * @throws UnsupportedOperationException remove is not supported by this
     *         collection.
     */
    boolean remove(float value);

    // Bulk Operations

    /**
     * Returns <tt>true</tt> if this collection contains all of the elements
     * in the specified collection.
     *
     * @param c collection to be checked for containment in this collection.
     * @return <tt>true</tt> if this collection contains all of the elements
     *	       in the specified collection
     * @see #contains(float)
     */
    boolean containsAll(FloatCollection c);

    /**
     * Returns <tt>true</tt> if this collection contains all of the elements
     * in the specified array.
     *
     * @param values array of values to be checked for containment in this collection.
     * @return <tt>true</tt> if this collection contains all of the elements
     *	       in the specified array.
     * @see #contains(float)
     */
    boolean containsAll(float[] values);

    /**
     * Adds all of the elements in the specified collection to this collection
     * (optional operation).
     *
     * @param c elements to be added to this collection.
     * @return <tt>true</tt> if this collection changed as a result of the
     *         call
     *
     * @throws UnsupportedOperationException if this collection does not
     *         support the <tt>addAll</tt> method.
     * @throws IllegalArgumentException some aspect of an element of the
     *	       specified collection prevents it from being added to this
     *	       collection.
     *
     * @see #add(float)
     */
    boolean addAll(FloatCollection c);

    /**
     * Adds all of the elements in the specified array to this collection
     * (optional operation).
     *
     * @param values elements to be added to this collection.
     * @return <tt>true</tt> if this collection changed as a result of the
     *         call
     *
     * @throws UnsupportedOperationException if this collection does not
     *         support the <tt>addAll</tt> method.
     * @throws IllegalArgumentException some aspect of an element of the
     *	       specified array prevents it from being added to this
     *	       collection.
     *
     * @see #add(float)
     */
    boolean addAll(float[] values);

    /**
     *
     * Removes all this collection's elements that are also contained in the
     * specified collection (optional operation).  After this call returns,
     * this collection will contain no elements in common with the specified
     * collection.
     *
     * @param c elements to be removed from this collection.
     * @return <tt>true</tt> if this collection changed as a result of the
     *         call
     *
     * @throws UnsupportedOperationException if the <tt>removeAll</tt> method
     * 	       is not supported by this collection.
     *
     * @see #remove(float)
     * @see #contains(float)
     */
    boolean removeAll(FloatCollection c);

    /**
     *
     * Removes all this collection's elements that are also contained in the
     * specified array (optional operation).
     *
     * @param values elements to be removed from this collection.
     * @return <tt>true</tt> if this collection changed as a result of the
     *         call
     *
     * @throws UnsupportedOperationException if the <tt>removeAll</tt> method
     * 	       is not supported by this collection.
     *
     * @see #remove(float)
     * @see #contains(float)
     */
    boolean removeAll(float[] values);

    /**
     * Retains only the elements in this collection that are contained in the
     * specified collection (optional operation).  In other words, removes from
     * this collection all of its elements that are not contained in the
     * specified collection.
     *
     * @param c elements to be retained in this collection.
     * @return <tt>true</tt> if this collection changed as a result of the
     *         call
     *
     * @throws UnsupportedOperationException if the <tt>retainAll</tt> method
     * 	       is not supported by this Collection.
     *
     * @see #remove(float)
     * @see #contains(float)
     */
    boolean retainAll(FloatCollection c);

    /**
     * Retains only the elements in this collection that are contained in the
     * specified array (optional operation).
     *
     * @param values elements to be retained in this collection.
     * @return <tt>true</tt> if this collection changed as a result of the
     *         call
     *
     * @throws UnsupportedOperationException if the <tt>retainAll</tt> method
     * 	       is not supported by this Collection.
     *
     * @see #remove(float)
     * @see #contains(float)
     */
    boolean retainAll(float[] values);

    /**
     * Removes all of the elements from this collection (optional operation).
     * This collection will be empty after this method returns unless it
     * throws an exception.
     *
     * @throws UnsupportedOperationException if the <tt>clear</tt> method is
     *         not supported by this collection.
     */
    void clear();

    /**
     * Returns an iterator over this collection.
     * @return FloatIterator
     */
    FloatCollectionIterator iterator();

    // Comparison and hashing

    /**
     * Compare with another object for equality.
     * @see Object#equals(Object)
     */
    boolean equals(Object o);

    /**
     *
     * Returns the hash code value for this collection.     *
     * @return the hash code value for this collection
     *
     * @see Object#hashCode()
     * @see Object#equals(Object)
     */
    int hashCode();

}
