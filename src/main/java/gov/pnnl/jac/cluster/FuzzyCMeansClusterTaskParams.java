package gov.pnnl.jac.cluster;

import cern.colt.map.HashFunctions;
import gov.pnnl.jac.geom.distance.DistanceFunc;
import gov.pnnl.jac.geom.distance.EuclideanNoNaN;
import gov.pnnl.jac.util.ExceptionUtil;

public class FuzzyCMeansClusterTaskParams implements ClusterTaskParams {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8469596387839348998L;
	
	public static final double DEFAULT_FUZZINESS = 2.0;
	public static final double DEFAULT_EPSILON = 1.0;
	
    // Desired number of clusters.
    private int mNumClusters;
    // The fuzziness used.
    private double mFuzziness = DEFAULT_FUZZINESS;
    
    private double mEpsilon = DEFAULT_EPSILON;
    
    // Maximum number of iterations before quitting.
    private int mMaxIterations = Integer.MAX_VALUE;
    // The number of worker threads to use for finding nearest clusters, 
    // recomputing centers, and any other concurrently-processed tasks.
    // If -1, then select based on the number of processors.
    private int mNumWorkerThreads = -1;
    // The distance function.
    private DistanceFunc mDistanceFunc;
    // The cluster seeder.
    private ClusterSeeder mSeeder;
    
    private long mRandomSeed;
    
    public FuzzyCMeansClusterTaskParams(
    		int numClusters, 
    		int maxIterations,
    		double fuzziness, 
    		double epsilon, 
    		DistanceFunc distanceFunc,
    		ClusterSeeder seeder,
    		int numThreads,
    		long randomSeed) {
    	ExceptionUtil.checkInBounds(fuzziness, 2.0, Double.MAX_VALUE);
    	ExceptionUtil.checkNonNegative(epsilon);
    	ExceptionUtil.checkNotNull(distanceFunc, seeder);
    	mNumClusters = numClusters;
    	if (maxIterations > 0) {
    		mMaxIterations = maxIterations;
    	}
    	mFuzziness = fuzziness;
    	mEpsilon = epsilon;
    	mDistanceFunc = distanceFunc;
    	mSeeder = seeder;
    	if (numThreads > 0) {
    		mNumWorkerThreads = numThreads;
    	}
    	mRandomSeed = randomSeed;
    }
    
    public FuzzyCMeansClusterTaskParams(int numClusters) {
    	this(numClusters, Integer.MAX_VALUE, DEFAULT_FUZZINESS, DEFAULT_EPSILON,
    			new EuclideanNoNaN(), new KMeansPlusPlusSeeder(
    					-1L, new EuclideanNoNaN()), -1, -1L); 			
    }
    
    public FuzzyCMeansClusterTaskParams() {
    	this(0, Integer.MAX_VALUE, DEFAULT_FUZZINESS, DEFAULT_EPSILON,
    			new EuclideanNoNaN(), new KMeansPlusPlusSeeder(
    					-1L, new EuclideanNoNaN()), -1, -1L);
    }
    
    public final int getNumClusters() {
        return mNumClusters;
    }
    
    public void setNumClusters(int numClusters) {
		if (numClusters <= 0) {
			throw new IllegalArgumentException("cluster count must be greater than 0");
		}
		mNumClusters = numClusters;
    }

    public final int getMaxIterations() {
        return mMaxIterations;
    }
    
    public final void setMaxIterations(int maxIterations) {
    	mMaxIterations = maxIterations > 0 ? maxIterations : Integer.MAX_VALUE;
    }

    public final double getFuzziness() {
        return mFuzziness;
    }
    
    public final void setFuzziness(double fuzziness) {
    	ExceptionUtil.checkInBounds(fuzziness, 1.0, Double.MAX_VALUE);
    	mFuzziness = fuzziness;
    }
    
    public final double getEpsilon() {
    	return mEpsilon;
    }
    
    public final void setEpsilon(double epsilon) {
    	ExceptionUtil.checkNonNegative(epsilon);
    	mEpsilon = epsilon;
    }
 
    public final DistanceFunc getDistanceFunc() {
        return mDistanceFunc;
    }
    
    public final void setDistanceFunc(DistanceFunc distanceFunc) {
    	ExceptionUtil.checkNotNull(distanceFunc);
    	mDistanceFunc = distanceFunc;
    }

    public final ClusterSeeder getClusterSeeder() {
        return mSeeder;
    }
    
    public final void setClusterSeeder(ClusterSeeder seeder) {
    	ExceptionUtil.checkNotNull(seeder);
    	mSeeder = seeder;
    }

    public final int getNumWorkerThreads() {
        return mNumWorkerThreads;
    }
    
    public final void setNumWorkerThreads(int numThreads) {
    	if (numThreads <= 0) numThreads = -1;
    	mNumWorkerThreads = numThreads;
    }
    
    public final long getRandomSeed() {
    	return mRandomSeed;
    }
    
    public final void setRandomSeed(long randomSeed) {
    	mRandomSeed = randomSeed;
    }

    public int hashCode() {
    	int hc = mNumClusters;
    	hc = 31*hc + HashFunctions.hash(mFuzziness);
    	hc = 31*hc + HashFunctions.hash(mEpsilon);
    	hc = 31*hc + mMaxIterations;
    	hc = 31*hc + mNumWorkerThreads;
    	hc = 31*hc + mDistanceFunc.hashCode();
    	hc = 31*hc + mSeeder.hashCode();
    	hc = 38*hc + HashFunctions.hash(mRandomSeed);
    	return hc;
    }
    
    public boolean equals(Object o) {
    	if (o == this) return true;
    	if (o instanceof FuzzyCMeansClusterTaskParams) {
    		FuzzyCMeansClusterTaskParams that = (FuzzyCMeansClusterTaskParams) o;
    		return this.mNumClusters == that.mNumClusters &&
    				this.mMaxIterations == that.mMaxIterations &&
    				Double.doubleToLongBits(this.mFuzziness) == Double.doubleToLongBits(that.mFuzziness) &&
    				Double.doubleToLongBits(this.mEpsilon) == Double.doubleToLongBits(that.mEpsilon) &&
    				this.mRandomSeed == that.mRandomSeed &&
    				this.mNumWorkerThreads == that.mNumWorkerThreads &&
    				this.mDistanceFunc.equals(that.mDistanceFunc) &&
    				this.mSeeder.equals(that.mSeeder);
    	}
    	return false;
    }

    public static class Builder {
    	
    	private FuzzyCMeansClusterTaskParams mParams;
    	
    	public Builder(int numClusters) {
    		mParams = new FuzzyCMeansClusterTaskParams(
    				numClusters, 
    				Integer.MAX_VALUE, 
    				DEFAULT_FUZZINESS, 
    				DEFAULT_EPSILON,
        			new EuclideanNoNaN(), new KMeansPlusPlusSeeder(
        					-1L, new EuclideanNoNaN()), -1, -1L);
    	}
    	
    	public Builder numClusters(int numClusters) {
    		mParams.setNumClusters(numClusters);
    		return this;
    	}
    	
    	public Builder fuzziness(double fuzziness) {
    		mParams.setFuzziness(fuzziness);
    		return this;
    	}
    	
    	public Builder maxIterations(int maxIterations) {
    		mParams.setMaxIterations(maxIterations);
    		return this;
    	}
    	
    	public Builder epsilon(double epsilon) {
    		mParams.setEpsilon(epsilon);
    		return this;
    	}
    	
    	public Builder distanceFunc(DistanceFunc distanceFunc) {
    		mParams.setDistanceFunc(distanceFunc);
    		return this;
    	}
    	
    	public Builder clusterSeeder(ClusterSeeder seeder) {
    		mParams.setClusterSeeder(seeder);
    		return this;
    	}
    	
    	public Builder randomSeed(long seed) {
    		mParams.setRandomSeed(seed);
    		return this;
    	}
    	
    	public Builder numWorkerThreads(int numThreads) {
    		mParams.setNumWorkerThreads(numThreads);
    		return this;
    	}

    	public FuzzyCMeansClusterTaskParams build() {
    		return mParams;
    	}
    }
}
