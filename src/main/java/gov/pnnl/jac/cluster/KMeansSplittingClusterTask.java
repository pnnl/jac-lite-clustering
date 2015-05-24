package gov.pnnl.jac.cluster;

import gov.pnnl.jac.geom.CoordinateList;
import gov.pnnl.jac.geom.SimpleCoordinateList;
import gov.pnnl.jac.geom.distance.AbstractDistanceFunc;
import gov.pnnl.jac.geom.distance.CoordinateListColumnarDoubles;
import gov.pnnl.jac.geom.distance.DistanceFunc;
import gov.pnnl.jac.task.ProgressHandler;
import gov.pnnl.jac.task.TaskEvent;
import gov.pnnl.jac.task.TaskListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public abstract class KMeansSplittingClusterTask extends ClusterTask {
    
	private CoordinateList mInitialClusterSeeds;
	
    public KMeansSplittingClusterTask(CoordinateList cs, 
            KMeansSplittingClusterTaskParams  params) {
        super(cs, params);
    }
    
    /**
     * Set the initial seeds for clusters.  This <em>must</em> be called
     * before running the task, or it will have no effect on the clustering.
     * 
     * @param seeds
     */
    public void setInitialClusterSeeds(CoordinateList seeds) {
    	mInitialClusterSeeds = seeds;
    }

    protected abstract void initializeIteration(ClusterList clusters);
    
    protected abstract ClusterSplitter createSplitter(ClusterList clusters, Cluster cluster);

    @Override
    protected ClusterList doTask() throws Exception {

        final CoordinateList cs = getCoordinateList();
        final int numCoords = cs.getCoordinateCount();
        
        ProgressHandler ph = new ProgressHandler(this);
        ph.postBegin();

        KMeansSplittingClusterTaskParams params = (KMeansSplittingClusterTaskParams) getParams();

        DistanceFunc distanceFunc = params.getDistanceFunc();
        // Ensure that the data source is set for the distance function.
        if (distanceFunc instanceof AbstractDistanceFunc) {
            ((AbstractDistanceFunc) distanceFunc).setDataSource(
                    new CoordinateListColumnarDoubles(cs));
        }

        int[] allIDs = new int[numCoords];
        for (int i = 0; i < numCoords; i++) {
            allIDs[i] = i;
        }

        mUnsplittables = new HashSet<Cluster>();

        int numWorkerThreads = params.getNumWorkerThreads();
        if (numWorkerThreads <= 0) {
            numWorkerThreads = Runtime.getRuntime().availableProcessors();
        }

        ClusterList clusterList = null;
        ExecutorService threadPool = null;

        try {
            
            if (numWorkerThreads > 1) {
                threadPool = Executors.newFixedThreadPool(numWorkerThreads);
            }

            int minClusters = Math.max(1, params.getMinClusters());
            if (minClusters > numCoords) {
                minClusters = numCoords;
            }
            
            mMaxClusters = params.getMaxClusters();
            if (mMaxClusters <= 0) {
                mMaxClusters = Integer.MAX_VALUE;
            }
            
            ClusterList workingList = null;
            
            if (mInitialClusterSeeds != null || minClusters > 1) {
            	
                int initialSeeds = mInitialClusterSeeds != null ? 
            			mInitialClusterSeeds.getCoordinateCount() : 0;
            	
                int nc = Math.max(minClusters, initialSeeds);
            	
                ClusterSeeder seeder = params.getClusterSeeder();
            	
                if (mInitialClusterSeeds != null) {
            		seeder = new PreassignedSeeder(mInitialClusterSeeds);
            	}
            
                mLocalKMeans = new KMeansClusterTask(cs, 
            			new KMeansClusterTaskParams(nc, 
            					Integer.MAX_VALUE, 0,
            					params.getNumWorkerThreads(),
            					params.getDistanceFunc(),
            					seeder));
            	mLocalKMeans.run();
            	workingList = mLocalKMeans.get();
            	mLocalKMeans = null;

            } else { // mInitialClusterSeeds == null && minClusters == 1
            	workingList = new ClusterList(new Cluster[] {
            			new Cluster(allIDs, cs) });
            }
            
            int iteration = 0;

            double progress = 0.0;
            final double perIterationProgress = 0.95/10.0;
            
            do {

                initializeIteration(workingList);
                
                mSplits = mSplitsGoingOn = 0;
                mCurrentClusters = new ArrayList<Cluster>();

                final int numClusters = workingList.getClusterCount();
                
                List<SplitCallable> splitterList = new ArrayList<SplitCallable>();
                
                for (int i=0; i<numClusters; i++) {
                    this.checkForCancel();
                    Cluster cluster = workingList.getCluster(i);
                    if (!isUnsplittable(cluster)) {
                        incrementSplitsGoingOn();
                        splitterList.add(new SplitCallable(cluster, createSplitter(workingList, cluster)));
                    } else {
                        addToCurrentClusters(cluster);
                    }
                }

                if (splitterList.size() > 0) {
                    if (threadPool != null) {
                        List<Future<Collection<Cluster>>> results = threadPool.invokeAll(splitterList);
                        for (Future<Collection<Cluster>> result: results) {
                            Collection<Cluster> clusters = result.get();
                            addToCurrentClusters(clusters);
                            if (clusters.size() > 1) {
                                incrementSplits();
                            } else if (clusters.size() == 1) {
                                Cluster[] c = clusters.toArray(new Cluster[1]);
                                addToUnsplittables(c[0]);
                            }
                        }
                    } else {
                        
                        for (SplitCallable sc: splitterList) {
                            Collection<Cluster> clusters = sc.call();
                            addToCurrentClusters(clusters);
                            if (clusters.size() > 1) {
                                incrementSplits();
                            } else if (clusters.size() == 1) {
                                Cluster[] c = clusters.toArray(new Cluster[1]);
                                addToUnsplittables(c[0]);
                            } 
                        }

                    }
                }

                int newNumClusters = mCurrentClusters.size();
                Cluster[] c = new Cluster[newNumClusters];
                mCurrentClusters.toArray(c);
                workingList = new ClusterList(c);
                
                iteration++;
                
                int pctSplit = (int) (0.5 + 100.0 * ((double) mSplits)/numClusters);
                
                progress = Math.min(0.95, progress + perIterationProgress);
                
                if (pctSplit < 100 && progress < 0.5) {
                	progress = 0.5;
                }
                
                ph.postFraction(progress);
                
                ph.postMessage("loop " + iteration + ", percentage of clusters split = " + 
                		pctSplit + ", number of clusters = " + newNumClusters);

            } while (mSplits > 0 && 
                    workingList.getClusterCount() < mMaxClusters);

            int numClusters = workingList.getClusterCount();
                        
            int dim = cs.getDimensionCount();
            CoordinateList finalSeeds = new SimpleCoordinateList(dim, numClusters);
            for (int i=0; i<numClusters; i++) {
                finalSeeds.setCoordinates(i, workingList.getCluster(i).getCenterDirect());
            }
            
            workingList = null;
            mCurrentClusters = null;
            
            ph.postMessage("performing final round of k-means to polish up clusters");
            
            mLocalKMeans = new KMeansClusterTask(cs, new KMeansClusterTaskParams(numClusters, 
                    Integer.MAX_VALUE, 0,
                    params.getNumWorkerThreads(),
                    false,
                    params.getDistanceFunc(),
                    new PreassignedSeeder(finalSeeds)));
            
            mLocalKMeans.addTaskListener(new TaskListener() {
                @Override
                public void taskBegun(TaskEvent e) {
                }

                @Override
                public void taskMessage(TaskEvent e) {
                    postMessage(" ... final k-means: " + e.getMessage());
                }

                @Override
                public void taskProgress(TaskEvent e) {
                }

                @Override
                public void taskEnded(TaskEvent e) {
                }                
            });

            mLocalKMeans.run();
            
            clusterList = mLocalKMeans.get();
            
            double minThreshold = params.getMinClusterToMeanThreshold();
            if (minThreshold > 0.0) {
                double avgSize = 0;
                int minSize = Integer.MAX_VALUE;
                for (int i=0; i<numClusters; i++) {
                    Cluster c = clusterList.getCluster(i);
                    int size = c.getSize();
                    avgSize += size;
                    if (size < minSize) {
                        minSize = size;
                    }
                }
                avgSize /= numClusters;
                
                int intThreshold = (int) (0.5 + minThreshold * avgSize);
                if (minSize < intThreshold) {
                    // Some clusters were too small.
                    List<Cluster> bigEnough = new ArrayList<Cluster>(numClusters);
                    for (int i=0; i<numClusters; i++) {
                        Cluster c = clusterList.getCluster(i);
                        if (c.getSize() >= intThreshold) {
                            bigEnough.add(c);
                        }
                    }
                    
                    int discard = numClusters - bigEnough.size();
                    numClusters = bigEnough.size();
      
                    finalSeeds = new SimpleCoordinateList(dim, numClusters);
                    for (int i=0; i<numClusters; i++) {
                        finalSeeds.setCoordinates(i, bigEnough.get(i).getCenterDirect());
                    }
                    
                    ph.postMessage(String.valueOf(discard) + " clusters will be discarded because of size");
                    
                    mLocalKMeans = new KMeansClusterTask(cs, new KMeansClusterTaskParams(numClusters, 
                            Integer.MAX_VALUE, 0,
                            params.getNumWorkerThreads(),
                            params.getDistanceFunc(),
                            new PreassignedSeeder(finalSeeds)));
                    
                    mLocalKMeans.run();
                    
                    clusterList = mLocalKMeans.get();                    
                }
            }
            
            ph.postMessage("final cluster count = " + numClusters);

            mLocalKMeans = null;

            setClusterList(clusterList);
            
        } finally {
            if (threadPool != null) {
                threadPool.shutdownNow();
            }
        }

        ph.postEnd();

        return clusterList;
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        if (super.cancel(mayInterruptIfRunning)) {
            if (mLocalKMeans != null) {
                mLocalKMeans.cancel(mayInterruptIfRunning);
            }
            synchronized (this) {
            	notifyAll();
            }
            return true;
        }
        return false;
    }
    
    private KMeansClusterTask mLocalKMeans = null;
    
    private int mSplits = 0;

    private int mSplitsGoingOn = 0;

    private int mMaxClusters;
    private List<Cluster> mCurrentClusters;

    private Set<Cluster> mUnsplittables;

    private boolean isUnsplittable(Cluster cluster) {
        return mUnsplittables.contains(cluster);
    }

    private void addToUnsplittables(Cluster cluster) {
        mUnsplittables.add(cluster);
    }

    private void addToCurrentClusters(Collection<Cluster> clusters) {
        Iterator<Cluster> it = clusters.iterator();
        int numClusters = mCurrentClusters.size();
        while(numClusters < mMaxClusters && it.hasNext()) {
            mCurrentClusters.add(it.next());
            numClusters++;
        }
    }

    private void addToCurrentClusters(Cluster cluster) {
        if (mCurrentClusters.size() < mMaxClusters) {
            mCurrentClusters.add(cluster);
        }
    }

    private void incrementSplitsGoingOn() {
        mSplitsGoingOn++;
    }

    private void incrementSplits() {
        mSplits++;
    }

    class SplitCallable implements Callable<Collection<Cluster>> {

        private Cluster mCluster;
        private ClusterSplitter mSplitter;
        
        SplitCallable(Cluster cluster, ClusterSplitter splitter) {
            mCluster = cluster;
            mSplitter = splitter;
        }

        public Collection<Cluster> call() throws Exception {
            return mSplitter.possiblySplitCluster(mCluster);
        }
    }
}
