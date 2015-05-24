package gov.pnnl.jac.cluster;

import gov.pnnl.jac.collections.IntArrayList;
import gov.pnnl.jac.geom.CoordinateList;
import gov.pnnl.jac.geom.SimpleCoordinateList;
import gov.pnnl.jac.geom.distance.DistanceFunc;
import gov.pnnl.jac.geom.distance.EuclideanNoNaN;

import java.util.Arrays;
import java.util.Random;

import cern.colt.bitvector.BitVector;

/**
 * <p>Implements a seeding method proposed in 2007 by 
 * David Arthur and Sergei Vassilvitskii.  See:
 * http://en.wikipedia.org/wiki/K-means%2B%2B</p>
 * 
 * @author D3J923
 *
 */
public class KMeansPlusPlusSeeder extends RandomSeeder {

    private DistanceFunc mDistFunc;
    private boolean mCancelFlag;
	
	public KMeansPlusPlusSeeder(long seed) {
		this (seed, new Random(), new EuclideanNoNaN());
	}
	
	public KMeansPlusPlusSeeder(long seed, 
			Random random, 
			DistanceFunc distanceFunc) {
		super(seed, random);
		mDistFunc = distanceFunc;
	}
	
	public KMeansPlusPlusSeeder(long seed, DistanceFunc distanceFunc) {
		this(seed, new Random(), distanceFunc);
	}

	public void cancel() {
	    mCancelFlag = true;
	}
	
	private static int[] generatePotentialSeeds(final int coordCount, final Random random) {
		int[] potentialSeeds = new int[coordCount];
		// Place the indexes in an array.
		for (int i=0; i<coordCount; i++) {
			potentialSeeds[i] = i;
 		}
		// Now shuffle them.
		for (int i=coordCount-1; i>0; i--) {
			int j = random.nextInt(i);
			if (i != j) {
				int tmp = potentialSeeds[i];
				potentialSeeds[i] = potentialSeeds[j];
				potentialSeeds[j] = tmp;
			}
		}
		return potentialSeeds;
	}
	
    public synchronized CoordinateList generateSeeds(final CoordinateList coords, int numSeeds) {

            long seed = this.getRandomSeed();
            if (seed == 0L) {
                seed = System.currentTimeMillis();
            }

            Random random = this.getRandom();
            random.setSeed(seed);

            final int coordCount = coords.getCoordinateCount();
            final int coordLen = coords.getDimensionCount();

            // An idiot check.
            if (numSeeds > coordCount) {
                numSeeds = coordCount;
            }

            // Just shuffles the indexes of the coordinates.
            int[] potentialSeeds = generatePotentialSeeds(coordCount, random);            
            // Should be equal to coords.getCoordinateCount().
            final int potentialSeedCount = potentialSeeds.length;
            // Fixed size array to store the minimum distances to any of the seeds (squared).
            double[] minSeedDistances2 = new double[potentialSeedCount];
            // Used to flag potentialSeeds as no longer available.
            BitVector unavailableBits = new BitVector(potentialSeedCount);
            
            // To build up a list of the seed indexes.
            IntArrayList seedList = new IntArrayList(numSeeds);

            // Choose the first seed at random.
            int firstSeed = random.nextInt(potentialSeedCount);
            // Add it to the list.
            seedList.add(potentialSeeds[firstSeed]);
            // Mark it as no longer available.
            unavailableBits.set(firstSeed);
            
            // Working buffers for retrieving coord values.
            double[] buf1 = new double[coordLen];
            double[] buf2 = new double[coordLen];

            // Initialize the min seed distance array.  Since there's only one
            // seed so far, only one distance needs to be computed for the
            // remaining potential seeds.
            
            coords.getCoordinates(potentialSeeds[firstSeed], buf1);
            
            for (int i = 0; i<potentialSeedCount; i++) {
            	if (i != firstSeed) {
            		int ndx = potentialSeeds[i];
            		coords.getCoordinates(ndx, buf2);
            		double d = mDistFunc.distanceBetween(buf1, buf2);
            		minSeedDistances2[i] = d*d;
            	}
            }
            
            while(seedList.size() < numSeeds) {
            	
                if (mCancelFlag) {
                    break;
                }
                
                // Overall sum of square distances.
                double distSqSum = 0;

                for (int i=0; i<potentialSeedCount; i++) {
                	if (!unavailableBits.get(i)) {
                		// If the potential seed is still available add its min squared distance
                		// to the distance squared sum.
                		distSqSum += minSeedDistances2[i];
                	}
                }
                
                if (mCancelFlag) {
                    break;
                }
                
                // Compute a random threshold.
                double t = random.nextDouble() * distSqSum;
                
                double probSum = 0.0;
                int newSeedIndex = -1;
                int lastAvailable = -1;
                
                for (int i=0; i<potentialSeedCount; i++) {
                	
                	if (!unavailableBits.get(i)) {
                	
                	    lastAvailable = i;
                	    
                		probSum += minSeedDistances2[i];
                	
                		if (probSum >= t) {
                			newSeedIndex = i;
                			break;
                		}
                		
                	}
                }
                
                // Didn't find one, possibly because of tiny distances that
                // don't add up to enough to exceed the threshold.  In that case,
                // pick the last available.
                if (newSeedIndex == -1) {
                    newSeedIndex = lastAvailable;
                }
                
                if (newSeedIndex >= 0) {
                    
                    seedList.add(potentialSeeds[newSeedIndex]);
                	unavailableBits.set(newSeedIndex);
                			
                	if (seedList.size() < numSeeds) {

                	    // Update the min seed distances2 array.  Only have
                	    // to compute distance to the new seed.
                	    coords.getCoordinates(potentialSeeds[newSeedIndex], buf1);
                				
                	    for (int i=0; i<potentialSeedCount; i++) {
                	        if (!unavailableBits.get(i)) {
                	            
                	            if (mCancelFlag) {
                	                break;
                	            }
                	            
                	            int ndx = potentialSeeds[i];
                	            coords.getCoordinates(ndx, buf2);
                	            double d = mDistFunc.distanceBetween(buf1, buf2);
                	            double d2 = d*d;
                	            if (d2 < minSeedDistances2[i]) {
                	                minSeedDistances2[i] = d2;
                	            }
                	        }
                	    }
                	}
                	
                } else { // newSeedIndex == -1
                    
                    // Need to break from while to avoid being caught in an
                    // infinite loop.
                    break;
                }
            }

            numSeeds = seedList.size();
            
            CoordinateList seeds = new SimpleCoordinateList(coordLen, numSeeds);
            int[] seedIndices = seedList.toArray();
            seedList = null;
            Arrays.sort(seedIndices);

            for (int i=0; i<numSeeds; i++) {
                int index = seedIndices[i];
                coords.getCoordinates(index, buf1);
                seeds.setCoordinates(i, buf1);
            }

            return seeds;
    }

    public int hashCode() {
    	int hc = super.hashCode();
    	return 37*hc + this.mDistFunc.hashCode();
    }
    
    public boolean equals(Object o) {
		if (o == this) return true;
		if (super.equals(o)) {
			if (o instanceof KMeansPlusPlusSeeder) {
				return this.mDistFunc.equals(((KMeansPlusPlusSeeder) o).mDistFunc);
			}
		}
		return false;
	}
}
