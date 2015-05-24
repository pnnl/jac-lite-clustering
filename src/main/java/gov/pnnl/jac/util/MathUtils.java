package gov.pnnl.jac.util;

import gov.pnnl.jac.collections.DoubleList;

import java.math.BigInteger;
import java.util.Random;

public final class MathUtils {

	// Factorials for 0 - 20, the only int values having factorials within the
	// range of longs.
	private static long[] FACTORIALS = {
		1L, 1L, 2L, 6L, 24L, 120L, 720L, 5040L, 40320L, 362880L, 3628800L, 39916800L, 
		479001600L, 6227020800L, 87178291200L, 1307674368000L, 20922789888000L, 
		355687428096000L, 6402373705728000L, 121645100408832000L, 2432902008176640000L 
	};
	
	static Random RANDOM = new Random(System.nanoTime());
	
	private MathUtils() {}
	
	public static int min(int... values) {
		final int len = values.length;
		int min = Integer.MAX_VALUE;
		for (int i=0; i<len; i++) {
			int v = values[i];
			if (v < min) min = v;
		}
		return min;
	}
	
	public static int max(int... values) {
		final int len = values.length;
		int max = Integer.MIN_VALUE;
		for (int i=0; i<len; i++) {
			int v = values[i];
			if (v > max) max = v;
		}
		return max;
	}
	
	/**
	 * Set the random number generator used for in the methods for selecting
	 * random integers, floats, etc..  If not called, the random number generator
	 * defaults to a <code>java.util.Random</code> seeded with the system time.
	 * 
	 * @param random
	 */
	public static void setRandom(final Random random) {
		ExceptionUtil.checkNotNull(random);
		RANDOM = random;
	}
	
	/**
	 * Pick a random integer in the range [min - max], inclusive.
	 * 
	 * @param min
	 * @param max
	 * @return
	 */
	public static int randomInt(final int min, final int max) {
		return min + RANDOM.nextInt(max - min + 1);
	}
	
	public static float randomFloat(final float min, final float max) {
		return min + RANDOM.nextFloat() * (max - min);
	}
	
	public static double randomDouble(final double min, final double max) {
		return min + RANDOM.nextDouble() * (max - min);
	}
	
	public static boolean isPowerOf2(final int n) {
		return n > 0 && (n & (n - 1)) == 0;
	}

	public static int nextPowerOf2(int n) {

		if (isPowerOf2(n)) {
			return n;
		}

		if (n == 0) {
			return 1;
		}

		return Integer.highestOneBit(n) << 1;
	}

	/**
	 * Returns true if the absolute value of the difference between two numbers is
	 * less than or equal to the specified tolerance. 
	 * 
	 * @param d1 - the first number to compare
	 * @param d2 - the second number to compare
	 * @param tolerance - the tolerance.
	 * @return
	 */
	public static boolean approxEqual(double d1, double d2, double tolerance) {
		if (!Double.isNaN(d1) && !Double.isNaN(d2)) {
			return Math.abs(d1 - d2) <= tolerance;
		}
		if (tolerance < 0) {
			throw new IllegalArgumentException("tolerance must be positive");
		}
		return false;
	}
	
	public static long factorial(int n) {
		try {
			return FACTORIALS[n];
		} catch (IndexOutOfBoundsException e) {
			throw new IllegalArgumentException("n must be in [0 - " + (FACTORIALS.length - 1) + "]: " + n);
		}
	}
	
	public static BigInteger largeFactorial(int n) {
		if (n < 0) {
			throw new IllegalArgumentException("n cannot be negative: " + n);
		}
		if (n < FACTORIALS.length) {			
			return BigInteger.valueOf(factorial(n));
		}
		BigInteger fact = BigInteger.valueOf(n);
		while(--n > FACTORIALS.length - 1) {
			fact = fact.multiply(BigInteger.valueOf(n));
		}
		fact = fact.multiply(BigInteger.valueOf(FACTORIALS[FACTORIALS.length-1]));
		return fact;
	}
	
	public static long permutations(int n, int r) {
		ExceptionUtil.checkNonNegative(n);
		ExceptionUtil.checkNonNegative(r);
		ExceptionUtil.checkLessThanOrEqual(r, n);
		long result = 1L;
		long lim = Math.max(1, n-r);
		for (long L=n; L>lim; L--) {
			result *= L;
		}
		return result;
	}
	
	public static int constrain(int value, int min, int max) {
		return Math.min(max, Math.max(value, min));
	}
	
	public static long constrain(long value, long min, long max) {
		return Math.min(max, Math.max(value, min));
	}
	
	public static float constrain(float value, float min, float max) {
		return Math.min(max, Math.max(value, min));
	}

	public static double constrain(double value, double min, double max) {
		return Math.min(max, Math.max(value, min));
	}
	
	public static int imin(int... values) {
		if (values.length == 0) {
			return 0;
		}
		int min = Integer.MAX_VALUE;
		for (int v: values) {
			if (v < min) min = v;
		}
		return min;
	}

	public static long lmin(long... values) {
		if (values.length == 0) {
			return 0;
		}
		long min = Long.MAX_VALUE;
		for (long v: values) {
			if (v < min) min = v;
		}
		return min;
	}
	
	public static float fmin(float... values) {
		if (values.length == 0) {
			return 0;
		}
		float min = Float.MAX_VALUE;
		for (float v: values) {
			if (v < min) min = v;
		}
		return min;
	}
	
	public static double dmin(double... values) {
		if (values.length == 0) {
			return 0;
		}
		double min = Double.MAX_VALUE;
		for (double v: values) {
			if (v < min) min = v;
		}
		return min;
	}
	
	public static int imax(int... values) {
		if (values.length == 0) {
			return 0;
		}
		int max = Integer.MIN_VALUE;
		for (int v: values) {
			if (v > max) max = v;
		}
		return max;
	}

	public static long lmax(long... values) {
		if (values.length == 0) {
			return 0;
		}
		long max = Long.MIN_VALUE;
		for (long v: values) {
			if (v > max) max = v;
		}
		return max;
	}

    public static float fmax(float... values) {
		if (values.length == 0) {
			return 0;
		}
		float max = -Float.MAX_VALUE;
		for (float v: values) {
			// NaN
			if (Float.isNaN(v)) return v;
			if (v > max) max = v;
		}
		return max;
	}
	
	public static double dmax(double... values) {
		if (values.length == 0) {
			return 0;
		}
		double max = -Double.MAX_VALUE;
		for (double v: values) {
			// NaN
			if (Double.isNaN(v)) return v;
			if (v > max) max = v;
		}
		return max;
	}
	
	public static double log(double value, double base) {
		return Math.log(value)/Math.log(base);
	}
	
	/**
	 * Returns the value of the first integer argument raised to the value
	 * of the second integer argument.  The value returned is a long.
	 *
	 * @param value
	 * @param power - the power, which cannot be negative.
	 * 
	 * @return
	 * 
	 * @throws IllegalArgumentException if power is negative.
	 * @throws ArithmeticException if the answer is too large to be represented
	 *   by a long.  (In that case, use <code>largeIPow(value, power)</code>.
	 */
	public static long ipow(int value, int power) {
		if (power < 0) {
			throw new IllegalArgumentException("power cannot be negative: " + power);
		}
		if (value == 0) return 0L;
		if (value == 1) return 1L;
		long lv = value;
		long result = 1L;
		for (int i=0; i<power; i++) {
			if (Long.MAX_VALUE/result < lv) {
				throw new ArithmeticException("result exceeds Long.MAX_VALUE");
			}
			result *= value;
		}
		return result;
	}
	
	/**
	 * Returns the value of the first integer argument raised to the value
	 * of the second integer argument.  The value returned is a <code>BigInteger</code>,
	 * so that large powers can be used.
	 *
	 * @param value
	 * @param power - the power, which cannot be negative.
	 * 
	 * @return
	 * 
	 * @throws IllegalArgumentException if power is negative.
	 */
	public static BigInteger largeIPow(int value, int power) {
		if (power < 0) {
			throw new IllegalArgumentException("power cannot be negative: " + power);
		}
		if (value == 0) return BigInteger.ZERO;
		if (value == 1) return BigInteger.ONE;
		BigInteger bv = BigInteger.valueOf(value);
		BigInteger result = BigInteger.ONE;
		for (int i=0; i<power; i++) {
			result = result.multiply(bv);
		}
		return result;
	}
	
	/**
	 * Returns the greatest common factor of the two arguments.
	 * 
	 * @param n
	 * @param m
	 * 
	 * @return
	 */
	public static int gcf(int n, int m) {
		n = Math.abs(n);
		m = Math.abs(m);
		while (m > 0) {
			int t = m;
			m = n % m;
			n = t;
		}
		return n;
	}

	/**
	 * Returns the greatest common factor of the two arguments.
	 * 
	 * @param n
	 * @param m
	 * 
	 * @return
	 */
	public static long gcf(long n, long m) {
		n = Math.abs(n);
		m = Math.abs(m);
		while (m > 0L) {
			long t = m;
			m = n % m;
			n = t;
		}
		return n;
	}
	
	public static double length(double[] vec) {
		double sumSq = 0.0;
		for (int i=0; i<vec.length; i++) {
			sumSq += vec[i]*vec[i];
		}
		return Math.sqrt(sumSq);
	}

	public static double length(DoubleList vec) {
		double sumSq = 0.0;
		final int len = vec.size();
		for (int i=0; i<len; i++) {
			double v = vec.get(i);
			sumSq += v*v;
		}
		return Math.sqrt(sumSq);
	}

	public static void main(String[] args) {
		try {
			int v = 25;
			for (int i=0; i<15; i++) {
				BigInteger p = largeIPow(v, i);
				System.out.printf("%d^%d = %s\n", v, i, p);
			}
		} catch (ArithmeticException ae) {
			
		}
	}
	
}
