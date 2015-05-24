package gov.pnnl.jac.collections;

import java.util.*;

public class ObjectIntIdentityHashMap<E> extends ObjectIntHashMap<E> {

    /**
     * Constructor
     * @param     int initialCapacity The initial capacity of the map.
     * @param     float loadFactor The load factor to use in determining when
     *   to rehash.  This should be > 0.0f and < 1.0f.
     * @exception IllegalArgumentException If initialCapacity < 0 or if the loadFactor is invalid.
     */
    public ObjectIntIdentityHashMap (int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    /**
     * Constructor specifying initial capacity only.  The load factor defaults
     * to 0.75f.
     * @param     int initialCapacity The initial capacity of the map.
     * @exception IllegalArgumentException If initialCapacity < 0.
     */
    public ObjectIntIdentityHashMap (int initialCapacity) {
        this(initialCapacity, 0.75f);
    }

    /**
     * Default constructor.  The initial capacity defaults to 11 and the
     * load factor defaults to 0.75f.
     */
    public ObjectIntIdentityHashMap () {
        this(11, 0.75f);
    }
    
    @Override
    public Set<ObjectIntMap.Entry<E>> entrySet() {
        Set<ObjectIntMap.Entry<E>> set = new HashSet<ObjectIntMap.Entry<E>>(2*mFullCount);
        if (mFullCount > 0) {
          final byte[] states = mStates;
          final Object[] keys = mKeys;
          final int[] values = mValues;
          int n = states.length;
          for (int i=0; i<n; i++) {
            if (states[i] == FULL) {
              set.add(new Entry((E) keys[i], values[i]));
            }
          }
        }
        return set;
    }

    // Find the insertion index for the provided key.
    @Override
    protected int insertionIndex (E key, boolean equalsCheck) {
        final byte[] states = mStates;
        final Object[] keys = mKeys;
        final int length = mStates.length;
        
        int hash = System.identityHashCode(key) & 0x7FFFFFFF;
        
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
            if (o instanceof ObjectIntMap.Entry) {
                ObjectIntMap.Entry e = (ObjectIntMap.Entry) o;
                return (key == e.getKey() &&
                        (value == e.getValue()));
            }
            return false;
        }

        @Override
        public int hashCode() {
            return 37*System.identityHashCode(key) + value;
        }

        @Override
        public String toString() {
            return String.valueOf(key) + "=" + value;
        }
    }
}
