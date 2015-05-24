package gov.pnnl.jac.cluster;

import gov.pnnl.jac.geom.distance.BasicDistanceFuncFactory;
import gov.pnnl.jac.geom.distance.BasicDistanceMethod;
import gov.pnnl.jac.geom.distance.DistanceFunc;
import cern.colt.map.HashFunctions;

public class SamplingXMeansClusterTaskParams extends
		XMeansClusterTaskParams {

	private static final long serialVersionUID = 3960749674998970807L;

	private int mMinSamples, mMaxSamples;
	private double mSamplingFraction;
	private long mRandomSeed;

	public SamplingXMeansClusterTaskParams(int numWorkerThreads,
			DistanceFunc distanceFunc, ClusterSeeder seeder, int minClusters,
			int maxClusters,
			boolean useOverallBIC,
			double samplingFraction, int minSamples,
			int maxSamples, long randomSeed) {
		super(numWorkerThreads, distanceFunc, seeder, minClusters, maxClusters, useOverallBIC);
		if (minSamples <= 0) {
			throw new IllegalArgumentException("minSamples < 0: " + minSamples);
		}
		if (samplingFraction <= 0.0 || samplingFraction > 1.0) {
			throw new IllegalArgumentException("invalid sampling fraction: "
					+ samplingFraction);
		}
		mSamplingFraction = samplingFraction;
		mMinSamples = minSamples;
		mMaxSamples = Math.max(minSamples, maxSamples);
		mRandomSeed = randomSeed;
	}

	public SamplingXMeansClusterTaskParams(double samplingFraction,
	        int minSamples, int maxSamples, long randomSeed) {
		this(1, new BasicDistanceFuncFactory(null).getDistanceFunc(BasicDistanceMethod.EUCLIDEAN_NO_NAN), 
		        new RandomSeeder(0L), 1,
		        -1, true, samplingFraction, minSamples, maxSamples, randomSeed);
	}

	public SamplingXMeansClusterTaskParams() {
		this(0.1, 1000, 10000, -1L);
	}

	public double getSamplingFraction() {
		return mSamplingFraction;
	}

	public int getMinSamples() {
		return mMinSamples;
	}

	public int getMaxSamples() {
		return mMaxSamples;
	}
	
	public long getRandomSeed() {
		return mRandomSeed;
	}

	public int hashCode() {
		int hc = super.hashCode();
		hc = 37 * hc + HashFunctions.hash(mSamplingFraction);
		hc = 37 * hc + mMinSamples;
		hc = 37 * hc + mMaxSamples;
		hc = 37 * hc + HashFunctions.hash(mRandomSeed);
		return hc;
	}

	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (super.equals(o)) {
			SamplingXMeansClusterTaskParams other = (SamplingXMeansClusterTaskParams) o;
			return Double.doubleToLongBits(other.mSamplingFraction) == Double
					.doubleToLongBits(this.mSamplingFraction)
					&& other.mMinSamples == this.mMinSamples
					&& other.mMaxSamples == this.mMaxSamples 
					&& other.mRandomSeed == this.mRandomSeed;
		}
		return false;
	}
}
