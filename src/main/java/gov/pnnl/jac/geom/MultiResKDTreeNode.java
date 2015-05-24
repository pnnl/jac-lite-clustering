package gov.pnnl.jac.geom;

import gov.pnnl.jac.util.SortUtils;

import java.util.*;
import cern.colt.list.*;

/**
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: </p>
 *
 * @author not attributable
 * @version 1.0
 */
public class MultiResKDTreeNode {

    private CoordinateList mCS;

    private int mLevel;
    private HyperRect mRect;
    private int[] mIndices;
    private double[] mCenter;

    private int mSplitDim; 		// Dimension used for splitting.
    private double mSplitValue; // Value used for splitting.

    private MultiResKDTreeNode mLeft, mRight;

    /**
     * Fully-qualified constructor.  To build the root node, pass null
     * for the indices and zero for the level.  For non-root nodes (level > 0),
     * indices must be non-null.
     * @param cs CoordinateSet
     * @param indices int[]
     * @param level int
     */
    private MultiResKDTreeNode(CoordinateList cs, int[] indices, int level) {
        int numCoords = 0;
        if (indices != null) {
            numCoords = indices.length;
            mIndices = indices;
        } else { // Root node
            if (level != 0) {
                throw new IllegalArgumentException(
                        "indices must be supplied for non-root nodes");
            }
            numCoords = cs.getCoordinateCount();
            mIndices = new int[numCoords];
            for (int i=0; i<numCoords; i++) {
                mIndices[i] = i;
            }
        }
        if (numCoords == 0) {
            throw new IllegalArgumentException("zero coordinates");
        }
        if (level < 0) {
            throw new IllegalArgumentException("level cannot be negative");
        }
        
        mCS = cs;
        mLevel = level;
        
        double[] max = cs.computeMaximum(mIndices, null);
        double[] min = cs.computeMinimum(mIndices, null);

        mRect = new HyperRect(min, max);

        // Initialize to value that indicates the node cannot be split.
        mSplitDim = -1;

        if (!mRect.isPoint()) {

        	int dim = max.length;
        	double[] spreads = new double[dim];
        	int[] dims = new int[dim];
        	for (int i=0; i<dim; i++) {
        		spreads[i] = max[i] - min[i];
        		dims[i] = i;
        	}
        
        	// This sorts the spreads, rearranging dims to keep the 1:1
        	// correspondence.
        	SortUtils.parallelSort(spreads, dims);

        	int splitDim = -1;
        	
        	// Now try to determine the optimal split dimension, which should be 
        	// the one with the maximum spread.
        	for (int i=dim-1; i>=0; i--) {
        		// NaN sorts out as the largest.
        		if (!Double.isNaN(spreads[i])) {
        			if (spreads[i] > 0.0) {
        				splitDim = i;
        			}
        			break;
        		}
        	}
        	
        	// It can be split.
        	if (splitDim >= 0) {
        		double median = mCS.computeMedian(mIndices, splitDim);
        		if (!Double.isNaN(median) && (median < max[splitDim])) {
        			mSplitDim = splitDim;
        			mSplitValue = median;
        		}
        	}
        }
        
        mCenter = cs.computeAverage(mIndices, null);
    }

    /**
     * Root node constructor
     * @param cs CoordinateSet
     */
    public static MultiResKDTreeNode createKDTree(CoordinateList cs, int[] indices, int splits) {
        MultiResKDTreeNode root = new MultiResKDTreeNode(cs, indices, 0);
        root.split(splits);
        return root;
    }

    public MultiResKDTreeNode[] getLeafNodes(int maxLevel) {
        List<MultiResKDTreeNode> nodeList = new ArrayList<MultiResKDTreeNode>();
        _getLeafNodes(nodeList, maxLevel);
        int sz = nodeList.size();
        MultiResKDTreeNode[] rtn = new MultiResKDTreeNode[sz];
        nodeList.toArray(rtn);
        return rtn;
    }

    private void _getLeafNodes(List<MultiResKDTreeNode> nodeList, int maxLevel) {
        if (mLevel <= maxLevel) {
            if (isLeaf()) {
                nodeList.add(this);
            } else if (mLevel < maxLevel) {
                if (mLeft != null) {
                    mLeft._getLeafNodes(nodeList, maxLevel);
                    mRight._getLeafNodes(nodeList, maxLevel);
                }
            }
        }
    }

    public MultiResKDTreeNode[] getNodesAtLevel(int level) {
        List<MultiResKDTreeNode> nodeList = new ArrayList<MultiResKDTreeNode>();
        _getNodesAtLevel(level, nodeList);
        int sz = nodeList.size();
        MultiResKDTreeNode[] rtn = new MultiResKDTreeNode[sz];
        nodeList.toArray(rtn);
        return rtn;
    }

    private void _getNodesAtLevel(int level, List<MultiResKDTreeNode> nodeList) {
        if (level == mLevel) {
            nodeList.add(this);
        } else if (level > mLevel) {
            if (mLeft != null) {
                mLeft._getNodesAtLevel(level, nodeList);
                mRight._getNodesAtLevel(level, nodeList);
            }
        }
    }

    public int getLevel() {
        return mLevel;
    }

    public MultiResKDTreeNode getLeft() {
        return mLeft;
    }

    public MultiResKDTreeNode getRight() {
        return mRight;
    }

    public boolean isLeaf() {
    	return mSplitDim == -1;
    }

    public boolean isSplit() {
        return mLeft != null;
    }

    public int getSize() {
    	return mIndices != null ? mIndices.length : 0;
    }
    
    public int[] getIndices() {
    	int sz = getSize();
    	int[] rtn = new int[sz];
    	if (sz > 0) {
    		System.arraycopy(mIndices, 0, rtn, 0, sz);
    	}
    	return rtn;
    }
    
    public double[] getCenter(double[] buffer) {
        double[] rtn = null;
        if (buffer != null) {
            if (buffer.length != mCenter.length) {
                throw new IllegalArgumentException(String.valueOf(buffer.length) + " != " + mCenter.length);
            }
            rtn = buffer;
        } else {
            rtn = new double[mCenter.length];
        }
        System.arraycopy(mCenter, 0, rtn, 0, rtn.length);
        return rtn;
    }

    public void split(int splits) {

    	if (splits <= 0 || isLeaf()) {
            return;
        }

        if (!isSplit()) { // Don't resplit if already split.
        	            
            IntArrayList leftList = new IntArrayList();
            IntArrayList rightList = new IntArrayList();
            
            int n = mIndices.length;
            
            for (int i=0; i<n; i++) {
                int ndx = mIndices[i];
                if (mCS.getCoordinate(ndx, mSplitDim) <= mSplitValue) {
                    leftList.add(ndx);
                } else {
                    rightList.add(ndx);
                }
            }
            
            leftList.trimToSize();
            rightList.trimToSize();
            
            int[] leftElements = leftList.elements();
            int[] rightElements = rightList.elements();
            
            if (leftElements.length == 0 || rightElements.length == 0) {
            	System.err.println("got a problem...");
            }
            
            mLeft = new MultiResKDTreeNode(mCS, leftElements, mLevel + 1);
            mRight = new MultiResKDTreeNode(mCS, rightElements, mLevel + 1);
        }
        
        splits--;
        
        if (splits > 0) {
            if (!mLeft.isLeaf()) {
                mLeft.split(splits);
            }
            if (!mRight.isLeaf()) {
                mRight.split(splits);
            }
        }
    }

//    public static void main(String[] args) {
//        try {
//            CoordinateSet cs = gov.pnnl.misc.VectorFileImporter.importAsCoordinateSet(args[0]);
//            int numCoords = cs.getCoordinateCount();
//            int numClusters = numCoords/100;
//            int splits = (int) Math.ceil(Math.log(numClusters)/Math.log(2));
//            MultiResKDTreeNode node = new MultiResKDTreeNode(cs, Integer.MAX_VALUE);
//            MultiResKDTreeNode[] bottomNodes = node.getNodesAtLevel(splits);
//            System.out.println("number of coordinates = " + numCoords);
//            System.out.println("number of clusters    = " + numClusters);
//            System.out.println("number of nodes       = " + bottomNodes.length);
//        } catch (Exception ioe) {
//            ioe.printStackTrace();
//        }
//    }
}
