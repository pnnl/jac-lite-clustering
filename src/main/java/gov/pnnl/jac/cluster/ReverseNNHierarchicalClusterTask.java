/*
 * ReverseNNHierarchicalClusterTask.java
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
import gov.pnnl.jac.geom.distance.Cosine;
import gov.pnnl.jac.geom.distance.DistanceCacheFactory;
import gov.pnnl.jac.geom.distance.DistanceFunc;
import gov.pnnl.jac.geom.distance.EuclideanNoNaN;
import gov.pnnl.jac.task.ProgressHandler;
import gov.pnnl.jac.task.Task;
import gov.pnnl.jac.task.TaskEvent;
import gov.pnnl.jac.task.TaskListener;
import gov.pnnl.jac.task.TaskOutcome;
import gov.pnnl.jac.util.ArrayUtils;
import gov.pnnl.jac.util.MethodTimer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cern.colt.bitvector.BitVector;

/**
 * <p>An implementation of hierarchical clustering which uses the
 * Reverse-nearest-neighbor approach.  In each merge iteration, rather than
 * exhaustively searching for the two nearest nodes and merging them, RNN merges the
 * first pair of nodes found which are nearest neighbors.  The RNN approach 
 * can handle a vastly larger amount of records than standard 
 * hierarchical, but does not give identical results.</p>
 * 
 * <p>Any <tt>distanceFunc</tt> implementation can be used with this
 * class.  By definition, RNN hierarchical clustering with Euclidean 
 * distances is called Ward's clustering.  With cosine distances, the 
 * algorithm is called Group Average clustering.</p>
 * 
 * @author d3j923
 *
 */
public class ReverseNNHierarchicalClusterTask extends
AbstractHierarchicalClusterTask {

    private Task<?> mCurrentSubtask;
    
    public ReverseNNHierarchicalClusterTask(
            CoordinateList cs,
            HierarchicalClusterTaskParams params,
            Dendrogram dendrogram) {
        super(cs, params, dendrogram);
    }

    public ReverseNNHierarchicalClusterTask(
            CoordinateList cs,
            HierarchicalClusterTaskParams params) {
        this(cs, params, null);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (super.cancel(mayInterruptIfRunning)) {
            // If in the middle of doing one of the subtasks, cancel it.
            if (mCurrentSubtask != null) {
                mCurrentSubtask.cancel(mayInterruptIfRunning);
            }
            return true;
        }
        return false;
    }

    @Override
    public String getAlgorithmName() {
        HierarchicalClusterTaskParams params = (HierarchicalClusterTaskParams) getParams();
        if (params.getDistanceFunc() instanceof EuclideanNoNaN) {
            return "Ward's";
        } else if (params.getDistanceFunc() instanceof Cosine) {
            return "group average";
        }
        return "reverse nearest neighbor hierarchical using distance metric " + params.getDistanceFunc().methodName();
    }

    // Each element n holds the index of the nearest
    // neighbor to coordinate n, or -1 if not set.
    private int[] mNearestNeighbors;

    // If mNearestNeighbors[n] >= 0, mNearestNeighborDistances[n]
    // holds the distance to n's nearest neighbor.
    private double[] mNearestNeighborDistances;

    // To keep track of indices still in contention for
    // nearest neighbor searches.
    //	private IntSet mAvailableIndices;

    // Distance function used.
    private DistanceFunc mDistFunc;

    // For clusters of size > 1, holds the cluster centroids.
    private IntObjectMap<double[]> mCentroidMap;

    private BitVector mUnavailabilityBits;
    private BitVector mWhichDistancesToCalculate;
    
    private ExecutorService mThreadPool;

    private List<DistanceCalculator> mCalculators;

    // Used by the DistanceCalculators -- these must be set by
    // nearestNeighbor() each time before invoking the calculators.
    private int mCurrentIndex;
    private int mCurrentSize;
    private double[] mCurrentCoordValues;
    private double[] mCurrentDistances;

    // Find nearest neighbor of coordinate with specified index.
    // The two buffers are passed to avoid repeated reallocation.
    private int nearestNeighbor(int index /*, ClusterList hintClusters */) throws Exception {

        try {

            FROM1 = MethodTimer.startMethodTimer(FROM1, null);
            int nn = mNearestNeighbors[index];

            if (nn == -1) {

                CoordinateList cs = getCoordinateList();
                int coordCount = cs.getCoordinateCount();

                int sz = mDendrogram.nodeSize(index);

                if (sz > 1) {
                    // Use the centroid from the centroid map. One HAS to be
                    // in the map, otherwise there's a programming error.
                    double[] centroid = (double[]) mCentroidMap.get(index);
                    System.arraycopy(centroid, 0, mCurrentCoordValues, 0, centroid.length); 
                } else {
                    // It's still a leaf node. Have to use the coordinate
                    // from the coordinate set.
                    cs.getCoordinates(index, mCurrentCoordValues);
                }

//                mWhichDistancesToCalculate.clear();
//                if (hintClusters != null) {
//                    int[] inSameCluster = getIndexesInSameCluster(index, hintClusters);
//                    final int len = inSameCluster.length;
//                    for (int i=0; i<len; i++) {
//                        int n = inSameCluster[i];
//                        if (!mUnavailabilityBits.get(n)) {
//                            mWhichDistancesToCalculate.set(n);
//                        }
//                    }
//                }
                
                mCurrentIndex = index;
                mCurrentSize = sz;

                if (mThreadPool != null) {
                    mThreadPool.invokeAll(mCalculators);
                } else {
                    mCalculators.get(0).call();
                }

                double minDist = Double.MAX_VALUE;

                for (int ni = 0; ni < coordCount; ni++) {
                    if (ni != index && !mUnavailabilityBits.get(ni)) {
                        double d = mCurrentDistances[ni];
                        if (d < minDist) {
                            minDist = d;
                            nn = ni;
                        }
                    }
                }

                mNearestNeighbors[index] = nn;
                mNearestNeighborDistances[index] = minDist;
            }

            return nn;

        } finally {
            MethodTimer.endMethodTimer(FROM1);
        }
    }

    private ClusterList generateHintClusters(ProgressHandler ph, double progressFraction) {
    
        HierarchicalClusterTaskParams params = (HierarchicalClusterTaskParams) getParams();
        
        DistanceFunc df = params.getDistanceFunc().clone();
        int threadCount = params.getNumWorkerThreads();
        
        ClusterSeeder seeder = new KMeansPlusPlusSeeder(1234L, df.clone());
        
        SamplingXMeansClusterTaskParams xmeansParams = new SamplingXMeansClusterTaskParams(
                threadCount,
                df,  seeder, 1, params.getClustersDesired(), true,
                0.1, 10000, 100000, 1234L);
        
        SamplingXMeansClusterTask samplingTask = new SamplingXMeansClusterTask(this.getCoordinateList(), 
                xmeansParams);
        
        double startP = ph.getCurrentProgress();
        double endP = startP + progressFraction * (this.getEndProgress() - this.getBeginProgress());
        
        samplingTask.setProgressEndpoints(startP, endP);
        
        samplingTask.addTaskListener(new TaskListener() {

            @Override
            public void taskBegun(TaskEvent e) {
                postMessage("generating hint clusters using sampling xmeans");
            }

            @Override
            public void taskMessage(TaskEvent e) {
            }

            @Override
            public void taskProgress(TaskEvent e) {
                postProgress(e.getProgress());
            }

            @Override
            public void taskEnded(TaskEvent e) {
            }
            
        });
        
        mCurrentSubtask = samplingTask;
        
        samplingTask.run();
        
        ClusterList result = null;
        if (samplingTask.getTaskOutcome() == TaskOutcome.SUCCESS) {
            try {
                result = samplingTask.get();
            } catch (InterruptedException e1) {
            } catch (ExecutionException e1) {
            }
        }
        
        return result;
    }
    
    // Used with MethodTimer to benchmark sections.
    private static MethodTimer.Marker FROM1, FROM2;

    @Override
    protected void buildDendrogram() throws Exception {

        try {

            HierarchicalClusterTaskParams params = (HierarchicalClusterTaskParams) getParams();

            double beginP = getBeginProgress();
            double endP = getEndProgress();

            ProgressHandler ph = new ProgressHandler(this, beginP, endP);

            if (endP > beginP) {
                ph.setMinProgressIncrement((endP - beginP)/100);
            }

            ph.setMinTimeIncrement(500L);

            ph.postBegin();
            
//            double hintFraction = 0.1;
            double buildFraction = params.getMinimizeInterleafDistances() ? 0.95 : 1.0;
            double minimizeFraction = params.getMinimizeInterleafDistances() ? 0.05 : 0.0;

//            ph.subsection(hintFraction);
//            ClusterList hintClusters = generateHintClusters(ph, hintFraction);
//            ph.postEnd();
            
            CoordinateList cs = getCoordinateList();
            int coordCount = cs.getCoordinateCount();
            int dim = cs.getDimensionCount();

            mCurrentCoordValues = new double[dim];
            mCurrentDistances = new double[coordCount];

            mUnavailabilityBits = new BitVector(coordCount);
            mWhichDistancesToCalculate = new BitVector(coordCount);
            int[] shuffledCoordIndices = new int[coordCount];
            for (int i=0; i<coordCount; i++) {
                shuffledCoordIndices[i] = i;
            }

            Random r = new Random(params.getRandomSeed());

            ArrayUtils.shuffle(shuffledCoordIndices, r);

            mCentroidMap = new IntObjectHashMap<double[]>();

            int threadCount = params.getNumWorkerThreads();
            if (threadCount <= 0) {
                threadCount = Runtime.getRuntime().availableProcessors();
            }

            int coordsSoFar = 0;

            // Element at n holds the index m of the nearest
            // neighbor of coordinate n. -1 indicates not
            // set.
            mNearestNeighbors = new int[coordCount];
            Arrays.fill(mNearestNeighbors, -1);
            mNearestNeighborDistances = new double[coordCount];

            mDistFunc = params.getDistanceFunc();
            mDendrogram = new Dendrogram(coordCount);

            // Create the DistanceCalculators.
            mCalculators = new ArrayList<DistanceCalculator>(threadCount);

            // Need to apportion the work among the workers.
            for (int i=0; i<threadCount; i++) {
                int coordsForThisWorker = (int) Math.round(((double)coordCount)*(i+1)/threadCount) - coordsSoFar;
                mCalculators.add(new DistanceCalculator(coordsSoFar, coordsSoFar + coordsForThisWorker));
                coordsSoFar += coordsForThisWorker;
            }
            
            assert coordsSoFar == coordCount;

            if (threadCount > 1) {
                mThreadPool = Executors.newFixedThreadPool(threadCount);
            }

            final double[] coordBuf1 = new double[dim];
            final double[] coordBuf2 = new double[dim];

            int currentIndexPos = 0;
            // Arbitrarily pick the starting point for the first search.
            int currentIndex = shuffledCoordIndices[currentIndexPos];

            ph.subsection(buildFraction, coordCount - 1);

            while (!mDendrogram.isFinished()) {

                int nn = nearestNeighbor(currentIndex);

                if (nearestNeighbor(nn) == currentIndex) {

                    try {

                        FROM2 = MethodTimer.startMethodTimer(FROM2, null);

                        int sz1 = mDendrogram.nodeSize(currentIndex);
                        int sz2 = mDendrogram.nodeSize(nn);

                        double[] buf1 = null, buf2 = null;
                        if (sz1 > 1) {
                            buf1 = (double[]) mCentroidMap.get(currentIndex);
                        } else {
                            buf1 = coordBuf1;
                            cs.getCoordinates(currentIndex, buf1);
                        }
                        if (sz2 > 1) {
                            buf2 = (double[]) mCentroidMap.get(nn);
                        } else {
                            buf2 = coordBuf2;
                            cs.getCoordinates(nn, buf2);
                        }

                        int mergeIndex = mDendrogram.mergeNodes(currentIndex, nn,
                                mNearestNeighborDistances[currentIndex]);

                        int invalidatedIndex = mergeIndex == currentIndex ? nn
                                : currentIndex;
                        mUnavailabilityBits.set(invalidatedIndex);

                        if (mCentroidMap.containsKey(invalidatedIndex)) {
                            // Save some memory by not storing centroids we no
                            // longer need.
                            mCentroidMap.remove(invalidatedIndex);
                        }

                        double totalSz = sz1 + sz2;

                        double[] centroid = null;
                        if (sz1 > 1) {
                            // Reuse the array.
                            centroid = buf1;
                        } else if (sz2 > 1) {
                            // Reuse the array.
                            centroid = buf2;
                        } else {
                            // Have to allocate a new centroid.
                            centroid = new double[dim];
                        }

                        for (int i = 0; i < dim; i++) {
                            centroid[i] = (sz1 * buf1[i] + sz2 * buf2[i]) / totalSz;
                        }

                        mCentroidMap.put(mergeIndex, centroid);
                        mNearestNeighbors[mergeIndex] = -1;
                        mNearestNeighbors[invalidatedIndex] = -1;

                        for (int i = 0; i < coordCount; i++) {

                            final int nni = mNearestNeighbors[i];

                            if (nni >= 0) {

                                final int nsz = mDendrogram.nodeSize(i);

                                double[] buf = null;
                                if (nsz > 1) {
                                    buf = (double[]) mCentroidMap.get(i);
                                } else {
                                    cs.getCoordinates(i, coordBuf1);
                                    buf = coordBuf1;
                                }

                                double m = ((double) nsz * totalSz) / (nsz + totalSz);
                                double d = m * mDistFunc.distanceBetween(centroid, buf);

                                // The old nearest neighbor was one of the nodes that
                                // were just merged. If it's moved closer, the merged
                                // node is still the nearest neighbor.
                                if (nni == mergeIndex || nni == invalidatedIndex) {
                                    if (d <= mNearestNeighborDistances[i]) {
                                        mNearestNeighbors[i] = mergeIndex;
                                        mNearestNeighborDistances[i] = d;
                                    } else {
                                        // It moved away, so will have to compute
                                        // all the distances to determine who the
                                        // new nearest neighbor is.
                                        mNearestNeighbors[i] = -1;
                                    }
                                } else {
                                    // The old nearest neighbor was neither of the
                                    // merged
                                    // nodes. If the merged node is now nearer than the
                                    // old nearest neighbor, make it the new nearest
                                    // neighbor.
                                    if (d < mNearestNeighborDistances[i]) {
                                        mNearestNeighbors[i] = mergeIndex;
                                        mNearestNeighborDistances[i] = d;
                                    }
                                }

                            }
                        }

                        if (!mDendrogram.isFinished()) {
                            // Set the beginning of the next NN search chain to be the
                            // first index < nn, if one is present. If not, pick an
                            // arbitrary point.
                            currentIndex = nn - 1;
                            if (currentIndex < 0 || mUnavailabilityBits.get(currentIndex)) {
                                do {
                                    currentIndexPos++;
                                    if (currentIndexPos >= coordCount) {
                                        currentIndexPos = 0;
                                    }
                                    currentIndex = shuffledCoordIndices[currentIndexPos];
                                } while (mUnavailabilityBits.get(currentIndex));
                            }
                        }

                        ph.postStep();

                    } finally {

                        MethodTimer.endMethodTimer(FROM2);

                    }

                } else {

                    currentIndex = nn;

                }
            }

            ph.postEnd();

            if (params.getMinimizeInterleafDistances()) {

                ph.subsection(minimizeFraction);
                
                // Delegate the minimization of interleaf distances to another
                // Task, but call that Task's run method directly.
                InterleafDistanceMinimizerTask minimizerTask = new InterleafDistanceMinimizerTask(mDendrogram,
                        DistanceCacheFactory.asReadOnlyDistanceCache(cs, mDistFunc));

                // This anonymous TaskListener just forwards messages
                // to the listeners for this Task.
                minimizerTask.addTaskListener(new TaskListener() {
                    @Override
                    public void taskBegun(TaskEvent e) {
                        postMessage("minimizing dendrogram interleaf distances");
                    }

                    @Override
                    public void taskMessage(TaskEvent e) {
                        postMessage(e.getMessage());
                    }

                    @Override
                    public void taskProgress(TaskEvent e) {
                    }

                    @Override
                    public void taskEnded(TaskEvent e) {
                    }
                });

                mCurrentSubtask = minimizerTask;
                
                minimizerTask.run();
                // If something goes wrong in the minimizer, be sure to detect it
                if (minimizerTask.getTaskOutcome() == TaskOutcome.ERROR) {
                    error(minimizerTask.getErrorMessage());
                }
                
                ph.postEnd();

            }

            ph.postEnd();

        } finally {

            if (mThreadPool != null) {
                mThreadPool.shutdownNow();
                mThreadPool = null;
            }
        }
    }
    
    private static int[] getIndexesInSameCluster(int id, ClusterList hintClusters) {
        
        IntList indexList = new IntArrayList();
        
        final int numClusters = hintClusters.getClusterCount();
        for (int i=0; i<numClusters; i++) {
            Cluster c = hintClusters.getCluster(i);
            if (c.contains(id)) {
                indexList.addAll(c.getMembership());
                break;
            }
        }

        return indexList.toArray();
    }

    private static int[] getIndexesInOtherClusters(int id, ClusterList hintClusters) {
        
        IntList indexList = new IntArrayList();
        
        final int numClusters = hintClusters.getClusterCount();
        for (int i=0; i<numClusters; i++) {
            Cluster c = hintClusters.getCluster(i);
            if (!c.contains(id)) {
                indexList.addAll(c.getMembership());
            }
        }

        return indexList.toArray();
    }

    // Used for parallel calculation of distances.
    //
    class DistanceCalculator implements Callable<Void> {

        private int mStartIndex, mEndIndex;
        private CoordinateList mCS;
        private double[] mBuf;
        private DistanceFunc mDF;

        DistanceCalculator(int startIndex, int endIndex) {
            mStartIndex = startIndex;
            mEndIndex = endIndex;
            mCS = getCoordinateList();
            int dim = mCS.getDimensionCount();
            mBuf = new double[dim];
            mDF = (DistanceFunc) mDistFunc.clone();
        }

        @Override
        public Void call() throws Exception {

            for (int i = mStartIndex; i < mEndIndex; i++) {
                if (i != mCurrentIndex && !mUnavailabilityBits.get(i)) {
                    double[] buf = null;
                    int sz = mDendrogram.nodeSize(i);
                    if (sz > 1) {
                        buf = (double[]) mCentroidMap.get(i);
                    } else {
                        buf = mBuf;
                        mCS.getCoordinates(i, buf);
                    }
                    double m = ((double) mCurrentSize * sz) / (mCurrentSize + sz);
                    mCurrentDistances[i] = m * mDF.distanceBetween(mCurrentCoordValues, buf);
                }
            }

            return null;
        }

    }
}
