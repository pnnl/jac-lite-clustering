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

import gov.pnnl.jac.util.ClassUtils;

import java.util.*;

/**
 * <p>Implementation of <tt>IntObjectMap</tt> that uses hashing to map
 * integer keys to object values.</p>
 *
 * @see  IntObjectMap
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: Battelle Memorial Institute</p>
 *
 * @author R. Scarberry
 * @version 1.0
 */
public class IntObjectHashMap<E> implements IntObjectMap<E>, Cloneable {

  private static final byte FREE = 0;
  private static final byte FULL = 1;
  private static final byte REMOVED = 2;

  private int[] mKeys;
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

  // This never has to be instantiated.
  private transient ClassUtils<E> mCU;
  
  /**
   * Constructor
   * @param     int initialCapacity The initial capacity of the map.
   * @param     float loadFactor The load factor to use in determining when
   *   to rehash.  This should be > 0.0f and < 1.0f.
   * @exception IllegalArgumentException If initialCapacity < 0 or if the loadFactor is invalid.
   */
  public IntObjectHashMap (int initialCapacity, float loadFactor) {
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
      mKeys = new int[initialCapacity];
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
  public IntObjectHashMap (int initialCapacity) {
      this(initialCapacity, 0.75f);
  }

  /**
   * Default constructor.  The initial capacity defaults to 11 and the
   * load factor defaults to 0.75f.
   */
  public IntObjectHashMap () {
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
   * Returns <tt>true</tt> if the receiver maps the specifed integer key
   * to an Object value.
   *
   * @param key an integer key.
   * @return <tt>true</tt> if this map contains a mapping for the key.
   */
  public boolean containsKey(int key) {
    return  insertionIndex(key, true) < 0;
  }

  /**
   * Returns <tt>true</tt> if the receiver maps at least one key to the
   * specified value.
   *
   * @param value the value.
   * @return <tt>true</tt> if at least one key maps to the value.
   */
  public boolean containsValue(Object value) {
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
   * @see #containsKey(int)
   */
  public E get(int key) {
    E rtn = null;
    int ndx = insertionIndex(key, true);
    if (ndx < 0) {
        rtn = ClassUtils.uncheckedCast(mValues[-ndx - 1], mCU);
    }
    return  rtn;
  }

  // Modification Operations

  /**
   * Maps the specified integer key to the specified value in this map.
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
  public E put(int key, E value) {
    E rtn = null;
    // Get the insertion index -- same index used for all arrays.
    int ndx = insertionIndex(key, true);
    if (ndx < 0) { // Negative index means the key was already contained.
        ndx = -ndx - 1;
        rtn = ClassUtils.uncheckedCast(mValues[ndx], mCU);
    }
    if (mStates[ndx] == FREE && mElementCount >= mMaxElementCount) {
        int oldCapacity = mKeys.length;
        int newCapacity = MapUtils.newCapacity(oldCapacity);
        if (oldCapacity == newCapacity) {
          throw new UnsupportedOperationException("map cannot be expanded");
        }
        rehash(newCapacity);      // Expand the available space.
        return  put(key, value);  // Recursive call to this same method.
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
   * Remove the mapping for the specified key from the receiver.
   *
   * @param key the key.
   * @return the previous value paired to the given key, or <tt>null</tt> if no
   *   previous mapping was present.  (A <tt>null</tt> return does not
   *   <i>necessarily</i> prove that no mapping was present: the key may have
   *   been explicitly mapped to <tt>null</tt>.)
   */
  public E remove(int key) {
    E rtn = null;
    int ndx = insertionIndex(key, true);
    if (ndx < 0) {
        ndx = -ndx - 1;
        rtn = ClassUtils.uncheckedCast(mValues[ndx], mCU);
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
   * Adds all key-value mapping from the specified <tt>IntObjectMap</tt> to the
   * receiver.   These mappings may replace mappings already present
   * in the receiver.
   *
   * @param map contains mappings to be copied to this map.
   *
   * @throws IllegalArgumentException some aspect of a key or value in the
   *	          specified map prevents it from being stored in this map.
   *
   * @throws NullPointerException this map does not permit <tt>null</tt>
   *            values, and the specified value is
   *            <tt>null</tt>.
   */
  public void putAll(IntObjectMap<? extends E> map) {
    int[] keys = map.keys();
    int n = keys.length;
    for (int i=0; i<n; i++) {
      int k = keys[i];
      put(k, map.get(k));
    }
  }

  /**
   * Remove all key-value mappings from the receiver.
   *
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
   * Returns the integer keys for which the receiver contains mappings.  The
   * order of the returned keys is undefined.  In the event the receiver contains
   * no mappings, the returned array should be length zero, not <tt>null</tt>.
   *
   * @return an array containing the keys with mappings.
   */
  public int[] keys() {
    final byte[] states = mStates;
    final int[] keys = mKeys;
    final int length = mStates.length;
    int[] rtn = new int[mFullCount];
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
   * @return IntIterator
   */
  public IntCollectionIterator keyIterator() {
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
            valueList.add(ClassUtils.uncheckedCast(values[i], mCU));
        }
    }
    return valueList;
  }

  /**
   * Returns the key-value pairs in a <tt>Set</tt>.  The elements of the set
   * are instances of <tt>IntObjectMap.Entry</tt>.  Modifications to this set do not
   * affect the map and vice-versa.
   * @return set containing instances of <tt>IntObjectMap.Entry</tt>.
   */
  @SuppressWarnings({ "unchecked" })
  public Set<IntObjectMap.Entry<E>> entrySet() {
    Set<IntObjectMap.Entry<E>> set = new HashSet<IntObjectMap.Entry<E>>(2*mFullCount);
    if (mFullCount > 0) {
      final byte[] states = mStates;
      final int[] keys = mKeys;
      final Object[] values = mValues;
      int n = states.length;
      for (int i=0; i<n; i++) {
        if (states[i] == FULL) {
          set.add(new Entry<E>(keys[i], (E) values[i]));
        }
      }
    }
    return set;
  }

  // Comparison and hashing
  /**
   * Compares the receiver with the specified object for equality.  Returns
   * <tt>true</tt> if the specified object is also an <tt>IntObjectMap</tt> and the two
   * represent the same mappings.  They may not be the same implementation class.
   *
   * @param o object to be compared with this map.
   * @return <tt>true</tt> if the specified object contains the same
   *   key-value mappings as this map.
   */
  public boolean equals(Object o) {
    return (o instanceof IntObjectMap<?> ? 
            MapUtils.checkEqual(this, (IntObjectMap<?>) o) : false);
  }

  /**
   * Returns the hash code for this map.
   *
   * @return the hash code for this map.
   * @see Object#hashCode()
   * @see Object#equals(Object)
   * @see #equals(Object)
   */
  public int hashCode() {
    return MapUtils.computeHash(this);
  }

  /**
   * Returns a deep-copy clone.
   * @return Object
   */
  public Object clone() {
    try {
      @SuppressWarnings("unchecked")
      IntObjectHashMap<E> clone = (IntObjectHashMap<E>) super.clone();
      clone.mKeys = (int[]) this.mKeys.clone();
      clone.mValues = (Object[]) this.mValues.clone();
      clone.mStates = (byte[]) this.mStates.clone();
      clone.mModCount = 0;
      return clone;
    } catch (CloneNotSupportedException e) {
      throw new InternalError();
    }
  }

  // Find the insertion index for the provided String.
   private int insertionIndex (int key, boolean equalsCheck) {
       final byte[] states = mStates;
       final int[] keys = mKeys;
       final int length = mStates.length;
       int hash = key & 0x7FFFFFFF;
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
       int[] oldKeys = mKeys;
       Object[] oldValues = mValues;
       byte[] oldStates = mStates;
       mKeys = new int[newCapacity];
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

   private class Itr implements IntCollectionIterator {

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

     public int next() {
       try {
         int rtn = 0;
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
           IntObjectHashMap.this.remove(mKeys[mLastReturnNdx]);
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

  private static class Entry<E> implements IntObjectMap.Entry<E> {

    int key;
    E value;

    Entry(int key, E value) {
        this.key = key;
        this.value = value;
    }

    public int getKey() {
        return key;
    }

    public E getValue() {
        return value;
    }

    public boolean equals(Object o) {
        if (o == this) {
          return true;
        }
        if (o instanceof IntObjectMap.Entry<?>) {
          IntObjectMap.Entry<?> e = (IntObjectMap.Entry<?>) o;
          return (key == e.getKey() &&
           (value==null ? e.getValue()==null : value.equals(e.getValue())));
        }
        return false;
    }

    public int hashCode() {
      return 37*key + (value == null ? 0 : value.hashCode());
    }

    public String toString() {
        return String.valueOf(key) + "=" + value;
    }
  }

}
