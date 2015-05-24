package gov.pnnl.jac.geom;

import gov.pnnl.jac.util.ComparableList;
import gov.pnnl.jac.util.SortUtils;

import java.util.*;

/**
 * <p><tt>CoordinateMath</tt> contains static utility
 * methods for common calculations on coordinates.
 * </p>
 * 
 * @author R. Scarberry, Grant Nakamura (for the nice code that R. Scarberry
 *         lifted from the IN-SPIRE codebase.)
 * 
 * @version 1.0
 */
public final class CoordinateMath {

	/** Square root of (2 * pi). */
	static public final double SQRT2PI = Math.sqrt(2 * Math.PI);

	private CoordinateMath() {
	}
	
	/**
	 * Computes the mean of values contained in an
	 * array.  Any NaNs contained in the array are left out of the 
	 * calculation.
	 * 
	 * @param values - the values for which to compute the mean.
	 * 
	 * @return the mean, if at least one non-NaN is contained in the array,
	 *   Double.NaN otherwise.
	 */
	public static double mean(double[] values) {
		double mean = Double.NaN;
		int len = values.length;
		if (len > 0) {
			double sum = 0.0;
			int n = 0;
			for (int i=0; i<len; i++) {
				double v = values[i];
				if (!Double.isNaN(v)) {
					sum += v;
					n++;
				}
			}
			if (n > 0) {
				mean = sum/n;
			}
		}
		return mean;
	}
	
	/**
	 * Computes the mean and sample variance of a distribution of values.
	 * 
	 * (Grant Nakamura originally wrote this code for the DistributionUtils
	 * class in the IN-SPIRE codebase. R. Scarberry adapted it for JAC.)
	 * 
	 * @param values
	 *            the values for which to compute the statistics.
	 * 
	 * @return double array of length 2 with mean the 0th element and variance
	 *         the 1st. (These will be NaN if the values are of length 0.)
	 */
	public static double[] meanAndVariance(double[] values) {
		double mean = Double.NaN, variance = Double.NaN;
		int n = values.length;
		if (n > 0) {
			double sumX = 0;
			double sumX2 = 0;
			int nonNaN = 0;
			for (int i = 0; i < n; i++) {
				double v = values[i];
				if (!Double.isNaN(v)) {
				    sumX += v;
				    sumX2 += v * v;
				    nonNaN++;
				}
			}
			if (nonNaN > 0) {
			    mean = sumX / nonNaN;
			    variance = (sumX2 - mean * sumX) / nonNaN;
			}
		}
		return new double[] { mean, variance };
	}

	/**
	 * Computes the probability density function for a standard (mean = 0,
	 * variance = 1) normal distribution.
	 * 
	 * (Grant Nakamura originally wrote this code for the DistributionUtils
	 * class in the IN-SPIRE codebase. R. Scarberry adapted it for JAC.)
	 */
	static public double normalPdf(double x) {
		double pdf = Math.exp(-x * x / 2) / SQRT2PI;
		return pdf;
	}

	/**
	 * Computes the cumulative distribution function for a standard (mean = 0,
	 * variance = 1) normal distribution. This uses a numerical approximation
	 * with absolute error < 7.5e-8, (eq. 26.2.17) from the Handbook of
	 * Mathematical Functions, Abramowitz & Stegun, 10th printing.
	 * 
	 * (Grant Nakamura originally wrote this code for the DistributionUtils
	 * class in the IN-SPIRE codebase. R. Scarberry adapted it for JAC.)
	 */
	public static double normalCdf(double x) {
		final double p = 0.2316419;
		final double[] b = { 0.319381530, -0.356563782, 1.781477937,
				-1.821255978, 1.330274429 };

		double xAbs = Math.abs(x);
		double t = 1 / (1 + p * xAbs);

		double sum = 0;
		for (int i = b.length - 1; i >= 0; i--)
			sum = (sum + b[i]) * t;

		double pdf = normalPdf(xAbs);
		double cdf = pdf * sum;
		if (x > 0)
			cdf = 1 - cdf;

		return cdf;
	}

	/**
	 * Tests a one-dimensional distribution to see if it may be Gaussian, based
	 * on an Anderson-Darling test.
	 * 
	 * (Grant Nakamura originally wrote this code for the DistributionUtils
	 * class in the IN-SPIRE codebase. R. Scarberry adapted it for JAC.)
	 */
	public static boolean andersonDarlingGaussianTest(double[] values) {

		boolean result = false;

		int n = values.length;

		if (n > 0) {

			// Estimate mean and variance of the distribution
			double[] distrib = meanAndVariance(values);
			double mean = distrib[0];
			double variance = distrib[1];

			variance *= n / (n - 1.0); // Bessel's correction

			double stdDev = Math.sqrt(variance);

			double[] z = new double[n];
			for (int i = 0; i < n; i++) {
				// Normalize to a standard normal (mean=0, variance=1)
				double xNorm = (values[i] - mean) / stdDev;

				// Find the cumulative distribution
				z[i] = normalCdf(xNorm);
			}

			Arrays.sort(z);
			double sum = 0;
			for (int i = 0; i < n; i++) {
				sum += (2 * i + 1)
						* (Math.log(z[i]) + Math.log(1 - z[(n - 1) - i]));
			}
			double andersonDarling = sum / (-n) - n;
			andersonDarling *= (1 + 0.75 / n + 2.25 / (n * n));

			// Use critical value corresponding to significance level of 0.0001.
			// This will be conservative about rejecting the null hypothesis
			// that
			// the distribution is Gaussian.
			result = (andersonDarling <= 1.8692);
		}

		return result;
	}

	/**
	 * Computes the median from a number of values contained in an
	 * array.  NaNs are not included in the calculation.
	 * 
	 * @param values - the values for which to compute the median.
	 * 
	 * @return the median value.
	 */
	public static double median(double[] values) {
		return median(values, false);
	}
	
	/**
	 * Compute the median from a number of values contained
	 * in an array.  If NaNs are present in the array, they
	 * are not included in the calculation.
	 * @param values - an array containing the values.
	 * @param canRearrangeValues - if true, the previous array is rearranged
	 *   by sorting.  If false, the array of values is not altered, but
	 *   the method is slightly less efficient.
	 * @return - the median value, or NaN if the array is null,
	 *   of length 0, or contains no non-NaN elements.
	 */
	public static double median(double[] values, boolean canRearrangeValues) {
		double median = Double.NaN;
		if (values != null) {
			double[] copy = null;
			if (canRearrangeValues) {
				copy = values; // Don't really copy, since the values can be rearranged.
			} else {
				copy = new double[values.length];
				System.arraycopy(values, 0, copy, 0, values.length);
			}
			// Sort the values.  If there are any NaNs, they'll end up
			// at the end of the array.
			Arrays.sort(copy);
			int n = copy.length - 1;
			while(n >= 0 && Double.isNaN(copy[n])) {
				n--;
			}
			// Increment n once, and it becomes the number of
			// non-NaN values.
			n++;
			if (n > 0) {
				int mid = n/2;
				median = (n%2 == 0) ? (copy[mid-1] + copy[mid])/2.0 : copy[mid];
			}
		}
		return median;
	}

	/**
	 * Computes the L1-Norm of an array of double values.
	 * This is simply a sum of the absolutes values of the
	 * array elements.
	 * 
	 * @param coordBuffer - array containing the values.
	 * 
	 * @return - the norm
	 */
	public static double norm1(double[] coordBuffer) {
		double sum = 0.0;
		int n = coordBuffer.length;
		int nanCount = 0;
		for (int i = 0; i < n; i++) {
		    double d = coordBuffer[i];
		    if (Double.isNaN(d)) {
		        nanCount++;
		    } else {
			sum += Math.abs(d);
		    }
		}
		if (nanCount > 0 && nanCount < n) {
		    sum *= n/(n - nanCount);
		}
		return sum;
	}

	/**
	 * Computes the L2-Norm of an array of double values.
	 * This is simply the square root of the sum of the
	 * squares.  If coordBuffer contains a vector, the L2-Norm
	 * is the length of the vector in euclidean space.
	 * 
	 * @param coordBuffer - array containing the values.
	 * 
	 * @return - the norm
	 */
	public static double norm2(double[] coordBuffer) {
		double sum = 0.0;
		int n = coordBuffer.length;
		int nanCount = 0;
		for (int i = 0; i < n; i++) {
			double d = coordBuffer[i];
			if (Double.isNaN(d)) {
			    nanCount++;
			} else {
			    sum += d * d;
			}
		}
		if (nanCount > 0 && nanCount < n) {
		    sum *= n/(n - nanCount);
		}
		return Math.sqrt(sum);
	}

	/**
	 * Returns the dot product of the two arrays of double values.
	 * 
	 * @param buf1
	 * @param buf2
	 * 
	 * @return the dot product.
	 * 
	 * @throws IllegalArgumentException - if both arrays do not
	 *   have the same length.
	 */
	public static double dotProduct(double[] buf1, double[] buf2) {
		checkSameLength(buf1, buf2);
		int n = buf1.length;
		double sum = 0.0;
		int nanCount = 0;
		for (int i = 0; i < n; i++) {
		    double d = buf1[i] * buf2[i];
		    if (Double.isNaN(d)) {
		        nanCount++;
		    } else {
			sum += d;
		    }
		}
		if (nanCount > 0 && nanCount < n) {
		    sum *= n/(n-nanCount);
		}
		return sum;
	}

	/**
	 * Subtracts a value from each element in an array of doubles.
	 * 
	 * @param coordBuffer - the array of doubles.
	 * @param value - the value to subtract from each element.
	 */
	public static void subtract(double[] coordBuffer, double value) {
		int n = coordBuffer.length;
		for (int i = 0; i < n; i++) {
			coordBuffer[i] -= value;
		}
	}

	/**
	 * Adds a value to each element in an array of doubles.
	 * 
	 * @param coordBuffer - the array of doubles.
	 * @param value - the value to add to each element.
	 */
	public static void add(double[] coordBuffer, double value) {
		int n = coordBuffer.length;
		for (int i = 0; i < n; i++) {
			coordBuffer[i] += value;
		}
	}

	/**
	 * Multiplies each element in an array of doubles by a value.
	 * 
	 * @param coordBuffer - the array of doubles.
	 * @param value - the value by which each element is multiplied.
	 */
	public static void multiply(double[] coordBuffer, double value) {
		int n = coordBuffer.length;
		for (int i = 0; i < n; i++) {
			coordBuffer[i] *= value;
		}
	}

	/**
	 * Divides each element in an array of doubles by a value.
	 * 
	 * @param coordBuffer - the array of doubles.
	 * @param value - the value by which each element is divided.
	 * 
	 * @throws IllegalArgumentException - if value equals 0.
	 */
	public static void divide(double[] coordBuffer, double value) {
		if (value == 0) {
			throw new IllegalArgumentException("divide by zero");
		}
		int n = coordBuffer.length;
		for (int i = 0; i < n; i++) {
			coordBuffer[i] /= value;
		}
	}

	/**
	 * Computes the maximum element from and array of double values.
	 * Only non-NaN elements are considered.
	 * 
	 * @param buf - array containing the elements.
	 * 
	 * @return - the maximum value.
	 */
	public static double max(double[] buf) {
		double m = Double.NaN;
		int n = buf.length;
		int i = 0;
		for (; i < n; i++) {
			double d = buf[i];
			if (!Double.isNaN(d)) {
				m = d;
				break;
			}
		}
		i++;
		for (; i < n; i++) {
			double d = buf[i];
			if (!Double.isNaN(d) && d > m) {
				m = d;
			}
		}
		return m;
	}

	/**
	 * Computes the minimum element from and array of double values.
	 * Only non-NaN elements are considered.
	 * 
	 * @param buf - array containing the elements.
	 * 
	 * @return - the minimum value.
	 */
	public static double min(double[] buf) {
		double m = Double.NaN;
		int n = buf.length;
		int i = 0;
		for (; i < n; i++) {
			double d = buf[i];
			if (!Double.isNaN(d)) {
				m = d;
				break;
			}
		}
		i++;
		for (; i < n; i++) {
			double d = buf[i];
			if (!Double.isNaN(d) && d < m) {
				m = d;
			}
		}
		return m;
	}

	/**
	 * Computes the maximum of the absolute values
	 * of elements in an array of doubles.
	 * Only non-NaN elements are considered.
	 * 
	 * @param buf - array containing the elements.
	 * 
	 * @return - the maximum absolute value.
	 */
	public static double absMax(double[] buf) {
		double m = Double.NaN;
		int n = buf.length;
		int i = 0;
		for (; i < n; i++) {
			double d = buf[i];
			if (!Double.isNaN(d)) {
				m = Math.abs(d);
				break;
			}
		}
		i++;
		for (; i < n; i++) {
			double d = buf[i];
			if (!Double.isNaN(d)) {
				d = Math.abs(d);
				if (d > m) {
					m = d;
				}
			}
		}
		return m;
	}

	/**
	 * Computes the minimum of the absolute values
	 * of elements in an array of doubles.
	 * Only non-NaN elements are considered.
	 * 
	 * @param buf - array containing the elements.
	 * 
	 * @return - the minimum absolute value.
	 */
	public static double absMin(double[] buf) {
		double m = Double.NaN;
		int n = buf.length;
		int i = 0;
		for (; i < n; i++) {
			double d = buf[i];
			if (!Double.isNaN(d)) {
				m = Math.abs(d);
				break;
			}
		}
		i++;
		for (; i < n; i++) {
			double d = buf[i];
			if (!Double.isNaN(d)) {
				d = Math.abs(d);
				if (d < m) {
					m = d;
				}
			}
		}
		return m;
	}

	/**
	 * Computes the Pearson Correlation Coefficient between the coordinates
	 * stored in buf1 and buf2 using the population covarience.
	 * Both buffers should be non-null and have the same length.
	 * 
	 * @param buf1 -
	 *            buffer containing the first coordinate.
	 * @param buf2 -
	 *            buffer containing the second coordinate.

	 * @return - a value in the range [-1.0 - +1.0] or NaN. NaN is returned
	 *         whenever buf1 or buf2 contain nothing but NaNs or zeros. NaN 
	 *         is also returned if either buf1 or buf2 contain
	 *         values with zero variance.
	 * 
	 * @exception NullPointerException -
	 *                if either buf1 or buf2 are null.
	 * @exception IllegalArgumentException -
	 *                if buf1 and buf2 are not the same length.
	 */
	public static double correlation(double[] buf1, double[] buf2) {
		return correlation(buf1, buf2, true);
	}

	/**
	 * Computes the Pearson Correlation Coefficient between the coordinates
	 * stored in buf1 and buf2. Both buffers should be non-null and have the
	 * same length.
	 * 
	 * @param buf1 -
	 *            buffer containing the first coordinate.
	 * @param buf2 -
	 *            buffer containing the second coordinate.
	 * @param useCovar -
	 *            true to use the population covariance.
	 * @return - a value in the range [-1.0 - +1.0] or NaN. NaN is returned
	 *         whenever buf1 or buf2 contain nothing but NaNs or zeros. If
	 *         useCovar is true, NaN is returned if either buf1 or buf2 contain
	 *         values with zero variance.
	 * 
	 * @exception NullPointerException -
	 *                if either buf1 or buf2 are null.
	 * @exception IllegalArgumentException -
	 *                if buf1 and buf2 are not the same length.
	 */
	public static double correlation(double[] buf1, double[] buf2,
			boolean useCovar) {

		checkSameLength(buf1, buf2);

		double corr = Double.NaN;

		double absMax = Math.max(absMax(buf1), absMax(buf2));

		// If absMax is NaN, then all elements of at least one
		// of the arrays are NaN.
		// If absMax == 0.0, then all non-NaN elements of both
		// arrays are 0.0. In either case, there's no point
		// in executing the for-loop.
		if (!Double.isNaN(absMax) && absMax != 0.0) {

			int n = buf1.length;

			int dim = 0;

			if (useCovar) {

				double sum1 = 0.0, sum2 = 0.0;
				double sumSq1 = 0.0, sumSq2 = 0.0, sum12 = 0.0;

				for (int i = 0; i < n; i++) {
					double d1 = buf1[i];
					double d2 = buf2[i];
					if (!Double.isNaN(d1) && !Double.isNaN(d2)) {
						dim++;
						d1 /= absMax;
						d2 /= absMax;
						sum1 += d1;
						sumSq1 += d1 * d1;
						sum2 += d2;
						sumSq2 += d2 * d2;
						sum12 += d1 * d2;
					}
				}

				if (dim > 0) {
					corr = (dim * sum12 - sum1 * sum2)
							/ Math.sqrt((dim * sumSq1 - sum1 * sum1)
									* (dim * sumSq2 - sum2 * sum2));
				}

			} else { // useCover == false

				// Same as for-loop above, but without sum1, sum2.
				double sumSq1 = 0.0, sumSq2 = 0.0, sum12 = 0.0;

				for (int i = 0; i < n; i++) {
					double d1 = buf1[i];
					double d2 = buf2[i];
					if (!Double.isNaN(d1) && !Double.isNaN(d2)) {
						dim++;
						d1 /= absMax;
						d2 /= absMax;
						sumSq1 += d1 * d1;
						sumSq2 += d2 * d2;
						sum12 += d1 * d2;
					}
				}

				if (dim > 0) {
					corr = sum12 / Math.sqrt(sumSq1 * sumSq2);
				}
			}
		}

		return corr;
	}

	// Checks that 2 arrays of doubles are the same length.
	private static final void checkSameLength(double[] buf1, double[] buf2) {
		int n = buf1.length;
		if (n != buf2.length) {
			throw new IllegalArgumentException("length mismatch: " + n + " != "
					+ buf2.length);
		}
	}
	
	/**
	 * Determines the number of unique coordinates in the given 
	 * <tt>CoordinateList</tt>
	 * 
	 * @param coords
	 * 
	 * @return the number of unique coordinates (vectors) in the
	 *   coordinate list.
	 */
	public static int numberOfUniqueCoordinates(final CoordinateList coords) {
		
		final int coordCount = coords.getCoordinateCount();
		if (coordCount < 2) {
			return coordCount;
		}
		
		final int dim = coords.getDimensionCount();
		
		final double[] buffer1 = new double[dim];
		final double[] buffer2 = new double[dim];
		
		ComparableList compList = new ComparableList() {
			
			public int memberCount() {
				return coords.getCoordinateCount();
			}

			public int compareMembers(int i, int j) {
				if (i == j) return 0;
				coords.getCoordinates(i, buffer1);
				coords.getCoordinates(j, buffer2);
				for (int k=0; k<dim; k++) {
					double v1 = buffer1[k];
					double v2 = buffer2[k];
					if (!Double.isNaN(v1) && !Double.isNaN(v2)) {
						if (v1 < v2) return -1;
						if (v1 > v2) return +1;
					} else {
						if (Double.isNaN(v1) && !Double.isNaN(v2)) return +1;
						if (Double.isNaN(v2) && !Double.isNaN(v1)) return -1;
					}
				}
				return 0;
			}
			
		};
		
		int[] indices = SortUtils.sortIndices(compList);
		
		int uniqueCount = 1;
		for (int i=1; i<coordCount; i++) {
			if (compList.compareMembers(indices[i-1], indices[i]) != 0) {
				uniqueCount++;
			}
		}
		
		return uniqueCount;
	}
	
	/**
	 * Works the same as <code>numberOfUniqueCoordinates(coords)</code>,
	 * but stops counting as soon as
	 * it verifies at least <tt>minNeeded</tt> are unique.
	 * 	 
	 * @param coords
	 * 
	 * @return a number less than or equal to minNeeded.  If less than
	 *   minNeeded, it is the actual number of unique coordinates.
	 */
	public static int checkNumberOfUniqueCoordinates(final CoordinateList coords, 
			final int minNeeded) {
		
		final int coordCount = coords.getCoordinateCount();
		if (coordCount < 2) {
			return coordCount;
		}
		
		final int dim = coords.getDimensionCount();
		
		final double[] buffer1 = new double[dim];
		final double[] buffer2 = new double[dim];
		
		ComparableList compList = new ComparableList() {
			
			public int memberCount() {
				return coords.getCoordinateCount();
			}

			public int compareMembers(int i, int j) {
				if (i == j) return 0;
				coords.getCoordinates(i, buffer1);
				coords.getCoordinates(j, buffer2);
				for (int k=0; k<dim; k++) {
					double v1 = buffer1[k];
					double v2 = buffer2[k];
					if (!Double.isNaN(v1) && !Double.isNaN(v2)) {
						if (v1 < v2) return -1;
						if (v1 > v2) return +1;
					} else {
						if (Double.isNaN(v1) && !Double.isNaN(v2)) return +1;
						if (Double.isNaN(v2) && !Double.isNaN(v1)) return -1;
					}
				}
				return 0;
			}
			
		};
		
		int[] indices = SortUtils.sortIndices(compList);
		
		int uniqueCount = 1;
		for (int i=1; i<coordCount; i++) {
			if (compList.compareMembers(indices[i-1], indices[i]) != 0) {
				uniqueCount++;
				if (uniqueCount == minNeeded) {
					return uniqueCount;
				}
			}
		}
		
		return uniqueCount;
	}

}
