package gov.pnnl.jac.cluster.similarity;

import java.io.IOException;

import gov.pnnl.jac.geom.distance.DistanceCacheFactory;
import gov.pnnl.jac.geom.distance.ReadOnlyDistanceCache;

public class SimilarityDistances {

    private double mSimRange, mMaxSim;
    private Similarities mSimilarities;
    
    public SimilarityDistances(Similarities similarities) {
        if (similarities == null) {
            throw new NullPointerException();
        }
        mMaxSim = similarities.getMaxPossible();
        double minSim = similarities.getMinPossible();
        boolean computeMax = Double.isNaN(mMaxSim);
        boolean computeMin = Double.isNaN(minSim);
        if (computeMax || computeMin) {
            int rc = similarities.getRecordCount();
            double max = -Double.MAX_VALUE;
            double min = Double.MAX_VALUE;
            for (int i=0; i<rc; i++) {
                for (int j=i+1; j<rc; j++) {
                    double s = similarities.getSimilarity(i, j);
                    if (computeMax && s > max) {
                        max = s;
                    }
                    if (computeMin && s < min) {
                        min = s;
                    }
                }
            }
            if (computeMax) {
                mMaxSim = max;
            }
            if (computeMin) {
                minSim = min;
            }
        }
        mSimilarities = similarities;
        mSimRange = mMaxSim - minSim;
    }
    
    public int getRecordCount() {
        return mSimilarities.getRecordCount();
    }
    
    public double getDistance(int i, int j) {
        if (mSimRange == 0.0) return 0.0;
        return (mMaxSim - mSimilarities.getSimilarity(i, j))/mSimRange;
    }
    
    public ReadOnlyDistanceCache asReadOnlyDistanceCache() {
        return new SimilarityDistancesReadOnlyDistanceCache();
    }
    
    private class SimilarityDistancesReadOnlyDistanceCache implements ReadOnlyDistanceCache {

        public long distancePos(int index1, int index2) {
            if (index1 > index2) { // Swap them
                index1 ^= index2;
                index2 ^= index1;
                index1 ^= index2;
            }
            int n = getRecordCount() - index1;
            return getNumDistances() - n *(n - 1)/2 + index2 - index1 - 1;  
        }

        public double getDistance(int index1, int index2) throws IOException {
            return SimilarityDistances.this.getDistance(index1, index2);
        }

        public double getDistance(long n) throws IOException {
            int[] ids = new int[2];
            DistanceCacheFactory.getIndicesForDistance(n, this);
            return getDistance(ids[0], ids[1]);
        }

        public double[] getDistances(int[] indices1, int[] indices2,
                double[] distances) throws IOException {
            int len = indices1.length;
            if (distances == null || distances.length < len) {
                    distances = new double[len];
            }
            for (int i=0; i<len; i++) {
                    distances[i] = getDistance(indices1[i], indices2[i]);
            }
            return distances;
        }

        public long getNumDistances() {
            long rc = getRecordCount();
            return rc*(rc-1L)/2L;
        }

        public int getNumIndices() {
            return getRecordCount();
        }
        
    }
}
