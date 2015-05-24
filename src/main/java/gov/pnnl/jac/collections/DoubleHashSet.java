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
 * <p>Implementation of <tt>DoubleSet</tt> which uses hashing to add and
 * remove values.</p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: Battelle Memorial Institute</p>
 *
 * @author R. Scarberry
 * @version 1.0
 */
public class DoubleHashSet implements DoubleSet {

  private DoubleObjectHashMap mMap;  // We use the keys of a DoubleObjectHashMap
                                    // to store this DoubleHashSet's values.
                                    // The object values of the
                                    // DoubleObjectHashMap aren't used,
                                    // and can be null.
  /**
   * Constructor
   * @param     int initialCapacity The initial capacity of the set.
   * @param     double loadFactor The load factor to use in determining when
   *   to rehash.  This should be > 0.0f and < 1.0f.
   * @exception IllegalArgumentException If initialCapacity < 0 or if the loadFactor is invalid.
   */
  public DoubleHashSet (int initialCapacity, double loadFactor) {
    mMap = new DoubleObjectHashMap(initialCapacity, loadFactor);
  }

  /**
   * Constructor specifying initial capacity only.  The load factor defaults
   * to 0.75f.
   * @param     int initialCapacity The initial capacity of the set.
   * @exception IllegalArgumentException If initialCapacity < 0.
   */
  public DoubleHashSet (int initialCapacity) {
      this(initialCapacity, 0.75f);
  }

  /**
   * Default constructor.  The initial capacity defaults to 11 and the
   * load factor defaults to 0.75f.
   */
  public DoubleHashSet () {
      this(11, 0.75f);
  }
  
  public DoubleHashSet(DoubleCollection c) {
	  this(2 * c.size());
	  this.addAll(c);
  }
  
  public DoubleHashSet(double[] values) {
	  this(2 * values.length);
	  addAll(values);
  }

  /**
   * Test the set for containment of the specified value.
   * @param v - the value to be tested.
   * @return true, if the set contains the specified value, false otherwise.
   */
  public boolean contains(double v) {
    return mMap.containsKey(v);
  }

  /**
   * Test for containment of all values in the specified collection.
   * @param c - the values to be tested for containment.
   * @return true only if all values in the argument are contained in the set.
   */
  public boolean containsAll(DoubleCollection c) {
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
  public boolean containsAll(double[] values) {
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
  public boolean add(double value) {
    if (!mMap.containsKey(value)) {
      mMap.put(value, null);
      return true;
    }
    return false;
  }

  /**
   * Adds all the specified values to the set.
   * @param c - a collection containing the values to be added.
   */
  public boolean addAll(DoubleCollection c) {
    if (c == this) {
      return false;
    }
    return addAll(c.toArray());
  }

  /**
   * Adds all the specified values to the set.
   * @param values - an array of values to be added.
   */
  public boolean addAll(double[] values) {
    int originalSize = size();
    int n = values.length;
    for (int i=0; i<n; i++) {
      double v = values[i];
      mMap.put(v, null);
    }
    return size() != originalSize;
  }

  /**
   * Remove the specified value from the set, if contained in the set.
   * @param n - the value to be removed.
   */
  public boolean remove(double value) {
    if (mMap.containsKey(value)) {
      mMap.remove(value);
      return true;
    }
    return false;
  }

  /**
   * Remove all values from the specified array from the set, if present.
   * @param values - the values to be removed.
   */
  public boolean removeAll(DoubleCollection c) {
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
  public boolean removeAll(double[] values) {
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
   * @see #remove(double)
   * @see #contains(double)
   */
  public boolean retainAll(DoubleCollection c) {
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
   * @see #remove(double)
   * @see #contains(double)
   */
  public boolean retainAll(double[] values) {
    int originalSize = size();
    clear();
    addAll(values);
    return size() != originalSize;
  }

  /**
   * Returns an iterator over the elements of this set.
   * @return DoubleIterator
   */
  public DoubleCollectionIterator iterator() {
    return mMap.keyIterator();
  }

  /**
   * Returns the number of elements in the set.
   * @return int - the number of unique values contained in the set.
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
   * @return double[] - an array containing the values contained in the set.
   */
  public double[] toArray() {
    return mMap.keys();
  }

  /**
   * Returns a new <tt>DoubleSet</tt> containing the union of the values
   * in this set and the specified set.
   * @param other - the other set.
   * @return - a new <tt>DoubleSet</tt> object with the same class as the
   *   receiver.
   */
  public DoubleSet unionWith(DoubleSet other) {
    // Using this initial capacity, the new set shouldn't have to
    // rehash more than once.
    int initialCap = 2 * Math.max(this.size(), other.size());
    DoubleSet newSet = new DoubleHashSet(initialCap);
    newSet.addAll(this.toArray());
    newSet.addAll(other.toArray());
    return newSet;
  }

  /**
   * Returns a new <tt>DoubleSet</tt> containing the intersection of the values
   * in this set and the specified set.
   * @param other - the other set.
   * @return - a new <tt>DoubleSet</tt> object with the same class as the
   *   receiver.
   */
  public DoubleSet intersectionWith(DoubleSet other) {
    DoubleSet newSet = new DoubleHashSet();
    double[] values = this.toArray();
    int n = values.length;
    for (int i=0; i<n; i++) {
      double v = values[i];
      if (other.contains(v)) {
        newSet.add(v);
      }
    }
    return newSet;
  }

  /**
   * Returns a new <tt>DoubleSet</tt> containing the values
   * contained either in the receiver or the specified set, but not both.
   * @param other - the other set.
   * @return - a new <tt>DoubleSet</tt> object with the same class as the
   *   receiver.
   */
  public DoubleSet xorWith(DoubleSet other) {
    DoubleSet newSet = new DoubleHashSet();
    double[] values = this.toArray();
    int n = values.length;
    for (int i=0; i<n; i++) {
      double v = values[i];
      if (!other.contains(v)) {
        newSet.add(v);
      }
    }
    values = other.toArray();
    n = values.length;
    for (int i=0; i<n; i++) {
      double v = values[i];
      if (!this.contains(v)) {
        newSet.add(v);
      }
    }
    return newSet;
  }

  /**
   * Returns the hash code for the set.  Returns value computed by
   * <tt>MapUtils.computeHash(DoubleSet)</tt>.
   * @return int
   */
  public int hashCode() {
    return MapUtils.computeHash(this);
  }

  /**
   * Check equality with another <tt>DoubleSet</tt>.
   * @param o Object
   * @return true if the other set is the same size as this set and contains
   *   the same values, whether or not the other set is the same class as
   *   this set.
   */
  public boolean equals(Object o) {
    if (o instanceof DoubleSet) {
      return MapUtils.checkEqual(this, (DoubleSet) o);
    }
    return false;
  }

}
