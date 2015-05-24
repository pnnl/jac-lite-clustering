package gov.pnnl.jac.collections;

import gov.pnnl.jac.util.ClassUtils;

import java.util.*;

public class ObjectIntHashMap<E> implements ObjectIntMap<E>, Cloneable {

    protected static final byte FREE = 0;
    protected static final byte FULL = 1;
    protected static final byte REMOVED = 2;

    protected Object[] mKeys;
    protected int[] mValues;
    protected byte[] mStates;
    // The load factor; used to determine when to resize.
    protected float mLoadFactor;
    // Number of entries not equal to FREE.
    protected int mElementCount;
    // Max number of elements before having to resize/rehash.
    protected int mMaxElementCount;
    // Count of elements equal to FULL.
    protected int mFullCount;
    // Mainly used by iterator to check for concurrent modification.
    protected int mModCount;

    /**
     * Constructor
     * 
     * @param int initialCapacity The initial capacity of the map.
     * @param float loadFactor The load factor to use in determining when to
     *        rehash. This should be > 0.0f and < 1.0f.
     * @exception IllegalArgumentException
     *                If initialCapacity < 0 or if the loadFactor is invalid.
     */
    public ObjectIntHashMap(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("invalid initial capacity: "
                    + initialCapacity);
        }
        if (Float.isNaN(loadFactor) || loadFactor <= 0.0f || loadFactor >= 1.0f) {
            throw new IllegalArgumentException("invalid load factor: "
                    + loadFactor);
        }
        // Turn the initial capacity into a prime number >= 3.
        // (If not a prime, insertionIndex() can get caught in infinite loops.)
        initialCapacity = MapUtils.nextRehashPrime(initialCapacity);
        // Initialize arrays
        mKeys = new Object[initialCapacity];
        mValues = new int[initialCapacity];
        mStates = new byte[initialCapacity];
        Arrays.fill(mStates, FREE);
        mLoadFactor = loadFactor;
        // Compute mMaxElementCount, the threshold used to determine when to
        // rehash.
        // It should be at least 1 and no greater than initialCapacity - 1.
        mMaxElementCount = Math.min(initialCapacity - 1,
                Math.max(1, (int) (mLoadFactor * initialCapacity)));
    }

    /**
     * Constructor specifying initial capacity only. The load factor defaults to
     * 0.75f.
     * 
     * @param int initialCapacity The initial capacity of the map.
     * @exception IllegalArgumentException
     *                If initialCapacity < 0.
     */
    public ObjectIntHashMap(int initialCapacity) {
        this(initialCapacity, 0.75f);
    }

    /**
     * Default constructor. The initial capacity defaults to 11 and the load
     * factor defaults to 0.75f.
     */
    public ObjectIntHashMap() {
        this(11, 0.75f);
    }

    /**
     * Remove all key-value mappings from the receiver.
     */
    public void clear() {
        Arrays.fill(mStates, FREE);
        Arrays.fill(mKeys, null);
        Arrays.fill(mValues, -1);
        mElementCount = mFullCount = 0;
        mModCount++;
    }

    /**
     * Returns <tt>true</tt> if the receiver maps the specifed key to an integer
     * value.
     * 
     * @param key
     *            the key.
     * @return <tt>true</tt> if this map contains a mapping for the key.
     */
    public boolean containsKey(E key) {
        return insertionIndex(key, true) < 0;
    }

    /**
     * Returns <tt>true</tt> if the receiver maps at least one key to the
     * specified value.
     * 
     * @param value
     *            the value.
     * @return <tt>true</tt> if at least one key maps to the value.
     */
    public boolean containsValue(int value) {
        final byte[] states = mStates;
        final int len = states.length;
        final int[] values = mValues;
        for (int i = 0; i < len; i++) {
            if (states[i] == FULL && value == values[i]) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
	public Set<ObjectIntMap.Entry<E>> entrySet() {
        Set<ObjectIntMap.Entry<E>> set = new HashSet<ObjectIntMap.Entry<E>>(
                2 * mFullCount);
        if (mFullCount > 0) {
            final byte[] states = mStates;
            final Object[] keys = mKeys;
            final int[] values = mValues;
            int n = states.length;
            for (int i = 0; i < n; i++) {
                if (states[i] == FULL) {
                    set.add(new Entry<E>((E) keys[i], values[i]));
                }
            }
        }
        return set;
    }

    /**
     * Returns the value to which the receiver maps the given key, or
     * <tt>-1</tt> if the key is not contained. Since a key may be associated
     * with -1, use <tt>containsKey</tt> to conclusively determine if the
     * receiver contains a mapping for the given key.
     * 
     * @param key
     *            the key.
     * @return the value to which the receiver maps the key, or <tt>-1</tt> if
     *         the map does not contain the key.
     * 
     * @see #containsKey(Object)
     */
    public int get(E key) {
        int rtn = -1;
        int ndx = insertionIndex(key, true);
        if (ndx < 0) {
            rtn = mValues[-ndx - 1];
        }
        return rtn;
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
     * Returns an iterator over the keys of this map.
     * 
     * @return Iterator
     */
    public Iterator<E> keyIterator() {
        return new Itr();
    }

    /**
     * Returns the keys for which the receiver contains mappings. The order of
     * the returned keys is undefined. In the event the receiver contains no
     * mappings, the returned array should be length zero, not <tt>null</tt>.
     * 
     * @return an array containing the keys with mappings.
     */
    @SuppressWarnings("unchecked")
	public E[] keys() {
        final byte[] states = mStates;
        final Object[] keys = mKeys;
        final int length = mStates.length;
		E[] rtn = (E[]) new Object[mFullCount];
        int count = 0;
        for (int i = 0; i < length; i++) {
            if (states[i] == FULL) {
                rtn[count++] = (E) keys[i];
            }
        }
        return rtn;
    }

    /**
     * Maps the specified key to the specified value in this map. The previous
     * mapping for the key, if present, is overwritten.
     * 
     * @param key
     *            the key.
     * @param value
     *            value to be associated with the key.
     * @return the previous value paired to the given key, or <tt>-1</tt> if no
     *         previous mapping was present.
     * 
     * @throws IllegalArgumentException
     *             if some aspect of this key or value prevents it from being
     *             stored in this map.
     * @throws NullPointerException
     *             if the key is null.
     */
    public int put(E key, int value) {
        int rtn = -1;
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
                throw new UnsupportedOperationException(
                        "map cannot be expanded");
            }
            rehash(newCapacity); // Expand the available space.
            return put(key, value); // Recursive call to this same method.
        } else { // Do not need to rehash
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
        return rtn;
    }

    /**
     * Adds all key-value mapping from the specified <tt>ObjectIntMap</tt> to
     * the receiver. These mappings may replace mappings already present in the
     * receiver.
     * 
     * @param map
     *            contains mappings to be copied to this map.
     * 
     * @throws IllegalArgumentException
     *             some aspect of a key or value in the specified map prevents
     *             it from being stored in this map.
     */
    public void putAll(ObjectIntMap<? extends E> map) {
        for (ObjectIntMap.Entry<? extends E> entry : map.entrySet()) {
            this.put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Remove the mapping for the specified key from the receiver.
     * 
     * @param key
     *            the key.
     * @return the previous value paired to the given key, or <tt>-1</tt> if no
     *         previous mapping was present. (A <tt>-1</tt> return does not
     *         <i>necessarily</i> prove that no mapping was present: the key may
     *         have been explicitly mapped to <tt>-1</tt>.)
     */
    public int remove(E key) {
        int rtn = -1;
        int ndx = insertionIndex(key, true);
        if (ndx < 0) {
            ndx = -ndx - 1;
            rtn = mValues[ndx];
            mKeys[ndx] = -1;
            mStates[ndx] = REMOVED;
            mFullCount--;
            mModCount++;
        }
        return rtn;
    }

    /**
     * Returns the number of key-value pairs contained in the map.
     * 
     * @return the number of mappings.
     */
    public int size() {
        return mFullCount;
    }

    public int[] values() {
      final byte[] states = mStates;
      final int[] values = mValues;
      final int length = mStates.length;
      int[] rtn = new int[mFullCount];
      int count = 0;
      for (int i=0; i<length; i++) {
        if (states[i] == FULL) {
          rtn[count++] = values[i];
        }
      }
      return rtn;
    }

    // Find the insertion index for the provided key.
    protected int insertionIndex(E key, boolean equalsCheck) {
        final byte[] states = mStates;
        final Object[] keys = mKeys;
        final int length = mStates.length;
        int hash = key.hashCode() & 0x7FFFFFFF;
        int i = hash % length;
        // double hashing, see
        // http://www.eece.unm.edu/faculty/heileman/hash/node4.html
        int decrement = hash % (length - 2);
        if (decrement == 0) {
            decrement = 1;
        }
        // stop if we find a removed or free slot, or if we find the key itself
        // do NOT skip over removed slots (yes, open addressing is like that...)
        while (states[i] != FREE && !(equalsCheck && key.equals(keys[i]))) {
            i -= decrement;
            if (i < 0) {
                i += length;
            }
        }
        if (states[i] == FULL) {
            // key already contained at slot i.
            // return a negative number identifying the slot.
            return -i - 1;
        }
        // not already contained, should be inserted at slot i.
        // return a number >= 0 identifying the slot.
        return i;
    }

    @SuppressWarnings("unchecked")
	private void rehash(int newCapacity) {
        int oldCapacity = mKeys.length;
        Object[] oldKeys = mKeys;
        int[] oldValues = mValues;
        byte[] oldStates = mStates;
        mKeys = new Object[newCapacity];
        mValues = new int[newCapacity];
        mStates = new byte[newCapacity];
        Arrays.fill(mStates, FREE);
        mMaxElementCount = (int) (mLoadFactor * newCapacity);
        if (mMaxElementCount == 0) {
            mMaxElementCount = 1;
        }
        for (int i = 0; i < oldCapacity; i++) {
            if (oldStates[i] == FULL) {
                int ndx = insertionIndex((E) oldKeys[i], false);
                mKeys[ndx] = oldKeys[i];
                mValues[ndx] = oldValues[i];
                mStates[ndx] = oldStates[i];
            }
        } // for
        mModCount++;
    }

    private static class Entry<E> implements ObjectIntMap.Entry<E> {

        E key;
        int value;

        Entry(E key, int value) {
            this.key = key;
            this.value = value;
        }

        public E getKey() {
            return key;
        }

        public int getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (o instanceof ObjectIntMap.Entry<?>) {
                ObjectIntMap.Entry<?> e = (ObjectIntMap.Entry<?>) o;
                return (key.equals(e.getKey()) && (value == e.getValue()));
            }
            return false;
        }

        @Override
        public int hashCode() {
            return 37 * key.hashCode() + value;
        }

        @Override
        public String toString() {
            return String.valueOf(key) + "=" + value;
        }
    }

    private class Itr implements Iterator<E> {

        private int mCursor = -1; // Init to -1, so first advanceCursor() will
                                  // work.
        private int mLastReturnNdx = -1;
        private int mExpectedModCount = mModCount;

        Itr() {
            adv();
        }

        private void adv() {
            mCursor++;
            int n = mStates.length;
            while (mCursor < n && mStates[mCursor] != FULL) {
                mCursor++;
            }
        }

        public boolean hasNext() {
            return (mCursor < mStates.length);
        }

        @SuppressWarnings("unchecked")
		public E next() {
            try {
                E rtn = null;
                if (mStates[mCursor] == FULL) {
                    rtn = (E) mKeys[mCursor];
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

        @SuppressWarnings("unchecked")
		public void remove() {
            if (mLastReturnNdx == -1) {
                throw new IllegalStateException();
            }
            checkForComodification();
            if (mStates[mLastReturnNdx] == FULL) {
                try {
                    ObjectIntHashMap.this.remove((E) mKeys[mLastReturnNdx]);
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
