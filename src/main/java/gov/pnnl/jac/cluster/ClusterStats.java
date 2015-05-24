/*
 * ClusterStats.java
 * 
 * JAC: Java Analytic Components
 * 
 * For information contact Randall Scarberry, randall.scarberry@pnl.gov
 * 
 * Notice: This computer software was prepared by Battelle Memorial Institute, 
 * hereinafter the Contractor, under Contract No. DE-AC05-76RL0 1830 with the 
 * Department of Energy (DOE).  All rights in the computer software are 
 * reserved by DOE on behalf of the United States Government and the Contractor
 * as provided in the Contract.  You are authorized to use this computer 
 * software for Governmental purposes but it is not to be released or 
 * distributed to the public.  NEITHER THE GOVERNMENT NOR THE CONTRACTOR MAKES 
 * ANY WARRANTY, EXPRESS OR IMPLIED, OR ASSUMES ANY LIABILITY FOR THE USE OF 
 * THIS SOFTWARE.  This notice including this sentence must appear on any 
 * copies of this computer software.
 */
package gov.pnnl.jac.cluster;

import java.util.*;

import gov.pnnl.jac.collections.*;
import gov.pnnl.jac.geom.CoordinateList;
import gov.pnnl.jac.geom.CoordinateMath;
import gov.pnnl.jac.geom.SimpleCoordinateList;
import gov.pnnl.jac.geom.distance.DistanceFunc;
import gov.pnnl.jac.util.SortUtils;

/**
 * <p>Contains statistical calculation methods relating to clustering.
 * All methods are static: the class is uninstantiable.</p>
 * 
 * @author d3j923
 *
 */
public final class ClusterStats {

	/**
	 * The natural logarithm of 2*Math.PI.
	 */
	public static final double LOG2PI = Math.log(2 * Math.PI);

	// Constructor private to prevent instantiation.
	private ClusterStats() {}
	
	/**
	 * Returns an array containing the members of the cluster, sorted in ascending order by their
	 * distance from the cluster center.
	 * 
	 * @param cs
	 * @param cluster
	 * @param distanceFunc
	 * @return
	 */
	public static int[] membersSortedByDistanceFromCenter(CoordinateList cs, Cluster cluster, DistanceFunc distanceFunc) {
		
		int[] members = cluster.getMembership();
		
		final int memberCount = members != null ? members.length : 0;
		final int coordLen = cs.getDimensionCount();
		
		double[] distances = new double[memberCount];
		
		double[] center = cluster.getCenter();
		double[] buffer = new double[coordLen];
		
		for (int i=0; i<memberCount; i++) {
			cs.getCoordinates(members[i], buffer);
			distances[i] = distanceFunc.distanceBetween(center, buffer);
		}
		
		// This sorts on the distances, but maintains the 1:1 correspondence between the 2 arrays.
		SortUtils.parallelSort(distances, members);
		
		return members;
	}
	
	public static double[][] computeMeanAndVariance(CoordinateList cs, Cluster cluster) {
		int dim = cs.getDimensionCount();
		if (dim != cluster.getDimensions()) {
			throw new IllegalArgumentException("dimension mismatch: " + 
					dim + " != " + cluster.getDimensions());
		}
		double[][] result = new double[dim][2];
		for (int i=0; i<dim; i++) {
			Arrays.fill(result[i], Double.NaN);
		}
		int sz = cluster.getSize();
		if (sz > 0) {
			double[] buffer = new double[dim];
			double[] sums = new double[dim];
			double[] sumSqs = new double[dim];
			for (int i=0; i<sz; i++) {
				cs.getCoordinates(cluster.getMember(i), buffer);
				for (int j=0; j<dim; j++) {
					double v = buffer[j];
					sums[j] += v;
					sumSqs[j] += v*v;
				}
			}
			for (int j=0; j<dim; j++) {
				double mean = sums[j]/sz;
				result[j][0] = mean;
				result[j][1] = (sumSqs[j] - mean * sums[j])/sz;
			}
		}
		return result;
	}
	
	/**
	 * Computes the Bayes Information Criterion (BIC) for a <tt>ClusterList</tt> object.
	 * 
	 * @param cs
	 * @param clusters
	 * 
	 * @return
	 */
	public static double computeBIC(CoordinateList cs, ClusterList clusters) {
		int n = clusters.getClusterCount();
		Cluster[] c = new Cluster[n];
		for (int i = 0; i < n; i++) {
			c[i] = clusters.getCluster(i);
		}
		return computeBIC(cs, c);
	}

        /**
         * Computes the Bayes Information Criterion (BIC) for a single <tt>Cluster</tt> object.
         * 
         * @param cs
         * @param clusters
         * 
         * @return
         */
	public static double computeBIC(CoordinateList cs, Cluster cluster) {
		return computeBIC(cs, new Cluster[] { cluster });
	}

	/**
	 * Computes the Bayes Information Criterion for an array of <tt>Cluster</tt>
	 * instances.
	 * 
	 * @param cs contains the coordinate data for the clusters.
	 * @param clusters an array of <tt>Cluster</tt> instances
	 * 
	 * @return
	 */
	public static double computeBIC(CoordinateList cs, Cluster[] clusters) {

		double bic = 0.0;
		int K = clusters.length;

		if (K > 0) {

			// Get the total number of coordinates in the clusters.
			// Don't assume that it's the same as the number of coordinates
			// in the coordinate set. The cluster set might contain a subset
			// of the coordinates.
			int R = 0;
			for (int i = 0; i < K; i++) {
				R += clusters[i].getSize();
			}

			// Get the dimensionality
			int M = cs.getDimensionCount();

			double LSum = 0;

			// For each cluster
			for (int i = 0; i < K; i++) {
			    
				Cluster cluster = clusters[i];
				int R_n = cluster.getSize();
				
				// If R_n < K, sigma2 will be < 0, which will make L NaN, because of 
				// Math.log(sigma2).
				//
				if (R_n > K) {
				
				    // Estimate variance
				    double sigma2 = computeDistortion(cs, cluster);
				    if (sigma2 > 0) {
				        sigma2 /= (R_n - K);
				    }

				    // Estimate log-likelihood
				    double L = -R_n / 2 * LOG2PI - (R_n * M / 2) * Math.log(sigma2)
						- (R_n - K) / 2 + R_n * Math.log(R_n) - R_n
						* Math.log(R);

				    LSum += L;
				}
			}

			// Count the parameters in the model
			double p = K * (M + 1);
			// Compute the criterion
			bic = LSum - p / 2 * Math.log(R);

			// Added this on 3/13/2006 to normalize on cluster size. I don't
			// think
			// the paper we got the bic formula from does this. -- R.Scarberry
			if (R > 0) {
				bic /= R;
			}
		}

		return bic;
	}

	private static double computeDistortion2(CoordinateList cs,
			Cluster cluster, gov.pnnl.jac.geom.distance.DistanceFunc distFunc) {

		int n = cluster.getSize();
		double[] center = cluster.getCenterDirect();

		if (center.length != cs.getDimensionCount()) {
			throw new IllegalArgumentException("dimension mismatch: "
					+ center.length + " != " + cs.getDimensionCount());
		}
		double sum = 0.0;
		if (n > 0) {
			double[] buffer = new double[center.length];
			for (int i = 0; i < n; i++) {
				cs.getCoordinates(cluster.getMember(i), buffer);
				double d = distFunc.distanceBetween(center, buffer);
				sum += d * d;
			}
			sum /= n;
		}
		return sum;
	}

	private static double computeDistortion(CoordinateList cs, Cluster cluster) {
		return cluster.getSize()
				* CoordinateMath.norm1(computeVariance(cs, cluster));
	}

	private static double computeDistSqFromCenter(CoordinateList cs,
			Cluster cluster) {

		int n = cluster.getSize();
		double[] center = cluster.getCenterDirect();

		double rtn = 0.0;

		if (n > 0) {
			
		    int dim = center.length;
			double[] medians = new double[dim];
			boolean[] medianFlags = new boolean[dim];
			double[] columnBuffer = null;
			
			double[] coordBuf = new double[dim];
			for (int i = 0; i < n; i++) {
				cs.getCoordinates(cluster.getMember(i), coordBuf);
				for (int j = 0; j < dim; j++) {				        
					double d = coordBuf[j] - center[j];
					if (Double.isNaN(d)) {
					    double median = Double.NaN;
					    if (medianFlags[j]) {
					        median = medians[j];
					    } else {
					        columnBuffer = cs.getDimensionValues(dim, columnBuffer);
					        median = medians[j] = CoordinateMath.median(columnBuffer, true);
					        medianFlags[j] = true;
					    }
					    if (Double.isNaN(coordBuf[j])) {
					        coordBuf[j] = median;
					    }
					    if (Double.isNaN(center[j])) {
					        center[j] = median;
					    }
					    d = coordBuf[j] - center[j];
					}
					if (!Double.isNaN(d)) {
					    rtn += d * d;
					}
				}
			}
		}

		return rtn;
	}

	private static double[] computeVariance(CoordinateList cs, Cluster cluster) {

		int n = cluster.getSize();
		double[] center = cluster.getCenterDirect();

		int dim = center.length;
		double[] variance = new double[dim];

		if (n > 0) {
		    
			double[] sum = new double[dim];
			double[] coordBuffer = new double[dim];
			int[] nonNaNCount = new int[dim];
			
			for (int i = 0; i < n; i++) {
				cs.getCoordinates(cluster.getMember(i), coordBuffer);
				for (int j = 0; j < dim; j++) {
					double d = coordBuffer[j];
					if (!Double.isNaN(d)) {
					    sum[j] += d;
					    variance[j] += d * d;
					    nonNaNCount[j]++;
					}
				}
			}
			
			for (int i = 0; i < dim; i++) {
			    if (nonNaNCount[i] > 0) {
			        variance[i] = Math.max(0.0, (variance[i] - center[i] * sum[i]) / nonNaNCount[i]);
			    }
			}
		}
		
		return variance;
	}

	static double computeSigma2(CoordinateList cs, Cluster[] clusters) {
		int K = clusters.length;
		int R = 0;
		double rtn = 0.0;
		for (int i = 0; i < K; i++) {
			Cluster cluster = clusters[i];
			rtn += computeDistSqFromCenter(cs, cluster);
			R += cluster.getSize();
		}
		rtn /= (R - K);
		return rtn;
	}

	private static double computeSigma2(CoordinateList cs, Cluster cluster) {
		return computeSigma2(cs, new Cluster[] { cluster });
	}
}
