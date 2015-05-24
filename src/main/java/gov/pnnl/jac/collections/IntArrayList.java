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

import java.util.*;

/**
 * <p><tt>IntArrayList</tt> is an implementation of <tt>IntList</tt> that
 * uses a backing array for storage.</p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: Battelle Memorial Institute</p>
 *
 * @author R. Scarberry
 * @version 1.0
 */
public class IntArrayList implements IntList, Cloneable {

  // The backing array.
  private int[] mData;
  // Size and the index of the next available spot in mData.
  private int mSize;
  // Mainly to trigger concurrent modification exceptions when attempting
  // to modify by directly calling methods while using an iterator.
  private transient int mModCount;

  /**
   * Constructor
   * @param initialCapacity the initial capacity of the backing array.
   */
  public IntArrayList(int initialCapacity) {
    int sz = Math.max(initialCapacity, 0);
    mData = new int[sz];
  }

  /**
   * Default constructor.
   */
  public IntArrayList() {
    this(10);
  }
  
  public IntArrayList(IntCollection c) {
	  this(c.size());
	  addAll(c);
  }
  
  public IntArrayList(int[] values) {
	  this(values.length);
	  addAll(values);
  }

  /**
   * Returns the number of integer elements in this collection.
   *
   * @return the number of elements, never negative.
   */
  public int size() {
    return mSize;
  }

  /**
   * Returns the current capacity of this collection.  
   * 
   * @return
   */
  public int capacity() {
      return mData.length;
  }
  
  /**
   * Returns <tt>true</tt> if this collection contains no integer elements.
   *
   * @return <tt>true</tt> if this collection contains no elements
   */
  public boolean isEmpty() {
    return mSize == 0;
  }

  /**
   * Returns <tt>true</tt> if this collection contains the specified
   * value.
   *
   * @param value element whose presence in this collection is to be tested.
   * @return <tt>true</tt> if this collection contains the specified
   *         value.
   */
  public boolean contains(int value) {
    for (int i=0; i<mSize; i++) {
      if (mData[i] == value) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns <tt>true</tt> if this collection contains all of the elements
   * in the specified collection.
   *
   * @param c collection to be checked for containment in this collection.
   * @return <tt>true</tt> if this collection contains all of the elements
   *	       in the specified collection
   * @see #contains(int)
   */
  public boolean containsAll(IntCollection c) {
    if (c == this) {
      return true;
    }
    return containsAll(c.toArray());
  }

  /**
   * Returns <tt>true</tt> if this collection contains all of the elements
   * in the specified array.
   *
   * @param values array of values to be checked for containment in this collection.
   * @return <tt>true</tt> if this collection contains all of the elements
   *	       in the specified array.
   * @see #contains(int)
   */
  public boolean containsAll(int[] values) {
    int n = values.length;
    for (int i=0; i<n; i++) {
      if (!contains(values[i])) {
        return false;
      }
    }
    return true;
  }

  /**
   * Add the specified integer value to this collection (optional
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
  public boolean add(int value) {
    if (mSize == mData.length) {
      expand();
    }
    mData[mSize++] = value;
    mModCount++;
    return true;
  }

  /**
   * Inserts all of the elements in the specified collection into this
   * list at the specified position (optional operation).  Shifts the
   * element currently at that position (if any) and any subsequent
   * elements up (increases their indices).  The new elements
   * will appear in this list in the order that they are returned by the
   * specified collection's <tt>toArray()</tt> method.
   *
   * @param index index at which to insert first element from the specified
   *	            collection.
   * @param c elements to be inserted into this list.
   * @return <tt>true</tt> if this list changed as a result of the call.
   *
   * @throws UnsupportedOperationException if the <tt>addAll</tt> method is
   *		  not supported by this list.
   * @throws IllegalArgumentException if some aspect of one of elements of
   *		  the specified collection prevents it from being added to
   *		  this list.
   * @throws IndexOutOfBoundsException if the index is out of range (index
   *		  &lt; 0 || index &gt; size()).
   */
  public boolean addAll(int index, IntCollection c) {
    return addAll(index, c.toArray());
  }

  /**
   * Inserts all of the elements in the specified array into this
   * list at the specified position (optional operation).  Shifts the
   * element currently at that position (if any) and any subsequent
   * elements up (increases their indices).  The new elements
   * will appear in this list in the same order as the array argument.
   *
   * @param index index at which to insert first element from the specified
   *	            collection.
   * @param values elements to be inserted into this list.
   * @return <tt>true</tt> if this list changed as a result of the call.
   *
   * @throws UnsupportedOperationException if the <tt>addAll</tt> method is
   *		  not supported by this list.
   * @throws IllegalArgumentException if some aspect of one of elements of
   *		  the specified collection prevents it from being added to
   *		  this list.
   * @throws IndexOutOfBoundsException if the index is out of range (index
   *		  &lt; 0 || index &gt; size()).
   */
  public boolean addAll(int index, int[] values) {
    if (index == mSize) {
      return addAll(values);
    }
    checkIndexForAdd(index);
    int n = values.length;
    if (n > 0) {
      int newSize = mSize + n;
      while(mData.length < newSize) {
        expand();
      }
      int lim = n + index - 1;
      for (int i=newSize-1; i>lim; i--) {
        mData[i] = mData[i-n];
      }
      lim = index + n;
      for (int i=index; i<lim; i++) {
        mData[i] = values[i-index];
      }
      mModCount += n;
      mSize = newSize;
      return true;
    }
    return false;
  }

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
   * @see #add(int)
   */
  public boolean addAll(IntCollection c) {
    return addAll(c.toArray());
  }

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
   * @see #add(int)
   */
  public boolean addAll(int[] values) {
    int n = values.length;
    if (n > 0) {
      int newSize = mSize + n;
      while(mData.length < newSize) {
        expand();
      }
      for (int i=0; i<n; i++) {
        mData[mSize++] = values[i];
      }
      mModCount += n;
      return true;
    }
    return false;
  }

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
   *		  supported by this list.
   * @throws    IllegalArgumentException if some aspect of the specified
   *		  element prevents it from being added to this list.
   * @throws    IndexOutOfBoundsException if the index is out of range
   *		  (index &lt; 0 || index &gt; size()).
   */
  public void add(int index, int value) {
    checkIndexForAdd(index);
    if (index == mSize) {
      add(value);
    } else { // Insert somewhere before the end.
      if (mSize == mData.length) {
        expand();
      }
      for (int i=mSize; i>index; i--) {
        mData[i] = mData[i-1];
      }
      mData[index] = value;
      mModCount++;
      mSize++;
    }
  }

  /**
   * Returns the element at the specified position in this list.
   *
   * @param index index of element to return.
   * @return the element at the specified position in this list.
   *
   * @throws IndexOutOfBoundsException if the index is out of range (index
   * 		  &lt; 0 || index &gt;= size()).
   */
  public int get(int index) {
    checkIndexForAccess(index);
    return mData[index];
  }
  
  /**
   * Quick access with no bounds checking before accessing the backing data structure.
   * 
   * @param index
   * @return
   */
  public int getQuick(int index) {
	  return mData[index];
  }

  /**
   * Replaces the element at the specified position in this list with the
   * specified element (optional operation).
   *
   * @param index index of element to replace.
   * @param element element to be stored at the specified position.
   * @return the element previously at the specified position.
   *
   * @throws UnsupportedOperationException if the <tt>set</tt> method is not
   *		  supported by this list.
   * @throws    IllegalArgumentException if some aspect of the specified
   *		  element prevents it from being added to this list.
   * @throws    IndexOutOfBoundsException if the index is out of range
   *		  (index &lt; 0 || index &gt;= size()).  */
  public int set(int index, int value) {
    checkIndexForAccess(index);
    int rtn = mData[index];
    mData[index] = value;
    mModCount++;
    return rtn;
  }

  /**
   * Removes all of the elements from this collection (optional operation).
   * This collection will be empty after this method returns unless it
   * throws an exception.
   *
   * @throws UnsupportedOperationException if the <tt>clear</tt> method is
   *         not supported by this collection.
   */
  public void clear() {
    mSize = 0;
    mModCount++;
  }

  /**
   * Reduce this list's capacity down to its current size.  Used to reduce the
   * object's memory usage.
   */
  public void shrinkToSize() {
    if (mData.length > mSize) {
      int[] newData = new int[mSize];
      System.arraycopy(mData, 0, newData, 0, mSize);
      mData = newData;
      mModCount++;
    }
  }

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
  public boolean remove(int value) {
    int index = indexOf(value);
    if (index >= 0) {
      removeAt(index);
      mModCount++;
      return true;
    }
    return false;
  }

  /**
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
   * @see #remove(int)
   * @see #contains(int)
   */
  public boolean removeAll(IntCollection c) {
    if (c == this) {
      if (mSize > 0) {
        clear();
        mModCount++;
        return true;
      }
      return false;
    }
    return removeAll(c.toArray());
  }

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
   * @see #remove(int)
   * @see #contains(int)
   */
  public boolean removeAll(int[] values) {
    if (!isEmpty()) {
      int n = values.length;
      if (n > 0) {
        IntSet valueSet = new IntHashSet(2*n);
        valueSet.addAll(values);
        int keep = 0;
        int[] newData = new int[mData.length];
        for (int i=0; i<mSize; i++) {
          int v = mData[i];
          if (!valueSet.contains(v)) {
            newData[keep++] = v;
          }
        }
        if (keep < mSize) {
          mData = newData;
          mSize = keep;
          mModCount++;
          return true;
        }
      }
    }
    return false;
  }

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
   *		  not supported by this list.
   *
   * @throws IndexOutOfBoundsException if the index is out of range (index
   *            &lt; 0 || index &gt;= size()).
   */
  public int removeAt(int index) {
    checkIndexForAccess(index);
    int rtn = mData[index];
    mSize--;
    for (int i=index; i<mSize; i++) {
      mData[i] = mData[i+1];
    }
    mModCount++;
    return rtn;
  }

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
   * @see #remove(int)
   * @see #contains(int)
   */
  public boolean retainAll(IntCollection c) {
    if (c == this) {
      return false;
    }
    return retainAll(c.toArray());
  }

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
   * @see #remove(int)
   * @see #contains(int)
   */
  public boolean retainAll(int[] values) {
    if (!isEmpty()) {
      int n = values.length;
      if (n == 0) {
        clear();
        return true;
      } else {
        int[] sortedValues = new int[n];
        System.arraycopy(values, 0, sortedValues, 0, n);
        Arrays.sort(sortedValues);
        int keep = 0;
        int[] newData = new int[mData.length];
        for (int i=0; i<mSize; i++) {
          int v = mData[i];
          if (Arrays.binarySearch(sortedValues, v) >= 0) {
            newData[keep++] = v;
          }
        }
        if (keep < mSize) {
          mData = newData;
          mSize = keep;
          mModCount++;
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Sort the elements of the list in ascending order.
   */
  public void sort() {
    sort(true);
  }

  /**
   * Sort the list in the order determined by the parameter.
   * @param ascending - true to sort in ascending order, false to sort in
   *   descending order.
   */
  public void sort(boolean ascending) {
    Arrays.sort(mData, 0, mSize);
    if (!ascending) {
      int m = mSize/2 - 1;
      for (int i=0, j=mSize-1; i<=m; i++, j--) {
        mData[i] ^= mData[j];
        mData[j] ^= mData[i];
        mData[i] ^= mData[j];
      }
    }
    mModCount++;
  }

  /**
   * Returns the index in this list of the first occurrence of the specified
   * element, or -1 if this list does not contain this element.
   *
   * @param value element to search for.
   * @return the index in this list of the first occurrence of the specified
   * 	       element, or -1 if this list does not contain this element.
   */
  public int indexOf(int value) {
    if (mSize > 0) {
      return indexOf(0, value);
    }
    return -1;
  }

  /**
   * Returns the index in this list of the first occurrence of the specified
   * value starting the search at the specified index, or -1 if this list does not
   * contain this element.
   *
   * @param startIndex the place to start searching for the value.
   * @param value element to search for.
   * @return the index in this list of the first occurrence of the specified
   * 	       element, or -1 if this list does not contain this element.
   * @throws IndexOutOfBoundsException if the start index is out of range
   *   (startIndex &lt; 0 || index &gt; size()).
   */
  public int indexOf(int startIndex, int value) {
    checkIndexForAdd(startIndex);
    if (startIndex < mSize) {
      for (int i=startIndex; i<mSize; i++) {
        if (mData[i] == value) {
          return i;
        }
      }
    }
    return -1;
  }

  /**
   * Returns the last index of the specified value in this list or -1, if the
   * list does not contain the element.
   * @param value the value to be searched for.
   * @return int
   */
  public int lastIndexOf(int value) {
    for (int i=mSize-1; i>=0; i--) {
      if (mData[i] == value) {
        return i;
      }
    }
    return -1;
  }

  public int[] data() {
	  return mData;
  }
  
  /**
   * Returns an array containing the elements of this list in the order
   * they are contained in the list.
   * @return int[]
   */
  public int[] toArray() {
    int[] rtn = new int[mSize];
    System.arraycopy(mData, 0, rtn, 0, mSize);
    return rtn;
  }

  public IntCollectionIterator iterator() {
    return new Itr();
  }

  public IntListIterator listIterator() {
    return new LItr(0);
  }

  public IntListIterator listIterator(int index) {
    return new LItr(index);
  }

  /**
   * Returns the hash code for the list.
   * @return int
   */
  public int hashCode() {
    return ListUtils.computeHash(this);
  }

  /**
   * Check for equality with the specified argument.  Will return true if
   * the specified object is an <tt>IntList</tt> of the same size and with the
   * same elements in the same order as this list.  The other list may be
   * a different implementation class of <tt>IntList</tt> and still be
   * considered equal.
   * @param o Object
   * @return boolean
   */
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if(!(o instanceof IntList)) {
      return false;
    }
    return ListUtils.checkEqual(this, (IntList) o);
  }

  /**
   * Returns a deep-copy clone.
   * @return Object
   */
  public Object clone() {
    IntArrayList clone = null;
    try {
      clone = (IntArrayList) super.clone();
      clone.mData = new int[this.mData.length];
      System.arraycopy(this.mData, 0, clone.mData, 0, this.mData.length);
      clone.mModCount = 0;
    } catch (CloneNotSupportedException e) {
      throw new InternalError();
    }
    return clone;
  }

  // Check the index for access or removal.
  private void checkIndexForAccess(int index) {
    if (index < 0 || index >= mSize) {
      throw new ArrayIndexOutOfBoundsException("out of bounds: " + index);
    }
  }

  // Check the index for adding.
  private void checkIndexForAdd(int index) {
    if (index < 0 || index > mSize) {
      throw new ArrayIndexOutOfBoundsException("out of bounds: " + index);
    }
  }

  // Expand the capacity of the list.
  private void expand() {
    int newCap = 2*mData.length;
    if (newCap == 0) { // Just in case initialSize was 0.
      newCap = 1;
    }
    int[] newData = new int[newCap];
    System.arraycopy(mData, 0, newData, 0, mSize);
    mData = newData;
  }

  private class Itr implements IntCollectionIterator {

    private LItr mIt = new LItr();

    public boolean hasNext() {
        return mIt.hasNext();
    }

    public int next() {
      return mIt.next();
    }

    public void remove() {
      mIt.remove();
    }
  }

  private class LItr implements IntListIterator {

    /**
     * Index of element to be returned by subsequent call to next.
     */
    int mCursor = 0;
    /**
     * Index of element returned by most recent call to next or
     * previous.  Reset to -1 if this element is deleted by a call
     * to remove.
     */
    int mLastRetNdx = -1;

    /**
     * The modCount value that the iterator believes that the backing
     * List should have.  If this expectation is violated, the iterator
     * has detected concurrent modification.
     */
    int mExpectedModCount = mModCount;

    LItr(int index) {
      mCursor = index;
    }

    LItr() {
      this (0);
    }

    public void gotoStart() {
      mCursor = 0;
      mLastRetNdx = -1;
    }

    public void gotoEnd() {
      mCursor = mSize;
      mLastRetNdx = -1;
    }

    public boolean hasNext() {
        return mCursor != size();
    }

    public int next() {
        try {
            int next = get(mCursor);
            checkForComodification();
            mLastRetNdx = mCursor++;
            return next;
        } catch(IndexOutOfBoundsException e) {
            checkForComodification();
            throw new NoSuchElementException();
        }
    }

    public void remove() {
        if (mLastRetNdx == -1) {
            throw new IllegalStateException();
        }
        checkForComodification();
        try {
            IntArrayList.this.removeAt(mLastRetNdx);
            if (mLastRetNdx < mCursor) {
                mCursor--;
            }
            mLastRetNdx = -1;
            mExpectedModCount = mModCount;
        } catch(IndexOutOfBoundsException e) {
            throw new ConcurrentModificationException();
        }
    }

    final void checkForComodification() {
        if (mModCount != mExpectedModCount) {
            throw new ConcurrentModificationException();
        }
    }

    public boolean hasPrevious() {
        return mCursor != 0;
    }

    public int previous() {
        try {
            int previous = get(--mCursor);
            checkForComodification();
            mLastRetNdx = mCursor;
            return previous;
        } catch(IndexOutOfBoundsException e) {
            checkForComodification();
            throw new NoSuchElementException();
        }
    }

    public int nextIndex() {
        return mCursor;
    }

    public int previousIndex() {
        return mCursor-1;
    }

    public void set(int value) {
        if (mLastRetNdx == -1) {
            throw new IllegalStateException();
        }
        checkForComodification();
        try {
            IntArrayList.this.set(mLastRetNdx, value);
            mExpectedModCount = mModCount;
        } catch(IndexOutOfBoundsException e) {
            throw new ConcurrentModificationException();
        }
    }

    public void add(int value) {
        checkForComodification();
        try {
            IntArrayList.this.add(mCursor++, value);
            mLastRetNdx = -1;
            mExpectedModCount = mModCount;
        } catch(IndexOutOfBoundsException e) {
            throw new ConcurrentModificationException();
        }
    }
  }
}
