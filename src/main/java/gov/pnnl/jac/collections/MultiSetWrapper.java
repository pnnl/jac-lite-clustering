package gov.pnnl.jac.collections;

import gov.pnnl.jac.util.ExceptionUtil;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class MultiSetWrapper<E> implements MultiSet<E> {
	
	private ObjectIntMap<E> mMap;
	
	public MultiSetWrapper(ObjectIntMap<E> map) {
		ExceptionUtil.checkNotNull(map);
		mMap = map;
	}
	
	public MultiSetWrapper() {
		this(new ObjectIntHashMap<E>());
	}

	@Override
	public int size() {
		return mMap.size();
	}

	@Override
	public int totalCount() {
		int[] counts = mMap.values();
		int sum = 0;
		for (int i=0; i<counts.length; i++) {
			sum += counts[i];
		}
		return sum;
	}

	@Override
	public int add(E e) {
		return add(e, 1);
	}

	@Override
	public int add(E e, int howMany) {
		ExceptionUtil.checkNonNegative(howMany);
		int newCount = mMap.containsKey(e) ? mMap.get(e) + howMany : howMany;
		mMap.put(e, newCount);
		return newCount;
	}

	@Override
	public int count(E e) {
		return mMap.containsKey(e) ? mMap.get(e) : 0;
	}

	@Override
	public int remove(E e) {
		return remove(e, 1);
	}

	@Override
	public int remove(E e, int howMany) {
		int newCount = 0;
		if (mMap.containsKey(e)) {
			newCount = Math.max(0, mMap.get(e) - howMany);
			if (newCount == 0) {
				mMap.remove(e);
			} else {
				mMap.put(e, newCount);
			}
		}
		return newCount;
	}
	
	@Override
	public boolean removeCompletely(E e) {
		if (mMap.containsKey(e)) {
			mMap.remove(e);
			return true;
		}
		return false;
	}
	
	@Override
	public boolean removeAll(Collection<E> c) {
		int numRemoved = 0;
		for (E e : c) {
			if (mMap.containsKey(e)) {
				int newCount = mMap.get(e) - 1;
				if (newCount == 0) {
					mMap.remove(e);
				} else {
					mMap.put(e, newCount);
				}
				numRemoved++;
			}
		}
		return numRemoved > 0;
	}

	@Override
	public boolean removeAllCompletely(Collection<E> c) {
		int numRemoved = 0;
		for (E e : c) {
			if (mMap.containsKey(e)) {
				mMap.remove(e);
				numRemoved++;
			}
		}
		return numRemoved > 0;
	}
	
	@Override
	public boolean containsAll(Collection<E> c) {
		for (E e : c) {
			if (!mMap.containsKey(e)) return false;
		}
		return true;
	}

	@Override
	public boolean isEmpty() {
		return mMap.isEmpty();
	}

	@Override
	public boolean containsKey(E key) {
		return mMap.containsKey(key);
	}

	@Override
	public boolean containsValue(int value) {
		return mMap.containsValue(value);
	}

	@Override
	public int put(E key, int value) {
		return mMap.put(key, value);
	}

	@Override
	public void putAll(ObjectIntMap<? extends E> map) {
		mMap.putAll(map);
	}

	@Override
	public void clear() {
		mMap.clear();
	}

	@Override
	public E[] keys() {
		return mMap.keys();
	}

	@Override
	public Iterator<E> keyIterator() {
		return mMap.keyIterator();
	}

	@Override
	public int[] values() {
		return mMap.values();
	}

	@Override
	public Set<gov.pnnl.jac.collections.ObjectIntMap.Entry<E>> entrySet() {
		return mMap.entrySet();
	}

	@Override
	public int get(E key) {
		return mMap.get(key);
	}

	@Override
	public int setCount(E e, int count) {
		ExceptionUtil.checkNonNegative(count);
		int oldCount = count(e);
		if (count == 0) {
			if (oldCount > 0) {
				mMap.remove(e);
			}
		} else {
			mMap.put(e, count);
		}
		return oldCount;
	}

	@Override
	public boolean setCount(E e, int newCount, int expectedCount) {
		if (count(e) == expectedCount) {
			setCount(e, newCount);
			return true;
		}
		return false;
	}
}
