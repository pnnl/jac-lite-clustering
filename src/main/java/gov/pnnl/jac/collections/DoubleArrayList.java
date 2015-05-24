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
 * <p><tt>DoubleArrayList</tt> is an implementation of <tt>DoubleList</tt> that
 * uses a backing array for storage.</p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: Battelle Memorial Institute</p>
 *
 * @author R. Scarberry, Vern Crow
 * @version 1.0
 */
public class DoubleArrayList extends AbstractDoubleList {

  // The backing array.
  private double[] mData;
  // Size and the index of the next available spot in mData.
  private int mSize;

  /**
   * Constructor
   * @param initialCapacity the initial capacity of the backing array.
   */
  public DoubleArrayList(int initialCapacity) {
    int sz = Math.max(initialCapacity, 0);
    mData = new double[sz];
  }

  /**
   * Default constructor.
   */
  public DoubleArrayList() {
    this(10);
  }

  public DoubleArrayList(double[] values) {
	  this(values.length);
	  addAll(values);
  }
  
  public DoubleArrayList(DoubleCollection c) {
	  this(c.size());
	  addAll(c);
  }
  
  public double dot(DoubleList other) {
	  final int len = Math.min(this.size(), other.size());
	  double dot = 0.0;
	  for (int i=0; i<len; i++) {
		  dot += this.get(i) * other.get(i);
	  }
	  return dot;
  }
  
  /**
   * Returns the number of elements in this collection.
   *
   * @return the number of elements, never negative.
   */
  public int size() {
    return mSize;
  }
  
  /**
   * Add the specified value to this collection (optional
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
  public boolean add(double value) {
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
   *                collection.
   * @param c elements to be inserted into this list.
   * @return <tt>true</tt> if this list changed as a result of the call.
   *
   * @throws UnsupportedOperationException if the <tt>addAll</tt> method is
   *              not supported by this list.
   * @throws IllegalArgumentException if some aspect of one of elements of
   *              the specified collection prevents it from being added to
   *              this list.
   * @throws IndexOutOfBoundsException if the index is out of range (index
   *              &lt; 0 || index &gt; size()).
   */
  public boolean addAll(int index, DoubleCollection c) {
	  // If adding to the end, just call the other method.  
	  if (index == mSize) {
		  return addAll(c);
	  }
	  checkIndexForAdd(index);
	  final int sz = c.size();
	  if (sz > 0) {
		  int newSize = mSize + sz;
	      while(mData.length < newSize) {
	          expand();
	      }
	      int lim = sz + index - 1;
	      for (int i=newSize-1; i>lim; i--) {
	    	  mData[i] = mData[i-sz];
	      }
	      lim = index + sz;
	      int i = index;
	      DoubleCollectionIterator it = c.iterator();
	      while(it.hasNext()) {
	    	  mData[i++] = it.next();
	      }
	      mModCount += sz;
	      mSize = newSize;
	      return true;
	   }
	   return false;
  }
  
  /**
   * Inserts all of the elements in the specified array into this
   * list at the specified position (optional operation).  Shifts the
   * element currently at that position (if any) and any subsequent
   * elements up (increases their indices).  The new elements
   * will appear in this list in the same order as the array argument.
   *
   * @param index index at which to insert first element from the specified
   *                collection.
   * @param values elements to be inserted into this list.
   * @return <tt>true</tt> if this list changed as a result of the call.
   *
   * @throws UnsupportedOperationException if the <tt>addAll</tt> method is
   *              not supported by this list.
   * @throws IllegalArgumentException if some aspect of one of elements of
   *              the specified collection prevents it from being added to
   *              this list.
   * @throws IndexOutOfBoundsException if the index is out of range (index
   *              &lt; 0 || index &gt; size()).
   */
  public boolean addAll(int index, double[] values) {
	  return addAll(index, new DoubleArrayList(values));
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
   *           specified array prevents it from being added to this
   *           collection.
   *
   * @see #add(double)
   */
  public boolean addAll(double[] values) {
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
   *              supported by this list.
   * @throws    IllegalArgumentException if some aspect of the specified
   *              element prevents it from being added to this list.
   * @throws    IndexOutOfBoundsException if the index is out of range
   *              (index &lt; 0 || index &gt; size()).
   */
  public void add(int index, double value) {
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
   *              &lt; 0 || index &gt;= size()).
   */
  public double get(int index) {
    checkIndexForAccess(index);
    return mData[index];
  }
  
  public double getQuick(int index) {
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
   *              supported by this list.
   * @throws    IllegalArgumentException if some aspect of the specified
   *              element prevents it from being added to this list.
   * @throws    IndexOutOfBoundsException if the index is out of range
   *              (index &lt; 0 || index &gt;= size()).  */
  public double set(int index, double value) {
    checkIndexForAccess(index);
    double rtn = mData[index];
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
      double[] newData = new double[mSize];
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
  public boolean remove(double value) {
    int index = indexOf(value);
    if (index >= 0) {
      removeAt(index);
      mModCount++;
      return true;
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
   *              not supported by this list.
   *
   * @throws IndexOutOfBoundsException if the index is out of range (index
   *            &lt; 0 || index &gt;= size()).
   */
  public double removeAt(int index) {
    checkIndexForAccess(index);
    double rtn = mData[index];
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
   *           is not supported by this Collection.
   *
   * @see #remove(double)
   * @see #contains(double)
   */
  public boolean retainAll(DoubleCollection c) {
	  if (c == this) {
		  return false;
	  }
	  if (!isEmpty()) {
		  final int sz = c.size();
		  if (sz == 0) {
			  clear();
			  return true;
		  } else {

			  DoubleSet fset = c instanceof DoubleSet ? ((DoubleSet) c) : new DoubleHashSet(c);

			  int keep = 0;
			  double[] newData = new double[mData.length];
			  for (int i=0; i<mSize; i++) {
				  double v = mData[i];
				  if (fset.contains(v)) {
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
        double tmp = mData[i];
        mData[i] = mData[j];
        mData[j] = tmp;
      }
    }
    mModCount++;
  }
  
  public double[] data() {
	  return mData;
  }
  
  @Override
  public double[] toArray() {
	  return toArray(null);
  }
  
  /**
   * Returns an array containing the elements of this list in the order
   * they are contained in the list.
   * @return double[]
   */
  public double[] toArray(double[] buffer) {
    double[] rtn = buffer != null && buffer.length >= mSize ? buffer : new double[mSize];
    System.arraycopy(mData, 0, rtn, 0, mSize);
    return rtn;
  }
  
  /**
   * Returns a deep-copy clone.
   * @return Object
   */
  public DoubleArrayList clone() {
    DoubleArrayList clone = null;
    try {
      clone = (DoubleArrayList) super._clone();
      clone.mData = new double[this.mData.length];
      System.arraycopy(this.mData, 0, clone.mData, 0, this.mData.length);
      clone.mModCount = 0;
    } catch (CloneNotSupportedException e) {
      throw new InternalError();
    }
    return clone;
  }
  
  // Expand the capacity of the list.
  private void expand() {
    int newCap = 2*mData.length;
    if (newCap == 0) { // Just in case initialSize was 0.
      newCap = 1;
    }
    double[] newData = new double[newCap];
    System.arraycopy(mData, 0, newData, 0, mSize);
    mData = newData;
  }

}
