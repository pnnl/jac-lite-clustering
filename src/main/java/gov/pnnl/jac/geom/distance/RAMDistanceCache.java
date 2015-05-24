package gov.pnnl.jac.geom.distance;

public class RAMDistanceCache implements DistanceCache {

	// The maximum number of indices for a RAMDistanceCache.
	// Any higher and mDistances would require a greater length than
	// an int can accommodate.
	public static final int MAX_INDEX_COUNT = 0x10000;
	
	private int mIndexCount;
	private double[] mDistances;
	
	public RAMDistanceCache(int indexCount) {
		if (indexCount < 0) {
			throw new IllegalArgumentException("number of indices < 0: " + indexCount);
		}
		if (indexCount > MAX_INDEX_COUNT) {
			throw new IllegalArgumentException("number of indices greater than " + MAX_INDEX_COUNT + ": " + indexCount);
		}
		mIndexCount = indexCount;
		int numDistances = indexCount*(indexCount-1)/2;
		mDistances = new double[numDistances];
	}
	
	RAMDistanceCache(int indexCount, double[] distances) {
		if (indexCount < 0) {
			throw new IllegalArgumentException("number of indices < 0: " + indexCount);
		}
		if (indexCount > MAX_INDEX_COUNT) {
			throw new IllegalArgumentException("number of indices greater than " + MAX_INDEX_COUNT + ": " + indexCount);
		}
		mIndexCount = indexCount;
		int numDistances = indexCount*(indexCount-1)/2;
		if (distances.length != numDistances) {
			throw new IllegalArgumentException("invalid number of distances: " + distances.length + " != " + numDistances);
		}
		mDistances = distances;
	}
	
	private void checkIndex(int index) {
		if (index < 0 || index >= mIndexCount) {
			throw new IllegalArgumentException("index not in [0 - (" + mIndexCount + " - 1)]: " + index);
		}
	}
	
	/**
	 * Get the number of indices, N.  Valid indices for the other methods are
	 * then [0 - (N-1)].
	 * @return - the number of indices.
	 */
	public int getNumIndices() {
		return mIndexCount;
	}
	
	public long getNumDistances() {
		return (long) mDistances.length;
	}
	
	public double getDistance(long n) {
		return mDistances[(int) n];
	}
	
	// Returns the index into mDistances of the distance measure for 
	// index1 and index2.
	private int distanceIndex(int index1, int index2) {
		if (index1 == index2) {
			throw new IllegalArgumentException("indices are equal: " + index1);
		}
        if (index1 > index2) { // Swap them
            index1 ^= index2;
            index2 ^= index1;
            index1 ^= index2;
        }
        int n = mIndexCount - index1;
        return mDistances.length - n *(n - 1)/2 + index2 - index1 - 1;	
	}
	
	public long distancePos(int index1, int index2) {
		return (long) distanceIndex(index1, index2);
	}
	
	/**
	 * Get the distance between the entities represented by index1 and index2.
	 * @param index1
	 * @param index2
	 * @return
	 */
	public double getDistance(int index1, int index2) {
		checkIndex(index1);
		checkIndex(index2);
		double d = 0.0;
		if (index1 != index2) {
			d = mDistances[distanceIndex(index1, index2)];
		}
		return d;
	}
	
	/**
	 * Get distances in bulk.  Element i of the returned array will contain the
	 * distance between indices1[i] and indices2[i].  Therefore, indices1 and indices2
	 * must be the same length.  If distances is non-null, it must be the same length
	 * as indices1 and indices2.  If it's null, a new distances array is allocated and
	 * returned.
	 * @param indices1
	 * @param indices2
	 * @param distances
	 * @return
	 */
	public double[] getDistances(int[] indices1, int[] indices2, double[] distances) {
		int n = indices1.length;
		if (n != indices2.length) {
			throw new IllegalArgumentException(String.valueOf(n) + " != " + indices2.length);
		}
		double[] d = distances;
		if (distances != null) {
			if (distances.length != n) {
				throw new IllegalArgumentException("distance buffer length not equal to number of indices");
			}
		} else {
			d = new double[n];
		}
		for (int i=0; i<n; i++) {
			d[i] = getDistance(indices1[i], indices2[i]);
		}
		return d;
	}
	
	/**
	 * Set the distance between the identities identified by index1 and index2.
	 * @param index1
	 * @param index2
	 * @param distance
	 */
	public void setDistance(int index1, int index2, double distance) {
		checkIndex(index1);
		checkIndex(index2);
		if (index1 != index2) {
			mDistances[distanceIndex(index1, index2)] = distance;
		}
	}
	
	/**
	 * Set distances in bulk.  All three arrays must be the same length.
	 * @param indices1
	 * @param indices2
	 * @param distances
	 */
	public void setDistances(int[] indices1, int[] indices2, double[] distances) {
		int n = indices1.length;
		if (n != indices2.length) {
			throw new IllegalArgumentException(String.valueOf(n) + " != " + indices2.length);
		}
		if (n != distances.length) {
			throw new IllegalArgumentException("distance buffer length not equal to number of indices");
		}
		for (int i=0; i<n; i++) {
			mDistances[distanceIndex(indices1[i], indices2[i])] = distances[i]; 
		}
	}

    private static int[] getIDsAtSimilarityIndex(long n, RAMDistanceCache cache) {
    	
    	int coordCount = cache.getNumIndices();       
    	double b = 2.0*coordCount - 1;
    	
        int i = (int)(-(Math.sqrt(b*b - 8.0*n) - b)/2.0);
        int j = i+1;
       
        j += (int) (n - cache.distanceIndex(i, j));
        
        return new int[] { i, j };
    }

	public static void main(String[] args) {
		int coordCount = 1000;
		RAMDistanceCache cache = new RAMDistanceCache(coordCount);
		long numDistances = cache.getNumDistances();
		for (long l=0; l<=numDistances; l++) {
			int[] indices = getIDsAtSimilarityIndex(l, cache);
			if (cache.distanceIndex(indices[0], indices[1]) != l) {
				System.out.println("ERROR: " + l + " ==> " + indices[0] + ", " + indices[1]);
			} else if (indices[0] < 0 || indices[1] < 0 || indices[1] <= indices[0] || indices[0] >= coordCount || indices[1] >= coordCount) {
				System.out.println("BAD COORDS: " + l + " ==> " + indices[0] + ", " + indices[1]);
			}
		}
		
		System.out.println("numDistances = " + numDistances);		
	}
}
