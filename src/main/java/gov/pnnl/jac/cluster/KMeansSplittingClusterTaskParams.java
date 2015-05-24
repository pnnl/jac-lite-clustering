/*
 * KMeansSplittingClusterTaskParams.java
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

import cern.colt.map.HashFunctions;
import gov.pnnl.jac.cluster.XMeansClusterTaskParams.Builder;
import gov.pnnl.jac.geom.distance.*;
import gov.pnnl.jac.util.ExceptionUtil;

public class KMeansSplittingClusterTaskParams implements ClusterTaskParams {

    /**
     * 
     */
    private static final long serialVersionUID = 4234591057353024383L;

    // The number of worker threads to use for finding nearest clusters, 
    // recomputing centers, and any other concurrently-processed tasks.
    // If -1, then select based on the number of processors.
    private int mNumWorkerThreads = Runtime.getRuntime().availableProcessors();

    private int mMinClusters = 1, mMaxClusters = -1;
    
    private double mMinClusterToMeanThreshold = 0.05;

    private DistanceFunc mDistanceFunc;

    private ClusterSeeder mSeeder;   

    public KMeansSplittingClusterTaskParams(int numWorkerThreads,
            DistanceFunc distanceFunc, ClusterSeeder seeder, int minClusters,
            int maxClusters) {
        if (distanceFunc == null || seeder == null)
            throw new NullPointerException();
        mNumWorkerThreads = numWorkerThreads;
        mDistanceFunc = distanceFunc;
        mSeeder = seeder;
        if (minClusters > 0) {
            mMinClusters = minClusters;
        }
        if (maxClusters > 0) {
            mMaxClusters = Math.max(mMinClusters, maxClusters);
        }
    }

    public KMeansSplittingClusterTaskParams(int numWorkerThreads,
            DistanceFunc distanceFunc, ClusterSeeder seeder) {
        this(numWorkerThreads, distanceFunc, seeder, 1, -1);
    }

    public KMeansSplittingClusterTaskParams() {
        this(1, new BasicDistanceFuncFactory(null).getDistanceFunc(BasicDistanceMethod.EUCLIDEAN_NO_NAN), 
                new KMeansPlusPlusSeeder(System.currentTimeMillis()), 1, -1);
    }
    
    public void setMinClusterToMeanThreshold(double t) {
        mMinClusterToMeanThreshold = Math.max(0.0, t);
    }
    
    /**
     * Returns a threshold for determining if small clusters are to be
     * thrown out at the end of clustering.  By default this threshold is
     * 0.05.  If set to zero, no clusters will be thrown out because of size.
     * If greater than zero, clusters having sizes less than this value
     * times the mean cluster size are thrown out.
     * @return
     */
    public double getMinClusterToMeanThreshold() {
        return mMinClusterToMeanThreshold;
    }

    public DistanceFunc getDistanceFunc() {
        return mDistanceFunc;
    }
    
    public void setDistanceFunc(DistanceFunc distanceFunc) {
        ExceptionUtil.checkNotNull(distanceFunc);
        mDistanceFunc = distanceFunc;
    }

    public int getMinClusters() {
        return mMinClusters;
    }
    
    public void setMinClusters(int minClusters) {
        ExceptionUtil.checkPositive(minClusters);
        mMinClusters = minClusters;
    }

    public int getMaxClusters() {
        return mMaxClusters;
    }
    
    public void setMaxClusters(int maxClusters) {
        ExceptionUtil.checkPositive(maxClusters);
        mMaxClusters = maxClusters;
    }

    public int getNumWorkerThreads() {
        return mNumWorkerThreads;
    }
    
    public void setNumWorkerThreads(int numWorkerThreads) {
        if (numWorkerThreads <= 0) {
            numWorkerThreads = -1;
        }
        mNumWorkerThreads = numWorkerThreads;
    }

    public ClusterSeeder getClusterSeeder() {
        return mSeeder;
    }
    
    public void setClusterSeeder(ClusterSeeder seeder) {
        ExceptionUtil.checkNotNull(seeder);
        mSeeder = seeder;
    }

    public int hashCode() {
        int hc = mNumWorkerThreads;
        hc = 31 * hc + mMinClusters;
        hc = 31 * hc + mMaxClusters;
        hc = 31 * hc + mDistanceFunc.hashCode();
        hc = 31 * hc + mSeeder.hashCode();
        hc = 31 * hc + HashFunctions.hash(mMinClusterToMeanThreshold);
        return hc;
    }

    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (o != null && o.getClass() == this.getClass()) {
            KMeansSplittingClusterTaskParams other = (KMeansSplittingClusterTaskParams) o;
            return this.mMinClusters == other.mMinClusters
            && this.mMaxClusters == other.mMaxClusters
            && this.mNumWorkerThreads == other.mNumWorkerThreads
            && Double.doubleToLongBits(this.mMinClusterToMeanThreshold) ==
                Double.doubleToLongBits(other.mMinClusterToMeanThreshold)
            && this.mDistanceFunc.equals(other.mDistanceFunc)
            && this.mSeeder.equals(other.mSeeder);
        }
        return false;
    }

    public static class Builder {
        
    	private KMeansSplittingClusterTaskParams mParams = new KMeansSplittingClusterTaskParams();
    	
        public Builder() {}
        
        public Builder numWorkerThreads(int numWorkerThreads) {
            mParams.setNumWorkerThreads(numWorkerThreads);
            return this;
        }
        
        public Builder minClusters(int minClusters) {
            mParams.setMinClusters(minClusters);
            return this;
        }
        
        public Builder maxClusters(int maxClusters) {
            mParams.setMaxClusters(maxClusters);
            return this;
        }
        
        public Builder minClusterToMeanThreshold(double clusterToMeanThreshold) {
            mParams.setMinClusterToMeanThreshold(clusterToMeanThreshold);
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

        public KMeansSplittingClusterTaskParams build() {
            return mParams;
        }
    }
    
}
