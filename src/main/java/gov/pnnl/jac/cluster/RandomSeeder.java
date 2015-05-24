/*
 * RandomSeeder.java
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import gov.pnnl.jac.geom.*;
import cern.colt.map.HashFunctions;

public class RandomSeeder implements ClusterSeeder {

	private long mSeed;
	private Random mRandom;
	
	public RandomSeeder(long seed, Random random) {
		if (random == null) {
			throw new NullPointerException();
		}
		mSeed = seed;
		mRandom = random;
	}
	
	public RandomSeeder(long seed) {
		this (seed, new Random());
	}
	
	public int hashCode() {
		return 37*HashFunctions.hash(mSeed) + mRandom.getClass().hashCode();
	}
	
	public boolean equals(Object o) {
		if (o == this) return true;
		if (o instanceof RandomSeeder) {
			RandomSeeder other = (RandomSeeder) o;
			return this.mSeed == other.mSeed && this.mRandom.getClass().equals(
					other.mRandom.getClass());
		}
		return false;
	}
	
	public synchronized CoordinateList generateSeeds(final CoordinateList coords, final int numSeeds) {

		if (numSeeds <= 0) {
			throw new IllegalArgumentException();
		}

		int coordCount = coords.getCoordinateCount();
		
		int[] indices = new int[coordCount];
        for (int i = 0; i < coordCount; i++) {
            indices[i] = i;
        }

        long seed = mSeed != 0L ? mSeed : System.currentTimeMillis();
        mRandom.setSeed(seed);
        
        for (int i = 0, m = coordCount; m > 0; i++, m--) {
            int j = i + mRandom.nextInt(m);
            if (i != j) {
                indices[i] ^= indices[j];
                indices[j] ^= indices[i];
                indices[i] ^= indices[j];
            }
        }

        int centersFound = 0;
        List<ClusterCenter> centerList = new ArrayList<ClusterCenter>(
                numSeeds);
        Set<ClusterCenter> centerSet = new HashSet<ClusterCenter>(
                numSeeds * 2);

        for (int i = 0; i < coordCount && centersFound < numSeeds; i++) {
            int ndx = indices[i];
            ClusterCenter center = new ClusterCenter(coords.getCoordinates(ndx, null));
            if (!centerSet.contains(center)) {
            	centerList.add(center);
                centerSet.add(center);
                centersFound++;
            }
        }

        int coordLen = coords.getDimensionCount();
        CoordinateList seeds = new SimpleCoordinateList(coordLen, centersFound);
        for (int i=0; i<centersFound; i++) {
        	seeds.setCoordinates(i, centerList.get(i).getCenter());
        }
        
        return seeds;
	}

	public long getRandomSeed() {
		return mSeed;
	}
	
	public Random getRandom() {
		return mRandom;
	}

    // Inner class used to assure that unique initial clusters are
    // selected.
    static class ClusterCenter {

    	double[] mCenter;

        ClusterCenter(double[] center) {
            mCenter = center;
        }

        double[] getCenter() {
            return mCenter;
        }

        public int hashCode() {
            int hc = 0;
            int len = mCenter.length;
            if (len > 0) {
                long l = Double.doubleToLongBits(mCenter[0]);
                hc = (int) (l ^ (l >>> 32));
                for (int i = 1; i < len; i++) {
                    l = Double.doubleToLongBits(mCenter[i]);
                    hc = 37 * hc + (int) (l ^ (l >>> 32));
                }
            }
            return hc;
        }

        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (o instanceof ClusterCenter) {
                double[] otherCenter = ((ClusterCenter) o).mCenter;
                int n = this.mCenter.length;
                if (n == otherCenter.length) {
                    for (int i = 0; i < n; i++) {
                        if (Double.doubleToLongBits(this.mCenter[i]) != Double
                                .doubleToLongBits(otherCenter[i])) {
                            return false;
                        }
                    }
                    return true;
                }
            }
            return false;
        }
    }
}
