package gov.pnnl.jac.cluster.similarity;

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

import gov.pnnl.jac.cluster.HierarchicalClusterTaskParams;
import gov.pnnl.jac.util.ExceptionUtil;

/**
 * <p>Implementation of <tt>ClusterTaskParams</tt> to encapsulate the 
 * parameters for clustering algorithms that extend <tt>AbstractHierarchicalClusterTask</tt>.</p>
 * 
 * @author d3j923
 *
 */
public class HierarchicalSimilarityClusterTaskParams implements SimilarityClusterTaskParams {

    private HierarchicalClusterTaskParams.Linkage mLinkage = HierarchicalClusterTaskParams.Linkage.COMPLETE;

    private HierarchicalClusterTaskParams.Criterion mCriterion = HierarchicalClusterTaskParams.Criterion.CLUSTERS;

    // Only one of these is relevant, depending on the value of mCriterion. 
    private int mClustersDesired;

    private double mCoherenceDesired;
    
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
    private int mNumWorkerThreads = -1;

    public HierarchicalSimilarityClusterTaskParams(int clustersDesired, boolean optimize,
            HierarchicalClusterTaskParams.Linkage linkage, int numWorkerThreads) {
        mCriterion = HierarchicalClusterTaskParams.Criterion.CLUSTERS;
        if (clustersDesired <= 0) {
            throw new IllegalArgumentException(
                    "desired number of clusters must be positive");
        }
        mClustersDesired = clustersDesired;
        mMinimizeInterleafDistances = optimize;
        if (linkage == null) {
            throw new NullPointerException();
        }
        mLinkage = linkage;
        if (numWorkerThreads > 0) {
            mNumWorkerThreads = numWorkerThreads;
        }
    }

    public HierarchicalSimilarityClusterTaskParams(int clustersDesired, boolean optimize,
            HierarchicalClusterTaskParams.Linkage linkage) {
        this(clustersDesired, optimize, linkage, -1);
    }

    public HierarchicalSimilarityClusterTaskParams(int clustersDesired, boolean optimize) {
        this(clustersDesired, optimize, HierarchicalClusterTaskParams.Linkage.COMPLETE, -1);
    }

    public HierarchicalSimilarityClusterTaskParams(
    		double coherenceDesired,
            boolean optimize, HierarchicalClusterTaskParams.Linkage linkage, int numWorkerThreads) {
    	this(coherenceDesired, 0.0, Double.NaN, optimize, linkage, numWorkerThreads);
    }
    
    public HierarchicalSimilarityClusterTaskParams(
    		double coherenceDesired,
    		double minCoherenceThreshold,
    		double maxCoherenceThreshold,
            boolean optimize, HierarchicalClusterTaskParams.Linkage linkage, int numWorkerThreads) {
        mCriterion = HierarchicalClusterTaskParams.Criterion.COHERENCE;
        if (coherenceDesired < 0.0 || coherenceDesired > 1.0) {
            throw new IllegalArgumentException("coherence not in [0.0 - 1.0]: "
                    + coherenceDesired);
        }
        mCoherenceDesired = coherenceDesired;
        mMinCoherenceThreshold = minCoherenceThreshold;
        mMaxCoherenceThreshold = maxCoherenceThreshold;
        mMinimizeInterleafDistances = optimize;
        if (linkage == null) {
            throw new NullPointerException();
        }
        mLinkage = linkage;
        if (numWorkerThreads > 0) {
            mNumWorkerThreads = numWorkerThreads;
        }
    }

    public HierarchicalSimilarityClusterTaskParams(double coherenceDesired,
            boolean optimize, HierarchicalClusterTaskParams.Linkage linkage) {
        this(coherenceDesired, optimize, linkage, -1);
    }

    public HierarchicalSimilarityClusterTaskParams(double coherenceDesired,
            boolean optimize) {
        this(coherenceDesired, optimize, HierarchicalClusterTaskParams.Linkage.COMPLETE, -1);
    }

    public int hashCode() {
        int hc = mLinkage.hashCode();
        hc = 37 * hc + mCriterion.hashCode();
        long bits = Double.doubleToLongBits(mCoherenceDesired);
        hc = 37 * hc + (int) (bits ^ (bits >>> 32));
        bits = Double.doubleToLongBits(mMinCoherenceThreshold);
        hc = 37 * hc + (int) (bits ^ (bits >>> 32));
        bits = Double.doubleToLongBits(mMaxCoherenceThreshold);
        hc = 37 * hc + (int) (bits ^ (bits >>> 32));        
        hc = 37 * hc + (mMinimizeInterleafDistances ? 1231 : 1237);
        hc = 37 * hc + mNumWorkerThreads;
        return hc;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof HierarchicalSimilarityClusterTaskParams) {
            HierarchicalSimilarityClusterTaskParams other = (HierarchicalSimilarityClusterTaskParams) o;
            return this.mLinkage == other.mLinkage
                    && this.mCriterion == other.mCriterion
                    && this.mClustersDesired == other.mClustersDesired
                    && Double.doubleToLongBits(this.mCoherenceDesired) == 
                    	Double.doubleToLongBits(other.mCoherenceDesired)
                    && Double.doubleToLongBits(this.mMinCoherenceThreshold) == 
                    	Double.doubleToLongBits(other.mMinCoherenceThreshold)
                    && Double.doubleToLongBits(this.mMaxCoherenceThreshold) == 
                    	Double.doubleToLongBits(other.mMaxCoherenceThreshold)
                    && this.mMinimizeInterleafDistances == other.mMinimizeInterleafDistances
                    && this.mNumWorkerThreads == other.mNumWorkerThreads;
        }
        return false;
    }

    public final int getClustersDesired() {
        return mClustersDesired;
    }

    public final double getCoherenceDesired() {
        return mCoherenceDesired;
    }

	public double getMinCoherenceThreshold() {
		return mMinCoherenceThreshold;
	}
	
	public double getMaxCoherenceThreshold() {
		return mMaxCoherenceThreshold;
	}
	
	public final HierarchicalClusterTaskParams.Criterion getCriterion() {
        return mCriterion;
    }

    public final HierarchicalClusterTaskParams.Linkage getLinkage() {
        return mLinkage;
    }

    public final boolean getMinimizeInterleafDistances() {
        return mMinimizeInterleafDistances;
    }

    public final int getNumWorkerThreads() {
        return mNumWorkerThreads;
    }
    
    /**
     * Builder for conveniently building an instance of the
     * immutable class KMeansClusterTaskParams without having to call a constructor 
     * with every parameter.
     * 
     * @author D3J923
     *
     */
    public static class Builder {
    	
    	private HierarchicalClusterTaskParams.Criterion mCriterion;
    	private int mNumClusters;
    	private double mCoherence;
    	private double mMinCoherenceThreshold;
    	private double mMaxCoherenceThreshold;
    	private HierarchicalClusterTaskParams.Linkage mLinkage;
    	private boolean mMinimizeDistances;
    	private int mNumWorkerThreads;
    	
    	public Builder(HierarchicalClusterTaskParams.Criterion criterion) {
    		ExceptionUtil.checkNotNull(criterion);
    		mCriterion = criterion;
    	}
    	
    	public HierarchicalSimilarityClusterTaskParams build() {
    		if (mCriterion == HierarchicalClusterTaskParams.Criterion.CLUSTERS) {
    			return new HierarchicalSimilarityClusterTaskParams(
    					mNumClusters,
    					mMinimizeDistances,
    					mLinkage,
    					mNumWorkerThreads);
    		} else {
    			return new HierarchicalSimilarityClusterTaskParams(
    					mCoherence,
    					mMinCoherenceThreshold,
    					mMaxCoherenceThreshold,
    					mMinimizeDistances,
    					mLinkage,
    					mNumWorkerThreads);
    		}
    	}
    	
    	public Builder clustersDesired(int numClusters) {
    		checkCriterion(HierarchicalClusterTaskParams.Criterion.CLUSTERS);
    		mNumClusters = numClusters;
    		return this;
    	}
    	
    	public Builder coherence(double coherence) {
    		checkCriterion(HierarchicalClusterTaskParams.Criterion.COHERENCE);
    		ExceptionUtil.checkInBounds(coherence, 0.0, 1.0);
    		mCoherence = coherence;
    		return this;
    	}
    	
    	public Builder minCoherenceThreshold(double t) {
    		checkCriterion(HierarchicalClusterTaskParams.Criterion.COHERENCE);
    		mMinCoherenceThreshold = t;
    		return this;
    	}
    	
    	public Builder maxCoherenceThreshold(double t) {
    		checkCriterion(HierarchicalClusterTaskParams.Criterion.COHERENCE);
    		mMaxCoherenceThreshold = t;
    		return this;
    	}
    	
    	public Builder linkage (HierarchicalClusterTaskParams.Linkage linkage) {
    		ExceptionUtil.checkNotNull(linkage);
    		mLinkage = linkage;
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
