package gov.pnnl.jac.collections;

import java.util.Iterator;

import java.io.*;
import java.util.*;

public class TwoWayHashMap<K, V> implements TwoWayMap<K, V>, Cloneable, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2445588983580490751L;

    static final int DEFAULT_INITIAL_CAPACITY = 16;
    static final float DEFAULT_LOAD_FACTOR = 0.75f;
	
	private HashMap<K, V> mForwardMap;
	private HashMap<V, K> mReverseMap;
	
	public TwoWayHashMap(int initialCapacity, float loadFactor) {
		mForwardMap = new HashMap<K, V> (initialCapacity, loadFactor);
		mReverseMap = new HashMap<V, K> (initialCapacity, loadFactor);
	}
	
	public TwoWayHashMap(int initialCapacity) {
		this(initialCapacity, DEFAULT_LOAD_FACTOR);
	}
	
	public TwoWayHashMap() {
		this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR);
	}
	
	@Override
	public void associate(K k, V v) {
		
		if (k == null || v == null) throw new NullPointerException();
		
		V oldV = mForwardMap.get(k);
		K oldK = mReverseMap.get(v);
		
		if (oldV != null) {
			disassociate(k, oldV);
		}
		if (oldK != null) {
			disassociate(oldK, v);
		}
		
		mForwardMap.put(k, v);
		mReverseMap.put(v, k);
	}

	@Override
	public boolean disassociate(K k, V v) {
		if (v.equals(mForwardMap.get(k)) && k.equals(mReverseMap.get(v))) {
			mForwardMap.remove(k);
			mReverseMap.remove(v);
			return true;
		}
		return false;
	}

	@Override
	public boolean hasForward(K k) {
		return mForwardMap.containsKey(k);
	}

	@Override
	public boolean hasReverse(V v) {
		return mReverseMap.containsKey(v);
	}

	@Override
	public V getForward(K k) {
		return mForwardMap.get(k);
	}

	@Override
	public K getReverse(V v) {
		return mReverseMap.get(v);
	}

	@Override
	public V removeForward(K k) {
		V v = mForwardMap.get(k);
		if (v != null) {
			disassociate(k, v);
		}
		return v;
	}

	@Override
	public K removeReverse(V v) {
		K k = mReverseMap.get(v);
		if (k != null) {
			disassociate(k, v);
		}
		return k;
	}

	@Override
	public int size() {
		return mForwardMap.size();
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	@Override
	public void clear() {
		mForwardMap.clear();
		mReverseMap.clear();
	}

	@Override
	public Set<K> forwardKeySet() {
		return mForwardMap.keySet();
	}
	
	@Override
	public Set<V> reverseKeySet() {
		return mReverseMap.keySet();
	}
	
	@Override
	public Iterator<K> forwardIterator() {
		return new TwoWayIterator<K, V>(mForwardMap, mReverseMap);
	}

	@Override
	public Iterator<V> reverseIterator() {
		return new TwoWayIterator<V, K>(mReverseMap, mForwardMap);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public TwoWayMap<K, V> copy() {
		return (TwoWayMap<K, V>) clone();
	}

	@SuppressWarnings("unchecked")
	public Object clone() {
		try {
			TwoWayHashMap<K, V> clone = (TwoWayHashMap<K, V>) super.clone();
			clone.mForwardMap = (HashMap<K, V>) this.mForwardMap.clone();
			clone.mReverseMap = (HashMap<V, K>) this.mReverseMap.clone();
			return clone;
		} catch (CloneNotSupportedException cnse) {
			throw new InternalError();
		}
	}
	
	private class TwoWayIterator<X, Y> implements java.util.Iterator<X> {
		
		private Iterator<X> mIt;
		private X mLast;
		private Map<X, Y> mMap1;
		private Map<Y, X> mMap2;
		
		private TwoWayIterator(Map<X, Y> map1, Map<Y, X> map2) {
			mMap1 = map1;
			mMap2 = map2;
			mIt = map1.keySet().iterator();
		}

		@Override
		public boolean hasNext() {
			return mIt.hasNext();
		}

		@Override
		public X next() {
			mLast = mIt.next();
			return mLast;
		}

		@Override
		public void remove() {
			Y y = null;
			if (mLast != null) {
				y = mMap1.get(mLast);
			}
			mIt.remove();
			if (y != null) {
				mMap2.remove(y);
			}
		}

	}
}
