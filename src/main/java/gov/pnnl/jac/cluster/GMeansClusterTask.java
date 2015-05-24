/*
 * GMeansClusterTask.java
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

/**
 * Variant of k-means clustering that can automatically pick k. This begins
 * with all coordinates belonging to one large cluster.
 * Then, during a splitting iteration, clusters undergo a 2-for-1 split where
 * that would produce a &quot;better&quot; clustering. Splits are created by running
 * k-means locally with k=2, with the seeds chosen deterministically. 
 * If there are any splits in an iteration, k-means
 * is then applied globally to refine the clusters. This continues until no
 * more splits are made.
 *
 * <p> The splitting criterion can be set in the form of a {@link GMeansSplitter}.
 *
 * @author Grant Nakamura; July 2005 (original), October 2005 (port to IN-SPIRE)
 *      R. Scarberry; March 2009, adapted to JAC.
 */
public class GMeansClusterTask extends KMeansSplittingClusterTask {

    public GMeansClusterTask(CoordinateList cs, KMeansSplittingClusterTaskParams params) {
        super(cs, params);
    }

    /**
     * Returns the algorithm name, which is &quot;g-means&quot;.
     */
    public String getAlgorithmName() {
        return "g-means";
    }

    /**
     * Returns a new GMeansSplitter.
     */
    @Override
    protected ClusterSplitter createSplitter(ClusterList clusters,
            Cluster cluster) {
        return new GMeansSplitter(getCoordinateList(), ((KMeansSplittingClusterTaskParams) getParams()));
    }

    /**
     * For g-means, does nothing.
     */
    @Override
    protected void initializeIteration(ClusterList clusters) {
        // Noop for g-means.
    }
}
