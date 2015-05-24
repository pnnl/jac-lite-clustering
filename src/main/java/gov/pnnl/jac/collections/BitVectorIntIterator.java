package gov.pnnl.jac.collections;

import cern.colt.bitvector.BitVector;

/**
 * <p>An implementation of <code>IntInterator</code> to iterate over the set or 
 * unset bits of a <code>cern.colt.bitvector.BitVector</code>.  The <code>BitVector</code>
 * is in no way modified by the iterator.</p>
 * <p>Do not use an instance of <code>BitVectorIntIterator</code> on a <code>BitVector</code>
 * that is being modified by other threads.  The iterator assumes the bits are static.</p>
 * 
 * @author R. Scarberry
 *
 */
public class BitVectorIntIterator implements IntIterator {
   
    // The bit vector
    private BitVector mBits;
    // Whether iterating over the set or the unset bits.
    private boolean mSet;
    // The position of the cursor, the first set bit, and the last set bit.
    private int mCursor = -1, mFirst = -1, mLast = -1;
    // Set to true by gotoLast().  
    private boolean mLastFlag;
    
    /**
     * Constructor.
     * 
     * @param bits - contains the bits to be iterated.
     * @param set - iterates over the set bit if true, over the unset bits if false.
     */
    public BitVectorIntIterator(BitVector bits, boolean set) {
        if (bits == null) throw new NullPointerException();
        mBits = bits;
        mSet = set;
        gotoFirst();
    }
    
    /**
     * Constructor for iterating over the set bits.
     * 
     * @param bits - contains the bits to be iterated.
     */
    public BitVectorIntIterator(BitVector bits) {
        this(bits, true);
    }

    /**
     * Returns the index of the first bit matching the constructor
     * argument <code>set</code>.  
     *
     * @return - a nonnegative index or -1 if no matching bits are present. 
     */
    public int getFirst() {
        gotoFirst();
        return getNext();
    }

    /**
     * Returns the index of the last bit matching the constructor
     * argument <code>set</code>.
     *
     * @return - a nonnegative index or -1 if no matching bits are present. 
     */
    public int getLast() {
        gotoLast();
        return getPrev();
    }

    /**
     * Returns the index of the next bit matching the constructor
     * argument <code>set</code>.  Call <code>hasNext()</code> prior to calling
     * this method to confirm that another matching bit is present.
     *
     * @return - a nonnegative index or -1 if no matching bits are present. 
     */
    public int getNext() {
        int rtn = mCursor;
        gotoNext();
        if (!hasNext()) {
            gotoLast();
        }
        return rtn;
    }

    /**
     * Returns the index of the previous bit matching the constructor
     * argument <code>set</code>.  Call <code>hasPrev()</code> prior to calling
     * this method to confirm that another matching bit is present.
     *
     * @return - a nonnegative index or -1 if no matching bits are present. 
     */
    public int getPrev() {
        gotoPrev();
        mLastFlag = false;
        return mCursor;
    }

    /**
     * Position the cursor for iterating forward starting from the first bit
     * that matches the constructor argument <code>set</code>
     */
    public void gotoFirst() {
        if (mFirst == -1) {
           int lim = mBits.size();
           for (int i=0; i<lim; i++) {
               if (mBits.getQuick(i) == mSet) {
                   mFirst = i;
                   break;
               }
           }
        }
        mCursor = mFirst;
    }

    /**
     * Position the cursor for iterating backwards starting from the last bit
     * that matches the constructor argument <code>set</code>
     */
    public void gotoLast() {
        mCursor = -1;
        if (mLast == -1) {
            int lim = mBits.size();
            for (int i=lim-1; i>=0; i--) {
                if (mBits.getQuick(i) == mSet) {
                    mLast = i;
                    break;
                }
            }
         }
         mLastFlag = (mLast >= 0);
    }

    /**
     * Is there another bit matching the constructor argument <code>set</code>
     * in the forward direction?  That is, will an immediate call to 
     * <code>getNext()</code> return a nonnegative index?
     */
    public boolean hasNext() {
        return mCursor >= 0;
    }

    /**
     * Is there another bit matching the constructor argument <code>set</code>
     * in the backward direction?  That is, will an immediate call to 
     * <code>getPrev()</code> return a nonnegative index?
     */
    public boolean hasPrev() {
        int cursor = mCursor;
        gotoPrev();
        boolean rtn = mCursor >= 0;
        mCursor = cursor;
        return rtn;
    }

    /** 
     * Returns the number of bits matching the constructor argument <code>set</code>.
     */
    public int size() {
        int sz = mBits.cardinality();
        if (!mSet) {
            sz = mBits.size() - sz;
        }
        return sz;
    }

    
    /** 
     * Returns the indexes of the bits matching the constructor argument <code>set</code>.
     * This array will always be ascendingly sorted.
     */
    public int[] toArray() {
        int sz = size();
        int[] rtn = new int[sz];
        int count = 0;
        int lim = mBits.size();
        for (int i=0; i<lim; i++) {
            if (mBits.getQuick(i) == mSet) {
                rtn[count++] = i;
            }
        }
        return rtn;
    }

    /**
     * Returns a deep clone of the receiving object.
     */
    public Object clone() {
        BitVectorIntIterator clone = null; 
        try {
            clone = (BitVectorIntIterator) super.clone();
            clone.mBits = (BitVector) this.mBits.clone();
        } catch (CloneNotSupportedException cnse) {
            // Won't happen.
        }
        return clone;
    }

    // Moves the cursor forward.
    private void gotoNext() {
        if (mCursor >= 0) {
            int start = mCursor + 1;
            mCursor = -1;
            int lim = mBits.size();
            for (int i=start; i<lim; i++) {
                if (mBits.getQuick(i) == mSet) {
                    mCursor = i;
                    break;
                }
            }
        }
    }
    
    // Moves the cursor backwards.
    private void gotoPrev() {
        if (mCursor >= 0) {
            int start = mCursor - 1;
            mCursor = -1;
            for (int i=start; i>=0; i--) {
                if (mBits.getQuick(i) == mSet) {
                    mCursor = i;
                    break;
                }
            }
        } else if (mLastFlag) {
            mCursor = mLast;
        }
    }
}
