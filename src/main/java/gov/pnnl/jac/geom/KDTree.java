package gov.pnnl.jac.geom;

import gov.pnnl.jac.collections.IntArrayList;
import gov.pnnl.jac.geom.distance.DistanceFunc;
import gov.pnnl.jac.util.ExceptionUtil;

import java.util.*;

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
public class KDTree {

    // All coordinates stored in the kd-tree come from this list of coordinates.
    // (However, not all coordinates in the list have to be in the kd-tree.)
    private CoordinateList mCS;
    
    // Number of coordinates in mCS - 1, for error checking.
    private int mMaxNdx;

    // 0-based indexes of the nodes that have been inserted.  Empty spots contain -1.
    private int[] mNodes;
    // 0-based indexes into mNodes of the left children of entries in mNodes.
    // For example if mLefts[0] = 4, mNodes[4] contains the left child of mNodes[0].   
    // -1 in mLefts[i] indicates a null left child for mNodes[i], if mNodes[i] >= 0.
    private int[] mLefts;
    // Same as mLefts, but for right children.
    private int[] mRights;
    
    // If a coordinate has been added, then deleted, this is set to true.
    private boolean[] mDeleted;

    // Actual number of nodes in the kd-tree.  It's the number of >=0 entries in mNodes that
    // do not also have mDeleted entries == true.
    private int mCount;

    /**
     * Constructor.
     * 
     * @param cs The source of coordinates for this kd-tree.  No points are
     *   inserted by the constructor.
     */
    public KDTree(CoordinateList cs) {
    	ExceptionUtil.checkNotNull(cs);
        mCS = cs;
        mMaxNdx = mCS.getCoordinateCount() - 1;
        ensureCapacity(100);
    }

    /**
     * Returns the list of coordinates associated with the kd-tree.
     * Note that only coordinates from this coordinate list can be stored in the
     * kd-tree.  However, the coordinates are not in the kd-tree unless they
     * have been explicitly inserted.
     * 
     * @return
     */
    public CoordinateList getCoordinateList() {
        return mCS;
    }

    /**
     * Generates a kd-tree for the specified coordinate list with all
     * points in the list inserted in the kd-tree.
     * 
     * @param cs
     * @return
     */
    public static KDTree forCoordinateList(CoordinateList cs) {
        KDTree kd = new KDTree(cs);
        int numCoords = cs.getCoordinateCount();
        for (int i=0; i<numCoords; i++) {
            kd.insert(i);
        }
        return kd;
    }

    // Expands the capacity of the arrays, if necessary, to support the
    // specified minimum capacity.
    //
    private void ensureCapacity(int minCap) {
        int curCap = currentCapacity();
        if (curCap < minCap) {
            int newCap = Math.max(curCap*2, minCap);
            // The arrays must always be the same lengths.
            int[] newNodes = new int[newCap];
            int[] newLefts = new int[newCap];
            int[] newRights = new int[newCap];
            boolean[] newDeleted = new boolean[newCap];
            if (curCap > 0) {
                System.arraycopy(mNodes, 0, newNodes, 0, curCap);
                System.arraycopy(mLefts, 0, newLefts, 0, curCap);
                System.arraycopy(mRights, 0, newRights, 0, curCap);
                System.arraycopy(mDeleted, 0, newDeleted, 0, curCap);
            }
            // Initialize all new spots to -1.
            Arrays.fill(newNodes, curCap, newCap, -1);
            Arrays.fill(newLefts, curCap, newCap, -1);
            Arrays.fill(newRights, curCap, newCap, -1);
            // Init new spots to false.
            Arrays.fill(newDeleted, curCap, newCap, false);
            // Replace with new arrays.
            mNodes = newNodes;
            mLefts = newLefts;
            mRights = newRights;
            mDeleted = newDeleted;
        }
    }

    private int currentCapacity() {
        // Current capacity is just the length of any of the 4 arrays.
        return mNodes != null ? mNodes.length : 0;
    }

    private void newNodeOnLeft(int parentIndex, int ndx) {
    	int m = mCount;
    	mCount++;
    	ensureCapacity(mCount);
    	mNodes[m] = ndx;
    	mLefts[parentIndex] = m;
    }

    private void newNodeOnRight(int parentIndex, int ndx) {
    	int m = mCount;
    	mCount++;
    	ensureCapacity(mCount);
    	mNodes[m] = ndx;
    	mRights[parentIndex] = m;
    }

    private void checkNdx(int ndx) {
        if (ndx < 0 || ndx > mMaxNdx) {
            throw new IndexOutOfBoundsException("out of bounds: " + ndx);
        }
    }

    public void insert(int ndx) {

        checkNdx(ndx);

        // First thing to be inserted -- it becomes the root.
        if (mCount == 0) {
        	ensureCapacity(1);
        	mNodes[0] = ndx;
        	mCount++;
            return;
        }

        int n = 0;
        int level = 0;
        int dim = mCS.getDimensionCount();

        while(true) {
            
          int curNode = mNodes[n];
          
          if (curNode == ndx) {
          
              if (mDeleted[n]) {
                  mDeleted[n] = false;
                  return;
              }
              throw new IllegalArgumentException("duplicate insertion: " + ndx);
          
          } else {
          
              // Iterate through the dimensions to determine which to split on.
              int d = level%dim;
              
              double coord = mCS.getCoordinate(ndx, d);
              double nodeCoord = mCS.getCoordinate(curNode, d);
              
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
          
          level++;
        } // while

    }

    public boolean delete(int ndx) {

    	checkNdx(ndx);
    	
        if (mCount == 0) {
            return false;
        }

        int n = 0;
        int level = 0;
        int dim = mCS.getDimensionCount();

        while(true) {
          int curNode = mNodes[n];
          if (curNode == ndx) {
              if (!mDeleted[n]) {
                  // There and not deleted.  Mark deleted and return true.
                  mDeleted[n] = true;
                  return true;
              }
              // Can't delete if already deleted.
              return false;
          } else {
              int d = level%dim;
              double coord = mCS.getCoordinate(ndx, d);
              double nodeCoord = mCS.getCoordinate(curNode, d);
              if (coord > nodeCoord) {
                if (mRights[n] < 0) {
                    return false;
                } else {
                    n = mRights[n];
                }
              } else {
                if (mLefts[n] < 0) {
                    return false;
                } else {
                    n = mLefts[n];
                }
              }
          }
          level++;
        } // while
        
    }

    public int search(double[] coords) {

        int n = 0;
        int level = 0;
        final int dim = mCS.getDimensionCount();
        final double[] nodeCoords = new double[dim];

        while(true) {
            
          int curNode = (n >= 0 && n < mCount) ? mNodes[n] : -1;
          
          if (curNode < 0) return -1; // Not found.
          
          int d = level%dim;
          
          double coord = coords[d];
          double nodeCoord = mCS.getCoordinate(curNode, d);
          
          if (coord > nodeCoord) {
              n = mRights[n];
          } else { // coord <= nodeCoord
              if (coord == nodeCoord && !mDeleted[n]) {
                  mCS.getCoordinates(curNode, nodeCoords);
                  if (coordsEqual(coords, nodeCoords)) {
                      return curNode;
                  }
              }
              n = mLefts[n];
          }

          level++;
        } // while

    }

    public int nearestNeighbor(int ndx, DistanceFunc distanceFunc) {
    	
    	checkNdx(ndx);

    	double[] coords = new double[mCS.getDimensionCount()];
    	mCS.getCoordinates(ndx, coords);
    	
    	int[] nn = nearest(coords, 2, distanceFunc);
    	
    	return nn[0] == ndx ? nn[1] : nn[0];
    }
    
    public int nearest(double[] coords, DistanceFunc distanceFunc) {
        int[] nn = nearest(coords, 1, distanceFunc);
        return nn[0];
    }

    public int[] nearest(int ndx, int num, DistanceFunc distanceFunc) {

    	if (num < 0 || num > mCount) {
            throw new IllegalArgumentException(
              "number of neighbors negative or greater than number of nodes: "
              + num);
        }

    	int dim = mCS.getDimensionCount();
    	double[] coords = new double[dim];
    	mCS.getCoordinates(ndx, coords);

        DistanceQueue dq = new DistanceQueue(num);

        rnearest(0, coords, num,
        		 distanceFunc,
                 HyperRect.infiniteHyperRect(dim),
                 Double.MAX_VALUE,
                 0, dim, dq, ndx);

        int[] ids = new int[num];
        for (int i=num-1; i>=0; i--) {
            ids[i] = dq.remove();
        }

        return ids;
    }

    public int[] nearest(double[] coords, int num, DistanceFunc distanceFunc) {

        if (num < 0 || num > mCount) {
            throw new IllegalArgumentException(
              "number of neighbors negative or greater than number of nodes: "
              + num);
        }

        int dim = coords.length;

        DistanceQueue dq = new DistanceQueue(num);

        rnearest(0, coords, num,
        		 distanceFunc,
                 HyperRect.infiniteHyperRect(dim),
                 Double.MAX_VALUE,
                 0, dim, dq, -1);

        int[] ids = new int[num];
        for (int i=num-1; i>=0; i--) {
            ids[i] = dq.remove();
        }

        return ids;
    }

    public int[] closeTo(int ndx, double maxDist, DistanceFunc distanceFunc) {
    	
    	int dim = mCS.getDimensionCount();
    	double[] coords = new double[dim];
    	mCS.getCoordinates(ndx, coords);

        DistanceQueue dq = new DistanceQueue();

        rcloseTo(0, coords,
        		 distanceFunc,
                 HyperRect.infiniteHyperRect(dim),
                 maxDist,
                 0, dim, dq, ndx);

        int sz = dq.size();
        int[] ids = new int[sz];
        for (int i=sz-1; i>=0; i--) {
            ids[i] = dq.remove();
        }

        return ids;
    }
    
    public int[] closeTo(double[] coords, double maxDistSquared, DistanceFunc distanceFunc) {

        int dim = coords.length;

        DistanceQueue dq = new DistanceQueue();

        rcloseTo(0, coords,
        		 distanceFunc,
                 HyperRect.infiniteHyperRect(dim),
                 maxDistSquared,
                 0, dim, dq, -1);

        int sz = dq.size();
        int[] ids = new int[sz];
        for (int i=sz-1; i>=0; i--) {
            ids[i] = dq.remove();
        }

        return ids;
    }
    
    public int[] inside(HyperRect rect) {
    	final int dim = mCS.getDimensionCount();
    	if (rect.getDimension() != dim) {
    		throw new IllegalArgumentException("dimension mismatch: " + rect.getDimension() + " != " + dim);
    	}
    	double[] midPoint = new double[dim];
    	double[] maxDiffs = new double[dim];
    	for (int i=0; i<dim; i++) {
    		double min = rect.getMinCornerCoord(i);
    		double max = rect.getMaxCornerCoord(i);
    		double maxDiff = (max - min)/2;
    		midPoint[i] = min + maxDiff;
    		maxDiffs[i] = maxDiff;
    	}
    	IntArrayList intList = new IntArrayList();
    	rcloseTo(0, midPoint, HyperRect.infiniteHyperRect(dim), maxDiffs, 0, dim, intList, -1);
    	return intList.toArray();
    }

    private void rnearest(int curNodeNdx, double[] targetCoords, int num,
    					  DistanceFunc distanceFunc,
                          HyperRect hr,
                          double maxDist, int level, int dim,
                          DistanceQueue dq,
                          int ndxToExclude) {

        int curNode = mNodes[curNodeNdx];
        if (curNode < 0) {
            return;
        }

        // Component of coords to use for splitting.
        int s = level%dim;

        double[] curCoords = mCS.getCoordinates(curNode, null);

        double targetCoord = targetCoords[s];
        double curCoord = curCoords[s];

        boolean targetInLeft = targetCoord < curCoord;

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
                oldCoord = hr.getMaxCornerCoord(s);
                hr.setMaxCornerCoord(s, curCoords[s]);
            } else {
                oldCoord = hr.getMinCornerCoord(s);
                hr.setMinCornerCoord(s, curCoords[s]);
            }
            rnearest(nearerNodeNdx, targetCoords, num, distanceFunc, hr, maxDist, level+1, dim, dq, ndxToExclude);
            if (targetInLeft) {
                hr.setMaxCornerCoord(s, oldCoord);
            } else {
                hr.setMinCornerCoord(s, oldCoord);
            }
            if (dq.size() == num) {
                maxDist = dq.getMaxDistance();
            }
        }

        if (furtherNodeNdx >= 0) {
            double oldCoord = 0.0;
            if (targetInLeft) {
                oldCoord = hr.getMinCornerCoord(s);
                hr.setMinCornerCoord(s, curCoords[s]);
            } else {
                oldCoord = hr.getMaxCornerCoord(s);
                hr.setMaxCornerCoord(s, curCoords[s]);
            }
            double distance = distanceFunc.distanceBetween(hr.closestPoint(targetCoords), targetCoords);
            if (distance < maxDist) {
                rnearest(furtherNodeNdx, targetCoords, num, distanceFunc, hr, maxDist, level+1, dim, dq, ndxToExclude);
            }
            if (targetInLeft) {
                hr.setMinCornerCoord(s, oldCoord);
            } else {
                hr.setMaxCornerCoord(s, oldCoord);
            }
            if (dq.size() == num) {
                maxDist = dq.getMaxDistance();
            }
        }

        if (!mDeleted[curNodeNdx] && curNodeNdx != ndxToExclude) {
            double curToTarget = distanceFunc.distanceBetween(curCoords, targetCoords);
            if (curToTarget < maxDist) {
                if (dq.size() == num) {
                    dq.remove();
                }
                dq.add(curNode, curToTarget);
            }
        }
    }

    private void rcloseTo(int curNodeNdx, double[] targetCoords,
    		DistanceFunc distanceFunc,
            HyperRect hr,
            double maxDist, int level, int dim,
            DistanceQueue dq, int ndxToExclude) {

    	int curNode = mNodes[curNodeNdx];
    	if (curNode < 0) {
    		return;
    	}

    	// Component of coords to use for splitting.
    	int s = level%dim;

    	double[] curCoords = mCS.getCoordinates(curNode, null);

    	double targetCoord = targetCoords[s];
    	double curCoord = curCoords[s];

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
    			oldCoord = hr.getMaxCornerCoord(s);
    			hr.setMaxCornerCoord(s, curCoords[s]);
    		} else {
    			oldCoord = hr.getMinCornerCoord(s);
    			hr.setMinCornerCoord(s, curCoords[s]);
    		}
    		rcloseTo(nearerNodeNdx, targetCoords, distanceFunc, hr, maxDist, level+1, dim, dq, ndxToExclude);
    		if (targetInLeft) {
    			hr.setMaxCornerCoord(s, oldCoord);
    		} else {
    			hr.setMinCornerCoord(s, oldCoord);
    		}
    	}

    	if (furtherNodeNdx >= 0) {
    		double oldCoord = 0.0;
    		if (targetInLeft) {
    			oldCoord = hr.getMinCornerCoord(s);
    			hr.setMinCornerCoord(s, curCoords[s]);
    		} else {
    			oldCoord = hr.getMaxCornerCoord(s);
    			hr.setMaxCornerCoord(s, curCoords[s]);
    		}
    		double distance = distanceFunc.distanceBetween(hr.closestPoint(targetCoords), targetCoords);
    		if (distance <= maxDist) {
    			rcloseTo(furtherNodeNdx, targetCoords, distanceFunc, hr, maxDist, level+1, dim, dq, ndxToExclude);
    		}
    		if (targetInLeft) {
    			hr.setMinCornerCoord(s, oldCoord);
    		} else {
    			hr.setMaxCornerCoord(s, oldCoord);
    		}
    	}

    	if (!mDeleted[curNodeNdx] && curNodeNdx != ndxToExclude) {
    		double curToTarget = distanceFunc.distanceBetween(curCoords, targetCoords);
    		if (curToTarget <= maxDist) {
    			dq.add(curNode, curToTarget);
    		}
    	}
    }

    private void rcloseTo(int curNodeNdx, double[] targetCoords,
            HyperRect hr,
            double[] maxDiffs, int level, int dim,
            IntArrayList intList, int ndxToExclude) {

    	int curNode = mNodes[curNodeNdx];
    	if (curNode < 0) {
    		return;
    	}

    	// Component of coords to use for splitting.
    	int s = level%dim;

    	double[] curCoords = mCS.getCoordinates(curNode, null);

    	double targetCoord = targetCoords[s];
    	double curCoord = curCoords[s];

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
    			oldCoord = hr.getMaxCornerCoord(s);
    			hr.setMaxCornerCoord(s, curCoords[s]);
    		} else {
    			oldCoord = hr.getMinCornerCoord(s);
    			hr.setMinCornerCoord(s, curCoords[s]);
    		}
    		rcloseTo(nearerNodeNdx, targetCoords, hr, maxDiffs, level+1, dim, intList, ndxToExclude);
    		if (targetInLeft) {
    			hr.setMaxCornerCoord(s, oldCoord);
    		} else {
    			hr.setMinCornerCoord(s, oldCoord);
    		}
    	}

    	if (furtherNodeNdx >= 0) {
    		double oldCoord = 0.0;
    		if (targetInLeft) {
    			oldCoord = hr.getMinCornerCoord(s);
    			hr.setMinCornerCoord(s, curCoords[s]);
    		} else {
    			oldCoord = hr.getMaxCornerCoord(s);
    			hr.setMaxCornerCoord(s, curCoords[s]);
    		}
    		
    		if (diffsWithinBoundaries(hr.closestPoint(targetCoords), targetCoords, maxDiffs)) {
    			rcloseTo(furtherNodeNdx, targetCoords, hr, maxDiffs, level+1, dim, intList, ndxToExclude);
    		}
    		
    		if (targetInLeft) {
    			hr.setMinCornerCoord(s, oldCoord);
    		} else {
    			hr.setMaxCornerCoord(s, oldCoord);
    		}
    	}

    	if (!mDeleted[curNodeNdx] && curNodeNdx != ndxToExclude) {
    		if (diffsWithinBoundaries(curCoords, targetCoords, maxDiffs)) {
    			intList.add(curNode);
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

    private static boolean diffsWithinBoundaries(double[] coords1, double[] coords2, double[] maxDiffs) {
    	final int dim = coords1.length;
    	for (int i=0; i<dim; i++) {
    		if (Math.abs(coords1[i] - coords2[i]) > maxDiffs[i]) return false;
    	}
    	return true;
    }
}
