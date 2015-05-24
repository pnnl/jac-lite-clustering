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
 * <p>Implementation of <tt>IntSet</tt> which uses hashing to add and
 * remove values.</p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: Battelle Memorial Institute</p>
 *
 * @author R. Scarberry
 * @version 1.0
 */
public class IntHashSet implements IntSet {

  private IntIntHashMap mMap;

  /**
   * Constructor
   * @param     int initialCapacity The initial capacity of the set.
   * @param     float loadFactor The load factor to use in determining when
   *   to rehash.  This should be > 0.0f and < 1.0f.
   * @exception IllegalArgumentException If initialCapacity < 0 or if the loadFactor is invalid.
   */
  public IntHashSet (int initialCapacity, float loadFactor) {
    mMap = new IntIntHashMap(initialCapacity, loadFactor);
  }

  /**
   * Constructor specifying initial capacity only.  The load factor defaults
   * to 0.75f.
   * @param     int initialCapacity The initial capacity of the set.
   * @exception IllegalArgumentException If initialCapacity < 0.
   */
  public IntHashSet (int initialCapacity) {
      this(initialCapacity, 0.75f);
  }

  /**
   * Default constructor.  The initial capacity defaults to 11 and the
   * load factor defaults to 0.75f.
   */
  public IntHashSet () {
      this(11, 0.75f);
  }
  
  public IntHashSet(int[] initialValues) {
	  this(2 * initialValues.length);
	  addAll(initialValues);
  }

  /**
   * Test the set for containment of the specified value.
   * @param n - the value to be tested.
   * @return true, if the set contains the specified value, false otherwise.
   */
  public boolean contains(int n) {
    return mMap.containsKey(n);
  }

  /**
   * Test for containment of all values in the specified collection.
   * @param c - the values to be tested for containment.
   * @return true only if all values in the argument are contained in the set.
   */
  public boolean containsAll(IntCollection c) {
    if (c == this) {
      return true;
    }
    return containsAll(c.toArray());
  }

  /**
   * Test for containment of all values in the specified array.
   * @param values - the values to be tested for containment.
   * @return true only if all values in the argument are contained in the set.
   */
  public boolean containsAll(int[] values) {
    int n = values.length;
    for (int i=0; i<n; i++) {
      if (!mMap.containsKey(values[i])) {
        return false;
      }
    }
    return true;
  }

  /**
   * Add the specified value to the set.  If the set already contains the
   * value, the set remains unchanged.
   * @param n - the value to be added.
   */
  public boolean add(int n) {
    if (!mMap.containsKey(n)) {
      mMap.put(n, n);
      return true;
    }
    return false;
  }

  /**
   * Adds all the specified values to the set.
   * @param c - a collection containing the values to be added.
   */
  public boolean addAll(IntCollection c) {
    if (c == this) {
      return false;
    }
    return addAll(c.toArray());
  }

  /**
   * Adds all the specified values to the set.
   * @param values - an array of values to be added.
   */
  public boolean addAll(int[] values) {
    int originalSize = size();
    int n = values.length;
    for (int i=0; i<n; i++) {
      int v = values[i];
      mMap.put(v, v);
    }
    return size() != originalSize;
  }

  /**
   * Remove the specified value from the set, if contained in the set.
   * @param n - the value to be removed.
   */
  public boolean remove(int n) {
    if (mMap.containsKey(n)) {
      mMap.remove(n);
      return true;
    }
    return false;
  }

  /**
   * Remove all values from the specified array from the set, if present.
   * @param values - the values to be removed.
   */
  public boolean removeAll(IntCollection c) {
    if (c == this && size() > 0) {
      clear();
      return true;
    }
    return removeAll(c.toArray());
  }

  /**
   * Remove all values from the specified array from the set, if present.
   * @param values - the values to be removed.
   */
  public boolean removeAll(int[] values) {
    int originalSize = size();
    int n = values.length;
    for (int i=0; i<n; i++) {
      mMap.remove(values[i]);
    }
    return size() != originalSize;
  }

  public void clear() {
    mMap.clear();
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
    if (c != this) {
      return retainAll(c.toArray());
    }
    return false;
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
    // Want to retain values already present in this set which are also in the passed array.
    int n = values != null ? values.length : 0;
    // Gather everything in values that's already in this set.  Place into tmpSet.
    IntHashSet tmpSet = new IntHashSet(2*n);
    for (int i=0; i<n; i++) {
      if (this.contains(values[i])) {
        tmpSet.add(values[i]);
      }
    }
    // this.size() will always be >= tmpSet.size().  If the sizes are equal, they
    // have to have the same elements, so no op is necessary.
    if (tmpSet.size() != this.size()) {
      // Just clear this set and add the contents of tmpSet.
      clear();
      addAll(tmpSet);
      return true;
    }
    return false;
  }

  /**
   * Returns an iterator over the elements of this set.
   * @return IntIterator
   */
  public IntCollectionIterator iterator() {
    return mMap.keyIterator();
  }

  /**
   * Returns the number of elements in the set.
   * @return int - the number of unique integer values contained in the set.
   */
  public int size() {
    return mMap.size();
  }

  /**
   * Returns whether or not this set contains any elements.
   * @return boolean
   */
  public boolean isEmpty() {
    return size() == 0;
  }

  /**
   * Returns the values contained in the set as an array.  The ordering of
   * the values is implementation dependent.
   * @return int[] - an array containing the values contained in the set.
   */
  public int[] toArray() {
    return mMap.keys();
  }

  /**
   * Returns a new <tt>IntSet</tt> containing the union of the values
   * in this set and the specified set.
   * @param other - the other set.
   * @return - a new <tt>IntSet</tt> object with the same class as the
   *   receiver.
   */
  public IntSet unionWith(IntSet other) {
    // Using this initial capacity, the new set shouldn't have to
    // rehash more than once.
    int initialCap = 2 * Math.max(this.size(), other.size());
    IntSet newSet = new IntHashSet(initialCap);
    newSet.addAll(this.toArray());
    newSet.addAll(other.toArray());
    return newSet;
  }

  /**
   * Returns a new <tt>IntSet</tt> containing the intersection of the values
   * in this set and the specified set.
   * @param other - the other set.
   * @return - a new <tt>IntSet</tt> object with the same class as the
   *   receiver.
   */
  public IntSet intersectionWith(IntSet other) {
    IntSet newSet = new IntHashSet();
    int[] values = this.toArray();
    int n = values.length;
    for (int i=0; i<n; i++) {
      int v = values[i];
      if (other.contains(v)) {
        newSet.add(v);
      }
    }
    return newSet;
  }

  /**
   * Returns a new <tt>IntSet</tt> containing the values
   * contained either in the receiver or the specified set, but not both.
   * @param other - the other set.
   * @return - a new <tt>IntSet</tt> object with the same class as the
   *   receiver.
   */
  public IntSet xorWith(IntSet other) {
    IntSet newSet = new IntHashSet();
    int[] values = this.toArray();
    int n = values.length;
    for (int i=0; i<n; i++) {
      int v = values[i];
      if (!other.contains(v)) {
        newSet.add(v);
      }
    }
    values = other.toArray();
    n = values.length;
    for (int i=0; i<n; i++) {
      int v = values[i];
      if (!this.contains(v)) {
        newSet.add(v);
      }
    }
    return newSet;
  }

  /**
   * Returns the hash code for the set.  Returns value computed by
   * <tt>MapUtils.computeHash(IntSet)</tt>.
   * @return int
   */
  public int hashCode() {
    return MapUtils.computeHash(this);
  }

  /**
   * Check equality with another <tt>IntSet</tt>.
   * @param o Object
   * @return true if the other set is the same size as this set and contains
   *   the same values, whether or not the other set is the same class as
   *   this set.
   */
  public boolean equals(Object o) {
    if (o instanceof IntSet) {
      return MapUtils.checkEqual(this, (IntSet) o);
    }
    return false;
  }

}
