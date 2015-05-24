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

import gov.pnnl.jac.util.HashAndEqualityUtils;

import java.util.*;

/**
 * <tt>LongDoubleHashMap</tt> is an implementation of <tt>LongDoubleMap</tt> that
 * maps long keys to double values using hashing.
 *
 * <p>Company: Battelle Memorial Institute</p>
 *
 * @author R. Scarberry
 * @version 1.0
 */
public class LongDoubleHashMap implements LongDoubleMap, Cloneable {

  private static final byte FREE = 0;
  private static final byte FULL = 1;
  private static final byte REMOVED = 2;

  private long[] mKeys;
  private double[] mValues;
  private byte[] mStates;
  
  private double mMissingValue = Double.NaN;

  // The load factor;  used to determine when to resize.
  private float mLoadFactor;
  // Number of entries not equal to FREE.
  private int mElementCount;
  // Max number of elements before having to resize/rehash.
  private int mMaxElementCount;
  // Count of elements equal to FULL.
  private int mFullCount;
  // Mainly for iterators to determine if modifications have been made
  // while iterating outside of the iterator.
  private int mModCount;

  /**
   * Constructor
   * @param     int initialCapacity The initial capacity of the map.
   * @param     float loadFactor The load factor to use in determining when
   *   to rehash.  This should be > 0.0f and < 1.0f.
   * @exception IllegalArgumentException If initialCapacity < 0 or if the loadFactor is invalid.
   */
  public LongDoubleHashMap (int initialCapacity, float loadFactor) {
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
      mKeys = new long[initialCapacity];
      mValues = new double[initialCapacity];
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
  public LongDoubleHashMap (int initialCapacity) {
      this(initialCapacity, 0.75f);
  }

  /**
   * Default constructor.  The initial capacity defaults to 11 and the
   * load factor defaults to 0.75f.
   */
  public LongDoubleHashMap () {
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
   * Returns <tt>true</tt> if the receiver maps the specifed long key
   * to a double value.
   *
   * @param key an long key.
   * @return <tt>true</tt> if this map contains a mapping for the key.
   */
  public boolean containsKey(long key) {
    return  insertionIndex(key, true) < 0;
  }

  /**
   * Returns <tt>true</tt> if the receiver maps at least one key to the
   * specified value.
   *
   * @param value the value.
   * @return <tt>true</tt> if at least one key maps to the value.
   */
  public boolean containsValue(double value) {
    final byte[] states = mStates;
    int len = states.length;
    final long valueBits = Double.doubleToLongBits(value);
    final double[] values = mValues;
    for (int i=0; i<len; i++) {
      if (states[i] == FULL) {
        if (valueBits == Double.doubleToLongBits(values[i])) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Returns the value to which the receiver maps the given key, or
   * <tt>null</tt> if the key is not contained.  Since a key may be
   * mapped to -1, this return value does not
   * <i>necessarily</i> prove that the map does not contain the key.
   * Use <tt>containsKey</tt> to conclusively determine if the receiver
   * contains a mapping for the given key.
   *
   * @param key the key.
   * @return the value to which the receiver maps the key, or
   *	       -1 if the map does not contain the key.
   *
   * @see #containsKey(Object)
   */
  public double get(long key) {
    double rtn = mMissingValue;
    int ndx = insertionIndex(key, true);
    if (ndx < 0) {
        rtn = mValues[-ndx - 1];
    }
    return  rtn;
  }

  // Modification Operations

  /**
   * Maps the specified long key to the specified value in this map.
   * (This is an optional operation to permit read-only implementations.)
   * The previous mapping for the key, if present, is overwritten.
   *
   * @param key the key.
   * @param value the value to be associated with the key.
   * @return the previous value paired to the given key, or -1 if no
   *   previous mapping was present.
   *
   * @throws UnsupportedOperationException if the <tt>put</tt> operation is
   *	          not supported by this map.
   * @throws IllegalArgumentException if some aspect of this key or value
   *	          prevents it from being stored in this map.
   */
  public double put(long key, double value) {
    double rtn = mMissingValue;
    // Get the insertion index -- same index used for all arrays.
    int ndx = insertionIndex(key, true);
    if (ndx < 0) { // Negative index means the key was already contained.
        ndx = -ndx - 1;
        rtn = mValues[ndx];
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
   * Remove the mapping for the specified key from the receiver. (To permit
   * read-only implementations, this is an optional operation.)
   *
   * @param key the key.
   * @return the previous value paired to the given key, or -1 if no
   *   previous mapping was present.  (A -1 return does not
   *   <i>necessarily</i> prove that no mapping was present: the key may have
   *   been explicitly mapped to this value.)
   * @throws UnsupportedOperationException if the <tt>remove</tt> method is
   *         not supported by this map.
   */
  public double remove(long key) {
    double rtn = mMissingValue;
    int ndx = insertionIndex(key, true);
    if (ndx < 0) {
        ndx = -ndx - 1;
        rtn = mValues[ndx];
        mKeys[ndx] = -1;
        mStates[ndx] = REMOVED;
        mFullCount--;
        mModCount++;
    }
    return  rtn;
  }

  // Bulk Operations
  /**
   * Adds all key-value mapping from the specified <tt>LongDoubleMap</tt> to the
   * receiver.  (This is an optional operation, since some implementations
   * may be read-only.)  These mappings may replace mappings already present
   * in the receiver.
   *
   * @param map contains mappings to be copied to this map.
   *
   * @throws UnsupportedOperationException if the <tt>putAll</tt> method is
   * 		  not supported by this map.
   *
   * @throws IllegalArgumentException some aspect of a key or value in the
   *	          specified map prevents it from being stored in this map.
   */
  public void putAll(LongDoubleMap map) {
    long[] keys = map.keys();
    int n = keys.length;
    for (int i=0; i<n; i++) {
      long k = keys[i];
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
    mElementCount = mFullCount = 0;
    mModCount++;
  }

  // Views

  /**
   * Returns the long keys for which the receiver contains mappings.  The
   * order of the returned keys is undefined.  In the event the receiver contains
   * no mappings, the returned array should be length zero, not <tt>null</tt>.
   *
   * @return an array containing the keys with mappings.
   */
  public long[] keys() {
    final byte[] states = mStates;
    final long[] keys = mKeys;
    final int length = mStates.length;
    long[] rtn = new long[mFullCount];
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
  public LongCollectionIterator keyIterator() {
    return new Itr();
  }

  /**
   * Returns an array containing the values stored in the map.
   * @return an array of double values.
   */
  public double[] values() {
    double[] rtn = new double[mFullCount];
    final byte[] states = mStates;
    final double[] values = mValues;
    final int length = mStates.length;
    int count = 0;
    for (int i = 0; i < length; i++) {
        if (states[i] == FULL) {
          rtn[count++] = values[i];
        }
    }
    return rtn;
  }

  // Comparison and hashing
  /**
   * Compares the receiver with the specified object for equality.  Returns
   * <tt>true</tt> if the specified object is also an <tt>LongDoubleMap</tt> and the two
   * represent the same mappings.  They may not be the same implementation class.
   *
   * @param o object to be compared with this map.
   * @return <tt>true</tt> if the specified object contains the same
   *   key-value mappings as this map.
   */
  public boolean equals(Object o) {
    return (o instanceof LongDoubleMap ? MapUtils.checkEqual(this, (LongDoubleMap) o) : false);
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
      LongDoubleHashMap clone = (LongDoubleHashMap) super.clone();
      clone.mKeys = (long[]) this.mKeys.clone();
      clone.mValues = (double[]) this.mValues.clone();
      clone.mStates = (byte[]) this.mStates.clone();
      clone.mModCount = 0;
      return clone;
    } catch (CloneNotSupportedException e) {
      throw new InternalError();
    }
  }

  @Override
  public double getMissingValue() {
	  return mMissingValue;
  }

  @Override
  public void setMissingValue(double v) {
	  mMissingValue = v;
  }
  
  // Find the insertion index for the provided String.
   private int insertionIndex (long key, boolean equalsCheck) {
       final byte[] states = mStates;
       final long[] keys = mKeys;
       final int length = mStates.length;
       int hash = HashAndEqualityUtils.hash(key) & 0x7FFFFFFF;
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
       long[] oldKeys = mKeys;
       double[] oldValues = mValues;
       byte[] oldStates = mStates;
       mKeys = new long[newCapacity];
       mValues = new double[newCapacity];
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

   private class Itr implements LongCollectionIterator {

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

     public long next() {
       try {
         long rtn = 0;
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
           LongDoubleHashMap.this.remove(mKeys[mLastReturnNdx]);
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

}
