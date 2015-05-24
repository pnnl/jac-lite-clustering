package gov.pnnl.jac.cluster;

import gov.pnnl.jac.geom.distance.*;

/**
 * <p>
 * Parameters class for Jarvis-Patrick Clustering.
 * </p>
 * 
 * @author D3J923
 *
 */
public class JarvisPatrickClusterTaskParams implements ClusterTaskParams {

	 /**
	  * 
	  */
	  private static final long serialVersionUID = 3204837520865086438L;
	  
	  public static final int NEAREST_NEIGHBORS_TO_EXAMINE_DEFAULT = 20;
	  public static final int NEAREST_NEIGHBOR_OVERLAP_DEFAULT = 2;
	  
	  // The number of nearest neighbors to examine for every tuple.
	  private int nearestNeighborsToExamine;
	  // The minimum required nn overlap between tuples in order
	  // for them to be assigned to the same cluster.
	  private int nearestNeighborOverlap;
	  // Whether or not tuples must be in each other's nn list
	  // in order to be assigned to the same cluster.
	  private boolean mutualNearestNeighbors;
	  // The distance metric to use for finding nearest neighbors.
	  private DistanceFunc distanceFunc;
	  // The number of threads to use for the concurrent parts.
	  private int workerThreadCount;
	  
	  /**
	   * Constructor
	   */
	  public JarvisPatrickClusterTaskParams() {
		  nearestNeighborsToExamine = NEAREST_NEIGHBORS_TO_EXAMINE_DEFAULT;
		  nearestNeighborOverlap = NEAREST_NEIGHBOR_OVERLAP_DEFAULT;
		  mutualNearestNeighbors = true;
		  distanceFunc = new EuclideanNoNaN();
		  workerThreadCount = Runtime.getRuntime().availableProcessors();
	  }

	  /**
	   * Returns the number of nearest neighbors to examine for each tuple.
	   * 
	   * @return
	   */
	  public int getNearestNeighborsToExamine() {
	    return nearestNeighborsToExamine;
	  }
	  
	  /**
	   * Set the number of nearest neighbors to examine for each tuple.
	   * 
	   * @param nearestNeighborsToExamine
	   */
	  public void setNearestNeighborsToExamine(int nearestNeighborsToExamine) {
	    if (nearestNeighborsToExamine < 2) {
	      throw new IllegalArgumentException("must be >= 2: " + nearestNeighborsToExamine);
	    }
	    this.nearestNeighborsToExamine = nearestNeighborsToExamine;
	  }
	  
	  /**
	   * Get the nearest neighbor overlap that must exist between two tuples in 
	   * order for them to be assigned to the same cluster.
	   * 
	   * @return
	   */
	  public int getNearestNeighborOverlap() {
	    return nearestNeighborOverlap;
	  }
	  
	  /**
	   * Set the nearest neighbor overlap required for two tuples to 
	   * be assigned to the same cluster.
	   * 
	   * @param nearestNeighborOverlap
	   */
	  public void setNearestNeighborOverlap(int nearestNeighborOverlap) {
	    if (nearestNeighborOverlap < 1) {
	      throw new IllegalArgumentException("must be >= 1: " + nearestNeighborOverlap);
	    }
	    this.nearestNeighborOverlap = nearestNeighborOverlap;
	  }
	  
	  /**
	   * Return whether or not two tuples must be in each other's nearest neighbor
	   * lists in order for them to be assigned to the same cluster.
	   * 
	   * @return
	   */
	  public boolean getMutualNearestNeighbors() {
	    return mutualNearestNeighbors;
	  }
	  
	  /**
	   * Set whether or not two tuples must be in each other's nearest neighbor lists
	   * in order for them to be in the same cluster.
	   * 
	   * @param b
	   */
	  public void setMutualNearestNeighbors(boolean b) {
	    mutualNearestNeighbors = b;
	  }
	  
	  /**
	   * Get the distance function.
	   * 
	   * @return
	   */
	  public DistanceFunc getDistanceFunc() {
	    return distanceFunc;
	  }
	  
	  /**
	   * Set the distance function.
	   * 
	   * @param distanceFunc
	   */
	  public void setDistanceFunc(DistanceFunc distanceFunc) {
	    if (distanceFunc == null) {
	      throw new NullPointerException();
	    }
	    this.distanceFunc = distanceFunc;
	  }

	  /**
	   * Get the number of worker threads to use for concurrent parts of
	   * the algorithm.
	   * 
	   * @return
	   */
	  public int getWorkerThreadCount() {
	    return workerThreadCount;
	  }
	  
	  /**
	   * Set the number of threads to be used for concurrent parts of the
	   * algorithm.
	   * 
	   * @param n
	   */
	  public void setWorkerThreadCount(int n) {
	    if (n <= 0) {
	      throw new IllegalArgumentException("worker thread count must be greater than 0");
	    }
	    this.workerThreadCount = n;
	  }

	  /**
	   * Builder class for JarvisPatrickClusterTaskParams.
	   * 
	   * @author R. Scarberry
	   *
	   */
	  public static class Builder {
	    
	    private JarvisPatrickClusterTaskParams params;
	  
	    public Builder() {
	      params = new JarvisPatrickClusterTaskParams();
	    }
	    
	    public Builder nearestNeighborsToExamine(int nearestNeighborsToExamine) {
	      params.setNearestNeighborsToExamine(nearestNeighborsToExamine);
	      return this;
	    }
	    
	    public Builder nearestNeighborOverlap(int nearestNeighborOverlap) {
	      params.setNearestNeighborOverlap(nearestNeighborOverlap);
	      return this;
	    }
	    
	    public Builder mutualNearestNeighbors(boolean b) {
	      params.setMutualNearestNeighbors(b);
	      return this;
	    }
	    
	    public Builder workerThreadCount(int n) {
	      params.setWorkerThreadCount(n);
	      return this;
	    }

	    public Builder distanceFunc(DistanceFunc distanceFunc) {
	      params.setDistanceFunc(distanceFunc);
	      return this;
	    }

	    public JarvisPatrickClusterTaskParams build() {
	      return params;
	    }
	  }	 
}
