package gov.pnnl.jac.cluster.similarity;

/*
 * AbstractHierarchicalClusterTask.java
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

import java.util.Arrays;
import java.util.List;

/**
 * <p>Abstract base class for implementations of hierarchical clustering.
 * Subclasses must implement the <tt>buildDendrogram</tt> method.  The <tt>doTask()</tt>
 * method is final; therefore, subclasses cannot implement <tt>doTask()</tt>.  The
 * difference between different hierarchical clustering algorithms is their methods
 * for generating the dendrogram.</p>
 * 
 * @author d3j923
 *
 */
public abstract class AbstractHierarchicalSimilarityClusterTask extends SimilarityClusterTask {

    // The dendrogram produced by the clustering implementation.
    protected Dendrogram mDendrogram;
    protected SimilarityDistances mDistances;

    /**
     * Constructor.
     * 
     * @param cs - contains the coordinates to be clustered.
     * @param params - the hierarchical clustering parameters.
     * @param dendrogram - if non-null, a dendrogram to be reused.  
     *   This dendrogram should have been produced by an earlier run
     *   of the algorithm on the same coordinate list.  If null, it is 
     *   ignored.
     */
    public AbstractHierarchicalSimilarityClusterTask(Similarities similarities,
            HierarchicalSimilarityClusterTaskParams params, Dendrogram dendrogram) {
        super(similarities, params);
        mDendrogram = dendrogram;
    }

    /**
     * Constructor for completely reclustering a
     * list of coordinates.
     * 
     * @param cs - contains the coordinates to be clustered.
     * @param params - the hierarchical clustering parameters.
     */
    public AbstractHierarchicalSimilarityClusterTask(Similarities similarities,
            HierarchicalSimilarityClusterTaskParams params) {
        this(similarities, params, null);
    }

    /**
     * Returns the dendrogram produced by the task, or the dendrogram reused by
     * the task if the dendrogram was provided by the constructor.
     * If creating a new dendrogram, this method should not be
     * called until the task is finished.
     * @return
     */
    public Dendrogram getDendrogram() {
        return mDendrogram;
    }

    public SimilarityDistances getDistances() {
        return mDistances;
    }
    
    /**
     * Perform the work of this task.  Since this method is final,
     * subclasses must perform their work in <tt>buildDendrogram()</tt>.
     */
    protected final List<int[]> doTask() throws Exception {

        HierarchicalSimilarityClusterTaskParams params = (HierarchicalSimilarityClusterTaskParams) super
                .getParams();

        Similarities similarities = super.getSimilarities();
        int coordCount = similarities.getRecordCount();

        // Have to have at least one coordinate.
        if (coordCount == 0) {
            error("zero records");
        }

        // If reusing an existing dendrogram, it has to have the same number
        // of leaves as coordinates.
        if (mDendrogram != null && mDendrogram.getLeafCount() != coordCount) {
            error("invalid dendrogram: leaf count = "
                    + mDendrogram.getLeafCount() + ", coordinate count = "
                    + coordCount);
        }

        if (mDendrogram == null) {
            // This should build a completely new dendrogram.
            mDistances = new SimilarityDistances(similarities);
            buildDendrogram();
        } else {
            postMessage("reclustering from existing dendrogram");
        }

        int clustersDesired = 0;

        HierarchicalClusterTaskParams.Criterion criterion = params.getCriterion();
        
        if (criterion == HierarchicalClusterTaskParams.Criterion.CLUSTERS) {
            clustersDesired = params.getClustersDesired();
            if (clustersDesired > coordCount) {
                postMessage("reducing number of desired clusters to the number of coordinates: "
                        + coordCount);
                clustersDesired = coordCount;
            }
        } else if (criterion == HierarchicalClusterTaskParams.Criterion.COHERENCE) {
            
        	mDendrogram.setMinCoherenceThreshold(params.getMinCoherenceThreshold());
            mDendrogram.setMaxCoherenceThreshold(params.getMaxCoherenceThreshold());

            clustersDesired = mDendrogram.clustersWithCoherenceExceeding(params
                    .getCoherenceDesired());
        } else {
            error("unsupported criterion: " + criterion);
        }

        List<int[]> clusters = mDendrogram.generateClusterGroupings(clustersDesired);
        int n = clusters.size();
        for (int i=0; i<n; i++) {
            int[] members = clusters.get(i);
            Arrays.sort(members);
        }
        
        setClusterList(clusters);

        return mClusters;
    }

    /**
     * Build a new dendrogram.  Subclasses must implement this method.
     * @throws Exception
     */
    protected abstract void buildDendrogram() throws Exception;
}
