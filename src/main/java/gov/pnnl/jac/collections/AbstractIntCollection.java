package gov.pnnl.jac.collections;

import java.util.Arrays;

public abstract class AbstractIntCollection implements IntCollection {

	/**
	 * Sole constructor, usually implicitly invoked by subclasses.
	 */
	protected AbstractIntCollection() {}
	
	/**
	 *  {@inheritDoc}
	 */
	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Iterates over the values, comparing each element with
	 * the specified value.  Runs in O(n) time.
	 */
	@Override
	public boolean contains(int value) {
		IntCollectionIterator it = iterator();
		while (it.hasNext()) {
			if (value == it.next()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Uses an iterator to copy the values into an array of size() elements.
	 * 
	 */
	@Override
	public int[] toArray() {
		int sz = size();
		int[] result = new int[sz];
		IntCollectionIterator it = iterator();
		int n = 0;
		while(it.hasNext()) {
			int value = it.next();
			result[n++] = value;
			// This could only happen if one thread is executing this method
			// while another thread is adding elements.
			if (n == sz && it.hasNext()) {
				int newSz = Math.max(n+1, size());
				int[] newResult = new int[newSz];
				System.arraycopy(result, 0, newResult, 0, sz);
				result = newResult;
				sz = newSz;
			}
		}
		if (n < sz) {
			// Trim it down in the rare case of other threads removing elements
			// while a thread is executing this method.
			result = Arrays.copyOf(result, n);
		}
		return result;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Implementation which iterates over the values looking for the
	 * first occurrence of the specified element and deleting it.
	 */
	@Override
	public boolean remove(int value) {
		IntCollectionIterator it = iterator();
		if (it.hasNext()) {
			if (value == it.next()) {
				it.remove();
				return true;
			}
		}
		return false;
	}

	/**
	 * {@inheritDoc} 
	 * 
	 * <p>Implementation which iterates over the specified collection,
	 * checking to see whether each element is contained in this collection.
	 * 
	 * @return true if all elements are contained in this collection, false
	 *   otherwise.
	 */
	@Override
	public boolean containsAll(IntCollection c) {
		IntCollectionIterator it = c.iterator();
		while(it.hasNext()) {
			if (!contains(it.next())) {
				return false;
			}
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsAll(int[] values) {
		final int sz = values.length;
		for (int i=0; i<sz; i++) {
			if (!contains(values[i])) {
				return false;
			}
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean addAll(IntCollection c) {
		boolean modified = false;
		IntCollectionIterator it = c.iterator();
		while(it.hasNext()) {
			if (add(it.next())) {
				modified = true;
			}
		}
		return modified;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean addAll(int[] values) {
		boolean modified = false;
		final int sz = values.length;
		for (int i=0; i<sz; i++) {
			if (add(values[i])) {
				modified = true;
			}
		}
		return modified;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean removeAll(IntCollection c) {
		boolean modified = false;
		IntCollectionIterator it = c.iterator();
		while(it.hasNext()) {
			if (remove(it.next())) {
				modified = true;
			}
		}
		return modified;
	}

	/**
	 * {@inheritDoc} 
	 */
	@Override
	public boolean removeAll(int[] values) {
		boolean modified = false;
		final int sz = values.length;
		for (int i=0; i<sz; i++) {
			if (remove(values[i])) {
				modified = true;
			}
		}
		return modified;
	}

	/**
	 * {@inheritDoc} 
	 */
	@Override
	public boolean retainAll(IntCollection c) {
		boolean modified = false;
		IntCollectionIterator it = iterator();
		while(it.hasNext()) {
			if (!c.contains(it.next())) {
				it.remove();
				modified = true;
			}
		}
		return modified;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean retainAll(int[] values) {
		boolean modified = false;
		IntSet valueSet = new IntHashSet(values);
		IntCollectionIterator it = iterator();
		while(it.hasNext()) {
			if (!valueSet.contains(it.next())) {
				it.remove();
				modified = true;
			}
		}
		return modified;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear() {
		IntCollectionIterator it = iterator();
		while(it.hasNext()) {
			it.next();
			it.remove();
		}
	}

	/**
	 * Returns a string representation of this collection in the 
	 * format of the values separated by commas between square brackets.
	 */
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		IntCollectionIterator it = iterator();
		while(it.hasNext()) {
			sb.append(String.valueOf(it.next()));
			if (it.hasNext()) {
				sb.append(',');
			}
		}
		sb.append(']');
		return sb.toString();
	}
}
