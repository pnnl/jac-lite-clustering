package gov.pnnl.jac.cluster.similarity;

/*
 * StandardHierarchicalClusterTask.java
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
import gov.pnnl.jac.cluster.Dendrogram;
import gov.pnnl.jac.cluster.HierarchicalClusterTaskParams;
import gov.pnnl.jac.cluster.InterleafDistanceMinimizerTask;
import gov.pnnl.jac.collections.ArrayUtil;
import gov.pnnl.jac.geom.distance.DistanceCache;
import gov.pnnl.jac.geom.distance.DistanceCacheFactory;
import gov.pnnl.jac.task.ProgressHandler;
import gov.pnnl.jac.task.TaskEvent;
import gov.pnnl.jac.task.TaskListener;
import gov.pnnl.jac.task.TaskOutcome;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import cern.colt.list.IntArrayList;

/**
 * <p>Implementation of standard hierarchical clustering which takes advantage of
 * the presence of multiple processors.</p>
 */
public class StandardHierarchicalSimilarityClusterTask extends
        AbstractHierarchicalSimilarityClusterTask {

    private InterleafDistanceMinimizerTask mMinimizerTask;

    // Threshold that determines the number of coordinates whose
    // pairwise distances can be cached in RAM.  Defaulting to 128MB,
    // this equates to 5793 coordinates.  If there are more coordinates
    // than this, a file-based cache will have to be used.
    private long mDistanceCacheMemThreshold = 128L * 1024L * 1024L;

    // The file threshold limits the number of coordinates that can be
    // hierarchically clustered.  By default, this threshold is 2GB, limiting
    // the number of coordinates to 23,170.
    private long mDistanceCacheFileThreshold = 2L * 1024L * 1024L * 1024L;

    // The directory in which to store cache files temporarily during the
    // construction of a new dendrogram.
    private File mCacheFileLocation;

    public StandardHierarchicalSimilarityClusterTask(Similarities similarities,
            HierarchicalSimilarityClusterTaskParams params,
            Dendrogram dendrogram) {
        super(similarities, params, dendrogram);
    }

    public StandardHierarchicalSimilarityClusterTask(Similarities similarities,
            HierarchicalSimilarityClusterTaskParams params) {
        this(similarities, params, null);
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        if (super.cancel(mayInterruptIfRunning)) {
            // If in the middle of minimizing interleaf distances,
            // cancel the minimizer task.
            if (mMinimizerTask != null) {
                mMinimizerTask.cancel(mayInterruptIfRunning);
            }
            return true;
        }
        return false;
    }

    /**
     * Returns the memory threshold for storing pairwise distances between coordinates
     * in RAM.  If the memory required is greater than this threshold, but less than
     * the file cache threshold, the
     * distances will be cached in a file.  If the memory required is even
     * greater than the distance cache file threshold, hierarchical clustering
     * will fail.
     * @return - the threshold as a number of bytes.
     */
    public long getDistanceCacheMemoryThreshold() {
        return mDistanceCacheMemThreshold;
    }

    /**
     * Sets the memory threshold for storing pairwise distances between coordinates
     * in RAM.  If the memory required is greater than this threshold, the
     * distances must be cached in a file, as long as the memory required is less than
     * the file threshold.  If the memory required is even
     * greater than the distance cache file threshold, hierarchical clustering
     * will fail.
     * @return - the threshold as a number of bytes.
     */
    public void setDistanceCacheMemoryThreshold(long threshold) {
        mDistanceCacheMemThreshold = threshold;
    }

    /**
     * Returns the file threshold for storing pairwise distances between
     * coordinates.  If the memory required is greater than this threshold,
     * hierarchical clustering will fail.
     * @return - the threshold as a number of bytes.
     */
    public long getDistanceCacheFileThreshold() {
        return mDistanceCacheFileThreshold;
    }

    /**
     * Sets the memory threshold for storing distances from coordinates to cluster
     * centers in RAM.  If the memory required is greater than this threshold, the
     * distances are not stored, but are computed as needed.  On platforms with large
     * amounts of RAM, setting this value high may result in better clustering speed
     * on large coordinate sets.  If not set, the threshold defaults to 128MB.
     */
    public void setDistanceCacheFileThreshold(long threshold) {
        mDistanceCacheFileThreshold = threshold;
    }

    /**
     * Gets the directory in which temporary distance cache files are to be
     * placed during building of a new dendrogram.
     *
     * @return - the directory or null if not set.
     */
    public File getCacheFileLocation() {
        return mCacheFileLocation;
    }

    /**
     * Set the directory in which temporary distance cache files are to be
     * placed during the construction of a new dendrogram.  If the parameter
     * is null, the default temporary directory will be used.  If location
     * does not exist, it will be created when necessary.  Temporary files
     * should not be left in this directory, since they are deleted when no
     * longer needed.
     *
     * @param location
     * @throws IllegalArgumentException - if the location exists but is not
     *   a directory.
     */
    public void setCacheFileLocation(File location) {
        if (location != null && location.exists() && !location.isDirectory()) {
            throw new IllegalArgumentException("not a directory: " + location);
        }
        mCacheFileLocation = location;
    }

    /**
     * Get the algorithm name.
     */
    public String getAlgorithmName() {
        return "hierarchical";
    }

    protected void buildDendrogram() throws IOException {

        ProgressHandler ph = new ProgressHandler(this);

        double beginP = this.getBeginProgress();
        double endP = this.getEndProgress();

        if (endP > beginP) {
            ph.setMinProgressIncrement((endP - beginP) / 100.0);
        }
        ph.setMinTimeIncrement(500L);

        ph.postBegin();

        HierarchicalSimilarityClusterTaskParams params = (HierarchicalSimilarityClusterTaskParams) super
                .getParams();

        double fracForMinimization = params.getMinimizeInterleafDistances() ? 0.05
                : 0.0;
        double fracForRest = 1.0 - fracForMinimization;

        double fracForCacheCreation = 0.05 * fracForRest;
        double fracForInitDistances = 0.10 * fracForRest;
        double fracForMerging = 0.85 * fracForRest;

        Similarities similarities = getSimilarities();
        int coordinateCount = similarities.getRecordCount();
        
        // File used for cache and copy of the cache.  cacheFile is only used if
        // the pairwise distances are stored in a FileDistanceCache.  cacheFile2
        // is only used if optimizing the dendrogram.
        File cacheFile = null, cacheFile2 = null;

        try {

            mDendrogram = new Dendrogram(coordinateCount);
            
            SubtaskManager mgr = null;
            DistanceCache cache = null;
            
            if (coordinateCount > 1) {
            
                ph.subsection(fracForCacheCreation);

                // Create a temp file for the cache, even though it might not be used.
                cacheFile = File.createTempFile("dcache", null, mCacheFileLocation);
                cacheFile.deleteOnExit();

                ph.postMessage("creating new distance cache");

                cache = DistanceCacheFactory.newDistanceCache(
                    coordinateCount, mDistanceCacheMemThreshold,
                    mDistanceCacheFileThreshold, cacheFile);

                ph.postEnd();

                int numProcessors = params.getNumWorkerThreads();
                if (numProcessors <= 0) {
                    numProcessors = Runtime.getRuntime().availableProcessors();
                }

                mgr = new SubtaskManager(numProcessors, params, similarities, cache);

                ph.subsection(fracForInitDistances);

                ph.postMessage("initializing distances in the cache");

                mgr.initializeDistances();

                ph.postEnd();
                
            } else {
                // Trivial case of only one coordinate -- the dendrogram is done!
                
                // Just update the progress.
                ph.subsection(fracForCacheCreation + fracForInitDistances);
                ph.postEnd();
            }
            
            boolean done = mDendrogram.isFinished();
            // To hold the indices of the nearest neighbors to be merged in each iteration.
            int[] nnPair = new int[2];
            double[] nnDistance = new double[1];

            ph.subsection(fracForMerging, coordinateCount - 1);

            ph.postMessage("merging nodes");

            while (!done) {

                if (!mgr.lookupNearestNeighbors(nnPair, nnDistance)) {
                    error("problem finding nearest neighbors");
                }

                int mergeID = mDendrogram.mergeNodes(nnPair[0], nnPair[1],
                        nnDistance[0]);

                done = mDendrogram.isFinished();

                if (!done) {
                    mgr.updateDistances(mergeID);
                    mgr.updateNearestNeighbors();
                }

                ph.postStep();

            } // while

            ph.postEnd();

            if( mgr != null ) {
            	mgr.shutdown();
            }
            
            mgr = null;
            cache = null;
            System.gc();

            if (params.getMinimizeInterleafDistances() && coordinateCount > 1) {

                ph.subsection(fracForMinimization);

                final ProgressHandler ph2 = ph;

                // Delegate the minimization of interleaf distances to another
                // Task, but call that Task's run method directly.
                mMinimizerTask = new InterleafDistanceMinimizerTask(
                        mDendrogram, mDistances.asReadOnlyDistanceCache());

                // This anonymous TaskListener just forwards messages
                // to the listeners for this Task.
                mMinimizerTask.addTaskListener(new TaskListener() {
                    public void taskBegun(TaskEvent e) {
                        ph2.postMessage("minimizing dendrogram interleaf distances");
                    }

                    public void taskMessage(TaskEvent e) {
                        ph2.postMessage(e.getMessage());
                    }

                    public void taskProgress(TaskEvent e) {
                    }

                    public void taskEnded(TaskEvent e) {
                    }
                });

                ph.postBegin();

                // Call the run method directly.  No need to create a new thread here.
                mMinimizerTask.run();

                // If something goes wrong in the minimizer, be sure to detect it
                if (mMinimizerTask.getTaskOutcome() == TaskOutcome.ERROR) {
                    error(mMinimizerTask.getErrorMessage());
                }

                ph.postEnd();
            }

            ph.postEnd();

        } finally {

            // Clean up temporary files.
            if (cacheFile != null && cacheFile.exists()) {
                cacheFile.delete();
            }
            if (cacheFile2 != null && cacheFile2.exists()) {
                cacheFile2.delete();
            }

        }

    }

    private class SubtaskManager {

        // Codes for what the workers are currently doing.
        //
        // Nothing currently.
        static final int DOING_NOTHING = 0;

        // Initial pairwise distance computation -- done once.
        static final int INITIALIZING_DISTANCES = 1;

        // Recomputation of distances affected by the last merge.
        static final int UPDATING_DISTANCES = 2;

        // Updating of the dendrogram nodes.
        static final int UPDATING_NEAREST_NEIGHBORS = 3;

        // What the object is currently doing.
        private int mDoing = DOING_NOTHING;

        // True if the at least one of the Workers is doing something.
        private boolean mWorking;

        // The executor that runs the Workers. When in single-processor mode, it
        // directly calls
        // the run method of the single worker instead of using a separate
        // thread.
        // In multiple-processor mode, it's a ThreadPoolExecutor.
        private Executor mExecutor;

        // A Barrier to wait on multiple Workers to finish up the current task.
        // This is null if in single-processor mode, since there's only one
        // Worker.
        private CyclicBarrier mBarrier;

        // The worker objects which implement Runnable.
        private Worker[] mWorkers;

        // Indices of nearest neighbors. The index of the nearest neighbor of
        // node n is
        // found at mNNIndices[n]
        private int[] mNNIndices;

        // Nearest neighbor distances corresponding 1:1 with mNNIndices.
        private double[] mNNDistances;

        private int mMergeIndex, mLeftIndex, mRightIndex;

        private int mLeftCount, mRightCount;

        private Similarities mSims;

        private int mCoordCount;

        private DistanceCache mCache;

        private HierarchicalClusterTaskParams.Linkage mLinkage;

        // Constructor.
        SubtaskManager(int numWorkers,
                HierarchicalSimilarityClusterTaskParams params,
                Similarities similarities, DistanceCache cache) {

            if (numWorkers <= 0) {
                throw new IllegalArgumentException("number of workers <= 0: "
                        + numWorkers);
            }

            mSims = similarities;
            mCache = cache;
            mCoordCount = mSims.getRecordCount();

            mNNIndices = new int[mCoordCount];
            Arrays.fill(mNNIndices, -1); // -1 indicates "not assigned"
            mNNDistances = new double[mCoordCount];
            Arrays.fill(mNNDistances, Double.MAX_VALUE);

            mLinkage = params.getLinkage();

            long distanceCount = ((long) mCoordCount)
                    * ((long) mCoordCount - 1L) / 2L;
            if (numWorkers > mCoordCount) {
                postMessage("reducing number of worker threads to the number of coordinates");
                numWorkers = mCoordCount;
            } else if (numWorkers > distanceCount) {
                postMessage("reducing number of worker threads to the number of distances");
                numWorkers = (int) distanceCount;
            }

            long distancesSoFar = 0L;
            int coordsSoFar = 0;

            // Create the Updaters.
            mWorkers = new Worker[numWorkers];

            // Need to apportion the work among the workers.
            for (int i = 0; i < numWorkers; i++) {

                long distancesForThisWorker = Math
                        .round(((double) (distanceCount * (i + 1)))
                                / numWorkers)
                        - distancesSoFar;

                int coordsForThisWorker = (int) Math
                        .round(((double) mCoordCount) * (i + 1) / numWorkers)
                        - coordsSoFar;

                mWorkers[i] = new Worker(distancesSoFar,
                        distancesForThisWorker, coordsSoFar,
                        coordsForThisWorker);

                distancesSoFar += distancesForThisWorker;
                coordsSoFar += coordsForThisWorker;
            }

            if (numWorkers == 1) {
                // Assign a simple executor which directly calls the single
                // worker's run method. Don't instantiate a barrier.
                mExecutor = new Executor() {
                    public void execute(Runnable runnable) {
                        if (!Thread.interrupted()) {
                            runnable.run();
                        } else {
                            throw new RejectedExecutionException();
                        }
                    }
                };
            } else { // numWorkers > 1 -- multi-processor mode.

                // Need the barrier to coordinate waiting on all the workers,
                // executing on different threads, to finish the current task.

                mBarrier = new CyclicBarrier(numWorkers, new Runnable() {
                    public void run() {
                        // Method to execute when all threads reach the barrier. This
                        // will break the subtask manager out of a wait.
                        notifyBarrier();
                    }
                });

                // Use the pooled executor, which manages the workers as a
                // thread pool.
                ThreadPoolExecutor pooledExecutor = (ThreadPoolExecutor) Executors
                        .newFixedThreadPool(numWorkers);
                // Keeps all threads alive until we interrupt them.
                pooledExecutor.setKeepAliveTime(Long.MAX_VALUE,
                        TimeUnit.MILLISECONDS);

                mExecutor = pooledExecutor;
            }
        }

        // Null the items that could be consuming large amounts of
        // memory.
        protected void finalize() {
            mSims = null;
            mCache = null;
        }

        // Called to stop the threads of the thread pool, which would otherwise
        // keep waiting for another request to do something.
        void shutdown() {
            if (mExecutor instanceof ThreadPoolExecutor) {
                ((ThreadPoolExecutor) mExecutor).shutdownNow();
            }
        }

        /**
         * Find the nearest neighbor pair, placing the indices into the
         * provided array of length 2.
         * @param indices
         * @return - true if a pair is found.
         */
        boolean lookupNearestNeighbors(int[] indices, double[] distance) {
            boolean found = false;
            double dmin = Double.MAX_VALUE;
            int index1 = -1, index2 = -1;
            int len = mNNIndices.length;
            for (int i = 0; i < len; i++) {
                int nindex = mNNIndices[i];
                if (nindex >= 0) {
                    double d = mNNDistances[i];
                    if (d < dmin) {
                        index1 = i;
                        index2 = nindex;
                        dmin = d;
                        found = true;
                    }
                }
            }
            if (found) {
                indices[0] = Math.min(index1, index2);
                indices[1] = Math.max(index1, index2);
                distance[0] = dmin;
            }
            return found;
        }

        boolean initializeDistances() {
            mDoing = INITIALIZING_DISTANCES;
            return work();
        }

        boolean updateDistances(int mergeID) {

            mMergeIndex = mergeID;

            // One of these, usually the left, is the same as mMergeIndex.
            mLeftIndex = mDendrogram.leftChildID(mMergeIndex);
            mRightIndex = mDendrogram.rightChildID(mMergeIndex);

            mLeftCount = mDendrogram.nodeSize(mLeftIndex);
            mRightCount = mDendrogram.nodeSize(mRightIndex);

            // The usual convention is for the merge index to be the lesser of
            // the left child index and the right child index.  Since the merge index
            // is definitely one of these, the count for the child that merge index
            // is equal to is too large.  It's actually the count of the merged
            // node and needs to be adjusted downward.
            //
            if (mLeftIndex == mMergeIndex) {
                mLeftCount -= mRightCount;
                // The other index is no longer in contention.
                mNNIndices[mRightIndex] = -1;
            } else {
                mRightCount -= mLeftCount;
                // The other index is no longer in contention.
                mNNIndices[mLeftIndex] = -1;
            }

            mDoing = UPDATING_DISTANCES;
            return work();
        }

        boolean updateNearestNeighbors() {
            mDoing = UPDATING_NEAREST_NEIGHBORS;
            return work();
        }

        // Perform the current -- mDoing should be set to the proper value.
        private boolean work() {
            boolean ok = false;
            mWorking = true;
            try {
                if (mBarrier != null) { // In multi-processor mode.
                    // Resets the barrier.
                    mBarrier.reset();
                }
                // Now execute the run methods on the Updaters.
                for (int i = 0; i < mWorkers.length; i++) {
                    mExecutor.execute(mWorkers[i]);
                }
                if (mBarrier != null) {
                    // All threads wait until the barrier issues a notify() to them all.
                    waitOnBarrier();
                    // Check the status of the barrier.
                    ok = !mBarrier.isBroken();
                } else {
                    // No barrier, so everything must be ok.
                    ok = true;
                }
            } catch (RejectedExecutionException ree) {
                // Could happen if shutdown while in the middle of something.
            } finally {
                mWorking = false;
            }
            return ok;
        }

        // Not called unless in multi-processor mode.
        private synchronized void waitOnBarrier() {
            // Don't wait unless mWorking is true. It's possible for the work to
            // be done so quickly, notifyBarrier() gets called before this
            // method.
            // If it went into a wait() after notifyBarrier(), it would
            // deadlock.
            if (mWorking) {
                try {
                    wait();
                } catch (InterruptedException ie) {
                }
            }
        }

        // Again, only applicable when in multi-processor mode when there's a
        // non-null barrier.
        private synchronized void notifyBarrier() {
            // Prevents waitOnBarrier() from waiting if notifyBarrier() gets
            // called first.
            mWorking = false;
            notify();
        }

        // Class that does the deeds.
        //
        private class Worker implements Runnable {

            private int mIndex1Min, mIndex1Max;

            private int mIndex2Min, mIndex2Max;

            private int mStartCoord;

            private int mCoordCount;

            // The coordinate set -- ref. to same object used by everything
            // else.
            // Set to prevent having to call getCoordinateSet() repeatedly.
            private SimilarityDistances mDistances;

            // Constructor
            Worker(long startDistance, long distanceCount, int startCoord,
                    int coordCount) {

                // Set the endpoints for the indices. These are the indices
                // into the distance cache.
                int[] indices = DistanceCacheFactory.getIndicesForDistance(
                        startDistance, mCache);
                mIndex1Min = indices[0];
                mIndex2Min = indices[1];

                indices = DistanceCacheFactory.getIndicesForDistance(
                        startDistance + distanceCount - 1, mCache);

                mIndex1Max = indices[0];
                mIndex2Max = indices[1];

                mStartCoord = startCoord;
                mCoordCount = coordCount;

                mDistances = getDistances();
            }

            public void run() {

                try {

                    switch (mDoing) {
                    case INITIALIZING_DISTANCES:
                        workerInitializeDistances();
                        break;
                    case UPDATING_DISTANCES:
                        workerUpdateDistances();
                        break;
                    case UPDATING_NEAREST_NEIGHBORS:
                        workerUpdateNearestNeighbors();
                        break;
                    }

                } finally {
                    // Barrier non-null only if in multi-processor mode.
                    if (mBarrier != null) {
                        try {
                            mBarrier.await();
                        }
                        // barrier.broken() will return true if either of these
                        // exceptions
                        // happen.
                        catch (InterruptedException ex) {
                        } catch (BrokenBarrierException ex) {
                        }
                    }
                }
            }

            // Compute the distances.
            //
            private void workerInitializeDistances() {

                if (mCache != null) {

                    final int setAtATime = 1024;
                    int[] indices1 = new int[setAtATime];
                    int[] indices2 = new int[setAtATime];
                    double[] distances = new double[setAtATime];
                    int count = 0;

                    int numIndices = mCache.getNumIndices();

                    try {

                        for (int i = mIndex1Min; i <= mIndex1Max; i++) {

                            int jmin = i == mIndex1Min ? mIndex2Min : i + 1;
                            int jmax = i == mIndex1Max ? mIndex2Max
                                    : numIndices - 1;

                            for (int j = jmin; j <= jmax; j++) {

                                indices1[count] = i;
                                indices2[count] = j;

                                double distance = mDistances.getDistance(i, j);
                                // These 2 if-blocks initialize the mNNDistances and
                                // mNNIndices.
                                if (distance < mNNDistances[i]) {
                                    mNNDistances[i] = distance;
                                    mNNIndices[i] = j;
                                }
                                if (distance < mNNDistances[j]) {
                                    mNNDistances[j] = distance;
                                    mNNIndices[j] = i;
                                }

                                distances[count++] = distance;

                                if (count == setAtATime) {
                                    mCache.setDistances(indices1, indices2,
                                            distances);
                                    count = 0;
                                }

                                checkForCancel();

                            } // for (int j...

                        } // for (int i...

                        if (count > 0) {

                            if (count < setAtATime) {
                                indices1 = ArrayUtil
                                        .section(indices1, 0, count);
                                indices2 = ArrayUtil
                                        .section(indices2, 0, count);
                                distances = ArrayUtil.section(distances, 0,
                                        count);
                            }

                            mCache.setDistances(indices1, indices2, distances);
                        }

                    } catch (IOException ioe) {

                        String errMsg = ioe.getMessage();
                        if (errMsg == null)
                            errMsg = ioe.toString();
                            
                        error("error initializing pairwise distances: " + errMsg);
                        
                        return;

                    } catch (CancellationException ce) {
                        // Ignore, since the thread running the cluster task
                        // will
                        // report the cancel.
                    }
                }
            }

            // Update nearest neighbors.
            //
            private void workerUpdateNearestNeighbors() {
                try {

                    int lim = mStartCoord + mCoordCount;

                    for (int i = mStartCoord; i < lim; i++) {

                        int nnIndex = mNNIndices[i];

                        if (nnIndex >= 0) {

                            if (i == mMergeIndex || nnIndex == mLeftIndex
                                    || nnIndex == mRightIndex) {

                                int newNNIndex = i;
                                double newNNDistance = Double.MAX_VALUE;

                                int n = mNNIndices.length;
                                for (int j = i + 1; j < n; j++) {
                                    if (mNNIndices[j] >= 0) {
                                        double d = mCache.getDistance(i, j);
                                        if (d < newNNDistance) {
                                            newNNIndex = j;
                                            newNNDistance = d;
                                        }
                                    }
                                }

                                checkForCancel();

                                // The "bug" discussed above will sometimes set a node's
                                // nearest neighbor id to itself with a nn distance of
                                // Double.MAX_VALUE.  But it won't cause any harm.
                                mNNIndices[i] = newNNIndex;
                                mNNDistances[i] = newNNDistance;

                            } // if (i == mMergeNodeID ...

                        } // if (mNodes[i] != null

                    } // for (int i=mStartNode...

                } catch (IOException ioe) {

                    String errMsg = ioe.getMessage();
                    if (errMsg == null)
                        errMsg = ioe.toString();
                    error("error updating nearest neighbors: " + errMsg);

                } catch (CancellationException ce) {
                    // Ignore, since the thread running the cluster task
                    // will
                    // report the cancel.
                }
            }

            private void workerUpdateDistances() {

                try {

                    int lim = mStartCoord + mCoordCount;

                    IntArrayList getList1 = new IntArrayList();
                    IntArrayList getList2 = new IntArrayList();

                    for (int i = mStartCoord; i < lim; i++) {
                        if (mNNIndices[i] >= 0 && i != mMergeIndex) {
                            getList1.add(i);
                            getList1.add(i);
                            getList2.add(mLeftIndex);
                            getList2.add(mRightIndex);
                        }
                        checkForCancel();
                    }

                    getList1.trimToSize();
                    getList2.trimToSize();

                    int sz = getList1.size();

                    if (sz > 0) { // getList1 and getList2 are the same length

                        int[] indices1 = getList1.elements();
                        int[] indices2 = getList2.elements();

                        getList1.clear();
                        getList2.clear();

                        double[] distances = new double[sz];
                        mCache.getDistances(indices1, indices2, distances);

                        // No longer need.
                        indices2 = null;

                        // sz is always even
                        double[] distancesToSet = new double[sz / 2];
                        int[] setIndices1 = new int[sz / 2];
                        Arrays.fill(setIndices1, mMergeIndex);
                        int[] setIndices2 = new int[sz / 2];

                        int count = 0;
                        for (int i = 0; i < sz; i += 2) {
                            switch (mLinkage) {
                            case COMPLETE:
                                distancesToSet[count] = Math.max(distances[i],
                                        distances[i + 1]);
                                break;
                            case SINGLE:
                                distancesToSet[count] = Math.min(distances[i],
                                        distances[i + 1]);
                                break;
                            case MEAN:
                                distancesToSet[count] = (mLeftCount
                                        * distances[i] + mRightCount
                                        * distances[i + 1])
                                        / (mLeftCount + mRightCount);
                                break;
                            default:
                                error("unsupported linkage type: " + mLinkage);
                            }

                            setIndices2[count] = indices1[i];
                            count++;
                            checkForCancel();
                        }

                        // No longer need.
                        indices1 = null;

                        mCache.setDistances(setIndices1, setIndices2,
                                distancesToSet);
                    }
                } catch (IOException ioe) {
                    String errMsg = ioe.getMessage();
                    if (errMsg == null)
                        errMsg = ioe.toString();
                    error("error updating pairwise distances: " + errMsg);
                } catch (CancellationException ce) {
                    // Ignore, since the thread running the cluster task
                    // will
                    // report the cancel.
                }
            }
        }
    }
}
