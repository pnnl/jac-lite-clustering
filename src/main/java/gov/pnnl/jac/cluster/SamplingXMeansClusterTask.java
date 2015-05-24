package gov.pnnl.jac.cluster;

import java.util.Random;

import gov.pnnl.jac.collections.IntArrayList;
import gov.pnnl.jac.geom.*;
import gov.pnnl.jac.task.*;

public class SamplingXMeansClusterTask extends ClusterTask {

    private XMeansClusterTask mSubTask;

    public SamplingXMeansClusterTask(CoordinateList cs,
            SamplingXMeansClusterTaskParams params) {
        super(cs, params);
    }

    @Override
    public String getAlgorithmName() {
        return "sampling x-means";
    }

    @Override
    protected ClusterList doTask() throws Exception {

        TaskListener tl = new TaskListener() {

            public void taskBegun(TaskEvent e) {
            }

            public void taskEnded(TaskEvent e) {
            }

            public void taskMessage(TaskEvent e) {
                postMessage(e.getMessage());
            }

            public void taskProgress(TaskEvent e) {
                postProgress(e.getProgress());
            }

        };

        CoordinateList coords = getCoordinateList();
        int numCoords = coords.getCoordinateCount();
        SamplingXMeansClusterTaskParams params = (SamplingXMeansClusterTaskParams) getParams();

        int coordsToSample = (int) (params.getSamplingFraction() * numCoords + 0.5);
        coordsToSample = Math.max(params.getMinSamples(), Math.min(params
                .getMaxSamples(), coordsToSample));

        if (coordsToSample >= numCoords) {

            // No actual sampling... Same as regular x-means.
            mSubTask = new XMeansClusterTask(coords, params);
            mSubTask.setProgressEndpoints(this.getBeginProgress(), this
                    .getEndProgress());
            mSubTask.addTaskListener(tl);
            mSubTask.run();

            checkForCancel();

            if (mSubTask.getTaskOutcome() == TaskOutcome.SUCCESS) {
                setClusterList(mSubTask.getClusterList());
            } else if (mSubTask.getTaskOutcome() == TaskOutcome.ERROR) {
                error(mSubTask.getErrorMessage());
            } else {
                error("x-means subtask did not complete successfully");
            }

        } else {

            // Will be sampling -- run two phases of x-means

            double actualSamplingFraction = (double) coordsToSample / numCoords;
            IntArrayList samplingList = new IntArrayList(coordsToSample);

            Random random = new Random(params.getRandomSeed());

            for (int i = 0; i < numCoords; i++) {
                if (random.nextDouble() <= actualSamplingFraction) {
                    samplingList.add(i);
                }
            }

            postMessage("clustering a sample of " + samplingList.size()
                    + " out of " + numCoords + " coordinates");

            FilteredCoordinateList filteredCoords = new FilteredCoordinateList(
                    samplingList.toArray(), coords);
            samplingList = null;

            double beginP = getBeginProgress();
            double endP = getEndProgress();
            double midP = beginP + (endP - beginP) / 2;

            double minThreshold = params.getMinClusterToMeanThreshold();
            
            try {
                // No min threshold for first run.
                params.setMinClusterToMeanThreshold(0.0);
                mSubTask = new XMeansClusterTask(filteredCoords, params);
                mSubTask.setProgressEndpoints(beginP, endP);
                mSubTask.addTaskListener(tl);
                mSubTask.run();
            } finally {
                // Set it back for the second run.
                params.setMinClusterToMeanThreshold(minThreshold);
            }
            
            checkForCancel();

            filteredCoords = null;

            if (mSubTask.getTaskOutcome() == TaskOutcome.SUCCESS) {

                ClusterList seedClusters = mSubTask.getClusterList();
                mSubTask = null;

                CoordinateList seedCoords = new ClusterCoordinateList(
                        seedClusters);

                postMessage("clustering all coordinates using "
                        + seedCoords.getCoordinateCount() + " seeds");

                mSubTask = new XMeansClusterTask(coords, params);
                mSubTask.setInitialClusterSeeds(seedCoords);
                mSubTask.setProgressEndpoints(midP, endP);
                mSubTask.addTaskListener(tl);
                mSubTask.run();

                checkForCancel();

                if (mSubTask.getTaskOutcome() == TaskOutcome.SUCCESS) {
                    setClusterList(mSubTask.getClusterList());
                } else if (mSubTask.getTaskOutcome() == TaskOutcome.ERROR) {
                    error(mSubTask.getErrorMessage());
                } else {
                    error("final x-means subtask did not complete successfully");
                }

            } else if (mSubTask.getTaskOutcome() == TaskOutcome.ERROR) {
                error(mSubTask.getErrorMessage());
            } else {
                error("sampling x-means subtask did not complete successfully");
            }

        }

        return mClusters;
    }

    /**
     * Override cancel in order to cancel the current subtask.
     */
    public boolean cancel(boolean mayInterruptIfRunning) {
        // Call super.cancel() first.
        if (super.cancel(mayInterruptIfRunning)) {
            if (mSubTask != null) {
                // Then cancel the subtask.
                mSubTask.cancel(mayInterruptIfRunning);
            }
            return true;
        }
        return false;
    }

}
