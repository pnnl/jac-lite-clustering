/*
 * HierarchicalClusterTaskParams.java
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

import gov.pnnl.jac.geom.distance.BasicDistanceMethod;
import gov.pnnl.jac.geom.distance.DistanceFunc;
import gov.pnnl.jac.util.ExceptionUtil;

/**
 * <p>Implementation of <tt>ClusterTaskParams</tt> to encapsulate the 
 * parameters for clustering algorithms that extend <tt>AbstractHierarchicalClusterTask</tt>.</p>
 * 
 * @author d3j923
 *
 */
public class HierarchicalClusterTaskParams implements ClusterTaskParams {

    private static final long serialVersionUID = -8868186479819761447L;

    /**
     * Kinds of linkage types, which determine how distances from one
     * hierarchical node to another are calculated.
     *
     * COMPLETE -- also known as max-pairwise.  Computed as the max
     *             distance between a coordinate in one node and a coordinate
     *             in the other node.
     * SINGLE   -- also known as min-pairwise. Computed as the min
     *             distance between a coordinate in one node and a coordinate
     *             in the other node.
     * MEAN     -- the distance between the node centers.
     *
     */
    public enum Linkage {
        COMPLETE, SINGLE, MEAN
    };

    /**
     * Defines whether generating clusters based on the number desired, or the min coherence desired.
     */
    public enum Criterion {
        CLUSTERS, COHERENCE
    };

    private DistanceFunc mDistanceFunc = BasicDistanceMethod.EUCLIDEAN_NO_NAN
            .newFunction();

    private Linkage mLinkage = Linkage.COMPLETE;

    private Criterion mCriterion = Criterion.CLUSTERS;

    // Only one of these is relevant, depending on the value of mCriterion. 
    private int mClustersDesired;

    private double mCoherenceDesired = 0.8;
    // These are used in computing the coherences if selecting clusters based on 
    // mCoherenceDesired.
    private double mMinCoherenceThreshold = 0.0;
    // Being NaN means that the max decision distance (min similarity) is used to compute the coherence.
    private double mMaxCoherenceThreshold = Double.NaN;

    // Whether or not to optimize the structure of newly created dendrograms
    // to minimize distances between adjacent leaf nodes.  This makes a prettier 
    // plot if the dendrogram is shown in a UI.
    private boolean mMinimizeInterleafDistances;

    // The number of worker threads to use for performing time-consuming concurrent tasks.
    // If -1, then select based on the number of processors.
    private int mNumWorkerThreads = Runtime.getRuntime().availableProcessors();
    
    // Random generator seed for variants of hierarchical that use it.
    private long mSeed = -1L;

    public HierarchicalClusterTaskParams(int clustersDesired, boolean optimize,
            Linkage linkage, DistanceFunc distanceFunc, int numWorkerThreads, long seed) {
        ExceptionUtil.checkPositive(clustersDesired);
        ExceptionUtil.checkNotNull(linkage, distanceFunc);
        mCriterion = Criterion.CLUSTERS;
        mClustersDesired = clustersDesired;
        mMinimizeInterleafDistances = optimize;
        mLinkage = linkage;
        mDistanceFunc = distanceFunc;
        if (numWorkerThreads > 0) {
            mNumWorkerThreads = numWorkerThreads;
        }
        mSeed = seed;
    }

    public HierarchicalClusterTaskParams(int clustersDesired, boolean optimize,
            Linkage linkage, DistanceFunc distanceFunc, int numWorkerThreads) {
    	this(clustersDesired, optimize, linkage, distanceFunc, numWorkerThreads, 1234L);
    }
    
    public HierarchicalClusterTaskParams(int clustersDesired, boolean optimize,
            Linkage linkage, DistanceFunc distanceFunc) {
        this(clustersDesired, optimize, linkage, distanceFunc, -1);
    }

    public HierarchicalClusterTaskParams(int clustersDesired, boolean optimize) {
        this(clustersDesired, optimize, Linkage.COMPLETE,
                BasicDistanceMethod.EUCLIDEAN_NO_NAN.newFunction(), -1);
    }

    public HierarchicalClusterTaskParams(double coherenceDesired,
            boolean optimize, Linkage linkage, DistanceFunc distanceFunc,
            int numWorkerThreads) {
    	this(coherenceDesired, 0.0, Double.NaN, optimize, linkage, distanceFunc, numWorkerThreads,
    			System.currentTimeMillis());
    }

    public HierarchicalClusterTaskParams(double coherenceDesired,
    		double minCoherenceThreshold, double maxCoherenceThreshold,
    		boolean optimize, Linkage linkage, DistanceFunc distanceFunc,
            int numWorkerThreads, long seed) {
    	ExceptionUtil.checkNotNull(linkage, distanceFunc);
        mCriterion = Criterion.COHERENCE;
        ExceptionUtil.checkInBounds(coherenceDesired, 0.0, 1.0);
        mCoherenceDesired = coherenceDesired;
        mMinCoherenceThreshold = minCoherenceThreshold;
        mMaxCoherenceThreshold = maxCoherenceThreshold;
        mMinimizeInterleafDistances = optimize;
        mLinkage = linkage;
        mDistanceFunc = distanceFunc;
        if (numWorkerThreads > 0) {
            mNumWorkerThreads = numWorkerThreads;
        }
        mSeed = seed;
    }

    public HierarchicalClusterTaskParams(double coherenceDesired,
            boolean optimize, Linkage linkage, DistanceFunc distanceFunc) {
        this(coherenceDesired, optimize, linkage, distanceFunc, -1);
    }

    public HierarchicalClusterTaskParams(double coherenceDesired,
            boolean optimize) {
        this(coherenceDesired, optimize, Linkage.COMPLETE,
                BasicDistanceMethod.EUCLIDEAN_NO_NAN.newFunction(), -1);
    }
    
    public HierarchicalClusterTaskParams() {  	
    }
    
    public static Criterion criterionFor(String criterionName) {
    	return Criterion.valueOf(criterionName.toUpperCase());
    }
    
    public static Linkage linkageFor(String linkageName) {
    	return Linkage.valueOf(linkageName.toUpperCase());
    }

    public int hashCode() {
        int hc = mDistanceFunc.hashCode();
        hc = 37 * hc + mLinkage.hashCode();
        hc = 37 * hc + mCriterion.hashCode();
        long bits = Double.doubleToLongBits(mCoherenceDesired);
        hc = 37 * hc + (int) (bits ^ (bits >>> 32));
        bits = Double.doubleToLongBits(mMinCoherenceThreshold);
        hc = 37 * hc + (int) (bits ^ (bits >>> 32));
        bits = Double.doubleToLongBits(mMaxCoherenceThreshold);
        hc = 37 * hc + (int) (bits ^ (bits >>> 32));
        hc = 37 * hc + (mMinimizeInterleafDistances ? 1231 : 1237);
        hc = 37 * hc + mNumWorkerThreads;
        hc = 37 * hc + (int) (mSeed ^ (mSeed >>> 32));
        return hc;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof HierarchicalClusterTaskParams) {
            HierarchicalClusterTaskParams other = (HierarchicalClusterTaskParams) o;
            return this.mDistanceFunc.equals(other.mDistanceFunc)
                    && this.mLinkage == other.mLinkage
                    && this.mCriterion == other.mCriterion
                    && this.mClustersDesired == other.mClustersDesired
                    && Double.doubleToLongBits(this.mCoherenceDesired) == Double
                            .doubleToLongBits(other.mCoherenceDesired)
                    && Double.doubleToLongBits(this.mMinCoherenceThreshold) == Double
                            .doubleToLongBits(other.mMinCoherenceThreshold)
                    && Double.doubleToLongBits(this.mMaxCoherenceThreshold) == Double
                            .doubleToLongBits(other.mMaxCoherenceThreshold)
                    && this.mMinimizeInterleafDistances == other.mMinimizeInterleafDistances
                    && this.mNumWorkerThreads == other.mNumWorkerThreads
                    && this.mSeed == other.mSeed;
        }
        return false;
    }

    public final int getClustersDesired() {
        return mClustersDesired;
    }
    
    public void setClustersDesired(int clustersDesired) {
		if (clustersDesired <= 0) {
			throw new IllegalArgumentException("cluster count must be greater than 0");
		}
    	mClustersDesired = clustersDesired;
    }

    public final double getCoherenceDesired() {
        return mCoherenceDesired;
    }
    
    public void setCoherenceDesired(double coherenceDesired) {
    	if (coherenceDesired <= 0.0 || coherenceDesired > 1.0) {
    		throw new IllegalArgumentException("coherence not in (0 - 1]: " + coherenceDesired);
    	}
    	mCoherenceDesired = coherenceDesired;
    }
    
    public final double getMinCoherenceThreshold() {
    	return mMinCoherenceThreshold;
    }
    
    /**
     * Set the minimum distance threshold used in computing coherances.
     * 
     * @param minCoherenceThreshold
     */
    public void setMinCoherenceThreshold(double minCoherenceThreshold) {
    	mMinCoherenceThreshold = minCoherenceThreshold;
    }

    /**
     * Get the maximum distance threshold used for computing coherences.
     * 
     * @return
     */
    public double getMaxCoherenceThreshold() {
    	return mMaxCoherenceThreshold;
    }

    /**
     * Set the maximum distance threshold used in computing coherences.
     * 
     * @param maxCoherenceThreshold
     */
    public void setMaxCoherenceThreshold(double maxCoherenceThreshold) {
    	mMaxCoherenceThreshold = maxCoherenceThreshold;
    }

    /**
     * Get the criterion used for selecting the clusters
     * from a completed dendrogram.
     * 
     * @return
     */
    public final Criterion getCriterion() {
        return mCriterion;
    }
    
    /**
     * Set the criterion used for selecting the clusters from a completed
     * dendrogram.
     * 
     * @param criterion
     */
    public void setCriterion(Criterion criterion) {
    	if (criterion == null) {
    		throw new NullPointerException();
    	}
    	mCriterion = criterion;
    }
    
    public final DistanceFunc getDistanceFunc() {
        return mDistanceFunc;
    }
    
    public void setDistanceFunc(DistanceFunc distanceFunc) {
    	if (distanceFunc == null) {
    		throw new NullPointerException();
    	}
    	mDistanceFunc = distanceFunc;
    }

    public final Linkage getLinkage() {
        return mLinkage;
    }
    
    public void setLinkage(Linkage linkage) {
    	if (linkage == null) {
    		throw new NullPointerException();
    	}
    	mLinkage = linkage;
    }

    public final boolean getMinimizeInterleafDistances() {
        return mMinimizeInterleafDistances;
    }
    
    public void setMinimizeInterleafDistances(boolean b) {
    	mMinimizeInterleafDistances = b;
    }
    
    public final int getNumWorkerThreads() {
        return mNumWorkerThreads;
    }
    
    public void setNumWorkerThreads(int numWorkerThreads) {
		if (numWorkerThreads <= 0) {
			throw new IllegalArgumentException("worker thread count must be greater than 0");
		}
		mNumWorkerThreads = numWorkerThreads;
    }
    
    public long getRandomSeed() {
    	return mSeed;
    }
    
    public void setRandomSeed(long seed) {
    	mSeed = seed;
    }
    
    /**
     * Builder class for convenience, so you don't have to remember the numerous constructor
     * parameters.
     * 
     * @author d3j923
     *
     */
    public static class Builder {
    	
        private DistanceFunc mDistFunc = BasicDistanceMethod.EUCLIDEAN_NO_NAN.newFunction();
        private Linkage mLinkage = Linkage.COMPLETE;
        private Criterion mCriterion;
        private int mClustersDesired;
        private double mCoherence;
        private double mMinCoherenceThreshold = 0.0;
        private double mMaxCoherenceThreshold = Double.NaN;
        private boolean mMinimizeDistances;
        private int mNumWorkerThreads = -1;
        private long mSeed = -1L;
    	
    	public Builder(Criterion criterion) {
    		ExceptionUtil.checkNotNull(criterion);
    		mCriterion = criterion;
    	}
    	
    	public HierarchicalClusterTaskParams build() {
    		if (mCriterion == HierarchicalClusterTaskParams.Criterion.CLUSTERS) {
    			return new HierarchicalClusterTaskParams(mClustersDesired, 
    					mMinimizeDistances,
    		            mLinkage,
    		            mDistFunc,
    		            mNumWorkerThreads,
    		            mSeed);
    		} else {
    			return new HierarchicalClusterTaskParams(mCoherence,
    					mMinCoherenceThreshold,
    					mMaxCoherenceThreshold,
    					mMinimizeDistances,
    					mLinkage,
    					mDistFunc,
    					mNumWorkerThreads,
    					mSeed);
    		}
    	}
    	
    	public Builder distanceFunc(DistanceFunc distFunc) {
    		ExceptionUtil.checkNotNull(distFunc);
    		mDistFunc = distFunc;
    		return this;
    	}
    	
    	public Builder clustersDesired(int clustersDesired) {
    		checkCriterion(HierarchicalClusterTaskParams.Criterion.CLUSTERS);
    		mClustersDesired = clustersDesired;
    		return this;
    	}
    	
    	public Builder coherence(double coherence) {
    		checkCriterion(HierarchicalClusterTaskParams.Criterion.COHERENCE);
    		ExceptionUtil.checkInBounds(coherence, 0.0, 1.0);
    		mCoherence = coherence;
    		return this;
    	}
    	
    	public Builder minCoherenceThreshold(double minThreshold) {
    		checkCriterion(HierarchicalClusterTaskParams.Criterion.COHERENCE);
    		mMinCoherenceThreshold = minThreshold;
    		return this;
    	}
    	
    	public Builder maxCoherenceThreshold(double maxThreshold) {
    		checkCriterion(HierarchicalClusterTaskParams.Criterion.COHERENCE);
    		mMaxCoherenceThreshold = maxThreshold;
    		return this;
    	}

    	public Builder linkage (HierarchicalClusterTaskParams.Linkage linkage) {
    		ExceptionUtil.checkNotNull(linkage);
    		mLinkage = linkage;
    		return this;
    	}

    	public Builder randomSeed(long seed) {
    		mSeed = seed;
    		return this;
    	}
    	
    	public Builder optimize(boolean b) {
    		mMinimizeDistances = b;
    		return this;
    	}
    	
    	public Builder numWorkerThreads(int numThreads) {
    		mNumWorkerThreads = numThreads;
    		return this;
    	}
    	
    	private void checkCriterion(HierarchicalClusterTaskParams.Criterion criterion) {
    		if (criterion != mCriterion) {
    			throw new IllegalStateException("parameter does not apply to criterion " + mCriterion);
    		}
    	}
    }
}
