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
 * <p>Implementation of <tt>FloatObjectMap</tt> that uses hashing to map
 * float keys to object values.</p>
 *
 * @see  FloatObjectMap
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: Battelle Memorial Institute</p>
 *
 * @author R. Scarberry
 * @version 1.0
 */
public class FloatObjectHashMap<E> implements FloatObjectMap<E>, Cloneable {

  private static final byte FREE = 0;
  private static final byte FULL = 1;
  private static final byte REMOVED = 2;

  private float[] mKeys;
  private Object[] mValues;
  private byte[] mStates;
  // The load factor;  used to determine when to resize.
  private float mLoadFactor;
  // Number of entries not equal to FREE.
  private int mElementCount;
  // Max number of elements before having to resize/rehash.
  private int mMaxElementCount;
  // Count of elements equal to FULL.
  private int mFullCount;
  // Mainly used by iterator to check for concurrent modification.
  private int mModCount;

  /**
   * Constructor
   * @param     int initialCapacity The initial capacity of the map.
   * @param     float loadFactor The load factor to use in determining when
   *   to rehash.  This should be > 0.0f and < 1.0f.
   * @exception IllegalArgumentException If initialCapacity < 0 or if the loadFactor is invalid.
   */
  public FloatObjectHashMap (int initialCapacity, float loadFactor) {
      if (initialCapacity < 0) {
          throw  new IllegalArgumentException("invalid initial capacity: "
                  + initialCapacity);
      }
      if (Float.isNaN(loadFactor) || loadFactor <= 0.0f || loadFactor >= 1.0f) {
          throw  new IllegalArgumentException("invalid load factor: " + loadFactor);
      }
      // Turn the initial capacity into a prime number >= 3.
      // (If not a prime, insertionIndex() can get caught in infinite loops.)
      initialCapacity = MapUtils.nextRehashPrime(initialCapacity);
      // Initialize arrays
      mKeys = new float[initialCapacity];
      mValues = new Object[initialCapacity];
      mStates = new byte[initialCapacity];
      Arrays.fill(mStates, FREE);
      mLoadFactor = loadFactor;
      // Compute mMaxElementCount, the threshold used to determine when to rehash.
      //    It should be at least 1 and no greater than initialCapacity - 1.
      mMaxElementCount = Math.min(initialCapacity - 1, Math.max(1, (int)(mLoadFactor*initialCapacity)));
  }

  /**
   * Constructor specifying initial capacity only.  The load factor defaults
   * to 0.75f.
   * @param     int initialCapacity The initial capacity of the map.
   * @exception IllegalArgumentException If initialCapacity < 0.
   */
  public FloatObjectHashMap (int initialCapacity) {
      this(initialCapacity, 0.75f);
  }

  /**
   * Default constructor.  The initial capacity defaults to 11 and the
   * load factor defaults to 0.75f.
   */
  public FloatObjectHashMap () {
      this(11, 0.75f);
  }

  /**
   * Returns the number of key-value pairs contained in the map.
   *
   * @return the number of mappings.
   */
  public int size () {
      return  mFullCount;
  }

  /**
   * Returns <tt>true</tt> if this map contains no key-value pairs.
   *
   * @return <tt>true</tt> if the number of mappings is zero.
   */
  public boolean isEmpty() {
      return mFullCount == 0;
  }

  /**
   * Returns <tt>true</tt> if the receiver maps the specifed float key
   * to an Object value.
   *
   * @param key a float key.
   * @return <tt>true</tt> if this map contains a mapping for the key.
   */
  public boolean containsKey(float key) {
    return  insertionIndex(key, true) < 0;
  }

  /**
   * Returns <tt>true</tt> if the receiver maps at least one key to the
   * specified value.
   *
   * @param value the value.
   * @return <tt>true</tt> if at least one key maps to the value.
   */
  public boolean containsValue(E value) {
    final byte[] states = mStates;
    int len = states.length;
    final Object[] values = mValues;
    for (int i=0; i<len; i++) {
      if (states[i] == FULL) {
        if (value == null) {
          if (values[i] == null) {
            return true;
          }
        } else if (value.equals(values[i])) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Returns the value to which the receiver maps the given key, or
   * <tt>null</tt> if the key is not contained.  Since a key may be
   * mapped to <tt>null</tt>, a <tt>null</tt> return value does not
   * <i>necessarily</i> prove that the map does not contain the key.
   * Use <tt>containsKey</tt> to conclusively determine if the receiver
   * contains a mapping for the given key.
   *
   * @param key the key.
   * @return the value to which the receiver maps the key, or
   *	       <tt>null</tt> if the map does not contain the key.
   *
   * @see #containsKey(E)
   */
  public E get(float key) {
    E rtn = null;
    int ndx = insertionIndex(key, true);
    if (ndx < 0) {
        rtn = (E) mValues[-ndx - 1];
    }
    return  rtn;
  }

  // Modification Operations

  /**
   * Maps the specified float key to the specified value in this map.
   * (This is an optional operation to permit read-only implementations.)
   * The previous mapping for the key, if present, is overwritten.
   *
   * @param key the key.
   * @param value value to be associated with the key.
   * @return the previous value paired to the given key, or <tt>null</tt> if no
   *   previous mapping was present.
   *
   * @throws UnsupportedOperationException if the <tt>put</tt> operation is
   *	          not supported by this map.
   * @throws IllegalArgumentException if some aspect of this key or value
   *	          prevents it from being stored in this map.
   * @throws NullPointerException this map does not permit <tt>null</tt>
   *            values, and the specified value is
   *            <tt>null</tt>.
   */
  public E put(float key, E value) {
    E rtn = null;
    // Get the insertion index -- same index used for all arrays.
    int ndx = insertionIndex(key, true);
    if (ndx < 0) { // Negative index means the key was already contained.
        ndx = -ndx - 1;
        rtn = (E) mValues[ndx];
    }
    if (mStates[ndx] == FREE && mElementCount >= mMaxElementCount) {
        int oldCapacity = mKeys.length;
        int newCapacity = MapUtils.newCapacity(oldCapacity);
        if (oldCapacity == newCapacity) {
          throw new UnsupportedOperationException("map cannot be expanded");
        }
        rehash(newCapacity);      // Expand the available space.
        return put(key, value);  // Recursive call to this same method.
    }
    else { // Do not need to rehash
        mKeys[ndx] = key;
        mValues[ndx] = value;
        // The element count is the number of mStates entries != FREE
        if (mStates[ndx] == FREE) {
            mElementCount++;
        }
        if (mStates[ndx] != FULL) {
            mStates[ndx] = FULL;
            // Only inc the full count if the entry was FREE or REMOVED.
            mFullCount++;
        }
        mModCount++;
    }
    return  rtn;
  }

  /**
   * Remove the mapping for the specified key from the receiver. (To permit
   * read-only implementations, this is an optional operation.)
   *
   * @param key the key.
   * @return the previous value paired to the given key, or <tt>null</tt> if no
   *   previous mapping was present.  (A <tt>null</tt> return does not
   *   <i>necessarily</i> prove that no mapping was present: the key may have
   *   been explicitly mapped to <tt>null</tt>.)
   * @throws UnsupportedOperationException if the <tt>remove</tt> method is
   *         not supported by this map.
   */
  public E remove(float key) {
    E rtn = null;
    int ndx = insertionIndex(key, true);
    if (ndx < 0) {
        ndx = -ndx - 1;
        rtn = (E) mValues[ndx];
        mKeys[ndx] = -1;
        mStates[ndx] = REMOVED;
        // DO NOT hang onto the reference or may cause the object to "loiter".
        mValues[ndx] = null;
        mFullCount--;
        mModCount++;
    }
    return  rtn;
  }

  // Bulk Operations
  /**
   * Adds all key-value mapping from the specified <tt>FloatObjectMap</tt> to the
   * receiver.  (This is an optional operation, since some implementations
   * may be read-only.)  These mappings may replace mappings already present
   * in the receiver.
   *
   * @param map contains mappings to be copied to this map.
   *
   * @throws UnsupportedOperationException if the <tt>putAll</tt> method is
   * 		  not supported by this map.
   *
   * @throws ClassCastException if the class of a key or value in the
   * 	          specified map prevents it from being stored in this map.
   *
   * @throws IllegalArgumentException some aspect of a key or value in the
   *	          specified map prevents it from being stored in this map.
   *
   * @throws NullPointerException this map does not permit <tt>null</tt>
   *            values, and the specified value is
   *            <tt>null</tt>.
   */
  public void putAll(FloatObjectMap<? extends E> map) {
    float[] keys = map.keys();
    int n = keys.length;
    for (int i=0; i<n; i++) {
      float k = keys[i];
      put(k, map.get(k));
    }
  }

  /**
   * Remove all key-value mappings from the receiver. (Optional operation.)
   *
   * @throws UnsupportedOperationException if this operation is not allowed
   *   by the implementation.
   */
  public void clear() {
    Arrays.fill(mStates, FREE);
    Arrays.fill(mKeys, -1);
    Arrays.fill(mValues, null);
    mElementCount = mFullCount = 0;
    mModCount++;
  }

  // Views

  /**
   * Returns the float keys for which the receiver contains mappings.  The
   * order of the returned keys is undefined.  In the event the receiver contains
   * no mappings, the returned array should be length zero, not <tt>null</tt>.
   *
   * @return an array containing the keys with mappings.
   */
  public float[] keys() {
    final byte[] states = mStates;
    final float[] keys = mKeys;
    final int length = mStates.length;
    float[] rtn = new float[mFullCount];
    int count = 0;
    for (int i = 0; i < length; i++) {
        if (states[i] == FULL) {
            rtn[count++] = keys[i];
        }
    }
    return  rtn;
  }

  /**
   * Returns an iterator over the keys of this map.
   * @return FloatIterator
   */
  public FloatCollectionIterator keyIterator() {
    return new Itr();
  }

  /**
   * Returns a collection of the values contained in the receiver.  The
   * returned collection should not be the backing store for the map, so
   * changes to the collection should not affect the map and vice-versa.  This
   * is unlike the <tt>Map</tt> interface defined in <tt>java.util</tt>.
   *
   * @return a collection containing the values contained in the map.
   */
  public Collection<E> values() {
    List<E> valueList = new ArrayList<E>(mFullCount);
    final byte[] states = mStates;
    final Object[] values = mValues;
    final int length = mStates.length;
    for (int i = 0; i < length; i++) {
        if (states[i] == FULL) {
            valueList.add((E) values[i]);
        }
    }
    return valueList;
  }

  /**
   * Returns the key-value pairs in a <tt>Set</tt>.  The elements of the set
   * are instances of <tt>FloatObjectMap.Entry</tt>.  Modifications to this set do not
   * affect the map and vice-versa.
   * @return set containing instances of <tt>FloatObjectMap.Entry</tt>.
   */
  public Set<FloatObjectMap.Entry<E>> entrySet() {
    Set<FloatObjectMap.Entry<E>> set = new HashSet<FloatObjectMap.Entry<E>>(2*mFullCount);
    if (mFullCount > 0) {
      final byte[] states = mStates;
      final float[] keys = mKeys;
      final Object[] values = mValues;
      int n = states.length;
      for (int i=0; i<n; i++) {
        if (states[i] == FULL) {
          set.add(new Entry(keys[i], (E) values[i]));
        }
      }
    }
    return set;
  }

  // Comparison and hashing
  /**
   * Compares the receiver with the specified object for equality.  Returns
   * <tt>true</tt> if the specified object is also an <tt>FloatObjectMap</tt> and the two
   * represent the same mappings.  They may not be the same implementation class.
   *
   * @param o object to be compared with this map.
   * @return <tt>true</tt> if the specified object contains the same
   *   key-value mappings as this map.
   */
  @Override
  public boolean equals(Object o) {
    return (o instanceof FloatObjectMap<?> ? MapUtils.checkEqual(this, (FloatObjectMap<?>) o) : false);
  }

  /**
   * Returns the hash code for this map.
   *
   * @return the hash code for this map.
   * @see Object#hashCode()
   * @see Object#equals(Object)
   * @see #equals(Object)
   */
  @Override
  public int hashCode() {
    return MapUtils.computeHash(this);
  }

  /**
   * Returns a deep-copy clone.
   * @return Object
   */
  @Override
  public Object clone() {
    try {
      FloatObjectHashMap<E> clone = (FloatObjectHashMap<E>) super.clone();
      clone.mKeys = (float[]) this.mKeys.clone();
      clone.mValues = (Object[]) this.mValues.clone();
      clone.mStates = (byte[]) this.mStates.clone();
      clone.mModCount = 0;
      return clone;
    } catch (CloneNotSupportedException e) {
      throw new InternalError();
    }
  }

  // Find the insertion index for the provided String.
   private int insertionIndex (float key, boolean equalsCheck) {
       final byte[] states = mStates;
       final float[] keys = mKeys;
       final int length = mStates.length;
       int hash = Float.floatToIntBits(key);
       int i = hash%length;
       // double hashing, see http://www.eece.unm.edu/faculty/heileman/hash/node4.html
       int decrement = hash%(length - 2);
       if (decrement == 0) {
           decrement = 1;
       }
       // stop if we find a removed or free slot, or if we find the key itself
       // do NOT skip over removed slots (yes, open addressing is like that...)
       while (states[i] != FREE && !(equalsCheck && key == keys[i])) {
           i -= decrement;
           if (i < 0) {
               i += length;
           }
       }
       if (states[i] == FULL) {
           // key already contained at slot i.
           // return a negative number identifying the slot.
           return  -i - 1;
       }
       // not already contained, should be inserted at slot i.
       // return a number >= 0 identifying the slot.
       return  i;
   }

   private void rehash (int newCapacity) {
       int oldCapacity = mKeys.length;
       float[] oldKeys = mKeys;
       Object[] oldValues = mValues;
       byte[] oldStates = mStates;
       mKeys = new float[newCapacity];
       mValues = new Object[newCapacity];
       mStates = new byte[newCapacity];
       Arrays.fill(mStates, FREE);
       mMaxElementCount = (int)(mLoadFactor*newCapacity);
       if (mMaxElementCount == 0) {
           mMaxElementCount = 1;
       }
       for (int i = 0; i < oldCapacity; i++) {
           if (oldStates[i] == FULL) {
               int ndx = insertionIndex(oldKeys[i], false);
               mKeys[ndx] = oldKeys[i];
               mValues[ndx] = oldValues[i];
               mStates[ndx] = oldStates[i];
           }
       }       // for
       mModCount++;
   }

   private class Itr implements FloatCollectionIterator {

     private int mCursor = -1; // Init to -1, so first advanceCursor() will work.
     private int mLastReturnNdx = -1;
     private int mExpectedModCount = mModCount;

     Itr() {
       adv();
     }

     private void adv() {
       mCursor++;
       int n = mStates.length;
       while(mCursor < n && mStates[mCursor] != FULL) {
         mCursor++;
       }
     }

     public boolean hasNext() {
       return (mCursor < mStates.length);
     }

     public float next() {
       try {
         float rtn = 0f;
         if (mStates[mCursor] == FULL) {
           rtn = mKeys[mCursor];
           checkForComodification();
           mLastReturnNdx = mCursor;
           adv();
         }
         return rtn;
       } catch (IndexOutOfBoundsException e) {
         checkForComodification();
         throw new NoSuchElementException();
       }
     }

     public void remove() {
       if (mLastReturnNdx == -1) {
         throw new IllegalStateException();
       }
       checkForComodification();
       if (mStates[mLastReturnNdx] == FULL) {
         try {
           E removed = FloatObjectHashMap.this.remove(mKeys[mLastReturnNdx]);
           mLastReturnNdx = -1;
           mExpectedModCount = mModCount;
         } catch (IndexOutOfBoundsException e) {
           throw new ConcurrentModificationException();
         }
       } else {
         throw new ConcurrentModificationException();
       }
     }

     private void checkForComodification() {
       if (mModCount != mExpectedModCount) {
         throw new ConcurrentModificationException();
       }
     }
   }

  private static class Entry<E> implements FloatObjectMap.Entry<E> {

    float key;
    E value;

    Entry(float key, E value) {
        this.key = key;
        this.value = value;
    }

    public float getKey() {
        return key;
    }

    public E getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
          return true;
        }
        if (o instanceof FloatObjectMap.Entry<?>) {
          FloatObjectMap.Entry e = (FloatObjectMap.Entry<?>) o;
          return (key == e.getKey() &&
           (value==null ? e.getValue()==null : value.equals(e.getValue())));
        }
        return false;
    }

    @Override
    public int hashCode() {
      return Float.floatToIntBits(key) + (value == null ? 0 : value.hashCode());
    }

    @Override
    public String toString() {
        return String.valueOf(key) + "=" + value;
    }
  }
}
