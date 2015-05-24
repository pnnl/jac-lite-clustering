package gov.pnnl.jac.cluster;

import java.util.*;

public abstract class ClusterSplitter {
	
	public ClusterSplitter() {}
	  
	/**
	   * Splits a cluster into two if that would make for a "better" clustering.
	   * Implementations decide what that means.
	   *
	   * @return  List of VectorClusters to use: either two subclusters or
	   *          the original cluster.
	   */
	  protected Collection<Cluster> possiblySplitCluster(Cluster cluster) {
	    if (isSplittable(cluster)) {
	      Collection<Cluster> children = splitCluster(cluster);
	      int childrenCount = children != null ? children.size() : 0;
	      if (childrenCount >= 2  &&  preferSplit(cluster, children)) {
	        return children;
	      }
	    }
	    return Arrays.asList(cluster);
	  }

	  /** Gets whether the cluster is splittable. */
	  protected boolean isSplittable(Cluster cluster) {
	    return (cluster.getSize() >= 2);
	  }

	  /**
	   * Whether or not a cluster's children should be
	   * used in preference to the cluster itself.
	   */
	  abstract protected boolean preferSplit(Cluster cluster, Collection<Cluster> children);

	  /**
	   * Creates two subclusters for a cluster.
	   *
	   * @return List of VectorClusters. This will normally contain two clusters,
	   *         but may contain only one if the cluster isn't split for any reason.
	   */
	  abstract protected Collection<Cluster> splitCluster(Cluster cluster);
}
