package gov.pnnl.jac.cluster;

import gov.pnnl.jac.geom.distance.BasicDistanceFuncFactory;
import gov.pnnl.jac.geom.distance.BasicDistanceMethod;
import gov.pnnl.jac.geom.distance.DistanceFunc;

public class XMeansClusterTaskParams extends KMeansSplittingClusterTaskParams {

    /**
     * 
     */
    private static final long serialVersionUID = 1178627764755164141L;
    
    private boolean mUseOverallBIC = true;
    
    public XMeansClusterTaskParams(int numWorkerThreads,
            DistanceFunc distanceFunc, ClusterSeeder seeder, int minClusters,
            int maxClusters, boolean useOverallBIC) {
        super(numWorkerThreads, distanceFunc, seeder, minClusters, maxClusters);
        mUseOverallBIC = useOverallBIC;
    }

    public XMeansClusterTaskParams(int numWorkerThreads,
            DistanceFunc distanceFunc, ClusterSeeder seeder) {
        this(1, distanceFunc, seeder, 1, -1, true);
    }

    public XMeansClusterTaskParams() {
        this(1, new BasicDistanceFuncFactory(null).getDistanceFunc(BasicDistanceMethod.EUCLIDEAN_NO_NAN), 
                new KMeansPlusPlusSeeder(System.currentTimeMillis()), 1, -1, true);
    }
    
    public boolean getUseOverallBIC() {
        return mUseOverallBIC;
    }
    
    public void setUseOverallBIC(boolean b) {
        mUseOverallBIC = b;
    }

    public int hashCode() {
        return 37*super.hashCode() + (mUseOverallBIC ? 1 : 0);
    }
    
    public boolean equals(Object o) {
        if (super.equals(o)) {
            return this.mUseOverallBIC == ((XMeansClusterTaskParams) o).mUseOverallBIC;
        }
        return false;
    }
    
    public static class Builder {
        
    	private XMeansClusterTaskParams mParams = new XMeansClusterTaskParams();
    	
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

        public Builder useOverallBIC(boolean b) {
            ((XMeansClusterTaskParams) mParams).setUseOverallBIC(b);
            return this;
        }
        
        public XMeansClusterTaskParams build() {
            return (XMeansClusterTaskParams) mParams;
        }
    }
}
