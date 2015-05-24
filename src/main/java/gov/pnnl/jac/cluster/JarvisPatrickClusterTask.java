package gov.pnnl.jac.cluster;

import java.util.*;
import java.util.concurrent.*;

import gov.pnnl.jac.collections.*;
import gov.pnnl.jac.geom.CoordinateList;
import gov.pnnl.jac.geom.KDTree;
import gov.pnnl.jac.geom.distance.DistanceFunc;
import gov.pnnl.jac.task.*;

public class JarvisPatrickClusterTask extends ClusterTask {

	public JarvisPatrickClusterTask(CoordinateList cs,
			ClusterTaskParams params) {
		super(cs, params);
	}

	@Override
	public String getAlgorithmName() {
		return "Jarvis-Patrick clustering";
	}

	@Override
	protected ClusterList doTask() throws Exception {
	    
		final CoordinateList coords = getCoordinateList();
		final JarvisPatrickClusterTaskParams params = (JarvisPatrickClusterTaskParams) getParams();
		
	    final int coordCount = coords.getCoordinateCount();

	    if (coordCount == 0) {
	      error("zero tuples");
	    }

	    ProgressHandler ph = new ProgressHandler(this);
	    ph.postBegin();
	    
	    int nearestNeighborsToExamine = params.getNearestNeighborsToExamine();
	    if (nearestNeighborsToExamine > coordCount - 1) {
	      // Can't exceed the number of neighbors that exist.
	      ph.postMessage(String
	          .format(
	              "reducing nearest neighbors to examine to %d, the number of possible neighbors",
	              coordCount - 1));
	      nearestNeighborsToExamine = coordCount - 1;
	    }

	    int nearestNeighborOverlap = params.getNearestNeighborOverlap();
	    if (nearestNeighborOverlap > nearestNeighborsToExamine) {
	      ph.postMessage(String
	          .format(
	              "reducing nearest neighbor overlap to %d, the number of nearest neighbors to examine",
	              nearestNeighborsToExamine));
	      nearestNeighborOverlap = nearestNeighborsToExamine;
	    }

	    boolean mutualNN = params.getMutualNearestNeighbors();

	    // Use this array to keep track of the cluster assignments. -1 means
	    // not assigned yet.
	    int[] clusterAssignments = new int[coordCount];
	    Arrays.fill(clusterAssignments, -1);

	    // Most of the time is taken to compute the nearest neighbors. Give this
	    // portion 95% of the time with coordCount steps.
	    //
	    ph.subsection(0.95, coordCount);

	    // For now, assume we can compute the nearest neighbors of every
	    // tuple once up front. (Might have to revisit this issue for large
	    // numbers of tuples and large numbers for nearestNeighborsToExamine)
	    int[][] nearestNeighbors = computeNearestNeighbors(ph, nearestNeighborsToExamine);

	    // This ends the subsection of progress.
	    ph.postEnd();
	    
	    List<IntList> clusterMemberships = new ArrayList<IntList>();

	    // The number of executions of the inner loop.
	    final int remainingSteps = coordCount * (coordCount - 1)/2;
	    
	    // Give this main assignment loop the remaining 5% of the progress.
	    ph.subsection(0.05, remainingSteps);
	    
	    for (int i = 0; i < coordCount; i++) {

	      int icluster = clusterAssignments[i];

	      int[] inearestNeighbors = nearestNeighbors[i];

	      for (int j = i + 1; j < coordCount; j++) {

	        int jcluster = clusterAssignments[j];

	        // If they're already both in the same cluster, there's no
	        // reason to test further.
	        //
	        if (icluster < 0 || jcluster < 0 || icluster != jcluster) {

	          // j must be one of i's nearest neighbors
	          if (!mutualNN || Arrays.binarySearch(inearestNeighbors, j) >= 0) {

	            int[] jnearestNeighbors = nearestNeighbors[j];

	            // And i must be one of j's nearest neighbors.
	            if (!mutualNN || Arrays.binarySearch(jnearestNeighbors, i) >= 0) {
	              // If the overlap is large enough, place them in the same cluster.
	              if (overlapAtLeast(inearestNeighbors, jnearestNeighbors,
	                  nearestNeighborOverlap)) {
	                // If they're already assigned to different clusters, merge
	                // them.
	                if (icluster >= 0 && jcluster >= 0) {
	                  icluster = mergeClusters(icluster, jcluster,
	                      clusterMemberships, clusterAssignments);
	                  // Assign to i's cluster
	                } else if (icluster >= 0) {
	                  assignToCluster(j, icluster, clusterMemberships);
	                  clusterAssignments[j] = icluster;
	                  // Assign to j's cluster
	                } else if (jcluster >= 0) {
	                  assignToCluster(i, jcluster, clusterMemberships);
	                  clusterAssignments[i] = jcluster;
	                  icluster = jcluster;
	                  // Assign them both to a new cluster, since neither are in a
	                  // cluster already.
	                } else {
	                  icluster = assignToNextCluster(i, clusterMemberships);
	                  assignToCluster(j, icluster, clusterMemberships);
	                  clusterAssignments[i] = clusterAssignments[j] = icluster;
	                }
	              }
	            }
	          }
	        }

	        ph.postStep();
	      }

	    }
	    
	    // Finish the main assignment loop subsection.
	    ph.postEnd();
	    
	    // There still may be unassigned tuples, because they didn't have a large
	    // enough nn overlap
	    // with any of the other tuples. Assign them to their own clusters.
	    for (int i = 0; i < coordCount; i++) {
	      if (clusterAssignments[i] < 0) {
	        clusterAssignments[i] = assignToNextCluster(i, clusterMemberships);
	      }
	    }

	    List<Cluster> clusters = new ArrayList<Cluster>();

	    final int coordLen = coords.getDimensionCount();
	    
	    for (IntList memberList : clusterMemberships) {
	      if (memberList.size() > 0) {
	        int[] members = memberList.toArray();
	        Arrays.sort(members);    
	        double[] center = new double[coordLen];
	        coords.computeAverage(members, center);
	        clusters.add(new Cluster(members, center));
	      }
	    }

	    ph.postEnd();
	    
	    ClusterList clusterList = new ClusterList(clusters.toArray(new Cluster[clusters.size()]));;
	    setClusterList(clusterList);
	    
	    return clusterList; 
	  }

	  // Assigns index to a new cluster.
	  //
	  private int assignToNextCluster(int index, List<IntList> clusterMemberships) {
	    int nextCluster = clusterMemberships.size();
	    IntList memberList = new IntArrayList();
	    memberList.add(index);
	    clusterMemberships.add(memberList);
	    return nextCluster;
	  }

	  // Assigns index to an existing cluster.
	  //
	  private void assignToCluster(int index, int cluster,
	      List<IntList> clusterMemberships) {
	    clusterMemberships.get(cluster).add(index);
	  }

	  // Merge the clusters identified by index1 and index2. The merge cluster
	  // is the one with the smaller index.
	  //
	  private int mergeClusters(int index1, int index2,
	      List<IntList> clusterMemberships, int[] clusterAssignments) {
	    int newIndex = Math.min(index1, index2);
	    int oldIndex = Math.max(index1, index2);
	    if (newIndex != oldIndex) {
	      IntList list1 = clusterMemberships.get(newIndex);
	      IntList list2 = clusterMemberships.get(oldIndex);
	      list1.addAll(list2);
	      int sz = list2.size();
	      for (int i = 0; i < sz; i++) {
	        clusterAssignments[list2.get(i)] = newIndex;
	      }
	      list2.clear();
	    }
	    return newIndex;
	  }

	  // Computes the nearest neighbors. This method comprises the bulk of
	  // the work.
	  //
	  private int[][] computeNearestNeighbors(ProgressHandler ph,
	      int nearestNeighborsToExamine) throws Exception {

		final CoordinateList coords = getCoordinateList();
		final JarvisPatrickClusterTaskParams params = (JarvisPatrickClusterTaskParams) getParams();
		
	    final int coordCount = coords.getCoordinateCount();
	    int[][] nearestNeighbors = new int[coordCount][];

	    // A KD-Tree provides an efficient way of quickly looking up nearest
	    // neighbors. Even for tuple lists with millions of members, this call
	    // typically takes under a second.
	    KDTree kdTree = KDTree.forCoordinateList(coords);
	    
	    // Compute the nearest neighbors concurrently.
	    final int workerCount = params.getWorkerThreadCount();
	    List<NearestNeighborWorker> workers = new ArrayList<NearestNeighborWorker> (workerCount);
	    
	    int perWorker = coordCount/workerCount;
	    // If workerCount isn't evenly divisible into coordCount, some of the workers
	    // will have 1 more than the rest.
	    int leftOver = coordCount - (workerCount * perWorker);
	    
	    DistanceFunc distanceFunc = params.getDistanceFunc();
	    
	    // Instantiate the workers.
	    int startTuple = 0;
	    for (int i=0; i<workerCount; i++) {
	      int endTuple = startTuple + perWorker;
	      if (i < leftOver) {
	        endTuple++;
	      }
	      workers.add(new NearestNeighborWorker(startTuple, endTuple, 
	          nearestNeighborsToExamine, kdTree, distanceFunc.clone(), nearestNeighbors, ph));
	      startTuple = endTuple;
	    }
	    
	    // If more than one worker, execute with a thread pool.
	    if (workerCount > 1) {
	        ExecutorService threadPool = null;
	        try {
	          threadPool = Executors.newFixedThreadPool(workerCount);
	          // This will block. However, canceling will cause execution
	          // to stop when the workers post progress.
	          threadPool.invokeAll(workers);
	        } finally {
	          // Be sure to gracefully shutdown the thread pool.
	          if (threadPool != null) {
	            threadPool.shutdown();
	          }
	        }
	    } else {      
	      // Only 1 worker, just call directly.
	        workers.get(0).call();
	    }
	    
	    return nearestNeighbors;
	  }

	  // Quick computation of minimum overlap. For this to work, the arrays
	  // must be sorted in ascending order.
	  //
	  private boolean overlapAtLeast(int[] arr1, int[] arr2, int minOverlap) {

	    final int len1 = arr1.length;
	    final int len2 = arr2.length;

	    int i = 0;
	    int j = 0;
	    int overlap = 0;
	    while (i < len1 && j < len2) {
	      if (arr1[i] < arr2[j]) {
	        i++;
	      } else if (arr1[i] > arr2[j]) {
	        j++;
	      } else {
	        overlap++;
	        // No need to compute the entire overlap. When the
	        // min overlap is reached, return true immediately.
	        if (overlap >= minOverlap) {
	          return true;
	        }
	        i++;
	        j++;
	      }
	    }

	    return false;
	  }
	  
	  // Simple worker class to compute nearest neighbors by calling 
	  // a method of the KD-Tree.
	  //
	  private class NearestNeighborWorker implements Callable<Void> {

	    private int startTuple;
	    private int endTuple;
	    private int nnCount;
	    private KDTree kdTree;
	    private DistanceFunc distanceFunc;
	    private int[][] nnArray;
	    private ProgressHandler ph;
	    
	    private NearestNeighborWorker(int startTuple, int endTuple, int nnCount, 
	        KDTree kdTree, DistanceFunc distanceFunc, int[][] nnArray, ProgressHandler ph) {
	      this.startTuple = startTuple;
	      this.endTuple = endTuple;
	      this.nnCount = nnCount;
	      this.kdTree = kdTree;
	      this.distanceFunc = distanceFunc;
	      this.nnArray = nnArray;
	      this.ph = ph;
	    }
	    
	    @Override
	    public Void call() throws Exception {
	      for (int i=startTuple; i<endTuple; i++) {
	        int[] nn = kdTree.nearest(i, nnCount, distanceFunc);
	        // Must remember of sort them. They come back from the kd-tree sorted by distance, 
	        // not by tuple index.
	        Arrays.sort(nn);
	        nnArray[i] = nn;
	        synchronized (ph) {
	          ph.postStep();
	        }
	      }
	      return null;
	    }
	    
	  }
}
