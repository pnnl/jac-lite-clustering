/*
 * InterleafDistanceMinimizerTask.java
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

import gov.pnnl.jac.geom.CoordinateList;
import gov.pnnl.jac.geom.distance.DistanceCacheFactory;
import gov.pnnl.jac.geom.distance.DistanceFunc;
import gov.pnnl.jac.geom.distance.ReadOnlyDistanceCache;
import gov.pnnl.jac.task.AbstractTask;

import java.io.IOException;

import cern.colt.list.IntArrayList;

/**
 * <p>Task which flips branches of a <tt>Dendrogram</tt> in order to 
 * minimize the sum of the distances between adjacent leaf nodes.
 * This renders the dendrogram prettier for plotting.</p>
 * 
 * @author R. Scarberry
 *
 */
public class InterleafDistanceMinimizerTask extends AbstractTask<Dendrogram> {

	// The dendrogram to be restructured.
	private Dendrogram mDendrogram;
	// Distance cache for computing the interleaf distances.
	private ReadOnlyDistanceCache mCache;

	/**
	 * Constructor.
	 * 
	 * @param dendrogram - the dendrogram whose branches are to be flipped.
	 * @param cache - a cache for looking up the distances.
	 */
	public InterleafDistanceMinimizerTask(Dendrogram dendrogram,
			ReadOnlyDistanceCache cache) {
		mDendrogram = dendrogram;
		mCache = cache;
	}

	/**
	 * Constructor which uses a coordinate list and distance function to 
	 * compute interleaf distances.
	 * 
	 * @param dendrogram - the dendrogram whose branches are to be flipped.
	 * @param cs - contains the coordinate data for computing distance.
	 * @param distFunc - the distance function.
	 */
	public InterleafDistanceMinimizerTask(Dendrogram dendrogram,
			CoordinateList cs, DistanceFunc distFunc) {
		this(dendrogram, DistanceCacheFactory.asReadOnlyDistanceCache(cs, distFunc));
	}

	protected Dendrogram doTask() throws Exception {

		// Get the tree depth.
		int leafLevel = mDendrogram.getLeafLevel();
		if (leafLevel == 0) { // Only one coordinate, trivial case.
			return mDendrogram;
		}

		double[] distChange = new double[1];

		double lastDistTotal = mDendrogram.distancesBetweenLeaves(mCache);

		// No progress events are sent, because it's difficult to estimate
		// accurately how much is left to do, and this generally doesn't take
		// very long to execute anyway.
		
		postMessage("starting interleaf distances = " + lastDistTotal);

		int iteration = 0;
		boolean modified = false;

		do {

			modified = minimizeInterleafDistances(0, lastDistTotal, distChange);

			if (modified) {
				lastDistTotal += distChange[0];
				postMessage("iteration " + (++iteration)
						+ ": interleaf distances = " + lastDistTotal);
			}

		} while (modified);

        return mDendrogram;
	}

	private boolean minimizeInterleafDistances(int startLevel,
			double startingDistTotal, double[] distChange) throws IOException {

		int leafLevel = mDendrogram.getLeafLevel();
		if (startLevel >= leafLevel) {
			return false;
		}

		double distTotal = startingDistTotal;
		boolean modified = false;

		IntArrayList nodeStack = new IntArrayList();

		int currentLevel = startLevel;

		while (currentLevel >= 0) {

			double minDistance = distTotal;

			boolean canFlipChildren = mDendrogram.flipChildren(currentLevel,
					mCache, distChange);

			// Flags to indicate that flipping the children or
			// the grand children caused improvement.
			boolean childFlipImp = false, grandChildImp = false;

			if (canFlipChildren) {

				double childFlipDistance = distTotal + distChange[0];
				if (childFlipDistance < minDistance) {
					minDistance = childFlipDistance;
					childFlipImp = true;
				}

				// Flip back to the previous state.
				mDendrogram.flipChildren(currentLevel, null, null);

				boolean canFlipGrandchildren = mDendrogram.flipGrandChildren(
						currentLevel, mCache, distChange);

				if (canFlipGrandchildren) {

					double grandchildFlipDistance = distTotal + distChange[0];

					if (grandchildFlipDistance < minDistance) {
						minDistance = grandchildFlipDistance;
						grandChildImp = true;
					}

					mDendrogram.flipChildren(currentLevel, mCache, distChange);
					double bothFlipDistance = grandchildFlipDistance + distChange[0];

					if (bothFlipDistance < minDistance) {
						minDistance = bothFlipDistance;
						modified = true;
					} else {
						// Get back to having only the grandchildren flipped.
						mDendrogram.flipChildren(currentLevel, null, null);
						// Need to include grandChildImp flag to prevent cycling,
						// since grandchildFlipDistance may be equal to minDistance
						// even though flipping the grand children didn't actually
						// reduce the distances.
						if (grandchildFlipDistance == minDistance && grandChildImp) {
							modified = true;
						} else {
							// Get back to the starting state of the loop.
							mDendrogram.flipGrandChildren(currentLevel, null, null);

							// Need to include childFlipImp to prevent cycling.
							if (minDistance == childFlipDistance && childFlipImp) {
								mDendrogram.flipChildren(currentLevel, null, null);
								modified = true;
							}
						}
					}

				  // Include childFlipImp to prevent cycling.
				} else if (minDistance == childFlipDistance && childFlipImp) {
					mDendrogram.flipChildren(currentLevel, null, null);
					modified = true;
				}
			}

			distTotal = minDistance;

			int leftLevel = mDendrogram.getLeftChildLevel(currentLevel);
			int rightLevel = mDendrogram.getRightChildLevel(currentLevel);

			if (leftLevel < leafLevel) {
				currentLevel = leftLevel;
				if (rightLevel < leafLevel) {
					nodeStack.add(rightLevel);
				}
			} else if (rightLevel < leafLevel) {
				currentLevel = rightLevel;
			} else {
				int sz = nodeStack.size();
				if (sz > 0) {
					currentLevel = nodeStack.get(sz - 1);
					nodeStack.remove(sz - 1);
				} else {
					currentLevel = -1;
				}
			}

			// So it won't keep going and going after
			// cancellation.
			checkForCancel();

		} // while

		distChange[0] = distTotal - startingDistTotal;
		return modified;
	}

	public String taskName() {
		return "minimization of dendrogram interleaf distances";
	}

}
