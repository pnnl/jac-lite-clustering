package gov.pnnl.jac.collections;

/**
 * <p>Implementation of <tt>IntIterator</tt> for iterating over
 * a continuous range of integer primitives.</p>
 * @author d3j923
 *
 */
public class IntervalIntIterator implements IntIterator {

    private int mLower, mUpper, mCursor;
    
    /**
     * Constructor which takes the upper and lower bounds,
     * inclusive of the lower bound, exclusive of the upper.
     * 
     * @param lower - the first element of the iteration.
     * @param upper - the last element of the iteration plus 1.
     */
    public IntervalIntIterator (int lower, int upper) {
        mLower = Math.min(lower, upper);
        mUpper = Math.max(lower, upper) + 1;
        mCursor = mLower;
    }

    public int getFirst() {
        gotoFirst();
        return getNext();
    }

    public int getLast() {
        gotoLast();
        return getPrev();
    }

    public int getNext() {
        return mCursor++;
    }

    public int getPrev() {
        return --mCursor;
    }

    public void gotoFirst() {
        mCursor = mLower;
    }

    public void gotoLast() {
        mCursor = mUpper;
    }

    public boolean hasNext() {
        return mCursor >= mLower && mCursor < mUpper;
    }

    public boolean hasPrev() {
        return mCursor > mLower && mCursor <= mUpper;
    }

    public int size() {
        return mUpper - mLower;
    }

    public int[] toArray() {
        int sz = size();
        int[] rtn = new int[sz];
        for (int i=0; i<sz; i++) {
            rtn[i] = mLower + i;
        }
        return rtn;
    }

    public Object clone() {
        Object clone = null;
        try {
            clone = super.clone();
        } catch (CloneNotSupportedException cnse) {
        }
        return clone;
    }
    
}
