package gov.pnnl.jac.collections;

import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;

public abstract class AbstractFloatList implements FloatList {

	// Mainly to trigger concurrent modification exceptions when attempting
	// to modify by directly calling methods while using an iterator.
	protected transient int mModCount;

	/**
	 * Returns <tt>true</tt> if this collection contains no elements.
	 * 
	 * @return <tt>true</tt> if this collection contains no elements
	 */
	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	/**
	 * Returns <tt>true</tt> if this collection contains the specified value.
	 * 
	 * @param value
	 *            element whose presence in this collection is to be tested.
	 * @return <tt>true</tt> if this collection contains the specified value.
	 */
	@Override
	public boolean contains(float value) {
		final int sz = size();
		if (sz > 0) {
			if (Float.isNaN(value)) {
				for (int i = 0; i < sz; i++) {
					if (Float.isNaN(get(i))) {
						return true;
					}
				}
			} else {
				for (int i = 0; i < sz; i++) {
					if (get(i) == value) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Removes a single instance of the specified value from this collection, if
	 * it is present (optional operation).
	 * 
	 * @param value
	 *            element to be removed from this collection, if present.
	 * @return <tt>true</tt> if this collection changed as a result of the call
	 * 
	 * @throws UnsupportedOperationException
	 *             remove is not supported by this collection.
	 */
	@Override
	public boolean remove(float value) {
		int index = indexOf(value);
		if (index >= 0) {
			removeAt(index);
			mModCount++;
			return true;
		}
		return false;
	}

	/**
	 * Returns <tt>true</tt> if this collection contains all of the elements in
	 * the specified collection.
	 * 
	 * @param c
	 *            collection to be checked for containment in this collection.
	 * @return <tt>true</tt> if this collection contains all of the elements in
	 *         the specified collection
	 * @see #contains(float)
	 */
	@Override
	public boolean containsAll(FloatCollection c) {
		if (c == this)
			return true;
		FloatCollectionIterator it = c.iterator();
		while (it.hasNext()) {
			if (!contains(it.next()))
				return false;
		}
		return true;
	}

	/**
	 * Returns <tt>true</tt> if this collection contains all of the elements in
	 * the specified array.
	 * 
	 * @param values
	 *            array of values to be checked for containment in this
	 *            collection.
	 * @return <tt>true</tt> if this collection contains all of the elements in
	 *         the specified array.
	 * @see #contains(float)
	 */
	@Override
	public boolean containsAll(float[] values) {
		int n = values.length;
		for (int i = 0; i < n; i++) {
			if (!contains(values[i])) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Adds all of the elements in the specified collection to this collection
	 * (optional operation).
	 * 
	 * @param c
	 *            elements to be added to this collection.
	 * @return <tt>true</tt> if this collection changed as a result of the call
	 * 
	 * @throws UnsupportedOperationException
	 *             if this collection does not support the <tt>addAll</tt>
	 *             method.
	 * @throws IllegalArgumentException
	 *             some aspect of an element of the specified collection
	 *             prevents it from being added to this collection.
	 * 
	 * @see #add(float)
	 */
	@Override
	public boolean addAll(FloatCollection c) {
		if (c.size() == 0)
			return false;
		int added = 0;
		FloatCollectionIterator it = c.iterator();
		while (it.hasNext()) {
			if (add(it.next())) {
				added++;
			}
		}
		return added > 0;
	}

	/**
	 * Adds all of the elements in the specified array to this collection
	 * (optional operation).
	 * 
	 * @param values
	 *            elements to be added to this collection.
	 * @return <tt>true</tt> if this collection changed as a result of the call
	 * 
	 * @throws UnsupportedOperationException
	 *             if this collection does not support the <tt>addAll</tt>
	 *             method.
	 * @throws IllegalArgumentException
	 *             some aspect of an element of the specified array prevents it
	 *             from being added to this collection.
	 * 
	 * @see #add(float)
	 */
	@Override
	public boolean addAll(float[] values) {
		final int len = values != null ? values.length : 0;
		int added = 0;
		for (int i = 0; i < len; i++) {
			if (add(values[i]))
				added++;
		}
		return added > 0;
	}

	/**
	 * Removes all this collection's elements that are also contained in the
	 * specified collection (optional operation). After this call returns, this
	 * collection will contain no elements in common with the specified
	 * collection.
	 * 
	 * @param c
	 *            elements to be removed from this collection.
	 * @return <tt>true</tt> if this collection changed as a result of the call
	 * 
	 * @throws UnsupportedOperationException
	 *             if the <tt>removeAll</tt> method is not supported by this
	 *             collection.
	 * 
	 * @see #remove(float)
	 * @see #contains(float)
	 */
	@Override
	public boolean removeAll(FloatCollection c) {
		if (c == this) {
			if (size() > 0) {
				clear();
				mModCount++;
				return true;
			}
			return false;
		}

		int removed = 0;
		FloatCollectionIterator it = c.iterator();
		while (it.hasNext()) {
			if (remove(it.next()))
				removed++;
		}

		return removed > 0;
	}

	/**
	 * 
	 * Removes all this collection's elements that are also contained in the
	 * specified array (optional operation).
	 * 
	 * @param values
	 *            elements to be removed from this collection.
	 * @return <tt>true</tt> if this collection changed as a result of the call
	 * 
	 * @throws UnsupportedOperationException
	 *             if the <tt>removeAll</tt> method is not supported by this
	 *             collection.
	 * 
	 * @see #remove(float)
	 * @see #contains(float)
	 */
	@Override
	public boolean removeAll(float[] values) {
		return removeAll(new FloatArrayList(values));
	}

	/**
	 * Retains only the elements in this collection that are contained in the
	 * specified array (optional operation).
	 * 
	 * @param values
	 *            elements to be retained in this collection.
	 * @return <tt>true</tt> if this collection changed as a result of the call
	 * 
	 * @throws UnsupportedOperationException
	 *             if the <tt>retainAll</tt> method is not supported by this
	 *             Collection.
	 * 
	 * @see #remove(float)
	 * @see #contains(float)
	 */
	@Override
	public boolean retainAll(float[] values) {
		return retainAll(new FloatHashSet(values));
	}

	@Override
	public FloatCollectionIterator iterator() {
		return new Itr(this);
	}

	@Override
	public boolean addAll(int index, FloatCollection c) {
		if (index == size()) {
			return addAll(c);
		}
		checkIndexForAdd(index);
		if (c.size() > 0) {
			FloatCollectionIterator it = c.iterator();
			while (it.hasNext()) {
				add(index++, it.next());
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean addAll(int index, float[] values) {
		if (index == size()) {
			return addAll(values);
		}
		checkIndexForAdd(index);
		int n = values != null ? values.length : 0;
		for (int i = 0; i < n; i++) {
			add(index++, values[i]);
		}
		return n > 0;
	}

	/**
	 * Returns the index in this list of the first occurrence of the specified
	 * element, or -1 if this list does not contain this element.
	 * 
	 * @param value
	 *            element to search for.
	 * @return the index in this list of the first occurrence of the specified
	 *         element, or -1 if this list does not contain this element.
	 */
	@Override
	public int indexOf(float value) {
		return indexOf(0, value);
	}

	/**
	 * Returns the index in this list of the first occurrence of the specified
	 * value starting the search at the specified index, or -1 if this list does
	 * not contain this element.
	 * 
	 * @param startIndex
	 *            the place to start searching for the value.
	 * @param value
	 *            element to search for.
	 * @return the index in this list of the first occurrence of the specified
	 *         element, or -1 if this list does not contain this element.
	 * @throws IndexOutOfBoundsException
	 *             if the start index is out of range (startIndex &lt; 0 ||
	 *             index &gt; size()).
	 */
	@Override
	public int indexOf(int startIndex, float value) {
		checkIndexForAdd(startIndex);
		final int sz = size();
		if (Float.isNaN(value)) {
			for (int i = startIndex; i < sz; i++) {
				if (Float.isNaN(get(i))) {
					return i;
				}
			}
		} else {
			for (int i = startIndex; i < sz; i++) {
				if (get(i) == value) {
					return i;
				}
			}
		}
		return -1;
	}

	/**
	 * Returns the last index of the specified value in this list or -1, if the
	 * list does not contain the element.
	 * 
	 * @param value
	 *            the value to be searched for.
	 * @return int
	 */
	@Override
	public int lastIndexOf(float value) {
		final int sz = size();
		if (sz > 0) {
			if (Float.isNaN(value)) {
				for (int i = sz - 1; i >= 0; i--) {
					if (Float.isNaN(get(i))) {
						return i;
					}
				}
			} else {
				for (int i = sz - 1; i >= 0; i--) {
					if (get(i) == value) {
						return i;
					}
				}
			}
		}
		return -1;
	}

	@Override
	public FloatListIterator listIterator() {
		return new LItr(this, 0);
	}

	@Override
	public FloatListIterator listIterator(int index) {
		return new LItr(this, index);
	}

	@Override
	public float[] toArray() {
		final int sz = size();
		float[] rtn = new float[sz];
		for (int i = 0; i < sz; i++) {
			rtn[i] = get(i);
		}
		return rtn;
	}

	/**
	 * Returns the hash code for the list.
	 * 
	 * @return int
	 */
	public int hashCode() {
		return ListUtils.computeHash(this);
	}

	/**
	 * Check for equality with the specified argument. Will return true if the
	 * specified object is an <tt>FloatList</tt> of the same size and with the
	 * same elements in the same order as this list. The other list may be a
	 * different implementation class of <tt>FloatList</tt> and still be
	 * considered equal.
	 * 
	 * @param o
	 *            Object
	 * @return boolean
	 */
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (o == null || o.getClass() != this.getClass()) {
			return false;
		}
		return ListUtils.checkEqual(this, (FloatList) o);
	}

	public abstract FloatList clone();
	
    protected Object _clone() throws CloneNotSupportedException {
    	return super.clone();
    }

	// Check the index for access or removal.
	protected void checkIndexForAccess(int index) {
		if (index < 0 || index >= size()) {
			throw new ArrayIndexOutOfBoundsException("out of bounds: " + index);
		}
	}

	// Check the index for adding.
	protected void checkIndexForAdd(int index) {
		if (index < 0 || index > size()) {
			throw new ArrayIndexOutOfBoundsException("out of bounds: " + index);
		}
	}

	private class Itr implements FloatCollectionIterator {

		private LItr mIt;

		Itr(AbstractFloatList list) {
			mIt = new LItr(list);
		}

		public boolean hasNext() {
			return mIt.hasNext();
		}

		public float next() {
			return mIt.next();
		}

		public void remove() {
			mIt.remove();
		}
	}

	private static class LItr implements FloatListIterator {

		/**
		 * Index of element to be returned by subsequent call to next.
		 */
		int mCursor = 0;
		/**
		 * Index of element returned by most recent call to next or previous.
		 * Reset to -1 if this element is deleted by a call to remove.
		 */
		int mLastRetNdx = -1;

		/**
		 * The modCount value that the iterator believes that the backing List
		 * should have. If this expectation is violated, the iterator has
		 * detected concurrent modification.
		 */
		int mExpectedModCount;

		private AbstractFloatList mList;

		LItr(AbstractFloatList list, int index) {
			mList = list;
			mCursor = index;
			mExpectedModCount = list.mModCount;
		}

		LItr(AbstractFloatList list) {
			this(list, 0);
		}

		public void gotoStart() {
			mCursor = 0;
			mLastRetNdx = -1;
		}

		public void gotoEnd() {
			mCursor = mList.size();
			mLastRetNdx = -1;
		}

		public boolean hasNext() {
			return mCursor != mList.size();
		}

		public float next() {
			try {
				float next = mList.get(mCursor);
				checkForComodification();
				mLastRetNdx = mCursor++;
				return next;
			} catch (IndexOutOfBoundsException e) {
				checkForComodification();
				throw new NoSuchElementException();
			}
		}

		public void remove() {
			if (mLastRetNdx == -1) {
				throw new IllegalStateException();
			}
			checkForComodification();
			try {
				mList.removeAt(mLastRetNdx);
				if (mLastRetNdx < mCursor) {
					mCursor--;
				}
				mLastRetNdx = -1;
				mExpectedModCount = mList.mModCount;
			} catch (IndexOutOfBoundsException e) {
				throw new ConcurrentModificationException();
			}
		}

		final void checkForComodification() {
			if (mList.mModCount != mExpectedModCount) {
				throw new ConcurrentModificationException();
			}
		}

		public boolean hasPrevious() {
			return mCursor != 0;
		}

		public float previous() {
			try {
				float previous = mList.get(--mCursor);
				checkForComodification();
				mLastRetNdx = mCursor;
				return previous;
			} catch (IndexOutOfBoundsException e) {
				checkForComodification();
				throw new NoSuchElementException();
			}
		}

		public int nextIndex() {
			return mCursor;
		}

		public int previousIndex() {
			return mCursor - 1;
		}

		public void set(float value) {
			if (mLastRetNdx == -1) {
				throw new IllegalStateException();
			}
			checkForComodification();
			try {
				mList.set(mLastRetNdx, value);
				mExpectedModCount = mList.mModCount;
			} catch (IndexOutOfBoundsException e) {
				throw new ConcurrentModificationException();
			}
		}

		public void add(float value) {
			checkForComodification();
			try {
				mList.add(mCursor++, value);
				mLastRetNdx = -1;
				mExpectedModCount = mList.mModCount;
			} catch (IndexOutOfBoundsException e) {
				throw new ConcurrentModificationException();
			}
		}
	}
}
