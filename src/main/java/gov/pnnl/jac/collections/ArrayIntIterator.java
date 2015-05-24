package gov.pnnl.jac.collections;


/**
 * <p><tt>ArrayIntIterator</tt> is a simple implementation
 * of <tt>IntIterator</tt> for iterating over an array of
 * integers.</p>
 * 
 * @author R. Scarberry
 *
 */
public class ArrayIntIterator implements IntIterator {

    private int[] mNodes;
    private int mCursor;

    public ArrayIntIterator(int[] nodes) {
        mNodes = nodes;
    }

    public void gotoFirst() {
        mCursor = 0;
    }

    public void gotoLast() {
        mCursor = mNodes.length;
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
        return mNodes[mCursor++];
    }

    public int getPrev() {
        return mNodes[--mCursor];
    }

    public boolean hasNext() {
        return mCursor >= 0 && mCursor < mNodes.length;
    }

    public boolean hasPrev() {
        return mCursor > 0 && mCursor <= mNodes.length;
    }

    public int size() {
        return mNodes.length;
    }

    public int[] toArray() {
        return (int[]) mNodes.clone();
    }

    public Object clone() {
        ArrayIntIterator clone = null;
        try {
            clone = (ArrayIntIterator) super.clone();
            clone.mNodes = (int[]) this.mNodes.clone();
        } catch (CloneNotSupportedException cnse) {
            // Won't happen.
        }
        return clone;
    }
}
