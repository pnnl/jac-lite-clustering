package gov.pnnl.jac.collections;

import gov.pnnl.jac.util.HashAndEqualityUtils;

import java.util.Arrays;
import java.util.IdentityHashMap;

import cern.colt.map.HashFunctions;

/**
 * <p>
 * Contains utility methods used by map and set classes.
 * </p>
 * 
 * <p>
 * Copyright: Copyright (c) 2005
 * </p>
 * 
 * <p>
 * Company: Battelle Memorial Institute
 * </p>
 * 
 * @author R. Scarberry, Vern Crow
 * @version 1.0
 */
public final class MapUtils {

	// Convenient primes to be used for new capacities of maps when they are
	// rehashed. This does not contain all the integer prime numbers. Notice
	// that each value is greater than twice the previous value, except for the
	// last, which is Integer.MAX_VALUE.
	private static int[] mPrimes = { 3, 7, 17, 37, 79, 163, 331, 673, 1361,
			2729, 5471, 10949, 21911, 43853, 87719, 175447, 350899, 701819,
			1403641, 2807303, 5614657, 11229331, 22458671, 44917381, 89834777,
			179669557, 359339171, 718678369, 1437356741, Integer.MAX_VALUE };

	// To prevent instantiation.
	private MapUtils() {
	}

	/**
	 * Compute the new capacity for a map being resized/rehashed. Normally, the
	 * returned value will be a prime number greater than twice the specified
	 * old capacity.
	 * 
	 * @param oldCapacity
	 *            int
	 * @return int
	 */
	public static int newCapacity(int oldCapacity) {
		// Prevents overflow.
		int newCap = oldCapacity < Integer.MAX_VALUE / 2 ? 2 * oldCapacity
				: Integer.MAX_VALUE;
		// Bops it up to the next prime in mPrimes.
		return nextRehashPrime(newCap);
	}

	/**
	 * Get the next prime number maintained by this class greater than or equal
	 * to the value specified. The returned prime number may not be the next
	 * prime number greater than n, just the next prime number recorded in this
	 * class.
	 * 
	 * @param n
	 *            int
	 * @return int
	 */
	public static int nextRehashPrime(int n) {
		int ndx = Arrays.binarySearch(mPrimes, n);
		if (ndx < 0) {
			ndx = -ndx - 1;
		}
		return mPrimes[ndx];
	}

	/**
	 * Computes the hash code for the <tt>IntObjectMap</tt> argument. Returns
	 * the same value for different maps containing the same key/value pairs
	 * regardless of their implementation classes.
	 * 
	 * @param map
	 * @return int
	 */
	public static int computeHash(IntObjectMap<?> map) {

		int hc = map.size();

		int[] keys = map.keys();

		// Critical that the keys be sorted, so all maps with the same
		// key-value mappings get the same hash code from this method.
		Arrays.sort(keys);
		int n = keys.length;
		for (int i = 0; i < n; i++) {
			int k = keys[i];
			hc = 37 * hc + k;
			Object v = map.get(k);
			hc = 37 * hc + (v != null ? v.hashCode() : 0);
		}

		return hc;
	}

	/**
	 * Check two <tt>IntObjectMap</tt>s for equality. Returns true for
	 * maps containing the same key/value pairs even if their implementation
	 * classes are different.
	 * 
	 * @param map1
	 * @param map2
	 * @return boolean
	 */
	public static boolean checkEqual(IntObjectMap<?> map1, IntObjectMap<?> map2) {
		// Trivial case
		if (map1 == map2) {
			return true;
		}
		// To be equal, they must be the same size.
		if (map1.size() != map2.size()) {
			return false;
		}
		int[] keys = map1.keys();
		int n = keys.length;
		for (int i = 0; i < n; i++) {
			int k = keys[i];
			// Map2 must contain the same keys as map1.
			if (!map2.containsKey(k)) {
				return false;
			}
			// Now compare the values.
			Object v1 = map1.get(k);
			Object v2 = map2.get(k);
			if ((v1 == null && v2 != null) || (v1 != null && !v1.equals(v2))) {
				return false;
			}
		}
		return true;
	}
	
	public static <E> boolean checkEqual(ObjectIntMap<E> map1, ObjectIntMap<E> map2) {
		if (map1.size() == map2.size()) {
			for (ObjectIntMap.Entry<E> entry : map1.entrySet()) {
				E key = entry.getKey();
				if (!map2.containsKey(key) || entry.getValue() != map2.get(key)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * Check two <tt>LongLongMap</tt>s for equality of content. Returns true for
	 * maps containing the same key/value pairs even if their implementation
	 * classes are different.
	 * 
	 * @param map1
	 * @param map2
	 * @return boolean
	 */
	public static boolean checkEqual(LongLongMap map1, LongLongMap map2) {
		// Trivial case
		if (map1 == map2) {
			return true;
		}
		// To be equal, they must be the same size.
		if (map1.size() != map2.size()) {
			return false;
		}
		long[] keys = map1.keys();
		int n = keys.length;
		for (int i = 0; i < n; i++) {
			long k = keys[i];
			// Map2 must contain the same keys as map1.
			if (!map2.containsKey(k)) {
				return false;
			}
			// Now compare the values.
			long v1 = map1.get(k);
			long v2 = map2.get(k);
			if (v1 != v2) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Computes the hash code for the <tt>FloatObjectMap</tt> argument. Returns
	 * the same value for different maps containing the same key/value pairs
	 * regardless of their implementation classes.
	 * 
	 * @param map
	 * @return int
	 */
	public static int computeHash(FloatObjectMap<?> map) {
		int hc = map.size();
		float[] keys = map.keys();
		// Critical that the keys be sorted, so all maps with the same
		// key-value mappings get the same hash code from this method.
		Arrays.sort(keys);
		int n = keys.length;
		for (int i = 0; i < n; i++) {
			float fk = keys[i];
			hc = 37 * hc + Float.floatToIntBits(fk);
			Object v = map.get(fk);
			hc = 37 * hc + (v != null ? v.hashCode() : 0);
		}
		return hc;
	}

	/**
	 * Computes the hash code for the <tt>FloatObjectMap</tt> argument. Returns
	 * the same value for different maps containing the same key/value pairs
	 * regardless of their implementation classes.
	 * 
	 * @param map
	 * @return int
	 */
	public static int computeHash(DoubleObjectMap<?> map) {
		int hc = map.size();
		double[] keys = map.keys();
		// Critical that the keys be sorted, so all maps with the same
		// key-value mappings get the same hash code from this method.
		Arrays.sort(keys);
		int n = keys.length;
		for (int i = 0; i < n; i++) {
			double fk = keys[i];
			hc = 37 * hc + HashAndEqualityUtils.hash(fk);
			Object v = map.get(fk);
			hc = 37 * hc + (v != null ? v.hashCode() : 0);
		}
		return hc;
	}

	/**
	 * Check two <tt>FloatObjectMap</tt>s for equality. Returns true for maps
	 * containing the same key/value pairs even if their implementation classes
	 * are different.
	 * 
	 * @param map1
	 * @param map2
	 * @return boolean
	 */
	public static boolean checkEqual(FloatObjectMap<?> map1, FloatObjectMap<?> map2) {
		// Trivial case
		if (map1 == map2) {
			return true;
		}
		// To be equal, they must be the same size.
		if (map1.size() != map2.size()) {
			return false;
		}
		float[] keys = map1.keys();
		int n = keys.length;
		for (int i = 0; i < n; i++) {
			float fk = keys[i];
			// Map2 must contain the same keys as map1.
			if (!map2.containsKey(fk)) {
				return false;
			}
			// Now compare the values.
			Object v1 = map1.get(fk);
			Object v2 = map2.get(fk);
			if ((v1 == null && v2 != null) || (v1 != null && !v1.equals(v2))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Check two <tt>FloatObjectMap</tt>s for equality. Returns true for maps
	 * containing the same key/value pairs even if their implementation classes
	 * are different.
	 * 
	 * @param map1
	 * @param map2
	 * @return boolean
	 */
	public static boolean checkEqual(DoubleObjectMap<?> map1, DoubleObjectMap<?> map2) {
		// Trivial case
		if (map1 == map2) {
			return true;
		}
		// To be equal, they must be the same size.
		if (map1.size() != map2.size()) {
			return false;
		}
		double[] keys = map1.keys();
		int n = keys.length;
		for (int i = 0; i < n; i++) {
			double fk = keys[i];
			// Map2 must contain the same keys as map1.
			if (!map2.containsKey(fk)) {
				return false;
			}
			// Now compare the values.
			Object v1 = map1.get(fk);
			Object v2 = map2.get(fk);
			if ((v1 == null && v2 != null) || (v1 != null && !v1.equals(v2))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Computes the hash code for the <tt>IntIntMap</tt> argument. Returns the
	 * same value for different maps containing the same key/value pairs
	 * regardless of their implementation classes.
	 * 
	 * @param map
	 * @return int
	 */
	public static int computeHash(IntIntMap map) {
		int hc = map.size();
		int[] keys = map.keys();
		// Critical that the keys be sorted, so all maps with the same
		// key-value mappings get the same hash code from this method.
		Arrays.sort(keys);
		int n = keys.length;
		for (int i = 0; i < n; i++) {
			int k = keys[i];
			hc = 37 * hc + k;
			hc = 37 * hc + map.get(k);
		}
		return hc;
	}

	/**
	 * Computes the hash code for the <tt>LongLongMap</tt> argument. Returns the
	 * same value for different maps containing the same key/value pairs
	 * regardless of their implementation classes.
	 * 
	 * @param map
	 * @return int
	 */
	public static int computeHash(LongLongMap map) {
		int hc = map.size();
		long[] keys = map.keys();
		// Critical that the keys be sorted, so all maps with the same
		// key-value mappings get the same hash code from this method.
		Arrays.sort(keys);
		int n = keys.length;
		for (int i = 0; i < n; i++) {
			long k = keys[i];
			hc = 37 * hc + HashFunctions.hash(k);
			hc = 37 * hc + HashFunctions.hash(map.get(k));
		}
		return hc;
	}

	public static int computeHash(DoubleDoubleMap map) {
		int hc = map.size();
		double[] keys = map.keys();
		// Critical that the keys be sorted, so all maps with the same
		// key-value mappings get the same hash code from this method.
		Arrays.sort(keys);
		int n = keys.length;
		for (int i = 0; i < n; i++) {
			double k = keys[i];
			hc = 37 * hc + HashAndEqualityUtils.hash(k);
			hc = 37 * hc + HashAndEqualityUtils.hash(map.get(k));
		}
		return hc;
	}

	/**
	 * Computes the hash code for the <tt>LongIntMap</tt> argument. Returns the
	 * same value for different maps containing the same key/value pairs
	 * regardless of their implementation classes.
	 * 
	 * @param map
	 * @return int
	 */
	public static int computeHash(LongIntMap map) {
		int hc = map.size();
		long[] keys = map.keys();
		// Critical that the keys be sorted, so all maps with the same
		// key-value mappings get the same hash code from this method.
		Arrays.sort(keys);
		int n = keys.length;
		for (int i = 0; i < n; i++) {
			long k = keys[i];
			hc = 37 * hc + (int) (k ^ (k >> 32));
			hc = 37 * hc + map.get(k);
		}
		return hc;
	}

	/**
	 * Check two <tt>IntIntMap</tt>s for equality. Returns true for maps
	 * containing the same key/value pairs even if their implementation classes
	 * are different.
	 * 
	 * @param map1
	 * @param map2
	 * @return boolean
	 */
	public static boolean checkEqual(IntIntMap map1, IntIntMap map2) {
		// Trivial case
		if (map1 == map2) {
			return true;
		}
		// To be equal, they must be the same size.
		if (map1.size() != map2.size()) {
			return false;
		}
		int[] keys = map1.keys();
		int n = keys.length;
		for (int i = 0; i < n; i++) {
			int k = keys[i];
			// Map2 must contain the same keys as map1.
			if (!map2.containsKey(k)) {
				return false;
			}
			// Now compare the values.
			if (map1.get(k) != map2.get(k)) {
				return false;
			}
		}
		return true;
	}

	public static boolean checkEqual(DoubleDoubleMap map1, DoubleDoubleMap map2) {
		// Trivial case
		if (map1 == map2) {
			return true;
		}
		// To be equal, they must be the same size.
		if (map1.size() != map2.size()) {
			return false;
		}
		double[] keys = map1.keys();
		int n = keys.length;
		for (int i = 0; i < n; i++) {
			double k = keys[i];
			// Map2 must contain the same keys as map1.
			if (!map2.containsKey(k)) {
				return false;
			}
			// Now compare the values.
			if (Double.doubleToLongBits(map1.get(k)) != Double.doubleToLongBits(map2.get(k))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Check two <tt>LongIntMap</tt>s for equality. Returns true for maps
	 * containing the same key/value pairs even if their implementation classes
	 * are different.
	 * 
	 * @param map1
	 * @param map2
	 * @return boolean
	 */
	public static boolean checkEqual(LongIntMap map1, LongIntMap map2) {
		// Trivial case
		if (map1 == map2) {
			return true;
		}
		// To be equal, they must be the same size.
		if (map1.size() != map2.size()) {
			return false;
		}
		long[] keys = map1.keys();
		int n = keys.length;
		for (int i = 0; i < n; i++) {
			long k = keys[i];
			// Map2 must contain the same keys as map1.
			if (!map2.containsKey(k)) {
				return false;
			}
			// Now compare the values.
			if (map1.get(k) != map2.get(k)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Computes the hash code for the <tt>IntFloatMap</tt> argument. Returns the
	 * same value for different maps containing the same key/value pairs
	 * regardless of their implementation classes.
	 * 
	 * @param map
	 * @return int
	 */
	public static int computeHash(IntFloatMap map) {
		int hc = map.size();
		int[] keys = map.keys();
		// Critical that the keys be sorted, so all maps with the same
		// key-value mappings get the same hash code from this method.
		Arrays.sort(keys);
		int n = keys.length;
		for (int i = 0; i < n; i++) {
			int k = keys[i];
			hc = 37 * hc + k;
			float v = map.get(k);
			hc = 37 * hc + (v != -1f ? Float.floatToIntBits(v) : 0);
		}
		return hc;
	}

	/**
	 * Computes the hash code for the <tt>LongDoubleMap</tt> argument. Returns the
	 * same value for different maps containing the same key/value pairs
	 * regardless of their implementation classes.
	 * 
	 * @param map
	 * @return int
	 */
	public static int computeHash(LongDoubleMap map) {
		int hc = map.size();
		long[] keys = map.keys();
		// Critical that the keys be sorted, so all maps with the same
		// key-value mappings get the same hash code from this method.
		Arrays.sort(keys);
		int n = keys.length;
		for (int i = 0; i < n; i++) {
			long k = keys[i];
			hc = 37 * hc + HashAndEqualityUtils.hash(k);
			double v = map.get(k);
			hc = 37 * hc + HashAndEqualityUtils.hash(v);
		}
		return hc;
	}

	/**
	 * Check two <tt>IntFloatMap</tt>s for equality. Returns true for maps
	 * containing the same key/value pairs even if their implementation classes
	 * are different.
	 * 
	 * @param map1
	 * @param map2
	 * @return boolean
	 */
	public static boolean checkEqual(IntFloatMap map1, IntFloatMap map2) {
		// Trivial case
		if (map1 == map2) {
			return true;
		}
		// To be equal, they must be the same size.
		if (map1.size() != map2.size()) {
			return false;
		}
		int[] keys = map1.keys();
		int n = keys.length;
		for (int i = 0; i < n; i++) {
			int k = keys[i];
			// Map2 must contain the same keys as map1.
			if (!map2.containsKey(k)) {
				return false;
			}
			// Now compare the values.
			if (Float.floatToIntBits(map1.get(k)) != Float.floatToIntBits(map2.get(k))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Check two <tt>LongDoubleMap</tt>s for equality. Returns true for maps
	 * containing the same key/value pairs even if their implementation classes
	 * are different.
	 * 
	 * @param map1
	 * @param map2
	 * @return boolean
	 */
	public static boolean checkEqual(LongDoubleMap map1, LongDoubleMap map2) {
		// Trivial case
		if (map1 == map2) {
			return true;
		}
		// To be equal, they must be the same size.
		if (map1.size() != map2.size()) {
			return false;
		}
		long[] keys = map1.keys();
		int n = keys.length;
		for (int i = 0; i < n; i++) {
			long k = keys[i];
			// Map2 must contain the same keys as map1.
			if (!map2.containsKey(k)) {
				return false;
			}
			// Now compare the values.
			if (Double.doubleToLongBits(map1.get(k)) != Double.doubleToLongBits(map2.get(k))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Computes the hash code for the <tt>IntSet</tt> argument. Returns the same
	 * value for different sets containing the same values regardless of their
	 * implementation classes.
	 * 
	 * @param set
	 * @return int
	 */
	public static int computeHash(IntSet set) {
		int hc = set.size();
		int[] elts = set.toArray();
		// Critical that the elements be sorted, so all sets with the same
		// elements get the same hc from this method.
		Arrays.sort(elts);
		int n = elts.length;
		for (int i = 0; i < n; i++) {
			int k = elts[i];
			hc = 37 * hc + k;
		}
		return hc;
	}

	/**
	 * Computes the hash code for the <tt>LongSet</tt> argument. Returns the same
	 * value for different sets containing the same values regardless of their
	 * implementation classes.
	 * 
	 * @param set
	 * @return int
	 */
	public static int computeHash(LongSet set) {
		int hc = set.size();
		long[] elts = set.toArray();
		// Critical that the elements be sorted, so all sets with the same
		// elements get the same hc from this method.
		Arrays.sort(elts);
		int n = elts.length;
		for (int i = 0; i < n; i++) {
			long k = elts[i];
			hc = 37*hc + (int)(k ^ (k >>> 32));
		}
		return hc;
	}

	/**
	 * Check two <tt>IntSet</tt>s for equality. Returns true for sets containing
	 * the same values even if their implementation classes are different.
	 * 
	 * @param set1
	 * @param set2
	 * @return boolean
	 */
	public static boolean checkEqual(IntSet set1, IntSet set2) {
		// Trivial case
		if (set1 == set2) {
			return true;
		}
		// To be equal, they must be the same size.
		if (set1.size() != set2.size()) {
			return false;
		}
		int[] elts = set1.toArray();
		int n = elts.length;
		for (int i = 0; i < n; i++) {
			if (!set2.contains(elts[i])) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Check two <tt>LongSet</tt>s for equality. Returns true for sets containing
	 * the same values even if their implementation classes are different.
	 * 
	 * @param set1
	 * @param set2
	 * @return boolean
	 */
	public static boolean checkEqual(LongSet set1, LongSet set2) {
		// Trivial case
		if (set1 == set2) {
			return true;
		}
		// To be equal, they must be the same size.
		if (set1.size() != set2.size()) {
			return false;
		}
		long[] elts = set1.toArray();
		int n = elts.length;
		for (int i = 0; i < n; i++) {
			if (!set2.contains(elts[i])) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Computes the hash code for the <tt>FloatSet</tt> argument. Returns the
	 * same value for different sets containing the same values regardless of
	 * their implementation classes.
	 * 
	 * @param set
	 * @return int
	 */
	public static int computeHash(FloatSet set) {
		int hc = 17*set.size();
		float[] elts = set.toArray();
		// Critical that the elements be sorted, so all sets with the same
		// elements get the same hc from this method.
		Arrays.sort(elts);
		int n = elts.length;
		for (int i = 0; i < n; i++) {
			float v = elts[i];
			hc = 37*hc + HashAndEqualityUtils.hash(v);
		}
		return hc;
	}

	/**
	 * Computes the hash code for the <tt>FloatSet</tt> argument. Returns the
	 * same value for different sets containing the same values regardless of
	 * their implementation classes.
	 * 
	 * @param set
	 * @return int
	 */
	public static int computeHash(DoubleSet set) {
		int hc = 17*set.size();
		double[] elts = set.toArray();
		// Critical that the elements be sorted, so all sets with the same
		// elements get the same hc from this method.
		Arrays.sort(elts);
		int n = elts.length;
		for (int i = 0; i < n; i++) {
			double v = elts[i];
			hc = 37*hc + HashAndEqualityUtils.hash(v);
		}
		return hc;
	}

	/**
	 * Check two <tt>FloatSet</tt>s for equality. Returns true for sets
	 * containing the same values even if their implementation classes are
	 * different.
	 * 
	 * @param set1
	 * @param set2
	 * @return boolean
	 */
	public static boolean checkEqual(FloatSet set1, FloatSet set2) {
		// Trivial case
		if (set1 == set2) {
			return true;
		}
		// To be equal, they must be the same size.
		if (set1.size() != set2.size()) {
			return false;
		}
		float[] elts = set1.toArray();
		int n = elts.length;
		for (int i = 0; i < n; i++) {
			if (!set2.contains(elts[i])) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Check two <tt>DoubleSet</tt>s for equality. Returns true for sets
	 * containing the same values even if their implementation classes are
	 * different.
	 * 
	 * @param set1
	 * @param set2
	 * @return boolean
	 */
	public static boolean checkEqual(DoubleSet set1, DoubleSet set2) {
		// Trivial case
		if (set1 == set2) {
			return true;
		}
		// To be equal, they must be the same size.
		if (set1.size() != set2.size()) {
			return false;
		}
		double[] elts = set1.toArray();
		int n = elts.length;
		for (int i = 0; i < n; i++) {
			if (!set2.contains(elts[i])) {
				return false;
			}
		}
		return true;
	}
}
