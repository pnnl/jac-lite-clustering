package gov.pnnl.jac.collections;

import java.util.Arrays;

public class SparseDoubleList extends AbstractDoubleList {

	// These lists are always the same length and always correspond 1:1.
	private IntList mIndexes;
	private DoubleList mValues;
	// The current maximum valid index.  Plus 1 is the size of the list.
	private int mMaxIndex = -1;

	/**
	 * Constructor
	 * 
	 * @param initialCapacity initial capacity to allocate for values not equal to 0.
	 */
	public SparseDoubleList(int initialCapacity) {
		mIndexes = new IntArrayList(initialCapacity);
		mValues = new DoubleArrayList(initialCapacity);
	}

	/**
	 * Default constructor.
	 */
	public SparseDoubleList() {
		this(10);
	}

	/**
	 * Constructor
	 * 
	 * @param c initial values to populate the list.
	 */
	public SparseDoubleList(DoubleCollection c) {
		this(c.size());
		addAll(c);
	}

	/**
	 * Constructor
	 * 
	 * @param values initial values to populate the list.
	 */
	public SparseDoubleList(double[] values) {
		this(values.length);
		addAll(values);
	}

	/**
	 * Gives the density of the list.  If only zeros have been added, the 
	 * density returned is 0.  If only non-zeros, the density returned is 1.
	 * 
	 * @return the density as a double in the range [0 - 1].
	 */
	public double density() {
		final int sz = size();
		return sz > 0 ? ((double) mValues.size()) / sz : 0f;
	}

	/**
	 * Returns the number of values in the list.
	 * 
	 */
	@Override
	public int size() {
		return mMaxIndex + 1;
	}

	@Override
	public boolean contains(double value) {
		if (value == 0f) {
			// In other words, it's sparse and
			// contains at least one zero. No need to 
			// search through the values.
			return size() > mIndexes.size();
		} else {
			return super.contains(value);
		}
	}

	@Override
	public boolean add(double value) {
		int index = size();
		if (value != 0f) {
			mIndexes.add(index);
			mValues.add(value);
		}
		mMaxIndex++;
		mModCount++;
		return true;
	}

	@Override
	public boolean remove(double value) {
		final int sz = size();
		for (int i = 0; i < sz; i++) {
			if (get(i) == value) {
				removeAt(i);
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean containsAll(DoubleCollection c) {
		DoubleCollectionIterator it = c.iterator();
		while (it.hasNext()) {
			if (!contains(it.next()))
				return false;
		}
		return true;
	}

	@Override
	public boolean containsAll(double[] values) {
		final int len = values != null ? values.length : 0;
		for (int i = 0; i < len; i++) {
			if (!contains(values[i]))
				return false;
		}
		return true;
	}

	@Override
	public boolean addAll(DoubleCollection c) {
		if (c.isEmpty())
			return false;
		int sz = size();
		DoubleCollectionIterator it = c.iterator();
		while (it.hasNext()) {
			add(sz++, it.next());
		}
		return true;
	}

	@Override
	public boolean addAll(double[] values) {
		final int len = values != null ? values.length : 0;
		if (len == 0)
			return false;
		int sz = size();
		for (int i = 0; i < len; i++) {
			add(sz++, values[i]);
		}
		return true;
	}

	@Override
	public boolean removeAll(DoubleCollection c) {
		int removed = 0;
		DoubleCollectionIterator it = c.iterator();
		while (it.hasNext()) {
			double v = it.next();
			if (remove(v)) {
				removed++;
			}
		}
		return removed > 0;
	}

	@Override
	public boolean removeAll(double[] values) {
		final int len = values != null ? values.length : 0;
		int removed = 0;
		for (int i = 0; i < len; i++) {
			if (remove(values[i]))
				removed++;
		}
		return removed > 0;
	}

	@Override
	public boolean retainAll(double[] values) {

		DoubleSet oldValuesSet = new DoubleHashSet(2 * (mValues.size() + 1));
		oldValuesSet.addAll(mValues);
		if (contains(0f)) {
			oldValuesSet.add(0f);
		}

		final int oldSize = size();
		final IntList oldIndexes = mIndexes;
		final DoubleList oldValues = mValues;
		final int oldMaxIndex = mMaxIndex;

		// Like clear(), but with new structures.
		mIndexes = new IntArrayList(mIndexes.size());
		mValues = new DoubleArrayList(mValues.size());
		mMaxIndex = -1;

		final int len = values != null ? values.length : 0;
		for (int i = 0; i < len; i++) {
			if (oldValuesSet.contains(values[i])) {
				add(values[i]);
			}
		}

		if (oldSize == size() && mIndexes.size() == oldIndexes.size()
				&& mValues.size() == oldValues.size()
				&& oldMaxIndex == mMaxIndex) {
			final int sz = mIndexes.size();
			for (int i = 0; i < sz; i++) {
				if (mIndexes.get(i) != oldIndexes.get(i)
						|| Double.doubleToLongBits(oldValues.get(i)) != Double
								.doubleToLongBits(mValues.get(i))) {
					return true;
				}
			}

			return false;
		}

		return true;
	}

	@Override
	public void clear() {
		mIndexes.clear();
		mValues.clear();
		mMaxIndex = -1;
		mModCount++;
	}

	@Override
	public double[] toArray() {
		return toArray(null);
	}
	
	@Override
	public double[] toArray(double[] buffer) {
		final int sz = size();
		double[] rtn = buffer != null && buffer.length >= sz ? buffer : new double[sz];	
		Arrays.fill(rtn, 0, sz, 0.0);
		final int len = mIndexes.size();
		for (int i = 0; i < len; i++) {
			rtn[mIndexes.get(i)] = mValues.get(i);
		}
		return rtn;
	}

	@Override
	public double get(int index) {
		checkIndexForAccess(index);
		int n = ListUtils.binarySearch(mIndexes, index);
		return n >= 0 ? mValues.get(n) : 0f;
	}

	@Override
	public double set(int index, double value) {

		checkIndexForAccess(index);

		double rtn = 0f;

		int n = ListUtils.binarySearch(mIndexes, index);
		if (n >= 0) { // Index is explicitly present in mIndexes

			// If the value is 0, reduce the sizes of the internal structures.
			if (value == 0f) {
				mIndexes.removeAt(n);
				rtn = mValues.removeAt(n);
			} else {
				rtn = mValues.set(n, value);
			}

		} else { // It needs to be inserted.

			// Change to an insertion point.
			n = -n - 1;

			mIndexes.add(n, index);
			mValues.add(n, value);

		}

		mModCount++;

		return rtn;
	}

	@Override
	public void add(int index, double value) {
		checkIndexForAdd(index);
		int n = ListUtils.binarySearch(mIndexes, index);
		if (n < 0) {
			n = -n - 1;
		}
		final int sz = mIndexes.size();
		for (int i = n; i < sz; i++) {
			mIndexes.set(i, mIndexes.get(i) + 1);
		}
		if (value != 0f) {
			mIndexes.add(n, index);
			mValues.add(n, value);
		}
		mMaxIndex++;
		mModCount++;
	}

	@Override
	public double removeAt(int index) {
		checkIndexForAccess(index);
		double rtn = 0f;
		int n = ListUtils.binarySearch(mIndexes, index);
		if (n >= 0) {
			mIndexes.removeAt(n);
			rtn = mValues.removeAt(n);
		} else {
			n = -n - 1;
		}
		final int sz = mIndexes.size();
		// All the indexes above index have to be decremented.
		for (int i = n; i < sz; i++) {
			mIndexes.set(i, mIndexes.get(i) - 1);
		}
		mMaxIndex--;
		return rtn;
	}

	@Override
	public void sort() {
		sort(true);
	}

	@Override
	public void sort(boolean ascending) {
		double[] values = this.toArray();
		Arrays.sort(values);
		this.clear();
		if (ascending) {
			for (int i = 0; i < values.length; i++) {
				add(values[i]);
			}
		} else {
			for (int i = values.length - 1; i >= 0; i--) {
				add(values[i]);
			}
		}
	}

	@Override
	public boolean retainAll(DoubleCollection c) {

		if (c == this || this.isEmpty()) {
			return false;
		}

		final int sz = c.size();

		if (sz == 0) {
			clear();
			return true;
		}

		DoubleSet fset = c instanceof DoubleSet ? ((DoubleSet) c)
				: new DoubleHashSet(c);

		double[] values = this.toArray();

		this.clear();

		boolean changed = false;

		for (int i = 0; i < values.length; i++) {
			if (fset.contains(values[i])) {
				add(values[i]);
			} else {
				changed = true;
			}
		}

		return changed;
	}

	public SparseDoubleList clone() {
		try {
			SparseDoubleList clone = (SparseDoubleList) super._clone();
			clone.mIndexes = new IntArrayList(this.mIndexes);
			clone.mValues = new DoubleArrayList(this.mValues);
			return clone;
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
	}

}
