/*
 * KMeansClusterTask.java
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

import gov.pnnl.jac.collections.IntObjectHashMap;
import gov.pnnl.jac.collections.IntObjectMap;
import gov.pnnl.jac.geom.*;
import gov.pnnl.jac.geom.distance.*;
import gov.pnnl.jac.task.*;
import gov.pnnl.jac.util.SortUtils;

import cern.colt.list.*;
import java.util.concurrent.*;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>
 * This class performs K-Means clustering of a group of coordinates contained in
 * a <code>CoordinateSet</code>.
 * </p>
 *
 * <p>
 * Copyright: Copyright (c) 2005
 * </p>
 *
 * @author R. Scarberry
 * @version 1.0
 */
public class KMeansClusterTask extends ClusterTask {

    // The number of iterations over which to monitor the number of moves in
    // order to detect when caught in an oscillation.
    private static final int TRACK_MOVES_LEN = 10;
    
    // The distance function.
    private DistanceFunc mDistanceFunc;

    // The clusters -- converted to a ClusterSet at the conclusion of the
    // task.
    private ProtoCluster[] mProtoClusters;
    
    // Used to prevent getting caught in an infinite loop replacing empty clusters.
    private Set<ProtoClusterState> mPastProtoClusterStates;

    // The maximum number of coordinates randomly sampled to initialize
    // cluster centers at the beginning of clustering.
    private int mInitCentersSamplingLimit = 100000;

    // Manages parallel processing of time-consuming subtasks.
    private SubtaskManager mSubtaskManager;

    // Used in makeAssignments() to figure out how many moves are made
    // during each iteration.
    private int[] mClusterAssignments;
    
    // The cluster seeder -- only set during use.  It's maintained
    // in case it's a kind of seeder such as KMeansPlusPlusSeeder that
    // may require canceling, so that this task can be cancelled in a 
    // timely manner.
    private ClusterSeeder mSeeder;
    
    // Set to true if it appears we may be caught in an assignment oscillation.
    private boolean mTrackMoves;

    /**
     * Fully-qualified constructor.
     *
     * @param cs -
     *            instance of <code>CoordinateSet</code> containing the
     *            coordinates to be clustered.
     * @param params -
     *            instance of <code>KMeansClusterTaskParams</code> containing
     *            the k-means parameters.
     * @param numProcessors -
     *            although this may be any positive number, the best performance
     *            is obtained when it equals the number of processors on the
     *            computing platform. Non-positive values are ignored, and one
     *            clustering thread is used.
     */
    public KMeansClusterTask(CoordinateList cs, KMeansClusterTaskParams params) {
        super(cs, params);
    }

    /**
     * Get the limit of the number of coordinates randomly sampled to initialize
     * the cluster centers. This limit, which defaults to 100,000, only matters
     * if it is less than the number of coordinates. If it's greater than the
     * number of coordinates, all coordinates are used in picking the initial
     * cluster centers. If less, then a random sample of coordinates are used to
     * initialize the centers.
     *
     * @return
     */
    public int getInitCentersSamplingLimit() {
        return mInitCentersSamplingLimit;
    }

    /**
     * Set the limit for the number of coordinates randomly sampled to
     * initialize the cluster centers. This limit should be set much larger than
     * the number of clusters desired.
     */
    public void setInitCentersSamplingLimit(int samplingLimit) {
        mInitCentersSamplingLimit = samplingLimit;
    }

    /**
     * Returns the algorithm name "k-means"
     */
    public final String getAlgorithmName() {
        return "k-means";
    }

    /**
     * Returns the distance function to be used in clustering.
     *
     * @return
     */
    public DistanceFunc getDistanceFunc() {
        return mDistanceFunc;
    }

    // Finds the nearest cluster to the coordinate with the
    // given index. If not using a cache to store coordinate-to-cluster
    // distances, coordBuf is used as a scratch buffer for fetching
    // coordinates.
    private int nearestCluster(int ndx, double[] coordBuf, DistanceFunc df) {

        // If the nearest cluster from the previous iteration did not change in
        // the previous iteration, then we can omit from consideration all those
        // other clusters that also didn't change.  This optimization speeds up
        // clustering by nearly a factor of 3!
        //
        int nearest = -1;
        double min = Double.MAX_VALUE;
        int oldNearest = mClusterAssignments[ndx];
        boolean onlyConsiderChanged = false;

        // Load up the coordinates, since we'll need 'em.
        getCoordinateList().getCoordinates(ndx, coordBuf);

        if (oldNearest >= 0) {
            ProtoCluster oldCluster = mProtoClusters[oldNearest];
            if (oldCluster.getConsiderForAssignment() && !oldCluster.getUpdateFlag()) {
                onlyConsiderChanged = true;
                nearest = oldNearest;
                min = df.distanceBetween(coordBuf, mProtoClusters[oldNearest].mCenter);
            }
        }

        int numClusters = mProtoClusters.length;
        for (int c = 0; c < numClusters; c++) {
            ProtoCluster cluster = mProtoClusters[c];
            if (cluster.getConsiderForAssignment()) {
                if (!onlyConsiderChanged || cluster.getUpdateFlag()) {
                    double d = mDistanceFunc.distanceBetween(coordBuf,
                            mProtoClusters[c].mCenter);
                    if (d < min) {
                        min = d;
                        nearest = c;
                    }
                }
            }
        }

        return nearest;
    }

    // Recomputes the centroids of the protoclusters with
    // update flags set to true.
    private void computeCenters() {
        // Update the update flags of the clusters that haven't been deleted.
        // (Empty clusters are deleted in makeAssignments().)
        int numClusters = mProtoClusters.length;
        for (int c = 0; c < numClusters; c++) {
            ProtoCluster cluster = mProtoClusters[c];
            if (!cluster.isEmpty()) {
                // This sets the protocluster's update flags to
                // true only if its membership changed in the iteration
                // just finishing.
                //
                // New clusters made in replaceEmptyClusters() by
                // splitting clusters with no bics will appear empty,
                // so their flags will remain as they are with only
                // the distance update flag true.
                cluster.setUpdateFlag();
            }
            checkForCancel();
        }

        mSubtaskManager.computeCenters();
    }

    // Make the cluster assignments for a given iteration.
    //
    private int makeAssignments() {
        // Checkpoint the clusters, so we'll be able to tell
        // which have changed after all the assignments have been
        // made.
        int numClusters = mProtoClusters.length;
        for (int c = 0; c < numClusters; c++) {
            // This moves the current membership to the
            // previous membership list. After making all the
            // assignments setUpdateFlag() compares the lists to
            // determine if the cluster center needs to be updated.
            mProtoClusters[c].checkPoint();
        }
        // Delegate the bulk of the work to the subtask manager and
        // its pool of worker threads.
        mSubtaskManager.makeAssignments();
        // Get the number of moves from the subtask manager
        return mSubtaskManager.getMoves();
    }

    private boolean replaceEmptyClusters() {

        boolean emptyClustersReplaced = false;

        // First of all, determine if there are any empty clusters.
        int numClusters = mProtoClusters.length;
        int emptyClusterCount = 0;
        for (int i = 0; i < numClusters; i++) {
            if (mProtoClusters[i].isEmpty()) {
                emptyClusterCount++;
            }
        }
        
        if (emptyClusterCount > 0) {

            if (mPastProtoClusterStates == null) {
                mPastProtoClusterStates = new HashSet<ProtoClusterState> ();
            }
            
            ProtoClusterState currentState = new ProtoClusterState(mProtoClusters);
            if (mPastProtoClusterStates.contains(currentState)) {
                postMessage("will not replace " + emptyClusterCount + " empty clusters, since the current condition has been encountered before");
                return false;
            }
            
            mPastProtoClusterStates.add(currentState);
            
            postMessage("attempting to replace " + emptyClusterCount
                    + " empty clusters");

            CoordinateList cs = getCoordinateList();
            int notEmptyCount = numClusters - emptyClusterCount;
            // Put the bics and indices of nonempty clusters in
            // two arrays.
            double[] bics = new double[notEmptyCount];
            int[] indices = new int[notEmptyCount];
            int count = 0;
            for (int i = 0; i < numClusters; i++) {
                ProtoCluster cluster = mProtoClusters[i];
                if (!cluster.isEmpty()) {
                    bics[count] = cluster.computeBIC(cs);
                    indices[count++] = i;
                }
            }
            // Sort on bics, but maintain 1:1 correspondence between arrays.
            SortUtils.parallelSort(bics, indices, true);
            count = 0;
            // Replace each empty cluster and the cluster with the lowest bic,
            // by the split of the cluster with the lowest bic.
            for (int i = 0; i < numClusters; i++) {
                ProtoCluster cluster = mProtoClusters[i];
                if (cluster.isEmpty()) {
                    boolean replaced = false;
                    if (count < indices.length) {
                        ProtoCluster clusterToSplit = mProtoClusters[indices[count]];
                        if (clusterToSplit.size() > 1) {
                            ProtoCluster[] newClusters = split(clusterToSplit);
                            if (newClusters.length == 2) {
                                mProtoClusters[indices[count]] = newClusters[0];
                                mProtoClusters[i] = newClusters[1];
                                replaced = true;
                                emptyClustersReplaced = true;
                            }
                        }
                        count++;
                    }
                    if (!replaced) {
                        cluster.setConsiderForAssignment(false);
                    }
                }
            }
        }

        return emptyClustersReplaced;
    }

    private ProtoCluster[] split(ProtoCluster cluster) {
        int[] memberIndices = cluster.getMembership();
        FilteredCoordinateList fcs = new FilteredCoordinateList(memberIndices,
                getCoordinateList());
        KMeansClusterTaskParams params = (KMeansClusterTaskParams) getParams();
        ClusterSeeder seeder = params.getClusterSeeder();
        // A preassigned seeder should definitely not be used for splitting clusters.
        if (seeder instanceof PreassignedSeeder) {
        	seeder = new RandomSeeder(0L);
        }
        
        KMeansClusterTask kmeans = new KMeansClusterTask(fcs,
                new KMeansClusterTaskParams(2, Integer.MAX_VALUE, 0, params.getNumWorkerThreads(), 
                        false, params.getDistanceFunc(), seeder));
        
        kmeans.run();
        
        ProtoCluster[] rtn = null;
        
        if (kmeans.getTaskOutcome() == TaskOutcome.SUCCESS) {
        	ClusterList clusters = kmeans.getClusterList();
        	int numClusters = clusters.getClusterCount();
        	rtn = new ProtoCluster[numClusters];
        	for (int i = 0; i < numClusters; i++) {
        		Cluster c = clusters.getCluster(i);
        		int[] indices = c.getMembership();
        		int n = indices.length;
        		for (int j = 0; j < n; j++) {
        			indices[j] = fcs.getWrappedIndex(indices[j]);
        		}
        		rtn[i] = new ProtoCluster(indices, c.getCenter());
        	}
        } else {
        	// Just return the cluster passed in.
        	rtn = new ProtoCluster[] { cluster };
        }
        
        return rtn;
    }

    protected final ClusterList doTask() {

        try { // Cleanup code in the finally clause

            KMeansClusterTaskParams params = (KMeansClusterTaskParams) getParams();

            int maxIterations = params.getMaxIterations();
            int steps = maxIterations + 2;
            
            // Create the progress and message posting object and post the
            // first progress.
            ProgressHandler ph = new ProgressHandler(this, getBeginProgress(),
                    getEndProgress(), steps);
            ph.postBegin();

            mDistanceFunc = params.getDistanceFunc();

            CoordinateList cs = getCoordinateList();
            int coordCount = cs.getCoordinateCount();

            // Error out if there are no coordinates to cluster.
            if (coordCount == 0) {
                error("zero coordinates");
            }
            
            // Ensure that the data source is set for the distance function.
            if (mDistanceFunc instanceof AbstractDistanceFunc) {
                ((AbstractDistanceFunc) mDistanceFunc).setDataSource(
                        new CoordinateListColumnarDoubles(cs));
            }

            // Randomly initialize the cluster centers. This creates the
            // array mProtoClusters, and may reduce the actual number of
            // clusters if there are too few unique coordinates in the
            // coordinate set.
            initCenters(ph);
            
            ph.postMessage("initial cluster seeds generated");
            
            ph.postStep();

            int numClusters = mProtoClusters.length;

            // Trival case -- put everything in one big cluster.
            if (numClusters == 1) {

                ProtoCluster cluster = mProtoClusters[0];
                for (int i = 0; i < coordCount; i++) {
                    cluster.add(i);
                }
                cluster.updateCenter(cs);

            } else { // More than 1 cluster, so we can perform k-means.

                int numProcessors = params.getNumWorkerThreads();
                if (numProcessors <= 0) {
                    numProcessors = Runtime.getRuntime().availableProcessors();
                }
                
                // Instantiate the subtask manager AFTER initializing
                // mProtoClusters,
                // since it must know how many clusters are to be generated.
                mSubtaskManager = new SubtaskManager(numProcessors);

                // Post a message about the state of concurrent subprocessing.
                if (numProcessors > 1) {
                    ph.postMessage("concurrent processing mode with "
                            + numProcessors + " subtask threads");
                } else {
                    ph.postMessage("non-concurrent processing mode");
                }

                // Keeps track of the cluster membership for each coordinate.
                // Used in makeAssignments() to determine the number of moves.
                mClusterAssignments = new int[coordCount];
                // Init. to -1, meaning no coordinates assigned yet.
                Arrays.fill(mClusterAssignments, -1);

                // Make the initial cluster assignments.
                makeAssignments();
                
                ph.postMessage("initial cluster assignments made");
                
                ph.postStep();
                
                // Number of moves and current iteration.
                int moves = 0, it = 0;
                // Two stopping criteria:
                // - the number of moves <= movesGoal (usually == 0)
                // OR
                // - the maximum number of iterations has been reached.
                //
                int movesGoal = params.getMovesGoal();
                int itLimit = params.getMaxIterations();
                boolean emptyClustersReplaced = false;

                Deque<Integer> changeInMovesDeque = new ArrayDeque<Integer> ();
                List<List<Move>> pastMoveLists = null;
                boolean caughtInOscillation = false;
                boolean emptyClustersReplacedOnce = false;
                
                do {

                    emptyClustersReplaced = false;

                    // Updates cluster centers and
                    // affected distances in the distance cache.
                    computeCenters();

                    int oldMoves = moves;
                    
                    // Make this iteration's assignments.
                    moves = makeAssignments();
                    
                    caughtInOscillation = false;
                    
                    if (mTrackMoves) {
                        List<Move> moveList = mSubtaskManager.getMoveList();
                        if (moveList.size() > 0) {
                            if (pastMoveLists.size() == TRACK_MOVES_LEN) {
                                pastMoveLists.remove(0);
                            }
                            pastMoveLists.add(moveList);
                            caughtInOscillation = hasCycle(pastMoveLists);
                        }
                    }
                
                    if (it >= 1) {
                        int changeInMoves = moves - oldMoves;
                        if (changeInMovesDeque.size() == TRACK_MOVES_LEN) {
                            changeInMovesDeque.removeFirst();
                        }
                        changeInMovesDeque.addLast(changeInMoves);
                        if (changeInMovesDeque.size() == TRACK_MOVES_LEN && !mTrackMoves) {
                            int sum = 0;
                            for (Integer cim : changeInMovesDeque) {
                                sum += cim.intValue();
                            }
                            int avgChangeInMoves = sum/TRACK_MOVES_LEN;
                            if (avgChangeInMoves <= 2) {
                                mTrackMoves = true;
                                pastMoveLists = new ArrayList<List<Move>> ();
                            }
                        }
                    }
                    
                    ph.postStep();
                    
                    // Post a progress event.
                    it++;
                    // Post a message about the number of moves.
                    ph.postMessage("moves in iteration " + it + " = " + moves);

                    if (moves <= movesGoal || it >= itLimit || caughtInOscillation) {
                        
                        if (!emptyClustersReplacedOnce) {
                            // Any new clusters made by splitting existing
                            // clusters will have the center update flag set to
                            // false
                            // and the distance update flag set to true.
                            emptyClustersReplaced = params.getReplaceEmptyClusters() && replaceEmptyClusters();
                            
                            if (emptyClustersReplaced) {
                            
                                emptyClustersReplacedOnce = true;
                             
                                mTrackMoves = false;
                                changeInMovesDeque.clear();
                                pastMoveLists = null;
                                
                                computeCenters();
                                int moves2 = makeAssignments();
                                ph.postMessage("additional moves after empty cluster replacement = "
                                            + moves2);
                            }
                        }
                    }

                    // If empty clusters are replaced, always do at least one
                    // more iteration.
                    //
                } while ((moves > movesGoal && it < itLimit && !caughtInOscillation)
                        || emptyClustersReplaced);

            }

            // Number of empty clusters deleted.
            int numDeleted = 0;

            // Convert the proto-clusters to the final ClusterSet.
            //
            // - accumulate in a list.
            List<Cluster> clusterList = new ArrayList<Cluster>(numClusters);
            for (int c = 0; c < numClusters; c++) {
                ProtoCluster pcluster = mProtoClusters[c];
                if (!pcluster.isEmpty()) {
                    clusterList.add(new Cluster(pcluster.getMembership(),
                            pcluster.mCenter));
                } else {
                    numDeleted++;
                }
            }
            // - convert list to an array.
            int sz = clusterList.size();
            Cluster[] clusters = new Cluster[sz];
            clusterList.toArray(clusters);
            // - instantiate the ClusterSet and call setClusterSet().
            setClusterList(new ClusterList(clusters));

            // Post a message if any empty clusters were deleted.
            if (numDeleted > 0) {
                ph.postMessage("number of clusters was reduced to " + sz
                        + ", because " + numDeleted
                        + " empty clusters were deleted");
            }

            // Post the final progress event.
            ph.postEnd();

        } finally {

            // Cleanup the intermediate data.
            mDistanceFunc = null;
            mProtoClusters = null;
            mClusterAssignments = null;
            mPastProtoClusterStates = null;
            if (mSubtaskManager != null) {
                mSubtaskManager.shutdown();
                mSubtaskManager = null;
            }
        }

        return mClusters;
    }
    
    private boolean hasCycle(List<List<Move>> moveLists) {
        
        final int numLists = moveLists.size();
        
        if (numLists > 1) {
            
            IntObjectMap<int[]> stateMap = new IntObjectHashMap<int[]>();
            List<Move> lastList = moveLists.get(numLists - 1);
            
            for (Move mv : lastList) {
                stateMap.put(mv.getCoordIndex(), new int[] { 
                    mv.getFromCluster(), mv.getToCluster()
                });
            }
            
            for (int i=numLists-2; i>=0; i--) {
                
                List<Move> moveList = moveLists.get(i);
                
                boolean checkMap = true;
                
                for (Move mv : moveList) {
                    
                    int coordIndex = mv.getCoordIndex();
                    
                    if (stateMap.containsKey(coordIndex)) {
                        int[] fromTo = stateMap.get(coordIndex);
                        if(mv.getToCluster() != fromTo[0]) {
                            error("inconsistent data in lists of past moves");
                        }
                        fromTo[0] = mv.getFromCluster();
                    } else {                        
                        stateMap.put(coordIndex, new int[] { mv.getFromCluster(), mv.getToCluster() });
                        // from and to for this coordinate cannot be the same, so no reason to check.
                        checkMap = false;
                    }
                    
                }
                
                if (checkMap) {
                    // Check all the values in the map to see if from and to are the same.  This would indicate an 
                    // oscillation back to the same membership state that occured previously.
                
                    int[] coordIndexes = stateMap.keys();
                    boolean fromTosTheSame = true;
                    
                    for (int j=0; j<coordIndexes.length; j++) {
                        int[] fromTo = stateMap.get(coordIndexes[j]);
                        if (fromTo[0] != fromTo[1]) {
                            fromTosTheSame = false;
                            break;
                        }
                    }
                    
                    if (fromTosTheSame) {
                        return true;
                    }
                    
                }
            }
            
        }
        
        return false;
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        if (super.cancel(mayInterruptIfRunning)) {
            final ClusterSeeder seeder = mSeeder;
            if (seeder instanceof KMeansPlusPlusSeeder) {
                ((KMeansPlusPlusSeeder) seeder).cancel();
            }
            return true;
        }
        return false;
    }
    
    // Create the initial seed ProtoClusters by randomly picking coordinates
    // from
    // the coordinate set. May want to add more ways of generating these.
    //
    private void initCenters(ProgressHandler ph) {

        try {
            
            KMeansClusterTaskParams params = (KMeansClusterTaskParams) getParams();

            CoordinateList cs = getCoordinateList();
        
            mSeeder = params.getClusterSeeder();
        
            int clustersRequested = params.getNumClusters();
        
            int minUniqueCoordCount = CoordinateMath.checkNumberOfUniqueCoordinates(cs, clustersRequested);

            int actualClusterCount = Math.min(clustersRequested, minUniqueCoordCount);
        
            CoordinateList seeds = mSeeder.generateSeeds(cs, actualClusterCount);
            
            this.checkForCancel();
            
            int seedsGenerated = seeds.getCoordinateCount(); 

            if (clustersRequested > 0 && seeds.getCoordinateCount() < clustersRequested) {
                ph.postMessage("number of requested clusters reduced to "
        			+ seedsGenerated + ", the number of unique coordinates");
            }

            mProtoClusters = new ProtoCluster[seedsGenerated];
        
            for (int i=0; i<seedsGenerated; i++) {
                mProtoClusters[i] = new ProtoCluster(new int[0], seeds.getCoordinates(i, null));
            }

            ph.postMessage("cluster centers initialized");
        
        } finally {
            
            mSeeder = null;
            
        }
    }
    
    private static class ProtoClusterState {
        
        private int[] mMembers;
        private int[] mSizes;
        
        ProtoClusterState(ProtoCluster[] protoClusters) {
            
            final int sz = protoClusters.length;
            int centerLength = 0;
            int nonEmptyCount = 0;
            int memberCount = 0;
            ProtoCluster[] clusterClones = new ProtoCluster[sz];
            
            for (int i=0; i<sz; i++) {
                ProtoCluster c = protoClusters[i];
                if (!c.isEmpty()) {
                    int[] members = (int[]) c.getMembership().clone();
                    memberCount += members.length;
                    double[] center = c.getCenter();
                    if (nonEmptyCount == 0) {
                        centerLength = center.length;
                    }
                    // They're probably sorted already, but make sure.
                    Arrays.sort(members);
                    clusterClones[nonEmptyCount++] = new ProtoCluster(members, center);
                }
            }
            
            Arrays.sort(clusterClones, 0, nonEmptyCount, new Comparator<ProtoCluster> () {
                @Override
                public int compare(ProtoCluster c1, ProtoCluster c2) {
                    final int sz1 = c1.size();
                    final int sz2 = c2.size();
                    if (sz1 < sz2) return -1;
                    if (sz1 > sz2) return 1;
                    // Neither can be empty and neither can have the same first member.
                    final int firstMember1 = c1.getMembership()[0];
                    final int firstMember2 = c2.getMembership()[0];
                    if (firstMember1 < firstMember2) return -1;
                    if (firstMember1 > firstMember2) return 1;
                    // Impossible situation.
                    return 0;
                }
            });
            
            mSizes = new int[nonEmptyCount];
            mMembers = new int[memberCount];
            
            int memberIndex = 0;
            
            for (int i=0; i<nonEmptyCount; i++) {
                int[] members = clusterClones[i].getMembership();
                mSizes[i] = members.length;
                System.arraycopy(members, 0, mMembers, memberIndex, members.length);
                memberIndex += members.length;
            }
            
        }
        
        public int hashCode() {
            int hc = 17;
            for (int i=0; i<mSizes.length; i++) {
                hc = 31*hc + mSizes[i];
            }
            for (int i=0; i<mMembers.length; i++) {
                hc = 31*hc + mMembers[i];
            }
            return hc;
        }
        
        public boolean equals(Object o) {
            if (o == this) return true;
            if (o != null && o.getClass() == this.getClass()) {
                ProtoClusterState other = (ProtoClusterState) o;
                if (other.mSizes.length == this.mSizes.length && other.mMembers.length == this.mMembers.length) {
                    int n = 0;
                    for (int i=0; i<this.mSizes.length; i++) {
                        if (other.mSizes[i] != this.mSizes[i]) return false;
                        int lim = n + mSizes[i];
                        for (int j=n; j<lim; j++) {
                            if (other.mMembers[j] != this.mMembers[j]) return false;
                        }
                        n += mSizes[i];
                    }
                    return true;
                }
            }
            return false;
        }
    }

    private static class ProtoCluster {

        private IntArrayList mPreviousMembership = new IntArrayList();

        private IntArrayList mCurrentMembership = new IntArrayList();

        private double[] mCenter;

        private boolean mUpdateFlag;

        // Whether or not nearestCluster() should consider this one.
        private boolean mConsiderForAssignment = true;

        ProtoCluster(PointCoordinate point) {
            int dim = point.getDimensions();
            mCenter = new double[dim];
            for (int i = 0; i < dim; i++) {
                mCenter[i] = point.getCoordinate(i);
            }
        }

        ProtoCluster(int[] membership, double[] center) {
            int n = membership.length;
            for (int i = 0; i < n; i++) {
                mCurrentMembership.add(membership[i]);
            }
            mCurrentMembership.trimToSize();
            mCenter = center;
        }

        // Returns the Bayes Information Criterion. Must only be called AFTER
        // the center has
        // been updated.
        double computeBIC(CoordinateList cs) {
            mCurrentMembership.trimToSize();
            return ClusterStats.computeBIC(cs, new Cluster[] { new Cluster(
                    mCurrentMembership.elements(), mCenter) });
        }

        int size() {
            return mCurrentMembership.size();
        }

        int[] getMembership() {
            mCurrentMembership.trimToSize();
            return mCurrentMembership.elements();
        }
        
        double[] getCenter() {
            return mCenter;
        }

        synchronized void add(int ndx) {
            mCurrentMembership.add(ndx);
        }

        boolean isEmpty() {
            return mCurrentMembership.size() == 0;
        }

        void setUpdateFlag() {
            mCurrentMembership.trimToSize();
            // Must sort the elements! Since multiple threads may be calling
            // making the cluster assignments, the coordinates may be added out
            // of order. Have to ensure both mCurrentMembership and
            // mPreviousMembership are sorted before the equality check.
            Arrays.sort(mCurrentMembership.elements());
            mUpdateFlag = !mPreviousMembership.equals(mCurrentMembership);
        }

        void checkPoint() {
            mPreviousMembership.clear();
            mPreviousMembership.addAllOf(mCurrentMembership);
            mPreviousMembership.trimToSize();
            mCurrentMembership.clear();
        }

        boolean getConsiderForAssignment() {
            return mConsiderForAssignment;
        }

        void setConsiderForAssignment(boolean b) {
            mConsiderForAssignment = b;
        }

        boolean getUpdateFlag() {
            return mUpdateFlag;
        }

        void updateCenter(CoordinateList cs) {
            if (mCurrentMembership.size() > 0) {
                cs.computeAverage(mCurrentMembership.elements(), mCenter);
            }
        }
    }

    private class SubtaskManager {

        // Lists of workers to which to delegate portions of each task.
        private final List<CenterComputation> mCenterComps = new ArrayList<CenterComputation>();
        private final List<MakeAssignments> mAssigners = new ArrayList<MakeAssignments>();
        
        // Only set if using multiple threads.
        private ExecutorService mThreadPool;
        
        // Constructor.
        SubtaskManager(int numWorkers) {

            if (numWorkers <= 0) {
                throw new IllegalArgumentException("number of workers <= 0: "
                        + numWorkers);
            }

            int coordCount = getCoordinateList().getCoordinateCount();
            int clusterCount = mProtoClusters.length;

            // No point in having more workers than coordinates...
            if (numWorkers > coordCount) {
                numWorkers = coordCount;
            }

            // Divide up the coordinates among the workers.
            int[] coordsPerWorker = new int[numWorkers];
            Arrays.fill(coordsPerWorker, coordCount/numWorkers);
            
            int leftOvers = coordCount - numWorkers * coordsPerWorker[0];
            
            for (int i = 0; i < leftOvers; i++) {
                coordsPerWorker[i]++;
            }

            // Divide up the clusters among the workers.
            int[] clustersPerWorker = new int[numWorkers];
            Arrays.fill(clustersPerWorker, clusterCount/numWorkers);
            
            leftOvers = clusterCount - numWorkers*clustersPerWorker[0];
            
            for (int i=0; i<leftOvers; i++) {
                clustersPerWorker[i]++;
            }

            int startCoord = 0, startCluster = 0;
            
            // Instantiate the workers.
            for (int i = 0; i < numWorkers; i++) {
                int endCoord = startCoord + coordsPerWorker[i];
                int endCluster = startCluster + clustersPerWorker[i];
                
                mCenterComps.add(new CenterComputation(startCluster, endCluster));
                mAssigners.add(new MakeAssignments(startCoord, endCoord));
                
                startCoord = endCoord;
                startCluster = endCluster;
            }

            if (numWorkers > 1) {
                mThreadPool = Executors.newFixedThreadPool(numWorkers);
            }
        }

        // Called to stop the threads of the thread pool, which would otherwise
        // keep waiting for another request to do something.
        void shutdown() {
            if (mThreadPool != null) {
                mThreadPool.shutdownNow();
            }
        }

        // Update the cluster centers.
        boolean makeAssignments() {
            boolean ok = false;
            if (mThreadPool != null) {
                try {
                    mThreadPool.invokeAll(mAssigners);
                    ok = true;
                } catch (InterruptedException ex) {
                    // Can occur if you cancel while the assigners are working.
                    if (!isCancelled()) {
                        Logger.getLogger(KMeansClusterTask.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } else {
                try {
                    // No thread pool, so call the single worker directly.
                    mAssigners.get(0).call();
                    ok = true;
                } catch (Exception ex) {
                    Logger.getLogger(KMeansClusterTask.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            return ok;
        }

        // Compute the distances between the coordinates and those centers with
        // update flags.
        boolean computeCenters() {
            boolean ok = false;
            if (mThreadPool != null) {
                try {
                    mThreadPool.invokeAll(mCenterComps);
                    ok = true;
                } catch (InterruptedException ex) {
                    Logger.getLogger(KMeansClusterTask.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                try {
                    mCenterComps.get(0).call();
                    ok = true;
                } catch (Exception ex) {
                    Logger.getLogger(KMeansClusterTask.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            return ok;
        }

        int getMoves() {
            int moves = 0;
            for (MakeAssignments m: mAssigners) {
                moves += m.getMoves();
            }
            return moves;
        }
        
        List<Move> getMoveList() {
            List<Move> moveList = null;
            if (mTrackMoves) {
                moveList = new ArrayList<Move>();
                for (MakeAssignments m : mAssigners) {
                    moveList.addAll(m.getMoveList());
                }
                Collections.sort(moveList);
            }
            return moveList;
        }
    }
    
    private class CenterComputation implements Callable<Void> {

        private int mStartCluster, mEndCluster;
        private CoordinateList mCS;
        
        CenterComputation(int startCluster, int endCluster) {
            mStartCluster = startCluster;
            mEndCluster = endCluster;
            mCS = getCoordinateList();
        }
        
        public Void call() throws Exception {
            try {
                for (int c = mStartCluster; c < mEndCluster; c++) {
                    ProtoCluster cluster = mProtoClusters[c];
                    if (cluster.getUpdateFlag()) {
                        cluster.updateCenter(mCS);
                    }
                    checkForCancel();
                }
            } catch (CancellationException ce) {
                // Ignore, since doTask() will detect the cancel.
            }
            return null;
        }
    }
    
    private class MakeAssignments implements Callable<Void> {

        private int mStartCoord, mEndCoord;
        private double[] mCoordBuf;
        private DistanceFunc mDistFunc;
        private int mMoves;
        private List<Move> mMoveList;
        
        MakeAssignments(int startCoord, int endCoord) {
            mStartCoord = startCoord;
            mEndCoord = endCoord;
            CoordinateList coords = getCoordinateList();
            mCoordBuf = new double[coords.getDimensionCount()];
            mDistFunc = (DistanceFunc) getDistanceFunc().clone();
        }
        
        public int getMoves() {
            return mMoves;
        }
        
        public List<Move> getMoveList() {
            return mMoveList;
        }
        
        public Void call() throws Exception {
            try {
                mMoves = 0;
                if (mTrackMoves) {
                    mMoveList = new ArrayList<Move> ();
                }
                for (int i = mStartCoord; i < mEndCoord; i++) {
                    int c = nearestCluster(i, mCoordBuf, mDistFunc);
                    if (c >= 0) {
                        mProtoClusters[c].add(i);
                        if (mClusterAssignments[i] != c) {
                            if (mTrackMoves) {
                                mMoveList.add(new Move(i, mClusterAssignments[i], c));
                            }
                            mClusterAssignments[i] = c;
                            mMoves++;
                        }
                    }
                }
            } catch (CancellationException ce) {
                // Ignore, since doTask() will detect the cancel.
            }
            return null;
        }
    }
    
    private static class Move implements Comparable<Move> {
        
        private int mCoordIndex;
        private int mFromCluster;
        private int mToCluster;
        
        Move(int coordIndex, int fromCluster, int toCluster) {
            mCoordIndex = coordIndex;
            mFromCluster = fromCluster;
            mToCluster = toCluster;
        }
        
        int getCoordIndex() {
            return mCoordIndex;
        }
        
        int getFromCluster() {
            return mFromCluster;
        }
        
        int getToCluster() {
            return mToCluster;
        }

        @Override
        public int compareTo(Move o) {
            if (this.mCoordIndex < o.mCoordIndex) return -1;
            if (this.mCoordIndex > o.mCoordIndex) return 1;
            return 0;
        }
        
        public int hashCode() {
            int hc = mCoordIndex;
            hc = 37*hc + mFromCluster;
            return 37*hc + mToCluster;
        }
        
        public boolean equals(Object o) {
            if (o == this) return true;
            if (o instanceof Move) {
                Move that = (Move) o;
                return this.mCoordIndex == that.mCoordIndex &&
                        this.mFromCluster == that.mFromCluster &&
                        this.mToCluster == that.mToCluster;
            }
            return false;
        }
    }
}
