package gov.pnnl.jac.collections;

/**
 * <p>Implementation of <tt>TwoWayIntIntMap</tt> that maintains the
 * forward and reverse mappings using two instances of <tt>IntIntHashMap.</tt></p>
 * 
 * @author R. Scarberry
 *
 */
public class TwoWayIntIntHashMap implements TwoWayIntIntMap {

    private IntIntHashMap mForwardMap, mReverseMap;
    
    public TwoWayIntIntHashMap(int initialCapacity, float loadFactor) {
        mForwardMap = new IntIntHashMap(initialCapacity, loadFactor);
        mReverseMap = new IntIntHashMap(initialCapacity, loadFactor);
    }
    
    public int getKey(int value) {
        return mReverseMap.get(value);
    }

    public void clear() {
        mForwardMap.clear();
        mReverseMap.clear();
    }

    public boolean containsKey(int key) {
        return mForwardMap.containsKey(key);
    }

    public boolean containsValue(int value) {
        return mReverseMap.containsKey(value);
    }

    public int get(int key) {
        return mForwardMap.get(key);
    }
    
    public int getSum(int[] keys) {
        return mForwardMap.getSum(keys);
    }

    public boolean isEmpty() {
        return mForwardMap.isEmpty();
    }

    public IntCollectionIterator keyIterator() {
        return mForwardMap.keyIterator();
    }
    
    public IntCollectionIterator valueIterator() {
        return mReverseMap.keyIterator();
    }

    public int[] keys() {
        return mForwardMap.keys();
    }

    public int put(int key, int value) {
        int rtn = mForwardMap.put(key, value);
        mReverseMap.put(value, key);
        return rtn;
    }

    public int putOrIncrement(int key, int value) {
        if (mForwardMap.containsKey(key)) {
            int currentValue = mForwardMap.remove(key);
            mReverseMap.remove(currentValue);
            put(key, value+currentValue);
            return currentValue;
        } else {
            return put(key, value);
        }
    }

    public void putAll(IntIntMap map) {
        int[] keys = map.keys();
        int n = keys.length;
        for (int i=0; i<n; i++) {
          int k = keys[i];
          put(k, map.get(k));
        }
    }

    public int remove(int key) {
        boolean b = mForwardMap.containsKey(key);
        int rtn = mForwardMap.remove(key);
        if (b) {
            mReverseMap.remove(rtn);
        }
        return rtn;
    }

    public int size() {
        return mForwardMap.size();
    }

    public int[] values() {
        return mReverseMap.keys();
    }

}
