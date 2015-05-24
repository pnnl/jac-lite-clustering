package gov.pnnl.jac.util;

public final class HashAndEqualityUtils {

	private HashAndEqualityUtils() {}
	
	public static int hash(byte b) {
		return (int) b;
	}
	
	public static int hash(char c) {
		return (int) c;
	}
	
	public static int hash(short s) {
		return (int) s;
	}
	
	public static int hash(int n) {
		return n;
	}
	
	public static int hash(long l) {
		return (int) (l ^ l >>> 32);
	}
	
	public static int hash(float f) {
		return Float.floatToIntBits(f);
	}
	
	public static int hash(double d) {
		long bits = Double.doubleToLongBits(d);
		return (int) (bits ^ bits >>> 32);
	}
	
	public static int hash(Object o) {
		return (o != null ? o.hashCode() : 0);
	}
	
	public static int hash(boolean b) {
		return b ? 1231 : 1237;
	}
	
	public static int hash(char... c) {
		if (c == null) return 0;
		int hc = 17;
		for (int i=0; i<c.length; i++) {
			hc = 31*hc + hash(c[i]);
		}
		return hc;
	}

	public static int hash(short... s) {
		if (s == null) return 0;
		int hc = 17;
		for (int i=0; i<s.length; i++) {
			hc = 31*hc + hash(s[i]);
		}
		return hc;
	}
	
	public static int hash(int... n) {
		if (n == null) return 0;
		int hc = 17;
		for (int i=0; i<n.length; i++) {
			hc = 31*hc + hash(n[i]);
		}
		return hc;
	}
	
	public static int hash(long... l) {
		if (l == null) return 0;
		int hc = 17;
		for (int i=0; i<l.length; i++) {
			hc = 31*hc + hash(l[i]);
		}
		return hc;
	}
	
	public static int hash(float... f) {
		if (f == null) return 0;
		int hc = 17;
		for (int i=0; i<f.length; i++) {
			hc = 31*hc + hash(f[i]);
		}
		return hc;
	}
	
	public static int hash(double... d) {
		if (d == null) return 0;
		int hc = 17;
		for (int i=0; i<d.length; i++) {
			hc = 31*hc + hash(d[i]);
		}
		return hc;
	}
	
	public static int hash(byte... b) {
		if (b == null) return 0;
		int hc = 17;
		for (int i=0; i<b.length; i++) {
			hc = 31*hc + hash(b[i]);
		}
		return hc;
	}

	public static boolean areEqual(double d1, double d2) {
		return Double.doubleToLongBits(d1) == Double.doubleToLongBits(d2);
	}
	
	public static boolean areEqual(float f1, float f2) {
		return Float.floatToIntBits(f1) == Float.floatToIntBits(f2);
	}
	
	public static boolean areEqual(Object o1, Object o2) {
		// Same reference or both null, then equal.
		if (o1 == o2) return true;
		Class<?> c1 = o1 != null ? o1.getClass() : null;
		Class<?> c2 = o2 != null ? o2.getClass() : null;
		// Both o1 and o2 have to be non-null and of the same class for this to hold.
		if (c1 == c2) {
			return o1.equals(o2);
		}
		return false;
	}
	
	public static boolean areEqual(byte[] f1, byte[] f2) {
		final int len1 = f1 != null ? f1.length : -1;
		final int len2 = f2 != null ? f2.length : -1;
		if (len1 == len2) {
			for (int i=0; i<len1; i++) {
				if (f1[i] != f2[i]) return false;
			}
			return true;
		}
		return false;
	}
	
	public static boolean areEqual(char[] f1, char[] f2) {
		final int len1 = f1 != null ? f1.length : -1;
		final int len2 = f2 != null ? f2.length : -1;
		if (len1 == len2) {
			for (int i=0; i<len1; i++) {
				if (f1[i] != f2[i]) return false;
			}
			return true;
		}
		return false;
	}

	public static boolean areEqual(int[] f1, int[] f2) {
		final int len1 = f1 != null ? f1.length : -1;
		final int len2 = f2 != null ? f2.length : -1;
		if (len1 == len2) {
			for (int i=0; i<len1; i++) {
				if (f1[i] != f2[i]) return false;
			}
			return true;
		}
		return false;
	}
	
	public static boolean areEqual(long[] f1, long[] f2) {
		final int len1 = f1 != null ? f1.length : -1;
		final int len2 = f2 != null ? f2.length : -1;
		if (len1 == len2) {
			for (int i=0; i<len1; i++) {
				if (f1[i] != f2[i]) return false;
			}
			return true;
		}
		return false;
	}
	
	public static boolean areEqual(float[] f1, float[] f2) {
		final int len1 = f1 != null ? f1.length : -1;
		final int len2 = f2 != null ? f2.length : -1;
		if (len1 == len2) {
			for (int i=0; i<len1; i++) {
				if (!areEqual(f1[i], f2[i])) return false;
			}
			return true;
		}
		return false;
	}
	
	public static boolean areEqual(double[] f1, double[] f2) {
		final int len1 = f1 != null ? f1.length : -1;
		final int len2 = f2 != null ? f2.length : -1;
		if (len1 == len2) {
			for (int i=0; i<len1; i++) {
				if (!areEqual(f1[i], f2[i])) return false;
			}
			return true;
		}
		return false;
	}
	
	public static boolean areEqual(Object[] f1, Object[] f2) {
		final int len1 = f1 != null ? f1.length : -1;
		final int len2 = f2 != null ? f2.length : -1;
		if (len1 == len2) {
			for (int i=0; i<len1; i++) {
				if (!areEqual(f1[i], f2[i])) return false;
			}
			return true;
		}
		return false;
	}
	
	public static boolean elementsEqual(byte[] f1, int offset1, byte[] f2, int offset2, int len) {
		
		ExceptionUtil.checkNonNegative(offset1);
		ExceptionUtil.checkNonNegative(offset2);
		
		final int len1 = f1 != null ? f1.length : 0;
		final int len2 = f2 != null ? f2.length : 0;
		
		final int lim1 = offset1 + len;
		final int lim2 = offset2 + len;

		ExceptionUtil.checkInBounds(lim1, offset1, len1);
		ExceptionUtil.checkInBounds(lim2, offset2, len2);
		
		for (int i=0; i<len; i++) {
			if (f1[i+offset1] != f2[i+offset2]) return false;
		}
		
		return true;
	}
	
	public static boolean elementsEqual(char[] f1, int offset1, char[] f2, int offset2, int len) {
		
		ExceptionUtil.checkNonNegative(offset1);
		ExceptionUtil.checkNonNegative(offset2);
		
		final int len1 = f1 != null ? f1.length : 0;
		final int len2 = f2 != null ? f2.length : 0;
		
		final int lim1 = offset1 + len;
		final int lim2 = offset2 + len;

		ExceptionUtil.checkInBounds(lim1, offset1, len1);
		ExceptionUtil.checkInBounds(lim2, offset2, len2);
		
		for (int i=0; i<len; i++) {
			if (f1[i+offset1] != f2[i+offset2]) return false;
		}
		
		return true;
	}
	
	public static boolean elementsEqual(short[] f1, int offset1, short[] f2, int offset2, int len) {
		
		ExceptionUtil.checkNonNegative(offset1);
		ExceptionUtil.checkNonNegative(offset2);
		
		final int len1 = f1 != null ? f1.length : 0;
		final int len2 = f2 != null ? f2.length : 0;
		
		final int lim1 = offset1 + len;
		final int lim2 = offset2 + len;

		ExceptionUtil.checkInBounds(lim1, offset1, len1);
		ExceptionUtil.checkInBounds(lim2, offset2, len2);
		
		for (int i=0; i<len; i++) {
			if (f1[i+offset1] != f2[i+offset2]) return false;
		}
		
		return true;
	}
	
	public static boolean elementsEqual(int[] f1, int offset1, int[] f2, int offset2, int len) {
		
		ExceptionUtil.checkNonNegative(offset1);
		ExceptionUtil.checkNonNegative(offset2);
		
		final int len1 = f1 != null ? f1.length : 0;
		final int len2 = f2 != null ? f2.length : 0;
		
		final int lim1 = offset1 + len;
		final int lim2 = offset2 + len;

		ExceptionUtil.checkInBounds(lim1, offset1, len1);
		ExceptionUtil.checkInBounds(lim2, offset2, len2);
		
		for (int i=0; i<len; i++) {
			if (f1[i+offset1] != f2[i+offset2]) return false;
		}
		
		return true;
	}
	
	public static boolean elementsEqual(long[] f1, int offset1, long[] f2, int offset2, int len) {
		
		ExceptionUtil.checkNonNegative(offset1);
		ExceptionUtil.checkNonNegative(offset2);
		
		final int len1 = f1 != null ? f1.length : 0;
		final int len2 = f2 != null ? f2.length : 0;
		
		final int lim1 = offset1 + len;
		final int lim2 = offset2 + len;

		ExceptionUtil.checkInBounds(lim1, offset1, len1);
		ExceptionUtil.checkInBounds(lim2, offset2, len2);
		
		for (int i=0; i<len; i++) {
			if (f1[i+offset1] != f2[i+offset2]) return false;
		}
		
		return true;
	}
	
	public static boolean elementsEqual(float[] f1, int offset1, float[] f2, int offset2, int len) {
		
		ExceptionUtil.checkNonNegative(offset1);
		ExceptionUtil.checkNonNegative(offset2);
		
		final int len1 = f1 != null ? f1.length : 0;
		final int len2 = f2 != null ? f2.length : 0;
		
		final int lim1 = offset1 + len;
		final int lim2 = offset2 + len;

		ExceptionUtil.checkInBounds(lim1, offset1, len1);
		ExceptionUtil.checkInBounds(lim2, offset2, len2);
		
		for (int i=0; i<len; i++) {
			if (!areEqual(f1[i+offset1], f2[i+offset2])) return false;
		}
		
		return true;
	}
	
	public static boolean elementsEqual(double[] f1, int offset1, double[] f2, int offset2, int len) {
		
		ExceptionUtil.checkNonNegative(offset1);
		ExceptionUtil.checkNonNegative(offset2);
		
		final int len1 = f1 != null ? f1.length : 0;
		final int len2 = f2 != null ? f2.length : 0;
		
		final int lim1 = offset1 + len;
		final int lim2 = offset2 + len;

		ExceptionUtil.checkInBounds(lim1, offset1, len1);
		ExceptionUtil.checkInBounds(lim2, offset2, len2);
		
		for (int i=0; i<len; i++) {
			if (!areEqual(f1[i+offset1], f2[i+offset2])) return false;
		}
		
		return true;
	}
}
