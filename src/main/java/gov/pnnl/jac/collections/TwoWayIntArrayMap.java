package gov.pnnl.jac.collections;

import gov.pnnl.jac.util.SortUtils;

import java.util.Arrays;
import java.util.ConcurrentModificationException;

public class TwoWayIntArrayMap implements TwoWayIntMap, Cloneable {

    private int mSize;
    
    private int[] mForwardMapping;    
    private int[] mReverseMapping;

    private int[] mSortedValues1;
    private int[] mSortedValues2;
    
    private int mModCount;
    
    public TwoWayIntArrayMap(int[] values) {
        mSortedValues1 = new int[values.length];
        for (int i=0; i<values.length; i++) {
            mSortedValues1[i] = i;
        }
        mForwardMapping = (int[]) values.clone();
        int[] sorted = (int[]) values.clone();
        int[] reverse = new int[sorted.length];
        for (int i=0; i<sorted.length; i++) {
            reverse[i] = i;
        }
        SortUtils.parallelSort(sorted, reverse);
        mSortedValues2 = sorted;
        mReverseMapping = reverse;
    }
    
    public TwoWayIntArrayMap() {
        mForwardMapping = new int[17];
        mReverseMapping = new int[17];
        mSortedValues1 = new int[17];
        mSortedValues2 = new int[17];
    }
    
    private void ensureCapacity(int cap) {
        if (mForwardMapping.length < cap) { 
            int newCap = Math.max(cap, 2*mForwardMapping.length);
            int[] newForwardMapping = new int[newCap];
            int[] newReverseMapping = new int[newCap];
            int[] newSortedValues1 = new int[newCap];
            int[] newSortedValues2 = new int[newCap];
            System.arraycopy(mForwardMapping, 0, newForwardMapping, 0, mSize);
            System.arraycopy(mReverseMapping, 0, newReverseMapping, 0, mSize);
            System.arraycopy(mSortedValues1, 0, newSortedValues1, 0, mSize);
            System.arraycopy(mSortedValues2, 0, newSortedValues2, 0, mSize);
            mForwardMapping = newForwardMapping;
            mReverseMapping = newReverseMapping;
            mSortedValues1 = newSortedValues1;
            mSortedValues2 = newSortedValues2;
        }
    }

    public void associate(int value1, int value2) {
        
        if (hasForward(value1)) {
            if(getForward(value1) == value2) {
                return;
            }
            removeForward(value1);
        }
        
        mModCount++;
        
        ensureCapacity(mSize + 1);
        int n = -Arrays.binarySearch(mSortedValues1, 0, mSize, value1) - 1;
        if (n < mSize) {
            // Shift values up to make room.
            for (int i=mSize; i>n; i--) {
                mSortedValues1[i] = mSortedValues1[i-1];
                mForwardMapping[i] = mForwardMapping[i-1];
            }
        }
        
        mSortedValues1[n] = value1;
        mForwardMapping[n] = value2;
        
        n = -Arrays.binarySearch(mSortedValues2, 0, mSize, value2) - 1;
        if (n < mSize) {
            // Shift values up to make room.
            for (int i=mSize; i>n; i--) {
                mSortedValues2[i] = mSortedValues2[i-1];
                mReverseMapping[i] = mReverseMapping[i-1];
            }
        }
        
        mSortedValues2[n] = value2;
        mReverseMapping[n] = value1;
        mSize++;
    }
    
    public boolean hasForward(int value) {
        return Arrays.binarySearch(mSortedValues1, 0, mSize, value) >= 0;
    }
    
    public boolean hasReverse(int value) {
        return Arrays.binarySearch(mSortedValues2, 0, mSize, value) >= 0;
    }

    public int getForward(int value) {
        int n = Arrays.binarySearch(mSortedValues1, 0, mSize, value);
        if (n >= 0) {
            return mForwardMapping[n];
        }
        return -1;
    }

    public int getReverse(int value) {
        int n = Arrays.binarySearch(mSortedValues2, 0, mSize, value);
        if (n >= 0) {
            return mReverseMapping[n];
        }
        return -1;
    }

    public int removeForward(int value) {
        int rtn = -1;
        int n = Arrays.binarySearch(mSortedValues1, 0, mSize, value);
        if (n >= 0) {
            mModCount++;
            rtn = mForwardMapping[n];
            for (int i=n; i<mSize; i++) {
                mSortedValues1[i] = mSortedValues1[i+1];
                mForwardMapping[i] = mForwardMapping[i+1];
            }
            n = Arrays.binarySearch(mSortedValues2, 0, mSize, rtn);
            for (int i=n; i<mSize; i++) {
                mSortedValues2[i] = mSortedValues2[i+1];
                mReverseMapping[i] = mReverseMapping[i+1];
            }
            mSize--;
        }
        return rtn;
    }

    public int removeReverse(int value) {
        int rtn = -1;
        int n = Arrays.binarySearch(mSortedValues2, 0, mSize, value);
        if (n >= 0) {
            mModCount++;
            rtn = mReverseMapping[n];
            for (int i=n; i<mSize; i++) {
                mSortedValues2[i] = mSortedValues2[i+1];
                mReverseMapping[i] = mReverseMapping[i+1];
            }
            n = Arrays.binarySearch(mSortedValues1, 0, mSize, rtn);
            for (int i=n; i<mSize; i++) {
                mSortedValues1[i] = mSortedValues1[i+1];
                mForwardMapping[i] = mForwardMapping[i+1];
            }
            mSize--;
        }
        return rtn;
    }

    public int size() {
        return mSize;
    }

    public boolean isEmpty() {
        return mSize == 0;
    }

    public void clear() {
        mModCount++;
        mSize = 0;
    }

    public IntCollectionIterator forwardIterator() {
        return new Iter(true);
    }

    public IntCollectionIterator reverseIterator() {
        return new Iter(false);
    }
    
    public Object clone() {
        try {
            TwoWayIntArrayMap clone = (TwoWayIntArrayMap) super.clone();
            clone.mForwardMapping = (int[]) this.mForwardMapping.clone();
            clone.mReverseMapping = (int[]) this.mReverseMapping.clone();
            clone.mSortedValues1 = (int[]) this.mSortedValues1.clone();
            clone.mSortedValues2 = (int[]) this.mSortedValues2.clone();
            return clone;
        } catch (CloneNotSupportedException cnse) {
            throw new InternalError();
        }
    }
    
    private class Iter implements IntCollectionIterator {

        private int mNext;
        private int mStep;
        private int mExpectedModCount;
        
        private Iter(boolean forward) {
            mNext = 0;
            mStep = forward ? +1 : -1;
            mExpectedModCount = mModCount;
        }
        
        private void checkConcurrentMod() {
            if (mModCount != mExpectedModCount) {
                throw new ConcurrentModificationException();
            }
        }
        
        public boolean hasNext() {
            checkConcurrentMod();
            return mNext >= 0 || mNext < mSize;
        }

        public int next() {
            checkConcurrentMod();
            int[] values = mStep == 1 ? mSortedValues1 : mSortedValues2;
            int nxt = values[mNext];
            mNext += mStep;
            return nxt;
        }

        public void remove() {
            checkConcurrentMod();
            try {
                int last = mNext - mStep;
                if (mStep == 1) {
                    removeForward(mSortedValues1[last]);
                } else {
                    removeReverse(mSortedValues2[last]);
                }
            } finally {
                mExpectedModCount = mModCount;
            }
        }
    }
  
}
