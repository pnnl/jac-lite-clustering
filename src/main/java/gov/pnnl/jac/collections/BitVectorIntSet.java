package gov.pnnl.jac.collections;

import gov.pnnl.jac.util.ExceptionUtil;

import java.io.IOException;
import java.io.Serializable;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;

import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.OpenBitSet;

public class BitVectorIntSet implements IntSet, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6552285662289985867L;
	
	private int mMinValue;
	private int mMaxValue;
	private OpenBitSet mBits;
	
	private transient int mModCount;
	
	public BitVectorIntSet(int minValue, int maxValue) {
		ExceptionUtil.checkTrue(maxValue >= minValue, "maxValue cannot be less than minValue");
		mBits = new OpenBitSet(maxValue - minValue + 1);
		mMinValue = minValue;
		mMaxValue = maxValue;
	}
	
	public BitVectorIntSet(OpenBitSet openbitset) {
	    this.mBits = openbitset;
	    this.mMinValue = 0;
	    this.mMaxValue = (int)(openbitset.size());
	}
	
	public BitVectorIntSet(int numBits) {
		this(0, numBits - 1);
	}
	
	public long[] getBits() {
		return mBits.getBits();
	}
	
	public int getMinValue() {
		return mMinValue;
	}
	
	public int getMaxValue() {
		return mMaxValue;
	}
	
	@Override
	public int size() {

		// This method is much faster than calling mBits.cardinality().
		//
		long[] lbits = mBits.getBits();
		final int len = lbits.length;

		int sz = 0;
		for (int i=0; i<len; i++) {
			sz += Long.bitCount(lbits[i]);
		}

		return sz;
	}

	@Override
	public boolean isEmpty() {	
		final long[] lbits = mBits.getBits();
		final int len = lbits.length;
		for (int i=0; i<len; i++) {
			if (Long.bitCount(lbits[i]) > 0) return false;
		}
		return true;
	}

	@Override
	public boolean contains(int value) {
		if (value >= mMinValue && value <= mMaxValue) {
			return mBits.get(value - mMinValue);
		}
		return false;
	}

	@Override
	public int[] toArray() {
		IntArrayList intList = new IntArrayList((int) mBits.cardinality());
		DocIdSetIterator it = mBits.iterator();
		int doc = 0;
		try {
			while((doc = it.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
				intList.add(doc + mMinValue);
			}
		} catch (IOException ioe) {
			// Won't happen with an OpenBitSet.
		}
		return intList.toArray();
	}

	public void addQuick(int value) {
		int bit = value - mMinValue;
		mBits.fastSet(bit);
		mModCount++;
	}
	
	public void removeQuick(int value) {
		int bit = value - mMinValue;
		mBits.fastClear(bit);
		mModCount++;
	}
	
	@Override
	public boolean add(int value) {
		ExceptionUtil.checkInBounds(value, mMinValue, mMaxValue);
		int bit = value - mMinValue;
		if (!mBits.get(bit)) {
			mBits.set(bit);
			mModCount++;
			return true;
		}
		return false;
	}

	@Override
	public boolean remove(int value) {
		if (value >= mMinValue && value <= mMaxValue) {
			int bit = value - mMinValue;
			if (mBits.get(bit)) {
				mBits.clear(bit);
				mModCount++;
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean containsAll(IntCollection c) {
		IntCollectionIterator it = c.iterator();
		while(it.hasNext()) {
			int value = it.next();
			if (!contains(value)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean containsAll(int[] values) {
		final int n = values != null ? values.length : 0;
		for (int i=0; i<n; i++) {
			if (!contains(values[i])) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean addAll(IntCollection c) {
		boolean rtn = false;
		IntCollectionIterator it = c.iterator();
		while(it.hasNext()) {
			int value = it.next();
			if (add(value)) {
				rtn = true;
			}
		}
		return rtn;
	}

	@Override
	public boolean addAll(int[] values) {
		boolean rtn = false;
		final int n = values != null ? values.length : 0;
		for (int i=0; i<n; i++) {
			if (add(values[i])) {
				rtn = true;
			}
		}
		return rtn;
	}

	@Override
	public boolean removeAll(IntCollection c) {
		boolean rtn = false;
		IntCollectionIterator it = c.iterator();
		while(it.hasNext()) {
			int value = it.next();
			if (remove(value)) {
				rtn = true;
			}
		}
		return rtn;
	}

	@Override
	public boolean removeAll(int[] values) {
		boolean rtn = false;
		final int n = values != null ? values.length : 0;
		for (int i=0; i<n; i++) {
			if (remove(values[i])) {
				rtn = true;
			}
		}
		return rtn;
	}

	@Override
	public boolean retainAll(IntCollection c) {
	    if (c != this) {
	        return retainAll(c.toArray());
	    }
	    return false;
	}

	@Override
	public boolean retainAll(int[] values) {
		
		BitVectorIntSet tmpSet = new BitVectorIntSet(this.mMinValue, this.mMaxValue);
		int n = values != null ? values.length : 0;
		for (int i=0; i<n; i++) {
			if (this.contains(values[i])) {
				tmpSet.add(values[i]);
			}
		}
		
		boolean rtn = false;
		
		OpenBitSet tmpBits = tmpSet.mBits;
		final int sz = (int) this.mBits.size();
		for (int i=0; i<sz; i++) {
			if (this.mBits.get(i) && !tmpBits.get(i)) {
				this.mBits.clear(i);
				mModCount++;
				rtn = true;
			}
		}
		
		return rtn;
	}

	@Override
	public void clear() {
		int card = (int) mBits.cardinality();
		if (card > 0) {
			mBits.clear(0, mBits.size());
			mModCount++;
		}
	}

	@Override
	public IntCollectionIterator iterator() {
		return new Itr();
	}

	@Override
	public IntSet unionWith(IntSet other) {
		
		if (other instanceof BitVectorIntSet) {
			
			BitVectorIntSet bvIntSet = (BitVectorIntSet) other;
			
			OpenBitSet unionBits = (OpenBitSet) this.mBits.clone();
			unionBits.union(bvIntSet.mBits);
			
			int minValue = Math.min(this.mMinValue, bvIntSet.mMinValue);
			int maxValue = Math.max(this.mMaxValue, bvIntSet.mMaxValue);
			
			BitVectorIntSet result = new BitVectorIntSet(minValue, maxValue);
			result.mBits = unionBits;
			
			return result;
			
		} else {
		
			int minValue = this.mMinValue;
			int maxValue = this.mMaxValue;
			IntCollectionIterator it = other.iterator();
			while(it.hasNext()) {
				int n = it.next();
				if (n < minValue) {
					minValue = n;
				} else if (n > maxValue) {
					maxValue = n;
				}
			}
		
			IntSet newSet = new BitVectorIntSet(minValue, maxValue);
			it = other.iterator();
			while(it.hasNext()) {
				newSet.add(it.next());
			}
			it = this.iterator();
			while(it.hasNext()) {
				newSet.add(it.next());
			}
		
			return newSet;
		}
	}

	/**
	 * Modifies the receiver so that its elements become a union of its 
	 * current elements with those in the other IntSet.
	 * 
	 * @param other
	 * @return
	 */
	public IntSet unionWithLocal(IntSet other) {
		if (other instanceof BitVectorIntSet) {
			BitVectorIntSet bvIntSet = (BitVectorIntSet) other;
			if (bvIntSet.getMinValue() == this.mMinValue) {
				this.mBits.union(bvIntSet.mBits);
				if (bvIntSet.getMaxValue() > this.getMaxValue()) {
					this.mBits.ensureCapacity(bvIntSet.mBits.capacity());
					this.mMaxValue = bvIntSet.mMaxValue;
				}
			}
			return this;
		}
		// Fallback to allAll() if other isn't a BitVectorIntSet or
		// if it doesn't have the same mMinValue.
		addAll(other);
		return this;
	}
	
	/**
	 * Modifies the receiver so that its elements become the intersection of its 
	 * current elements with those in the other IntSet.
	 * 
	 * @param other
	 * @return
	 */
	public IntSet intersectionWithLocal(IntSet other) {
		if (other instanceof BitVectorIntSet) {
			BitVectorIntSet bvIntSet = (BitVectorIntSet) other;
			if (bvIntSet.getMinValue() == this.getMinValue()) {
				this.mBits.intersect(bvIntSet.mBits);
				return this;
			}
		}
		retainAll(other);
		return this;
	}
	
	/**
	 * Modifies the receiver so that its elements become the xor of its 
	 * current elements with those in the other IntSet.
	 * 
	 * @param other
	 * @return
	 */
	public IntSet xorWithLocal(IntSet other) {
		if (other instanceof BitVectorIntSet) {
			BitVectorIntSet bvIntSet = (BitVectorIntSet) other;
			if (bvIntSet.getMinValue() == this.getMinValue()) {
				this.mBits.xor(bvIntSet.mBits);
				if (bvIntSet.getMaxValue() > this.getMaxValue()) {
					this.mBits.ensureCapacity(bvIntSet.mBits.capacity());
					this.mMaxValue = bvIntSet.getMaxValue();
				}
				return this;
			}
		}
		IntSet intersection = this.intersectionWith(other);
		this.addAll(other);
		this.removeAll(intersection);
		return this;
	}

	@Override
	public IntSet intersectionWith(IntSet other) {

		if (other instanceof BitVectorIntSet) {

			BitVectorIntSet bvIntSet = (BitVectorIntSet) other;

			OpenBitSet intersectBits = (OpenBitSet) this.mBits.clone();
			intersectBits.intersect(bvIntSet.mBits);

			int minValue = Math.min(this.mMinValue, bvIntSet.mMinValue);
			int maxValue = Math.max(this.mMaxValue, bvIntSet.mMaxValue);

			BitVectorIntSet result = new BitVectorIntSet(minValue, maxValue);
			result.mBits = intersectBits;

			return result;

		} else {

			int minValue = this.mMinValue;
			int maxValue = this.mMaxValue;
			IntCollectionIterator it = other.iterator();
			while(it.hasNext()) {
				int n = it.next();
				if (n < minValue) {
					minValue = n;
				} else if (n > maxValue) {
					maxValue = n;
				}
			}

			IntSet newSet = new BitVectorIntSet(minValue, maxValue);
			it = this.iterator();
			while(it.hasNext()) {
				int n = it.next();
				if (other.contains(n)) {
					newSet.add(n);
				}
			}

			return newSet;
		}
	}

	@Override
	public IntSet xorWith(IntSet other) {

		if (other instanceof BitVectorIntSet) {

			BitVectorIntSet bvIntSet = (BitVectorIntSet) other;

			OpenBitSet xorBits = (OpenBitSet) this.mBits.clone();
			xorBits.xor(bvIntSet.mBits);

			int minValue = Math.min(this.mMinValue, bvIntSet.mMinValue);
			int maxValue = Math.max(this.mMaxValue, bvIntSet.mMaxValue);

			BitVectorIntSet result = new BitVectorIntSet(minValue, maxValue);
			result.mBits = xorBits;

			return result;

		} else {

			int minValue = this.mMinValue;
			int maxValue = this.mMaxValue;
			IntCollectionIterator it = other.iterator();
			while(it.hasNext()) {
				int n = it.next();
				if (n < minValue) {
					minValue = n;
				} else if (n > maxValue) {
					maxValue = n;
				}
			}

			IntSet newSet = new BitVectorIntSet(minValue, maxValue);

			it = other.iterator();
			while(it.hasNext()) {
				int n = it.next();
				if (!this.contains(n)) {
					newSet.add(n);
				}
			}

			it = this.iterator();
			while(it.hasNext()) {
				int n = it.next();
				if (!other.contains(n)) {
					newSet.add(n);
				}
			}

			return newSet;
		}
	}

	private class Itr implements IntCollectionIterator {

		private DocIdSetIterator mIt;
		private int mNext;
		private int mLast = DocIdSetIterator.NO_MORE_DOCS;
		private int mExpectedModCount = mModCount;
		
		Itr() {
			mIt = mBits.iterator();
			try {
				mNext = mIt.nextDoc();
			} catch (IOException e) {
			}
		}

		@Override
		public boolean hasNext() {
			return mNext != DocIdSetIterator.NO_MORE_DOCS;
		}
		
		@Override
		public int next() {
			if (mNext == DocIdSetIterator.NO_MORE_DOCS) {
				throw new NoSuchElementException();
			}
			checkForComodification();
			mLast = mNext;
			try {
				mNext = mIt.nextDoc();
			} catch (IOException e) {
			}
			return mLast;
		}
		
		@Override
		public void remove() {
			if (mLast == DocIdSetIterator.NO_MORE_DOCS) {
				throw new IllegalStateException();
			}
			checkForComodification();
			mBits.clear(mLast);
			mLast = DocIdSetIterator.NO_MORE_DOCS;
		}
		
	    final void checkForComodification() {
	    	if (mModCount != mExpectedModCount) {
	    		throw new ConcurrentModificationException();
	    	}
	    }
	}
}
