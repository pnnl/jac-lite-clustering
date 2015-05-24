package gov.pnnl.jac.geom;

import gov.pnnl.jac.util.*;
import cern.colt.list.*;
import cern.colt.map.*;
import java.util.Arrays;

public class OptimizedKDTree {

	public static final int DEFAULT_SPLIT_THRESHOLD = 100;
	
    private CoordinateList mCS;
    private double[] mInsertBuf;
    private int mCoordCount, mDimensions;
    private int mSplitThreshold = DEFAULT_SPLIT_THRESHOLD;
    
    // The following 6 arrays are always the same length.
    // 
    // nonnegative values in mNodes are either coordinate indices or
    // keys into mCompositeNodes.  If the corresponding element
    // of mSplitDims == -1, the value in mNodes is an index into 
    // mCompositeNodes.
    private int[] mNodes;
    // The dimensions [0 - (dimensions - 1)] used to split
    // the nodes at the corresponding indices.  -1 indicates the
    // node hasn't been split.
    private int[] mSplitDims;
    // For each element n of mSplitDims such that mSplitDims[n] >= 0,
    // mSplitValues[n] holds the comparison value.
    private double[] mSplitValues;
    // Indices into mNodes of the left child nodes.
    private int[] mLefts;
    // Indices into mNodes of the right child nodes.
    private int[] mRights;
    
    // Both the number of entries in the previous 6 arrays and
    // the next valid index for additions.
    private int mCount;
    
    // Map containing the composite nodes.  Keys are ints (indices into mNodes).
    private OpenIntObjectHashMap mCompositeNodes = new OpenIntObjectHashMap();

    // Incremented whenever another CompositeNode is instantiated, so keys in
    // mCompositeNodes stay unique.
    private int mMaxCompositeNodeKey = 0;

    public OptimizedKDTree(CoordinateList cs, int splitThreshold) {
        mCS = cs;
        mCoordCount = mCS.getCoordinateCount();
        mDimensions = mCS.getDimensionCount();
        mInsertBuf = new double[mDimensions];
        mSplitThreshold = splitThreshold;
        ensureCapacity(100);
    }

    public static OptimizedKDTree forCoordinateSet(CoordinateList cs, int splitThreshold) {
        OptimizedKDTree kd = new OptimizedKDTree(cs, splitThreshold);
        int numCoords = cs.getCoordinateCount();
        for (int i=0; i<numCoords; i++) {
            kd.insert(i);
        }
        return kd;
    }

    public CoordinateList getCoordinateSet() {
        return mCS;
    }

    public int getSplitThreshold() {
    	return mSplitThreshold;
    }
    
    public void setSplitThreshold(int splitThreshold) {
    	
    	if (splitThreshold <= 0) {
    		throw new IllegalArgumentException(
    				"split threshold must be positive: " + 
    				splitThreshold);
    	}
    	
    	int oldSplitThreshold = mSplitThreshold;
    	mSplitThreshold = splitThreshold;
    	
    	if (mSplitThreshold < oldSplitThreshold) {
    		// May have to split some nodes.
    		for (int i=0; i<mCount; i++) {
    			if (mSplitDims[i] == -1) {
    				CompositeNode compositeNode = 
    					(CompositeNode) mCompositeNodes.get(mNodes[i]);
    				if (compositeNode.size() > mSplitThreshold) {
    					mCompositeNodes.removeKey(mNodes[i]);
    					CompositeNodeSplit nodeSplit = compositeNode.split(mCS);
    					mNodes[i] = nodeSplit.getMedianIndex();
    					mSplitDims[i] = nodeSplit.getSplitDim();
    					mSplitValues[i] = nodeSplit.getSplitValue();
    					CompositeNode leftNode = nodeSplit.getLeftNode();
    					if (leftNode != null) {
							ensureCapacity(mCount+1);
    						if (leftNode.size() == 1 && mSplitThreshold == 1) {
    							int[] ids = leftNode.elements();
    							mNodes[mCount] = ids[0];
    							int sd = mSplitDims[i] + 1;
    							if (sd == mDimensions) {
    								sd = 0;
    							}
    							mSplitDims[mCount] = sd;
    							mSplitValues[mCount] = mCS.getCoordinate(mNodes[i], sd);
    						} else {
    							mMaxCompositeNodeKey++;
    							mNodes[mCount] = mMaxCompositeNodeKey;
    							mCompositeNodes.put(mMaxCompositeNodeKey, leftNode);
    						}
							mLefts[i] = mCount;
							mCount++;
    					}
    					CompositeNode rightNode = nodeSplit.getRightNode();
    					if (rightNode != null) {
            		    	ensureCapacity(mCount+1);
    						if (rightNode.size() == 1 && mSplitThreshold == 1) {
    							int[] ids = rightNode.elements();
    							mNodes[mCount] = ids[0];
    							int sd = mSplitDims[i] + 1;
    							if (sd == mDimensions) {
    								sd = 0;
    							}
    							mSplitDims[mCount] = sd;
    							mSplitValues[mCount] = mCS.getCoordinate(mNodes[i], sd);
    						} else {
    							mMaxCompositeNodeKey++;
    							mNodes[mCount] = mMaxCompositeNodeKey;
    							mCompositeNodes.put(mMaxCompositeNodeKey, rightNode);
    						}
							mRights[i] = mCount;
            		    	mCount++;
    					}
    				}
    			}
    		}
    	}
    }
    
    /**
     * Reduces the memory footprint of the kd-tree by shrinking 
     * internal data structures down to their minimum sizes.
     * This method is best called after all coordinates have
     * been added to the kd-tree, since further coordinate 
     * insertions will cause internal data structures to grow again.
     */
    public void trimFootprint() {
    	int curCap = currentCapacity();
    	if (curCap > mCount) {
    		int[] newNodes = new int[mCount];
    		System.arraycopy(mNodes, 0, newNodes, 0, mCount);
    		mNodes = newNodes;
            int[] newLefts = new int[mCount];
            System.arraycopy(mLefts, 0, newLefts, 0, mCount);
            mLefts = newLefts;
            int[] newRights = new int[mCount];
            System.arraycopy(mRights, 0, newRights, 0, mCount);
            mRights = newRights;
            int[] newSplitDims = new int[mCount];
            System.arraycopy(mSplitDims, 0, newSplitDims, 0, mCount);
            mSplitDims = newSplitDims;
            double[] newSplitValues = new double[mCount];
            System.arraycopy(mSplitValues, 0, newSplitValues, 0, mCount);
            mSplitValues = newSplitValues;
    	}
    	IntArrayList keyList = mCompositeNodes.keys();
    	int sz = keyList.size();
    	OpenIntObjectHashMap newCompositeNodes = new OpenIntObjectHashMap(2*Math.max(sz, 1));
    	for (int i=0; i<sz; i++) {
    		int key = keyList.get(i);
    		newCompositeNodes.put(key, mCompositeNodes.get(key));
    	}
    	mCompositeNodes = newCompositeNodes;
    }
    
    private void ensureCapacity(int minCap) {
        int curCap = currentCapacity();
        if (curCap < minCap) {
            int newCap = Math.max(curCap*2, minCap);
            int[] newNodes = new int[newCap];
            int[] newLefts = new int[newCap];
            int[] newRights = new int[newCap];
            int[] newSplitDims = new int[newCap];
            double[] newSplitValues = new double[newCap];
            if (curCap > 0) {
                System.arraycopy(mNodes, 0, newNodes, 0, curCap);
                System.arraycopy(mLefts, 0, newLefts, 0, curCap);
                System.arraycopy(mRights, 0, newRights, 0, curCap);
                System.arraycopy(mSplitDims, 0, newSplitDims, 0, curCap);
                System.arraycopy(mSplitValues, 0, newSplitValues, 0, curCap);
            }
            Arrays.fill(newNodes, curCap, newCap, -1);
            Arrays.fill(newLefts, curCap, newCap, -1);
            Arrays.fill(newRights, curCap, newCap, -1);
            Arrays.fill(newSplitDims, curCap, newCap, -1);
            mNodes = newNodes;
            mLefts = newLefts;
            mRights = newRights;
            mSplitDims = newSplitDims;
            mSplitValues = newSplitValues;
        }
    }

    private int currentCapacity() {
        return mNodes != null ? mNodes.length : 0;
    }

    private void newNodeOnLeft(int parentIndex, int ndx) {
    	ensureCapacity(mCount+1);
    	if (mSplitThreshold > 1) {
    		mMaxCompositeNodeKey++;
    		mNodes[mCount] = mMaxCompositeNodeKey;
    		CompositeNode newNode = new CompositeNode(mDimensions);
    		mCS.getCoordinates(ndx, mInsertBuf);
    		newNode.add(ndx, mInsertBuf);
    		mCompositeNodes.put(mMaxCompositeNodeKey, newNode);
    	} else { // Composite nodes not permitted, because split threshold <= 1
    		mNodes[mCount] = ndx;
    		int sd = mSplitDims[parentIndex] + 1;
    		if (sd == mDimensions) {
    			sd = 0;
    		}
    		mSplitDims[mCount] = sd;
    		mSplitValues[mCount] = mCS.getCoordinate(ndx, sd);
    	}
    	mLefts[parentIndex] = mCount;
    	mCount++;
    }

    private void newNodeOnRight(int parentIndex, int ndx) {
    	ensureCapacity(mCount+1);
    	if (mSplitThreshold > 1) {
    		mMaxCompositeNodeKey++;
    		mNodes[mCount] = mMaxCompositeNodeKey;
    		CompositeNode newNode = new CompositeNode(mDimensions);
    		mCS.getCoordinates(ndx, mInsertBuf);
    		newNode.add(ndx, mInsertBuf);
    		mCompositeNodes.put(mMaxCompositeNodeKey, newNode);
    	} else { // Composite nodes not permitted, because split threshold <= 1
    		mNodes[mCount] = ndx;
    		int sd = mSplitDims[parentIndex] + 1;
    		if (sd == mDimensions) {
    			sd = 0;
    		}
    		mSplitDims[mCount] = sd;
    		mSplitValues[mCount] = mCS.getCoordinate(ndx, sd);
    	}
    	mRights[parentIndex] = mCount;
    	mCount++;
    }

    private void checkNdx(int ndx) {
        if (ndx < 0 || ndx >= mCoordCount) {
            throw new IndexOutOfBoundsException("out of bounds: " + ndx);
        }
    }

    public void insert(int ndx) {

        checkNdx(ndx);

        // First thing to be inserted -- it becomes the root.
        if (mCount == 0) {
        	ensureCapacity(1);
        	if (mSplitThreshold > 1) {
        		mMaxCompositeNodeKey++;
        		mNodes[0] = mMaxCompositeNodeKey;
        		CompositeNode compositeNode = new CompositeNode(mDimensions);
        		mCS.getCoordinates(ndx, mInsertBuf);
        		compositeNode.add(ndx, mInsertBuf);
        		mCompositeNodes.put(mMaxCompositeNodeKey, compositeNode);
        	} else { // mSplitThreshold == 1, so no composite nodes.
        		mNodes[0] = ndx;
        		mSplitDims[0] = 0;
        		mSplitValues[0] = mCS.getCoordinate(ndx, 0);
        	}
        	mCount++;
        	return;
        }

        int n = 0;

        while(true) {

        	if (mSplitDims[n] == -1) {
        		
        		// mNodes[n] contains the key for a composite node.
        		CompositeNode compositeNode = (CompositeNode) mCompositeNodes.get(mNodes[n]);
        		mCS.getCoordinates(ndx, mInsertBuf);
        		
        		compositeNode.add(ndx, mInsertBuf);
        		
        		if (compositeNode.size() > mSplitThreshold) {
        			
        			mCompositeNodes.removeKey(mNodes[n]);
        			
        			// Just exceeded the size limit.  Have to split it.
        			CompositeNodeSplit nodeSplit = compositeNode.split(mCS);
        			// Index of the median point becomes the node.
        			mNodes[n] = nodeSplit.getMedianIndex();
        			mSplitDims[n] = nodeSplit.getSplitDim();
        			mSplitValues[n] = nodeSplit.getSplitValue();
        			CompositeNode leftNode = nodeSplit.getLeftNode();
        			if (leftNode != null) {
        		    	ensureCapacity(mCount+1);
        		    	mMaxCompositeNodeKey++;
        		    	mNodes[mCount] = mMaxCompositeNodeKey;
        		    	mCompositeNodes.put(mMaxCompositeNodeKey, leftNode);
        		    	mLefts[n] = mCount;
        		    	mCount++;
        			}
        			CompositeNode rightNode = nodeSplit.getRightNode();
        			if (rightNode != null) {
        		    	ensureCapacity(mCount+1);
        		    	mMaxCompositeNodeKey++;
        		    	mNodes[mCount] = mMaxCompositeNodeKey;
        		    	mCompositeNodes.put(mMaxCompositeNodeKey, rightNode);
        		    	mRights[n] = mCount;
        		    	mCount++;
        			}
        		}
        		
        		return;
        		
        	} else { // mSplitDims[n] >= 0, so node is not a composite.
        		
              int d = mSplitDims[n];
              double coord = mCS.getCoordinate(ndx, d);
              double nodeCoord = mSplitValues[n];
              
              if (coord > nodeCoord) {
                if (mRights[n] < 0) {
                    newNodeOnRight(n, ndx);
                    return;
                } else {
                    n = mRights[n];
                }
              } else {
                if (mLefts[n] < 0) {
                    newNodeOnLeft(n, ndx);
                    return;
                } else {
                    n = mLefts[n];
                }
              }
          }
        	
        } // while
    }

    public int search(double[] coords) {

        int n = 0;
        double[] nodeCoords = new double[mDimensions];

        while(true) {
          
          int curNode = (n >= 0 && n < mCount) ? mNodes[n] : -1;
          if (curNode < 0) return -1; // Not found.

          if (mSplitDims[n] == -1) {
        	  
        	  // It's a composite node
        	  CompositeNode node = (CompositeNode) mCompositeNodes.get(curNode);
        	  return node.search(mCS, coords);
        	  
          } else {
        	  
        	  mCS.getCoordinates(curNode, nodeCoords);

        	  if (coordsEqual(coords, nodeCoords)) {
        		  return curNode;
        	  }

        	  int d = mSplitDims[n];
        	  if (coords[d] > nodeCoords[d]) {
        		  n = mRights[n];
        	  } else {
        		  n = mLefts[n];
        	  }
          }
          
        } // while

    }

    public int nearestNeighbor(int ndx) {
    	
    	checkNdx(ndx);

    	double[] coords = new double[mDimensions];
    	mCS.getCoordinates(ndx, coords);
    	
    	int[] nn = nearest(coords, 2);
    	
    	return nn[0] == ndx ? nn[1] : nn[0];
    }
    
    public int nearest(double[] coords) {
        int[] nn = nearest(coords, 1);
        return nn[0];
    }

    public int[] nearest(double[] coords, int num) {

        if (num < 0 || num > mCount) {
            throw new IllegalArgumentException(
              "number of neighbors negative or greater than number of nodes: "
              + num);
        }

        int dim = coords.length;

        DistanceQueue dq = new DistanceQueue(num);

        rnearest(0, coords, num,
                 HyperRect.infiniteHyperRect(dim),
                 Double.MAX_VALUE,
                 dq);

        int[] ids = new int[num];
        for (int i=num-1; i>=0; i--) {
            ids[i] = dq.remove();
        }

        return ids;
    }

    private void rnearest(int curNodeNdx, double[] targetCoords, int num,
                          HyperRect hr,
                          double maxDistSquared,
                          DistanceQueue dq) {

        int curNode = mNodes[curNodeNdx];
        if (curNode < 0) {
            return;
        }
        
        if (mSplitDims[curNodeNdx] == -1) { // It's a composite node.
        	
        	CompositeNode node = (CompositeNode) mCompositeNodes.get(curNode);
        	node.nearest(mCS, targetCoords, num, dq);
        	
        } else { // Not a composite node

        // Component of coords to use for splitting.
        int sdim = mSplitDims[curNodeNdx];

        double[] curCoords = mCS.getCoordinates(curNode, null);

        double targetCoord = targetCoords[sdim];
        double curCoord = curCoords[sdim];

        boolean targetInLeft = targetCoord <= curCoord;

        int nearerNodeNdx = -1, furtherNodeNdx = -1;
        
        if (targetInLeft) {
            nearerNodeNdx = mLefts[curNodeNdx];
            furtherNodeNdx = mRights[curNodeNdx];
        } else {
            nearerNodeNdx = mRights[curNodeNdx];
            furtherNodeNdx = mLefts[curNodeNdx];
        }

        if (nearerNodeNdx >= 0) {
            double oldCoord = 0.0;
            if (targetInLeft) {
                oldCoord = hr.getMaxCornerCoord(sdim);
                hr.setMaxCornerCoord(sdim, curCoords[sdim]);
            } else {
                oldCoord = hr.getMinCornerCoord(sdim);
                hr.setMinCornerCoord(sdim, curCoords[sdim]);
            }
            rnearest(nearerNodeNdx, targetCoords, num, hr, maxDistSquared, dq);
            if (targetInLeft) {
                hr.setMaxCornerCoord(sdim, oldCoord);
            } else {
                hr.setMinCornerCoord(sdim, oldCoord);
            }
            if (dq.size() == num) {
                maxDistSquared = dq.getMaxDistance();
            }
        }

        if (furtherNodeNdx >= 0) {
            double oldCoord = 0.0;
            if (targetInLeft) {
                oldCoord = hr.getMinCornerCoord(sdim);
                hr.setMinCornerCoord(sdim, curCoords[sdim]);
            } else {
                oldCoord = hr.getMaxCornerCoord(sdim);
                hr.setMaxCornerCoord(sdim, curCoords[sdim]);
            }
            if (euclideanDistSquared(hr.closestPoint(targetCoords), targetCoords) < maxDistSquared) {
                rnearest(furtherNodeNdx, targetCoords, num, hr, maxDistSquared, dq);
            }
            if (targetInLeft) {
                hr.setMinCornerCoord(sdim, oldCoord);
            } else {
                hr.setMaxCornerCoord(sdim, oldCoord);
            }
            if (dq.size() == num) {
                maxDistSquared = dq.getMaxDistance();
            }
        }

        double curToTarget = euclideanDistSquared(curCoords, targetCoords);
        if (curToTarget < maxDistSquared) {
        	if (dq.size() == num) {
        		dq.remove();
            }
            dq.add(curNode, curToTarget);
        }
        }
    }

    public static boolean coordsEqual(double[] coords1, double[] coords2) {
        int n = coords1.length;
        if (coords2.length != n) {
            throw new IllegalArgumentException("dimensions not equal: " + n + " != " + coords2.length);
        }
        for (int i=0; i<n; i++) {
            if (Double.doubleToLongBits(coords1[i]) != Double.doubleToLongBits(coords2[i])) {
                return false;
            }
        }
        return true;
    }

    public static double euclideanDist(double[] coords1, double[] coords2) {
        return Math.sqrt(euclideanDistSquared(coords1, coords2));
    }

    public static double euclideanDistSquared(double[] coords1, double[] coords2) {
        double d = 0.0;
        int len = coords1.length;
        for (int i=0; i<len; i++) {
            double c = coords1[i] - coords2[i];
            d += c*c;
        }
        return d;
    }

    static class CompositeNode {
    
    	private double[] mMaxima;
    	private double[] mMinima;
    	private IntArrayList mMemberIDs;
    	
    	CompositeNode(int dimensions) {
    		mMaxima = new double[dimensions];
    		Arrays.fill(mMaxima, -Double.MAX_VALUE);
    		mMinima = new double[dimensions];
    		Arrays.fill(mMinima, Double.MAX_VALUE);
    		mMemberIDs = new IntArrayList();
    	}
    	
    	void nearest(CoordinateList cs, double[] coords, int num, DistanceQueue dq) {
    		double[] memberCoords = new double[mMaxima.length];
    		int sz = mMemberIDs.size();
    		for (int i=0; i<sz; i++) {
    			int id = mMemberIDs.get(i);
    			cs.getCoordinates(id, memberCoords);
    			double d = euclideanDistSquared(coords, memberCoords);
    			if (dq.size() < num) {
    				dq.add(id, d);
    			} else if (d < dq.getMaxDistance()) {
    				dq.remove();
    				dq.add(id, d);
    			}
    		}
    	}
    	
    	int search(CoordinateList cs, double[] coords) {
    		double[] memberCoords = new double[mMaxima.length];
    		int sz = mMemberIDs.size();
    		for (int i=0; i<sz; i++) {
    			int id = mMemberIDs.get(i);
    			cs.getCoordinates(id, memberCoords);
    			if (coordsEqual(coords, memberCoords)) {
    				return id;
    			}
    		}
    		return -1;
    	}
    	
    	void add(int ndx, double[] coordBuf) {
    		mMemberIDs.add(ndx);
    		int dimensions = mMaxima.length;
    		for (int i=0; i<dimensions; i++) {
    			double d = coordBuf[i];
    			if (!Double.isNaN(d)) {
    				if (d > mMaxima[i]) {
    					mMaxima[i] = d;
    				}
    				if (d < mMinima[i]) {
    					mMinima[i] = d;
    				}
    			}
    		}
//    		if (mMemberIDs.size() == 4) {
//    			new Exception("...... size 4").printStackTrace();
//    		}
    	}
    	
    	int size() {
    		return mMemberIDs.size();
    	}
    	
    	CompositeNodeSplit split(CoordinateList cs) {
    		
    		CompositeNodeSplit nodeSplit = null;
    		
    		if (size() > 0) {
    			
    			int splitDimension = dimensionWithMaxSpread();
    			
    			if (splitDimension >= 0) {
    			
    				int[] ids = elements();
    				int numIDs = ids.length;
    				
    				double[] values = new double[numIDs];
    				for (int i=0; i<numIDs; i++) {
    					values[i] = cs.getCoordinate(ids[i], splitDimension);
    				}
    				
    				SortUtils.parallelSort(values, ids);
    				
    				int mid = numIDs/2;
    				
    				// Possible if most of the values are NaNs.
    				// Move the the last non-NaN.  We don't
    				// have to worry about all being NaN, because
    				// dimensionWithMaxSpread() excludes dimensions
    				// containing all NaNs.
    				while(Double.isNaN(values[mid])) {
    					mid--;
    				}
    				
    				int medianIndex = ids[mid];
    				double splitValue = values[mid];
    				
    				// Create the child nodes.
    				int dimensions = mMaxima.length;
    				double[] buf = new double[dimensions];
    				
    				// IDs on the left 
    				CompositeNode leftNode = null;
    				if (mid > 0) {
    					leftNode = new CompositeNode(dimensions);
    					for (int i=0; i<mid; i++) {
    						cs.getCoordinates(ids[i], buf);
    						leftNode.add(ids[i], buf);
    					}
    				}
    				
    				CompositeNode rightNode = null;
    				
    				if (numIDs - mid - 1 > 0) {
    					rightNode = new CompositeNode(dimensions);
    					for (int i=mid+1; i<numIDs; i++) {
    						cs.getCoordinates(ids[i], buf);
    						rightNode.add(ids[i], buf);
    					}
    				}
    		
    				nodeSplit = new CompositeNodeSplit(medianIndex, splitValue, splitDimension, leftNode, rightNode);
    			}
    		}
    		
    	    return nodeSplit;
    	}
    	
    	int dimensionWithMaxSpread() {
    		double maxSpread = -1.0;
    		int maxSpreadDim = -1;
    		int dimensions = mMaxima.length;
    		for (int i=0; i<dimensions; i++) {
    			// The maximum for that dimension will be less
    			// than the minimum if no non-NaN values have been
    			// encountered in that dimension.
    			if (mMaxima[i] >= mMinima[i]) {
    				double spread = mMaxima[i] - mMinima[i];
    				if (spread > maxSpread) {
    					maxSpread = spread;
    					maxSpreadDim = i;
    				}
    			}
    		}
    		return maxSpreadDim;
    	}
    	
    	int medianElement(int dim, CoordinateList cs) {
    		int[] ids = elements();
    		int idCount = ids.length;
    		double[] values = new double[idCount];
    		for (int i=0; i<idCount; i++) {
    			values[i] = cs.getCoordinate(ids[i], dim);
    		}
    		// Don't let median rearrange the values or they won't 
    		// correspond 1:1 with ids!
    		double median = CoordinateMath.median(values, false);
    		double smallestDistFromMedian = Double.MAX_VALUE;
    		int rtnNdx = -1;
    		for (int i=0; i<idCount; i++) {
				double distFromMedian = Math.abs(values[i] - median);
				if (distFromMedian < smallestDistFromMedian) {
					smallestDistFromMedian = distFromMedian;
					rtnNdx = ids[i];
				}
    		}
    		return rtnNdx;
    	}
    	
    	int[] elements() {
    		mMemberIDs.trimToSize();
    		int[] elts = mMemberIDs.elements();
    		int[] rtn = new int[elts.length];
    		System.arraycopy(elts, 0, rtn, 0, elts.length);
    		return rtn;
    	}
    }
    
    static class CompositeNodeSplit {
    	
    	private int mMedianNdx;
    	private double mSplitValue;
    	private int mSplitDimension;
    	private CompositeNode mLeftNode, mRightNode;
    	
    	CompositeNodeSplit(int medianNdx, double splitValue, int splitDimension, 
    			CompositeNode leftNode, CompositeNode rightNode) {
    		mMedianNdx = medianNdx;
    		mSplitValue = splitValue;
    		mSplitDimension = splitDimension;
    		mLeftNode = leftNode;
    		mRightNode = rightNode;
    	}
    	
    	int getMedianIndex() { return mMedianNdx; }
    	double getSplitValue() { return mSplitValue; }
    	int getSplitDim() { return mSplitDimension; }
    	CompositeNode getLeftNode() { return mLeftNode; }
    	CompositeNode getRightNode() { return mRightNode; }
    }
}
