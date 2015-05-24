package gov.pnnl.jac.util;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * <p>This is a non-instantiable class containing static
 * utility methods for checking method parameters and so
 * on.  Most most trigger IllegalArgumentExceptions if
 * the method conditions are not met.
 * </p>
 * 
 * @author D3J923
 *
 */
public final class ExceptionUtil {

	public static void checkNormalFileExists(File f) throws FileNotFoundException {
		if (!f.isFile()) {
			throw new FileNotFoundException("normal file not found: " + f.getAbsolutePath());
		}
	}
	
	public static void checkDirectoryExists(File dir) throws FileNotFoundException {
		if (!dir.isDirectory()) {
			if (dir.isFile()) {
				throw new FileNotFoundException("not a directory: " + dir.getAbsolutePath());
			}
			throw new FileNotFoundException("directory not found: " + dir.getAbsolutePath());
		}
	}
	
	public static void checkTrue(boolean b, String errMsg) {
	    if (!b) {
	        throw new IllegalArgumentException(errMsg);
	    }
	}

	public static void checkSameLength(int[] arr1, int[] arr2) {
		if (arr1.length != arr2.length) throw new IllegalArgumentException("arrays of unequal length: " + 
				arr1.length + " != " + arr2.length);
	}
	
	public static void checkNonNegative(int n) {
		if (n < 0) throw new IllegalArgumentException("negative value: " + n);
	}
	
	public static void checkNonNegative(double d) {
		if (Double.isNaN(d) || d < 0) throw new IllegalArgumentException("negative: " + d);
	}

	public static void checkNonNegative(long n) {
		if (n < 0L) throw new IllegalArgumentException("negative value: " + n);
	}

	public static void checkPositive(int n) {
		if (n <= 0) throw new IllegalArgumentException("not positive: " + n);
	}
	
	public static void checkPositive(double d) {
		if (Double.isNaN(d) || d <= 0) throw new IllegalArgumentException("not positive: " + d);
	}

	public static void checkPercent(double percent) {
		if (percent < 0.0 || percent > 100.0) {
			throw new IllegalArgumentException("invalid percentage: " + percent);
		}
	}
	
	public static void checkFraction(double fraction) {
		if (fraction < 0.0 || fraction > 1.0) {
			throw new IllegalArgumentException("invalid fraction: " + fraction);
		}
	}
	
	public static void checkInBounds(int value, int lower, int upper) {
		checkInBounds(value, lower, upper, true, true);
	}
	
	public static void checkBetween(int value, int lower, int upper) {
		checkInBounds(value, lower, upper, false, false);
	}
	
	public static void checkInBounds(int value, 
			int lower, int upper,
			boolean lowerInclusive, boolean upperInclusive) {
		boolean lowerOk = value > lower || (value == lower && lowerInclusive);
		boolean upperOk = true;
		if (lowerOk) {
			upperOk = value < upper || (value == upper && upperInclusive);
		}
		if (!lowerOk || !upperOk) {
			throw new IllegalArgumentException("value of of range: " + value);
		}
	}

	public static void checkInBounds(double value, double lower, double upper) {
		checkInBounds(value, lower, upper, true, true);
	}
	
	public static void checkBetween(double value, double lower, double upper) {
		checkInBounds(value, lower, upper, false, false);
	}
	
	public static void checkInBounds(double value, 
			double lower, double upper,
			boolean lowerInclusive, boolean upperInclusive) {
		boolean lowerOk = value > lower || (value == lower && lowerInclusive);
		boolean upperOk = true;
		if (lowerOk) {
			upperOk = value < upper || (value == upper && upperInclusive);
		}
		if (!lowerOk || !upperOk) {
			throw new IllegalArgumentException(String.format("value not in %s%f - %f%s: %f", 
					(lowerInclusive ? "[" : "("), lower, upper, (upperInclusive ? "]" : ")"), value));
		}
	}
	
	public static void checkGreaterThan(int value, int lowerBound) {
		if (value <= lowerBound) {
			throw new IllegalArgumentException("<= " + lowerBound + ": " + value);
		}
	}
	
	public static void checkGreaterThanOrEqual(int value, int lowerBound) {
		if (value < lowerBound) {
			throw new IllegalArgumentException("< " + lowerBound + ": " + value);
		}
	}

	public static void checkLessThan(int value, int upperBound) {
		if (value >= upperBound) {
			throw new IllegalArgumentException(">= " + upperBound + ": " + value);
		}
	}
	
	public static void checkLessThanOrEqual(int value, int upperBound) {
		if (value > upperBound) {
			throw new IllegalArgumentException("> " + upperBound + ": " + value);
		}
	}

	/**
	 * Ensures none of the specified objects are null.  If
	 * one is null, this method generates a 
	 * NullPointerException.
	 * 
	 * @param obs variable number of object references.
	 */
	public static void checkNotNull(Object ... obs) {
		for (Object o: obs) {
			if (o == null) {
				throw new NullPointerException();
			}
		}
	}
	
	/**
	 * Ensures none of the specified strings are null or blank.  If
	 * one is null or length 0, this method generates an IllegalArgumentException.
	 * 
	 * @param obs variable number of object references.
	 */
	public static void checkNotBlank(String ...strings) {
		for (String s: strings) {
			if (s == null || s.length() == 0) {
				throw new IllegalArgumentException("cannot be null or blank");
			}
		}
	}
}
