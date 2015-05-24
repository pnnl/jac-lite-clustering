package gov.pnnl.jac.collections;

/**
 * <p>A queue implementation (FIFO) containing integer primitives.</p>
 * 
 * @author R. Scarberry
 *
 */
public class ArrayIntQueue implements Cloneable {

	// Backing storage.
	private IntArrayList mIntList;
	// Index of the next call to get().
	private int mGetPtr;
	
	/**
	 * Constructor.
	 * 
	 * @param initialCapacity initial storage capacity.
	 */
	public ArrayIntQueue(int initialCapacity) {
		mIntList = new IntArrayList(initialCapacity);
	}
	
	/**
	 * Default constructor.
	 */
	public ArrayIntQueue() {
		this(10);
	}
	
	/**
	 * Returns the number of elements currently in the queue.
	 * 
	 * @return
	 */
	public int size() {
		return mIntList.size() - mGetPtr;
	}
	
	/**
	 * Returns the storage capacity.
	 * 
	 * @return
	 */
	public int capacity() {
	    return mIntList.capacity();
	}
	
	/**
	 * Compacts the queue to reduce memory requirements.
	 */
	public void compact() {
		if (mGetPtr > 0) {
			IntArrayList tempList = new IntArrayList(this.size());
			int listSz = mIntList.size();
			for (int i=mGetPtr; i<listSz; i++) {
				tempList.add(mIntList.get(i));
			}
			mGetPtr = 0;
			mIntList = tempList;
		} else {
			mIntList.shrinkToSize();
		}
	}
	
	/**
	 * Puts an element to the queue.
	 * 
	 * @param n
	 */
	public void put(int n) {
		int sz = mIntList.size();
		if (sz == mIntList.capacity()) {
			if (mGetPtr >= sz/2) {
				IntArrayList tempList = new IntArrayList(sz);
				for (int i=mGetPtr; i<sz; i++) {
					tempList.add(mIntList.get(i));
				}
				mGetPtr = 0;
				mIntList = tempList;
			}
		}
		mIntList.add(n);
	}
	
	/**
	 * Returns (and removes) the next element from the queue.
	 * 
	 * @return
	 */
	public int get() {
		if (mGetPtr == mIntList.size()) {
			throw new java.util.NoSuchElementException();
		}
		return mIntList.get(mGetPtr++);
	}
	
	/**
	 * Returns the next element from the queue without removing it.
	 * 
	 * @return
	 */
	public int peek() {
		if (mGetPtr == mIntList.size()) {
			throw new java.util.NoSuchElementException();
		}
		return mIntList.get(mGetPtr);
	}
	
	/**
	 * Returns the element at position n in the queue without removing it.
	 * 
	 * @param n
	 * @return
	 */
	public int peek(int n) {
		return mIntList.get(n+mGetPtr);
	}
	
	public Object clone() {
		try {
			ArrayIntQueue clone = (ArrayIntQueue) super.clone();
			clone.mIntList = (IntArrayList) this.mIntList.clone();
			return clone;
		} catch (CloneNotSupportedException cnse) {
			throw new InternalError();
		}
	}
	
	public int hashCode() {
		int hc = size();
		int listSz = mIntList.size();
		for (int i=mGetPtr; i<listSz; i++) {
			hc = 37*hc + mIntList.get(i);
		}
		return hc;
	}
	
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (o instanceof ArrayIntQueue) {
			ArrayIntQueue other = (ArrayIntQueue) o;
			int sz = this.size();
			if (sz == other.size()) {
				for (int i=0; i<sz; i++) {
					if (this.peek(i) != other.peek(i)) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}
}
