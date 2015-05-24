/*
 * Dendrogram.java
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

import gov.pnnl.jac.collections.*;
import gov.pnnl.jac.geom.CoordinateList;
import gov.pnnl.jac.geom.distance.ReadOnlyDistanceCache;
import static gov.pnnl.jac.io.IOUtils.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cern.colt.bitvector.BitVector;

/**
 * <p>The <tt>Dendrogram</tt> class represents dendrograms produced
 * by hierarchical clustering algorithms.  Even though methods make the 
 * dendrogram appear to be a tree structure, internally all nodes are represented
 * by primitives.  For this reason, this class is highly scalable and uses 
 * little memory as compared to more object-oriented implementations.  Also, 
 * traversal methods are not recursive, so there is no danger of stack overflow
 * exceptions from trying to traverse large and highly imbalanced dendrograms.
 * </p>
 * 
 * @author d3j923
 *
 */
public class Dendrogram implements Externalizable {

    private static final int EXTERNALIZABLE_VERSION = 1;
    
    // Contains IDs of the nodes, both the non-leaf and leaf.
	// Length is equal to 2*mLeafCount - 1.  The IDs are generally
    // the indexes of the items being clustered.
	private int[] mNodeIDs;
	
	// Indices of the parent nodes - same length as mNodeIDs. Initialized to -1
	// which means "parent not set".  When the dendrogram is finished all elements
	// except 0 should have values >= 0.  Element 0 will remain -1 since the
	// root node does not have a parent.
	private int[] mParentIndices;
	// Stores the indices into mNodeIDs of the left and right
	// children nodes of the non-leaf node at each level
	// 0 - (getLeafCount() - 2).
	private int[] mLeftIndices, mRightIndices;

	// Contains the size of the node at levels [0 - (getLeafCount() - 2)].
	private int[] mSizes;

	// Given an ID, what index into the above arrays will give
	// the pertinent info for that ID?
	private int[] mIndicesForIDs;

	private double[] mDistances, mCoherences;
	
	private boolean mCoherencesComputed;
	
	// Used in computation of coherences.  If not explicitly set,
	// the max threshold becomes the max value in mDistances.
	private double mMinCoherenceThreshold = 0.0;
	private double mMaxCoherenceThreshold = Double.NaN;

	// Number of leaf nodes -- same as the number of coordinates or vectors
	// that are being clustered.
	private int mLeafCount;

	// The current level -- counts down from getLeafCount()-1 to 0 as nodes are
	// merged.  The leaf level is getLeafCount() - 1.  The non-leaf levels
	// are [0 - (getLeafCount() - 2)].
	private int mCurrentLevel;
	
	/**
	 * Constructor.  Initially forms a dendrogram with only leaf nodes
	 * with IDs <code>[0 - (leafCount - 1)]</code>.  Completion of the dendrogram requires
	 * <code>(leafCount - 1)</code> calls to <code>merge()</code>.
	 *
	 * @param leafCount
	 */
	public Dendrogram(int leafCount) {

		if (leafCount == 0) {
			throw new IllegalArgumentException(
			        "number of leaves must be > 0");
		}

		mLeafCount = leafCount;

		// Total number of nodes
		int nodeCount = 2 * mLeafCount - 1;
		// Number of non-leaf nodes
		int nonLeafCount = mLeafCount - 1;

		// The array containing the node IDs, both leaf and non-leaf.
		mNodeIDs = new int[nodeCount];
		// Initialize the leaf node IDs, which are at the bottom of the array
		// and are numbered sequentially
		int id = 0;
		for (int i = nonLeafCount; i < nodeCount; i++) {
			mNodeIDs[i] = id++;
		}

		// Initialize the parent indices.
		mParentIndices = new int[nodeCount];
		Arrays.fill(mParentIndices, -1);

		int index = nonLeafCount;
		mIndicesForIDs = new int[mLeafCount];
		for (int i = 0; i < mLeafCount; i++) {
			mIndicesForIDs[i] = index++;
		}

		mLeftIndices = new int[nonLeafCount];
		mRightIndices = new int[nonLeafCount];
		mSizes = new int[nonLeafCount];

		mDistances = new double[nonLeafCount];
		mCoherences = new double[nonLeafCount];

		mCurrentLevel = nonLeafCount; // == (ids.numIDs() - 1)

	}

	public double getMinCoherenceThreshold() {
		return mMinCoherenceThreshold;
	}
	
	public void setMinCoherenceThreshold(double d) {
		mMinCoherenceThreshold = d;
		mCoherencesComputed = false;
	}
	
	public double getMaxCoherenceThreshold() {
		return mMaxCoherenceThreshold;
	}
	
	public void setMaxCoherenceThreshold(double d) {
		mMaxCoherenceThreshold = d;
		mCoherencesComputed = false;
	}
	
	/**
	 * Returns an array containing the mappings for the leaf ids.  The mapping
	 * for the leaf with index n will be found in the nth element of the array.
	 * These mappings can be regarded as the left-to-right positions of the nodes
	 * of level greater than or equal to the specified level containing each leaf
	 * id.  The dendrogram must be finished to call this method.  The specified level
	 * must be in the range <code>[0 - getLeafLevel()]</code>.  If level is equal to
	 * the leaf level, all mappings will be unique.  If level is less than the leaf level,
	 * for each non-leaf node at levels greater than level, all child leaf IDs will be
	 * collapsed to the smallest mapping.
	 * @param level
	 * @return - an array of length <code>getLeafCount()</code> containing the mappings
	 *   for each index n in the nth position.
	 * @exception - IllegalStateException, if the dendrogram is not finished.
	 * @exception - IndexOutOfBoundsException, if the level is out of range.
	 */
	public int[] getLeafIDMapping(final int level) {

		checkFinished();
		int leafLevel = getLeafLevel();
		if (level < 0 || level > leafLevel) {
			throw new IndexOutOfBoundsException("level not in [0 - " + leafLevel + "]");
		}

		int[] orderedIDs = getOrderedLeafIDs();
		int numIDs = orderedIDs.length;
		int[] mapping = new int[numIDs];
		// Set values in mapping to ID positions in orderedIDs
		for (int i=0; i<numIDs; i++) {
			mapping[orderedIDs[i]] = i;
		}

		int levelsToHandle = leafLevel - level;
		if (levelsToHandle > 0) {
			boolean[] handled = new boolean[levelsToHandle];
			for (int lvl=level; lvl<leafLevel; lvl++) {
				if (!handled[lvl-level]) {
					int[] oids = getOrderedLeafIDs(lvl);
					// Find the smallest leaf id.
					int sid = Integer.MAX_VALUE;
					for (int i=0; i<oids.length; i++) {
						if (oids[i] < sid) {
							sid = oids[i];
						}
					}
					if (sid == -1) {
						oids = getOrderedLeafIDs(lvl);
					}
					int smapping = mapping[sid];
					for (int i=0; i<oids.length; i++) {
						mapping[oids[i]] = smapping;
					}
					// Now have to mark child non-leaf levels as having
					// been handled.
					int[] clvls = getChildNonLeafLevels(lvl);
					for (int i=0; i<clvls.length; i++) {
						handled[clvls[i]-level] = true;
					}
				}
			}
		}

		return mapping;
	}

	public int[] getOrderedLeafIDs() {
		checkFinished();
		return getOrderedLeafIDs(0);
	}

	public int[] getOrderedLeafIDs(final int level) {

		if (level < mCurrentLevel || level >= getLeafLevel()) {
		    // Handle the trivial case of a dendrogram for a single point.
		    if ((level == 0) && (level == mCurrentLevel) && (level == getLeafLevel())) {
		        return new int[0];
		    }
			throw new IndexOutOfBoundsException("level not in [" +
					mCurrentLevel + " - (" +
					getLeafLevel() + " - 1)] : " + level);
		}

		IntArrayList rtnList = new IntArrayList();

		// To use as a stack for the next level to traverse.
		IntArrayList intList = new IntArrayList();

		int currentLevel = level;
		int leafLevel = getLeafLevel();

		if (currentLevel < leafLevel) {

			OUTER:
			while (true) {
				int leftLevel = mLeftIndices[currentLevel];
				int rightLevel = mRightIndices[currentLevel];
				if (leftLevel >= leafLevel) { // Encountered a leaf on the left
					rtnList.add(mNodeIDs[leftLevel]);
					if (rightLevel >= leafLevel) { // Encountered a leaf on the right.
						rtnList.add(mNodeIDs[rightLevel]);
						int sz = intList.size();
						INNER:
						while(true) {
							if (sz == 0) break OUTER;
							currentLevel = intList.get(sz - 1);
							intList.removeAt(sz - 1);
							sz--;
							if (currentLevel >= leafLevel) {
								rtnList.add(mNodeIDs[currentLevel]);
							} else {
								break INNER;
							}
						}
					} else { // Right is not a leaf
						currentLevel = rightLevel;
					}
				} else { // Left node is not a leaf
					// Store the right, so we can get back to it later.
					intList.add(rightLevel);
					// Make the left the current level, so it'll be
					// traversed down next.
					currentLevel = leftLevel;
				}
			}

		} else if (mLeafCount == 1) {

			// Must be only one leaf, so the dendrogram starts out
			// finished with one leaf node and no others.
			rtnList.add(mNodeIDs[0]);
		}

		return rtnList.toArray();
	}

	public int[] getChildNonLeafLevels(final int level) {

		if (level < mCurrentLevel || level >= getLeafLevel()) {
			throw new IndexOutOfBoundsException("level not in [" +
					mCurrentLevel + " - (" +
					getLeafLevel() + " - 1)]");
		}

		IntArrayList rtnList = new IntArrayList();

		// To use as a stack for the next level to traverse.
		IntArrayList intList = new IntArrayList();

		int currentLevel = level;
		int leafLevel = getLeafLevel();

		while (true) {

			if (currentLevel > level) {
				rtnList.add(currentLevel);
			}

			int leftLevel = mLeftIndices[currentLevel];
			int rightLevel = mRightIndices[currentLevel];

			if (leftLevel < leafLevel) {
				currentLevel = leftLevel;
				if (rightLevel < leafLevel) {
					intList.add(rightLevel);
				}
			} else if (rightLevel < leafLevel) {
				currentLevel = rightLevel;
			} else {
				int sz = intList.size();
				if (sz > 0) {
					currentLevel = intList.get(sz - 1);
					intList.removeAt(sz - 1);
				} else {
					break;
				}
			}
		}

		return rtnList.toArray();
	}

	/**
	 * Get the ID of the root dendrogram node, which is normally 0.
	 * @return
	 */
	public int getRootID() {
		checkFinished();
		// mCurrentLevel == 0
		return mNodeIDs[mCurrentLevel];
	}

	/**
	 * Get the ID for the non-leaf node at the specified level.
	 * The level must be greater than or equal to the current level and
	 * less than the leaf node level, otherwise -1 is returned.
	 * @param level
	 * @return - the ID for the specified level.
	 */
	public int getLevelID(int level) {
		if (level >= mCurrentLevel && level < mLeftIndices.length) {
			return mNodeIDs[level];
		}
		return -1;
	}

	/**
	 * Get the ID of the left child of the node at the specified
	 * level.
	 *
	 * @param parentLevel - a non-leaf node level greater than or equal to
	 *   the current level.
	 * @return - the left child ID or -1 if parentLevel is not a valid
	 *   non-leaf level.
	 */
	public int getLeftChildID(int parentLevel) {
		if (parentLevel >= mCurrentLevel) {
			return getChildID(parentLevel, mLeftIndices);
		}
		return -1;
	}

	/**
	 * Get the ID of the right child of the node at the specified
	 * level.
	 *
	 * @param parentLevel - a non-leaf node level greater than or equal to
	 *   the current level.
	 * @return - the right child ID or -1 if parentLevel is not a valid
	 *   non-leaf level.
	 */
	public int getRightChildID(int parentLevel) {
		return getChildID(parentLevel, mRightIndices);
	}

	/**
	 * Get the level (not the ID) of the left child of the node at the specified
	 * level.
	 *
	 * @param parentLevel - a non-leaf node level greater than or equal to
	 *   the current level.
	 * @return - the left child level or -1 if parentLevel is not a valid
	 *   non-leaf level.
	 */
	public int getLeftChildLevel(int parentLevel) {
		return getChildLevel(parentLevel, mLeftIndices);
	}

	/**
	 * Get the level (not the ID) of the right child of the node at the specified
	 * level.
	 *
	 * @param parentLevel - a non-leaf node level greater than or equal to
	 *   the current level.
	 * @return - the right child level or -1 if parentLevel is not a valid
	 *   non-leaf level.
	 */
	public int getRightChildLevel(int parentLevel) {
		return getChildLevel(parentLevel, mRightIndices);
	}

	// Gets the left or right child id of the non-leaf node at the specified
	// level.
	private int getChildID(int parentLevel, int[] childIndices) {
		if (parentLevel >= mCurrentLevel && parentLevel < childIndices.length) {
			return mNodeIDs[childIndices[parentLevel]];
		}
		return -1;
	}

	// Gets the level of the left or right child of the non-leaf node at the
	// specified level.
	private int getChildLevel(int parentLevel, int[] childIndices) {
		if (parentLevel >= mCurrentLevel && parentLevel < childIndices.length) {
			int childLevel = childIndices[parentLevel];
			if (childLevel > childIndices.length) {
				childLevel = childIndices.length;
			}
			return childLevel;
		}
		return -1;
	}

	/**
	 * Get the dendrogram's root node.  This may only be called when
	 * <code>isFinished()</code> returns true.
	 * @return - the root node.
	 * @exception - IllegalStateException if the dendrogram is not finished.
	 */
	public Node getRoot() {
		checkFinished();
		return new Node(0, mNodeIDs[0]);
	}

	/**
	 * Get the non-leaf node for the specified level.  The specified
	 * level must be in the range <code>[0 - (getLeafLevel() - 1)]</code>.
	 *
	 * @param level
	 * @return - the non-leaf node for the specified level.
	 */
	public Node getNode(int level) {
		checkFinished();
		if (level >= mCurrentLevel && level < getLeafLevel()) {
			return new Node(level, mNodeIDs[level]);
		}
		throw new IndexOutOfBoundsException("level not in [" +
				mCurrentLevel + " - (" + getLeafLevel() + " - 1)]: " +
				level);
	}

	/**
	 * Get the leaf node level, which is equal to the number of
	 * leaves minus one.
	 * @return - the leaf node level.
	 */
	public int getLeafLevel() {
		return mLeafCount - 1;
	}

	/**
	 * Exchanges the left and right children of the node at the specified level.
	 * @param parentLevel
	 * @return true - if the children were exchanged, which is always true if
	 *   parentLevel was greater than or equal to the current level
	 *   and less than the leaf node level.
	 */
	public boolean flipChildren(int parentLevel,
			ReadOnlyDistanceCache cache, double[] distChange) throws IOException {

		if (parentLevel >= mCurrentLevel && parentLevel < mLeftIndices.length) {

			boolean computeDistChange = cache != null && distChange != null;
			double dsub = 0.0;

			if (computeDistChange) {
				dsub = flipDistanceSum(parentLevel, cache);
			}

			int tmp = mLeftIndices[parentLevel];
			mLeftIndices[parentLevel] = mRightIndices[parentLevel];
			mRightIndices[parentLevel] = tmp;

			if (computeDistChange) {
				distChange[0] = flipDistanceSum(parentLevel, cache) - dsub;
			}

			return true;
		}

		return false;
	}


	private double flipDistanceSum(int parentLevel, ReadOnlyDistanceCache cache)
	throws IOException {

		double d = 0.0;

		int leftMostID = -1, middleLeftID = -1, rightMostID = -1, middleRightID = -1;
		int childLevel = -1;

		leftMostID = getLeftMostLeafID(parentLevel);
		rightMostID = getRightMostLeafID(parentLevel);

		childLevel = mLeftIndices[parentLevel];
		if (childLevel < mLeftIndices.length) {
			// Left child is not a leaf, so the middle left
			// is its rightmost descendent.
			middleLeftID = getRightMostLeafID(childLevel);
		} else {
		    // The middle left and the left are one and the same,
			// since the left child is a leaf.
			middleLeftID = leftMostID;
		}

		childLevel = mRightIndices[parentLevel];
		if (childLevel < mRightIndices.length) {
			// Right child is not a leaf, so the middle right
			// is its leftmost descendent.
			middleRightID = getLeftMostLeafID(childLevel);
		} else {
		    // The middle right and the right are one and the same,
			// since the right child is a leaf.
			middleRightID = rightMostID;
		}

		int neighborID = -1;

		// The neighbor of the leftmost leaf, which isn't
		// a descendent of parentLevel.
		neighborID = leftNeighborLeafID(leftMostID);
		if (neighborID >= 0) { // It might not have a neighbor.
			d += cache.getDistance(leftMostID, neighborID);
		}

		// Add the distance of the two leaves in the middle.
		d += cache.getDistance(middleLeftID, middleRightID);

		// The neighbor of the rightmost leaf, which isn't
		// a descendent of parentLevel.
		neighborID = rightNeighborLeafID(rightMostID);
		if (neighborID >= 0) { // Might not have one...
			d += cache.getDistance(rightMostID, neighborID);
		}

		return d;
	}

	/**
	 * Exchanges the grand children of the node at the specified level without
	 * exchanging the children of the node.  Thus,
	 * <code>
	 * boolean flipped = dendrogram.flipGrandChildren(grandParentLevel)
	 * </code>
	 * has the same effect as:
	 * <code>
	 * boolean flipped =
	 *   dendrogram.flipChildren(dendrogram.getLeftChildLevel(grandParentLevel)) ||
	 *   dendrogram.flipChildren(dendrogram.getRightChildLevel(grandParentLevel))
	 * </code>
	 * @param parentLevel
	 * @return true - if at least one pair of grandchildren were exchanged.
	 */
	public boolean flipGrandChildren(int grandParentLevel,
			ReadOnlyDistanceCache cache, double[] distChange) throws IOException {
		boolean rtn = false;
		if (grandParentLevel >= mCurrentLevel && grandParentLevel < mLeftIndices.length) {
			double dchange = 0.0;
			boolean computeDistChange = cache != null && distChange != null;
			if (flipChildren(mLeftIndices[grandParentLevel], cache, distChange)) {
				rtn = true;
				if (computeDistChange) {
					dchange += distChange[0];
				}
			}
			if (flipChildren(mRightIndices[grandParentLevel], cache, distChange)) {
				rtn = true;
				if (computeDistChange) {
					dchange += distChange[0];
				}
			}
//			if (flipChildren(grandParentLevel, cache, distChange)) {
//				rtn = true;
//				if (computeDistChange) {
//					dchange += distChange[0];
//				}
//			}
			if (rtn && computeDistChange) {
				distChange[0] = dchange;
			}

		}
		return rtn;
	}

	public double distancesBetweenLeaves (ReadOnlyDistanceCache cache) throws IOException {
		// Can only iterate down to all the leaf ids if the
		// dendrogram has been completely merged.
		checkFinished();

		double total = 0.0;

		int leafLevel = mLeafCount - 1;

		// If the dendrogram is finished and the current level is the
		// leaf level, the dendrogram was made with a leaf count of 1.
		// This is the trivial case...
		if (mCurrentLevel != leafLevel) {

			int currentLevel = mCurrentLevel;
			IntArrayList intList = new IntArrayList();

			while(true) {

				// Want the ids of the rightmost leaf on the
				// left and the leftmost leaf on the right.
				int leftLevel = getLeftChildLevel(currentLevel);
				int rightLevel = getRightChildLevel(currentLevel);

				int leftID = (leftLevel == leafLevel ? getLeftChildID(currentLevel) :
					getRightMostLeafID(leftLevel));
				int rightID = (rightLevel == leafLevel ? getRightChildID(currentLevel) :
					getLeftMostLeafID(rightLevel));

				total += cache.getDistance(leftID, rightID);

				if (!(leftLevel == leafLevel)) {
					if (!(rightLevel == leafLevel)) {
						intList.add(rightLevel);
					}
					currentLevel = leftLevel;
				} else if (!(rightLevel == leafLevel)) {
					currentLevel = rightLevel;
				} else if (!intList.isEmpty()) {
					int sz = intList.size();
					currentLevel = intList.get(sz - 1);
					intList.removeAt(sz - 1);
				} else {
					// No more levels to iterate through.
					break;
				}

			} // while

		}

		return total;
	}

	public int getRightMostLeafID(int parentLevel) {
		while(parentLevel < mRightIndices.length) {
			parentLevel = mRightIndices[parentLevel];
		}
		return mNodeIDs[parentLevel];
	}

	public int getLeftMostLeafID(int parentLevel) {
		while(parentLevel < mLeftIndices.length) {
			parentLevel = mLeftIndices[parentLevel];
		}
		return mNodeIDs[parentLevel];
	}


	/**
	 * Merge the nodes identified by id1 and id2 using the specified distance as
	 * the decision distance.
	 *
	 * @param id1
	 * @param id2
	 * @param distance
	 * @return - the ID of the new merged node, which is always the minimum of
	 *   id1 and id2.
	 */
	public int mergeNodes(int id1, int id2, double distance) {

		if (mCurrentLevel == 0) {
			throw new IllegalStateException("dendrogram is already finished");
		}

		int mergeID = Math.min(id1, id2);

		mCurrentLevel--;
		mNodeIDs[mCurrentLevel] = mergeID;
		int leftIndex = mIndicesForIDs[id1];
		int rightIndex = mIndicesForIDs[id2];
		mLeftIndices[mCurrentLevel] = leftIndex;
		mRightIndices[mCurrentLevel] = rightIndex;
		mParentIndices[leftIndex] = mCurrentLevel;
		mParentIndices[rightIndex] = mCurrentLevel;
		mDistances[mCurrentLevel] = distance;
		mSizes[mCurrentLevel] = nodeSize(id1) + nodeSize(id2);

		mIndicesForIDs[mergeID] = mCurrentLevel;

		return mergeID;
	}

	public int leftChildID(int parentID) {
		int parentIndex = mIndicesForIDs[parentID];
		return mNodeIDs[mLeftIndices[parentIndex]];
	}

	public int rightChildID(int parentID) {
		int parentIndex = mIndicesForIDs[parentID];
		return mNodeIDs[mRightIndices[parentIndex]];
	}

	/**
	 * Find the id of the neighbor leaf immediately to the right
	 * of the leaf with the specified id.
	 * @param id
	 * @return - the id of the neighbor leaf on the right or -1 if
	 *   there is no neighbor on the right.
	 */
	public int rightNeighborLeafID(int id) {
		return neighborID(id, mRightIndices, mLeftIndices);
	}

	/**
	 * Find the id of the neighbor leaf immediately to the left
	 * of the leaf with the specified id.
	 * @param id
	 * @return - the id of the neighbor leaf on the left or -1 if
	 *   there is no neighbor on the left.
	 */
	public int leftNeighborLeafID(int id) {
		return neighborID(id, mLeftIndices, mRightIndices);
	}

	private int neighborID(int id, int[] indices1, int[] indices2) {
		if (id >= 0 && id < mLeafCount) {
			// Start off with lastIndex being the leaf index for id, and
			// index being the parent level of the leaf with id.
			int lastIndex = mLeafCount - 1 + id;
			int index = mParentIndices[lastIndex];
			// If looking for the left neighbor id, we tranverse up the
			// parent hierarchy until we've gone one level to the left.
			// Then we go down one level to the left, then down to the right
			// until a leaf is encountered. (Draw a diagram of a dendrogram
			// and trace the path with a pen -- you'll understand.)
			while (index >= 0 && indices1[index] == lastIndex) {
				lastIndex = index;
				index = mParentIndices[index];
			}
			// If there is no neighbor index will be -1 here.
			if (index >= mCurrentLevel) {
				// Go down one level to the left if looking for a left neighbor.
				index = indices1[index];
				// Until a leaf is encounted, go down to the right.
				while (index < indices2.length) {
					index = indices2[index];
				}
				// Return the leaf id.
				return mNodeIDs[index];
			}
		}
		// Either the id was out of range or it had no neighbor on the
		// side requested.
		return -1;
	}

	public int nodeSize(int id) {
		int index = mIndicesForIDs[id];
		return index < mSizes.length ? mSizes[index] : 1;
	}

	public void computeCoherences() {
		
		checkFinished();
		
		double maxd = 0.0;
		
		if (!Double.isNaN(mMaxCoherenceThreshold)) {
			maxd = mMaxCoherenceThreshold;
		} else {
			for (int i = 0; i < mDistances.length; i++) {
				if (maxd < mDistances[i]) {
					maxd = mDistances[i];
				}
			}
		}
		
		double mind = 0.0;
		
		if (!Double.isNaN(mMinCoherenceThreshold)) {
			mind = mMinCoherenceThreshold;
		}
		
		if (maxd > 0.0) {
			
			double denom = maxd - mind;
			
			for (int i = 0; i < mDistances.length; i++) {
				// Coherences will range from 0.0 to 1.0. 1.0 means
				// the decision distance was 0.0, such as when merging
				// 2 identical coordinates. The coherence is 0.0 for
				// the node with the maximum decision distance.
				// The usual case is for the root node to have a
				// coherence of 0.0.
				mCoherences[i] = 1.0 - (mDistances[i] - mind)/denom;
			}
			
		} else {
			// All decision distances are 0.0, meaning all coordinates
			// are the same. Just set all the coherences to their max
			// value 1.0
			Arrays.fill(mCoherences, 1.0);
		}
		
		mCoherencesComputed = true;
	}

	/**
	 * Get the number of IDs used to create this dendrogram. This is also the
	 * number of levels in a finished dendrogram.
	 *
	 * @return
	 */
	public int getLeafCount() {
		return mLeafCount;
	}

	/**
	 * Has the dendrogram been completely formed? That is, have the nodes been
	 * merged until the current level is 0?
	 *
	 * @return
	 */
	public boolean isFinished() {
		return mCurrentLevel == 0;
	}

	/**
	 * Get the current level of the dendrogram, which ranges from
	 * <code>getLeafCount() - 1</code> to 0.
	 *
	 * @return
	 */
	public int getCurrentLevel() {
		return mCurrentLevel;
	}

	public int[] getNodeIDs(int index) {

		int nonLeafCount = mLeafCount - 1;

		int n = index < nonLeafCount ? mSizes[index] : 1;

		int[] rtn = new int[n];
		IntArrayList intList = null;
		if (n > 1) {
			intList = new IntArrayList();
		}

		int count = 0;
		int currentIndex = index;
		while (count < n) {
			if (currentIndex < nonLeafCount) {
				intList.add(mRightIndices[currentIndex]);
				currentIndex = mLeftIndices[currentIndex];
			} else {
				rtn[count++] = mNodeIDs[currentIndex];
				int lastIndex = intList != null ? intList.size() - 1 : -1;
				if (lastIndex >= 0) {
					currentIndex = intList.get(lastIndex);
					intList.removeAt(lastIndex);
				}
			}
		}

		return rtn;
	}

	public synchronized List<int[]> generateClusterGroupings(int clustersDesired) {
	    
            // Ensures current level == 0.
            checkFinished();

            if (clustersDesired <= 0 || clustersDesired > mLeafCount) {
                    throw new IllegalArgumentException("clusters desired not in [0 - ("
                                    + mLeafCount + " - 1)]: " + clustersDesired);
            }

            int nodeCount = mNodeIDs.length;
            BitVector bits = new BitVector(nodeCount);

            List<int[]> clusters = new ArrayList<int[]>(clustersDesired);
            
            int currentIndex = clustersDesired - 1;

            IntArrayList intList = new IntArrayList();

            for (int i = currentIndex; i < nodeCount; i++) {
                    if (!bits.get(i)) { // If an ancestor has not been turned into a
                            // cluster...
                            clusters.add(getNodeIDs(i));
                            // While loop is to keep descendents of node just turned into a
                            // cluster
                            // from also being turned into clusters.
                            int ci = i;
                            while (true) {
                                    if (ci < mLeftIndices.length) {
                                            bits.set(mLeftIndices[ci]);
                                            bits.set(mRightIndices[ci]);
                                            intList.add(mRightIndices[ci]);
                                            ci = mLeftIndices[ci];
                                    } else {
                                            int lastIndex = intList.size() - 1;
                                            if (lastIndex >= 0) {
                                                    ci = intList.get(lastIndex);
                                                    intList.removeAt(lastIndex);
                                            } else {
                                                    break;
                                            }
                                    }
                            }
                    }
            }

            return clusters;
	}
	
	public synchronized ClusterList generateClusters(int clustersDesired,
			CoordinateList cs) {

		// Ensures current level == 0.
		checkFinished();

		if (mLeafCount != cs.getCoordinateCount()) {
			throw new IllegalArgumentException(
					"dendrogram does not match coordinates: leaf node count = "
							+ mLeafCount + ", coordinate count = "
							+ cs.getCoordinateCount());
		}

		List<int[]> clusterGroups = generateClusterGroupings(clustersDesired);
		int numClusters = clusterGroups.size();
		
		Cluster[] clusters = new Cluster[numClusters];
		for (int i=0; i<numClusters; i++) {
		    clusters[i] = new Cluster(clusterGroups.get(i), cs);
		}

		return new ClusterList(clusters);
	}
	
	public synchronized ClusterList generateOptimalClusters(CoordinateList cs) {

	    // Ensures current level == 0.
        checkFinished();

        if (mLeafCount != cs.getCoordinateCount()) {
            throw new IllegalArgumentException(
                    "dendrogram does not match coordinates: leaf node count = "
                            + mLeafCount + ", coordinate count = "
                            + cs.getCoordinateCount());
        }     
        
        final int coordCount = cs.getCoordinateCount();
        
        double maxBIC = -Double.MAX_VALUE;
        ClusterList bestClusters = null;
        
        for (int numClusters = 1; numClusters <= coordCount; numClusters++) {
            ClusterList clusters = generateClusters(numClusters, cs);
            double bic = ClusterStats.computeBIC(cs, clusters);
            //System.err.printf("%d clusters, BIC = %f\n", numClusters, bic);
            if (bic > maxBIC) {
                maxBIC = bic;
                bestClusters = clusters;
            } else if (bic < 0.0 || maxBIC/bic >= 2.0) {
                break;
            }
        }
	    
        return bestClusters;
	}
	
	public int clustersWithCoherenceExceeding(double coherence) {

		checkFinished();

		// Ensure coherence is within valid range.
		if (coherence < 0.0 || coherence > 1.0) {
			throw new IllegalArgumentException("coherence not in [0.0 - 1.0]: "
					+ coherence);
		}
		
		if (!mCoherencesComputed) {
			computeCoherences();
		}

		int nonLeafCount = mLeafCount - 1;

		int clusters = mLeafCount;
		for (int i = 0; i < nonLeafCount; i++) {
			if (mCoherences[i] >= coherence) {
				clusters = i + 1;
				break;
			}
		}

		return clusters;
	}

	private void checkFinished() {
		if (!isFinished()) {
			throw new IllegalStateException("dendrogram is not finished");
		}
	}

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(EXTERNALIZABLE_VERSION);
        writeIntArray(out, this.mNodeIDs);
        writeIntArray(out, this.mParentIndices);
        writeIntArray(out, this.mLeftIndices);
        writeIntArray(out, this.mRightIndices);
        writeIntArray(out, this.mSizes);
        writeIntArray(out, this.mIndicesForIDs);
        writeDoubleArray(out, this.mDistances);
        writeDoubleArray(out, this.mCoherences);
        out.writeInt(mLeafCount);
        out.writeInt(mCurrentLevel);
    }
    
    @Override
    public void readExternal(ObjectInput in) throws IOException,
            ClassNotFoundException {
        int version = in.readInt();
        if (version != EXTERNALIZABLE_VERSION) {
            throw new IOException("invalid version: " + version);
        }
        this.mNodeIDs = readIntArray(in);
        this.mParentIndices = readIntArray(in);
        this.mLeftIndices = readIntArray(in);
        this.mRightIndices = readIntArray(in);
        this.mSizes = readIntArray(in);
        this.mIndicesForIDs = readIntArray(in);
        this.mDistances = readDoubleArray(in);
        this.mCoherences = readDoubleArray(in);
        this.mLeafCount = in.readInt();
        this.mCurrentLevel = in.readInt();
    }

	public class Node {

		private int mLevel;
		private int mID;

		private Node(int level, int id) {
			mLevel = level;
			mID = id;
		}

		public boolean isRoot() {
			return mLevel == 0;
		}

		public boolean isLeaf() {
			return mLevel == mLeafCount - 1;
		}

		public int getLevel() {
			return mLevel;
		}

		public int getID() {
			return mID;
		}

		public Node leftChild() {
			if (!isLeaf()) {
				return new Node(getLeftChildLevel(mLevel), getLeftChildID(mLevel));
			}
			return null;
		}

		public Node rightChild() {
			if (!isLeaf()) {
				return new Node(getRightChildLevel(mLevel), getRightChildID(mLevel));
			}
			return null;
		}

		public double distance() {
			return isLeaf() ? Double.NaN : mDistances[mLevel];
		}

		public double coherence() {
			return isLeaf() ? Double.NaN : mCoherences[mLevel];
		}

	}

}
