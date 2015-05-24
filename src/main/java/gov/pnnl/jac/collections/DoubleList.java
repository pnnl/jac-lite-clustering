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
 * <p>Defines a dynamically growing list for containing double
 * primitives (<tt>double</tt>s).</p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: Battelle Memorial Institute</p>
 *
 * @author R. Scarberry, Vern Crow
 * @version 1.0
 */
public interface DoubleList extends DoubleCollection, Cloneable {
	
//	double dot(DoubleList other);
//	
//	double lengthSquared();
//	
//	double length();

    /**
     * Returns the elements contained in this list in the order they appear in
     * the list.
     * @return double[] - array of values contained in the list.
     */
    double[] toArray();
    
    double[] toArray(double[] buffer);

    /**
     * Inserts all of the elements in the specified collection into this
     * list at the specified position (optional operation).  Shifts the
     * element currently at that position (if any) and any subsequent
     * elements up (increases their indices).  The new elements
     * will appear in this list in the order that they are returned by the
     * specified collection's <tt>toArray()</tt> method.
     *
     * @param index index at which to insert first element from the specified
     *              collection.
     * @param c elements to be inserted into this list.
     * @return <tt>true</tt> if this list changed as a result of the call.
     *
     * @throws UnsupportedOperationException if the <tt>addAll</tt> method is
     *            not supported by this list.
     * @throws IllegalArgumentException if some aspect of one of elements of
     *            the specified collection prevents it from being added to
     *            this list.
     * @throws IndexOutOfBoundsException if the index is out of range (index
     *            &lt; 0 || index &gt; size()).
     */
    boolean addAll(int index, DoubleCollection c);

    /**
     * Inserts all of the elements in the specified array into this
     * list at the specified position (optional operation).  Shifts the
     * element currently at that position (if any) and any subsequent
     * elements up (increases their indices).  The new elements
     * will appear in this list in the same order as the array argument.
     *
     * @param index index at which to insert first element from the specified
     *              collection.
     * @param values elements to be inserted into this list.
     * @return <tt>true</tt> if this list changed as a result of the call.
     *
     * @throws UnsupportedOperationException if the <tt>addAll</tt> method is
     *            not supported by this list.
     * @throws IllegalArgumentException if some aspect of one of elements of
     *            the specified collection prevents it from being added to
     *            this list.
     * @throws IndexOutOfBoundsException if the index is out of range (index
     *            &lt; 0 || index &gt; size()).
     */
    boolean addAll(int index, double[] values);

    // Positional Access Operations

    /**
     * Returns the element at the specified position in this list.
     *
     * @param index index of element to return.
     * @return the element at the specified position in this list.
     *
     * @throws IndexOutOfBoundsException if the index is out of range (index
     *            &lt; 0 || index &gt;= size()).
     */
    double get(int index);

    /**
     * Replaces the element at the specified position in this list with the
     * specified element (optional operation).
     *
     * @param index index of element to replace.
     * @param element element to be stored at the specified position.
     * @return the element previously at the specified position.
     *
     * @throws UnsupportedOperationException if the <tt>set</tt> method is not
     *            supported by this list.
     * @throws    IllegalArgumentException if some aspect of the specified
     *            element prevents it from being added to this list.
     * @throws    IndexOutOfBoundsException if the index is out of range
     *            (index &lt; 0 || index &gt;= size()).  */
    double set(int index, double value);

    /**
     * Inserts the specified element at the specified position in this list
     * (optional operation).  Shifts the element currently at that position
     * (if any) and any subsequent elements up (adds one to their
     * indices).
     *
     * @param index index at which the specified element is to be inserted.
     * @param element element to be inserted.
     *
     * @throws UnsupportedOperationException if the <tt>add</tt> method is not
     *            supported by this list.
     * @throws    IllegalArgumentException if some aspect of the specified
     *            element prevents it from being added to this list.
     * @throws    IndexOutOfBoundsException if the index is out of range
     *            (index &lt; 0 || index &gt; size()).
     */
    void add(int index, double value);

    /**
     * Removes the element at the specified position in this list (optional
     * operation).  Shifts any subsequent elements down (subtracts one
     * from their indices).  Returns the element that was removed from the
     * list.
     *
     * @param index the index of the element to removed.
     * @return the element previously at the specified position.
     *
     * @throws UnsupportedOperationException if the <tt>remove</tt> method is
     *            not supported by this list.
     *
     * @throws IndexOutOfBoundsException if the index is out of range (index
     *            &lt; 0 || index &gt;= size()).
     */
    double removeAt(int index);

    /**
     * Sort the elements of the list in ascending order.
     */
    void sort();

    /**
     * Sort the list in the order determined by the parameter.
     * @param ascending - true to sort in ascending order, false to sort in
     *   descending order.
     */
    void sort(boolean ascending);

    // Search Operations

    /**
     * Returns the index in this list of the first occurrence of the specified
     * element, or -1 if this list does not contain this element.
     *
     * @param value element to search for.
     * @return the index in this list of the first occurrence of the specified
     *         element, or -1 if this list does not contain this element.
     */
    int indexOf(double value);

    /**
     * Returns the index in this list of the first occurrence of the specified
     * element starting at the specified index, or -1 if this list does not
     * contain this element.
     *
     * @param startIndex the place to start searching for the value.
     * @param value element to search for.
     * @return the index in this list of the first occurrence of the specified
     *         element, or -1 if this list does not contain this element.
     * @throws IndexOutOfBoundsException if the start index is out of range
     *   (startIndex &lt; 0 || index &gt; size()).
     */
    int indexOf(int startIndex, double value);

    /**
     * Returns the index in this list of the last occurrence of the specified
     * element, or -1 if this list does not contain this element.
     *
     * @param value element to search for.
     * @return the index in this list of the last occurrence of the specified
     *         element, or -1 if this list does not contain this element.
     */
    int lastIndexOf(double value);

    /**
     * Returns a list iterator of the elements in this list (in proper
     * sequence).
     *
     * @return a list iterator of the elements in this list (in proper
     *         sequence).
     */
    DoubleListIterator listIterator();

    /**
     * Returns a list iterator of the elements in this list (in proper
     * sequence), starting at the specified position in this list.  The
     * specified index indicates the first element that would be returned by
     * an initial call to the <tt>next</tt> method.  An initial call to
     * the <tt>previous</tt> method would return the element with the
     * specified index minus one.
     *
     * @param index index of first element to be returned from the
     *              list iterator (by a call to the <tt>next</tt> method).
     * @return a list iterator of the elements in this list (in proper
     *         sequence), starting at the specified position in this list.
     * @throws IndexOutOfBoundsException if the index is out of range (index
     *         &lt; 0 || index &gt; size()).
     */
    DoubleListIterator listIterator(int index);
    
    /**
     * Implementations should provide a deep-copy clone.
     * @return
     */
    DoubleList clone();
}
