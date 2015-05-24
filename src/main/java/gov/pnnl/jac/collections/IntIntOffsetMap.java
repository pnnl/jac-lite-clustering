package gov.pnnl.jac.collections;

import gov.pnnl.jac.util.ExceptionUtil;

/**
 * <p>
 * Implementation of <tt>IntIntMap</tt> for which all values
 * equal the associated key plus a constant offset.  
 * </p>
 * <p>
 * This class is immutable.  Calls to mutator methods 
 * generate <tt>UnsupportedOperationException</tt>s.
 * 
 * @author D3J923
 *
 */
public class IntIntOffsetMap implements IntIntMap {

    private int mItemCount;
    private int mOffset;
    
    public IntIntOffsetMap(int itemCount, int offset) {
        ExceptionUtil.checkNonNegative(mItemCount);
        mItemCount = itemCount;
        mOffset = offset;
    }
    
    public int size() {
        return mItemCount;
    }

    public boolean isEmpty() {
        return mItemCount == 0;
    }

    public boolean containsKey(int key) {
        return key >= 0 && key < mItemCount;
    }

    public boolean containsValue(int value) {
        return containsKey(value - mOffset);
    }

    public int get(int key) {
        if (containsKey(key)) {
            return key + mOffset;
        }
        return -1;
    }

    public int getSum(int[] keys) {
        int sum = 0;
        for (int k: keys) {
            if (containsKey(k)) {
                sum += (k + mOffset);
            }
        }
        return sum;
    }

    public int put(int key, int value) {
        throw new UnsupportedOperationException();
    }

    public int putOrIncrement(int key, int value) {
        throw new UnsupportedOperationException();
    }

    public int remove(int key) {
        throw new UnsupportedOperationException();
    }

    public void putAll(IntIntMap map) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public int[] keys() {
        int[] rtn = new int[mItemCount];
        for (int i=0; i<mItemCount; i++) {
            rtn[i] = i;
        }
        return rtn;
    }

    public IntCollectionIterator keyIterator() {
        return new KeyIterator(mItemCount);
    }

    public int[] values() {
        int[] rtn = new int[mItemCount];
        for (int i=0; i<mItemCount; i++) {
            rtn[i] = i + mOffset;
        }
        return rtn;
    }

    static class KeyIterator implements IntCollectionIterator {

        private int mICount;
        private int mNext = 0;

        private KeyIterator(int itemCount) {
            mICount = itemCount;
        }
        
        public boolean hasNext() {
            return mNext < mICount;
        }

        public int next() {
            return mNext++;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
        
    }
}
