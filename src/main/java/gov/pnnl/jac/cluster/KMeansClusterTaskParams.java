/*
 * AbstractHierarchicalClusterTask.java
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

import gov.pnnl.jac.geom.distance.*;
import gov.pnnl.jac.util.ExceptionUtil;
import gov.pnnl.jac.util.MersenneTwisterRandom;

/**
 * <p>Encapsulates the parameters needed for KMeans clustering.  This class is
 * the implementation of <tt>ClusterTaskParams</tt> associated with 
 * <tt>KMeansClusterTask</tt>.</p>
 *
 * @author R. Scarberry
 *
 */
public class KMeansClusterTaskParams implements ClusterTaskParams {

    private static final long serialVersionUID = 8885574658404343728L;

    // Desired number of clusters.
    private int mNumClusters;
    // Maximum number of iterations before quitting.
    private int mMaxIterations = Integer.MAX_VALUE;
    // If true, kmeans will attempt to replace empty clusters by splitting
    // one the others in two.
    private boolean mReplaceEmptyClusters;
    // Number of moves in an iteration that, when not exceeded, results in
    // a termination of clustering. (Normally 0).
    private int mMovesGoal;
    // The number of worker threads to use for finding nearest clusters, 
    // recomputing centers, and any other concurrently-processed tasks.
    // If -1, then select based on the number of processors.
    private int mNumWorkerThreads = -1;
    // The distance function.
    private DistanceFunc mDistanceFunc;
    // The cluster seeder.
    private ClusterSeeder mSeeder;

    public KMeansClusterTaskParams(int numClusters, 
            int maxIterations,
            int movesGoal, 
            int numWorkerThreads, 
            boolean replaceEmptyClusters,
            DistanceFunc distanceFunc,
            ClusterSeeder seeder) {
        if (maxIterations > 0) {
            mMaxIterations = maxIterations;
        }
        if (movesGoal < 0) {
            throw new IllegalArgumentException("moves goal cannot be negative");
        }
        if (distanceFunc == null) {
            throw new NullPointerException();
        }
        if (seeder == null) {
            throw new NullPointerException();
        }
        mNumClusters = numClusters;
        mMovesGoal = movesGoal;
        mReplaceEmptyClusters = replaceEmptyClusters;
        mDistanceFunc = distanceFunc;
        mSeeder = seeder;
        if (numWorkerThreads > 0) {
            mNumWorkerThreads = numWorkerThreads;
        }
    }

    public KMeansClusterTaskParams(
            int numClusters, 
            int maxIterations,
            int movesGoal, 
            int numWorkerThreads, 
            DistanceFunc distanceFunc,
            ClusterSeeder seeder) {
    	this(numClusters, maxIterations, movesGoal, numWorkerThreads, true, distanceFunc, seeder);
    }
    
    public KMeansClusterTaskParams(
            int numClusters, 
            DistanceFunc distanceFunc) {
        this(numClusters, Integer.MAX_VALUE, 0, -1, true, distanceFunc,
                new KMeansPlusPlusSeeder(0L, distanceFunc));
    }
    
    public KMeansClusterTaskParams() {
    	this(0, Integer.MAX_VALUE, 0, Runtime.getRuntime().availableProcessors(), new EuclideanNoNaN(),
    			new KMeansPlusPlusSeeder(System.currentTimeMillis(), new EuclideanNoNaN()));
    }

    public final int getNumClusters() {
        return mNumClusters;
    }
    
    public void setNumClusters(int numClusters) {
    	this.mNumClusters = numClusters;
    }

    public final int getMaxIterations() {
        return mMaxIterations;
    }
    
    public void setMaxIterations(int maxIterations) {
    	mMaxIterations = maxIterations;
    }

    public final int getMovesGoal() {
        return mMovesGoal;
    }
    
    public void setMovesGoal(int movesGoal) {
    	mMovesGoal = movesGoal;
    }
    
    public boolean getReplaceEmptyClusters() {
    	return mReplaceEmptyClusters;
    }
    
    public void setReplaceEmptyClusters(boolean b) {
    	mReplaceEmptyClusters = b;
    }

    public final DistanceFunc getDistanceFunc() {
        return mDistanceFunc;
    }
    
    public void setDistanceFunc(DistanceFunc distanceFunc) {
    	if (distanceFunc == null) throw new NullPointerException();
    	mDistanceFunc = distanceFunc;
    }

    public final ClusterSeeder getClusterSeeder() {
        return mSeeder;
    }
    
    public void setClusterSeeder(ClusterSeeder seeder) {
    	if (seeder == null) {
    		throw new NullPointerException();
    	}
    	mSeeder = seeder;
    }

    public final int getNumWorkerThreads() {
        return mNumWorkerThreads;
    }
    
    public void setNumWorkerThreads(int numWorkerThreads) {
    	mNumWorkerThreads = numWorkerThreads;
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException cnse) {
            throw new InternalError();
        }
    }

    public int hashCode() {
        int hc = mNumClusters;
        hc = 31 * hc + mMaxIterations;
        hc = 31 * hc + mMovesGoal;
        hc = 31 * hc + mNumWorkerThreads;
        hc = 31 * hc + mDistanceFunc.hashCode();
        hc = 31 * hc + mSeeder.hashCode();
        return hc;
    }

    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (o instanceof KMeansClusterTaskParams) {
            KMeansClusterTaskParams other = (KMeansClusterTaskParams) o;
            return this.mNumClusters == other.mNumClusters
                    && this.mMaxIterations == other.mMaxIterations
                    && this.mMovesGoal == other.mMovesGoal
                    && this.mNumWorkerThreads == other.mNumWorkerThreads
                    && this.mDistanceFunc.equals(other.mDistanceFunc)
                    && this.mSeeder.equals(other.mSeeder);
        }
        return false;
    }
    
    /**
     * Builder for conveniently building and instance of the
     * immutable class KMeansClusterTaskParams without having to call a constructor 
     * with every parameter.
     * 
     * @author D3J923
     *
     */
    public static class Builder {

        // Desired number of clusters.
        private int mNumClusters;
        // Maximum number of iterations before quitting.
        private int mMaxIterations = Integer.MAX_VALUE;
        // If true, kmeans will attempt to replace empty clusters by splitting
        // one the others in two.
        private boolean mReplaceEmptyClusters = true;
        // Number of moves in an iteration that, when not exceeded, results in
        // a termination of clustering. (Normally 0).
        private int mMovesGoal;
        // The number of worker threads to use for finding nearest clusters, 
        // recomputing centers, and any other concurrently-processed tasks.
        // If -1, then select based on the number of processors.
        private int mNumWorkerThreads = -1;
        // The distance function.
        private DistanceFunc mDistanceFunc;
        // The cluster seeder.
        private ClusterSeeder mSeeder;
        
        public Builder(int numClusters) {
            ExceptionUtil.checkPositive(numClusters);
            this.mNumClusters = numClusters;
        }
        
        public Builder maxIterations(int maxIterations) {
            if (maxIterations <= 0) {
                maxIterations = Integer.MAX_VALUE;
            }
            this.mMaxIterations = maxIterations;
            return this;
        }
        
        public Builder replaceEmptyClusters(boolean b) {
            this.mReplaceEmptyClusters = b;
            return this;
        }
        
        public Builder movesGoal(int movesGoal) {
            ExceptionUtil.checkNonNegative(movesGoal);
            this.mMovesGoal = movesGoal;
            return this;
        }
        
        public Builder numWorkerThreads(int numWorkerThreads) {
            if (numWorkerThreads <= 0) {
                numWorkerThreads = -1;
            }
            this.mNumWorkerThreads = numWorkerThreads;
            return this;
        }
        
        public Builder distanceFunc(DistanceFunc distanceFunc) {
            ExceptionUtil.checkNotNull(distanceFunc);
            this.mDistanceFunc = distanceFunc;
            return this;
        }
        
        public Builder seeder(ClusterSeeder seeder) {
            ExceptionUtil.checkNotNull(seeder);
            this.mSeeder = seeder;
            return this;
        }
        
        public KMeansClusterTaskParams build() {
            if (this.mDistanceFunc == null) {
                this.mDistanceFunc = new EuclideanNoNaN();
            }
            if (this.mSeeder == null) {
                this.mSeeder = new RandomSeeder(System.currentTimeMillis(), 
                        new MersenneTwisterRandom());
            }
            return new KMeansClusterTaskParams(
                    this.mNumClusters, 
                    this.mMaxIterations,
                    this.mMovesGoal, 
                    this.mNumWorkerThreads, 
                    this.mReplaceEmptyClusters,
                    this.mDistanceFunc,
                    this.mSeeder);
        }
    }
}
