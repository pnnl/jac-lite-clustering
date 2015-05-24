/*
 * ClusterTask.java
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

import gov.pnnl.jac.geom.CoordinateList;
import gov.pnnl.jac.task.AbstractTask;
import gov.pnnl.jac.task.TaskOutcome;

/**
 * <p><tt>ClusterTask</tt> is an abstract base class for
 * <tt>Task</tt> implementations that produce a
 * <tt>ClusterList</tt>.</p>
 *
 * @author R. Scarberry
 * @version 1.0
 */
public abstract class ClusterTask extends AbstractTask<ClusterList> {

    // The inputs:
    // 
    // Subclasses of ClusterTask should have a 
    // corresponding subclass of ClusterTaskParams for their 
    // parameters.
    private ClusterTaskParams mParams;

    // The list of coordinates to be clustered.
    private CoordinateList mCS;

    // The output, returned from the doTask() and get() methods.
    protected ClusterList mClusters;

    /**
     * Protected constructor taking the list of coordinates to cluster and
     * the clustering parameters.
     * 
     * @param cs
     * @param params
     */
    protected ClusterTask(CoordinateList cs, ClusterTaskParams params) {
        // Params can be null as far is ClusterTask in concerned.  Certain
        // clustering algorithms may not need parameters.
        mParams = params;
        // But they do always require something to cluster.
        if (cs == null) {
            throw new NullPointerException();
        }
        mCS = cs;
    }

    /**
     * Returns the clustering parameters.  Subclasses usually
     * call this method in their doTask() methods and cast the
     * return to the appropriate subclass for that clustering
     * algorithm.
     * @return
     */
    public final ClusterTaskParams getParams() {
        return mParams;
    }

    /**
     * Returns the coordinate list.
     * @return
     */
    public final CoordinateList getCoordinateList() {
        return mCS;
    }

    /**
     * Subclasses must defined this method to return
     * the clustering algorithm name.
     * @return
     */
    public abstract String getAlgorithmName();

    /**
     * Returns the task name, which includes the return
     * from <tt>getAlgorithmName()</tt>.
     */
    public final String taskName() {
        return "performance of " + getAlgorithmName() + " clustering";
    }

    /**
     * Subclasses should call this method prior to the conclusion of
     * clustering to set the cluster list.
     * 
     * @param clusters
     */
    protected final void setClusterList(ClusterList clusters) {
        mClusters = clusters;
    }

    /**
     * Returns the <tt>ClusterList</tt> if clustering 
     * completed successfully.  Before calling, ensure that
     * <tt>getTaskOutcome()</tt> returns <tt>TaskOutcome.SUCCESS</tt>.
     * Unlike <tt>get()</tt>, this method does not block until the output 
     * becomes available.
     * 
     * @return - an instance of <tt>ClusterList</tt>.
     * 
     * @throws IllegalStateException - if clustering has not
     *   completed successfully.
     */
    public final ClusterList getClusterList() {
        TaskOutcome outCome = getTaskOutcome();
        if (outCome != TaskOutcome.SUCCESS) {
            throw new IllegalStateException(
                    "clustering not finished or did not succeed");
        } else if (mClusters == null) {
            throw new NullPointerException("forget to set the cluster set");
        }
        return mClusters;
    }
}
