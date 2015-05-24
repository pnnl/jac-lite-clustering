package gov.pnnl.jac.geom;

import gov.pnnl.jac.cluster.Cluster;
import gov.pnnl.jac.cluster.ClusterList;
import gov.pnnl.jac.util.RandEngine;

import java.io.File;
import java.io.IOException;

import cern.colt.list.IntArrayList;
import cern.jet.random.Normal;

/**
 * <p><tt>CoordinateGenerator</tt> is a class containing static 
 * utility methods for generating lists of test coordinates.
 * </p>
 * @author d3j923
 *
 */
public final class CoordinateGenerator {

	private CoordinateGenerator() {
	}

	/**
	 * Populates the specified <tt>CoordinateList</tt> with gaussian-distributed
	 * points having the specified number of clusters and the specified 
	 * standard deviation about their cluster centers.
	 * 
	 * @param cs - the coordinate list to populate.
	 * @param numClusters - number of desired clusters in the distribution.
	 * @param standardDev - standard deviation of the data.
	 * @param seed - a seed for the random number generator.
	 * 
	 * @return the reference to the <tt>CoordinateList</tt> passed as the
	 *   first parameter.
	 */
	public static CoordinateList generateGaussianCoordinates(CoordinateList cs,
			int numClusters, double standardDev, long seed) {

		int dimensions = cs.getDimensionCount();
		int coordCount = cs.getCoordinateCount();

		if (numClusters > coordCount) {
			numClusters = coordCount;
		}

		double[][] exemplars = new double[numClusters][dimensions];

		RandEngine random = new RandEngine(seed);

		for (int i = 0; i < numClusters; i++) {
			for (int j = 0; j < dimensions; j++) {
				exemplars[i][j] = random.nextDouble();
			}
		}

		double[] buf = new double[dimensions];

		Normal normal = new Normal(0.0, standardDev, random);

		for (int i = 0; i < coordCount; i++) {
			int cluster = random.nextInt(numClusters);
			double[] exemplar = exemplars[cluster];
			for (int j = 0; j < dimensions; j++) {
				buf[j] = normal.nextDouble(exemplar[j], standardDev);
			}
			cs.setCoordinates(i, buf);
		}

		return cs;
	}

	/**
	 * Generates a <tt>CoordinateList</tt> and a <tt>ClusterSet</tt> with 
	 * the desired characteristics.
	 * 
	 * @param coordCount - the number of coordinates.
	 * @param dimensions - the number of dimensions for each coordinate.
	 * @param numClusters - the number of clusters.
	 * @param standardDev - the standard deviation for the gaussian distribution
	 *   of the coordinates in each cluster.
	 * @param seed - the random number generator seed.
	 * 
	 * @return - an array of length 2 containing a <tt>SimpleCoordinateSet</tt> 
	 *   in element 0 and a <tt>ClusterSet</tt> in element 1.
	 */
	public static Object[] generateGaussianCoordinatesAndClusters(
			int coordCount, int dimensions, int numClusters,
			double standardDev, long seed) {

		CoordinateList cs = new SimpleCoordinateList(dimensions, coordCount);
		double[][] exemplars = new double[numClusters][dimensions];
		IntArrayList[] clusterIDLists = new IntArrayList[numClusters];

		RandEngine random = new RandEngine(seed);

		for (int i = 0; i < numClusters; i++) {
			for (int j = 0; j < dimensions; j++) {
				exemplars[i][j] = random.nextDouble();
			}
			clusterIDLists[i] = new IntArrayList();
		}

		double[] buf = new double[dimensions];

		Normal normal = new Normal(0.0, standardDev, random);

		for (int i = 0; i < coordCount; i++) {
			int cluster = random.nextInt(numClusters);
			double[] exemplar = exemplars[cluster];
			for (int j = 0; j < dimensions; j++) {
				buf[j] = normal.nextDouble(exemplar[j], standardDev);
			}
			cs.setCoordinates(i, buf);
			clusterIDLists[cluster].add(i);
		}

		Cluster[] clusters = new Cluster[numClusters];
		for (int i = 0; i < numClusters; i++) {
			clusterIDLists[i].trimToSize();
			clusters[i] = new Cluster(clusterIDLists[i].elements(), cs);
		}

		ClusterList clusterSet = new ClusterList(clusters);

		return new Object[] { cs, clusterSet };
	}

	/**
	 * Generates a <tt>CoordinateList</tt> and a <tt>ClusterSet</tt> with 
	 * the desired characteristics.  The coordinate list is created using the
	 * specified <tt>CoordinateListFactory</tt>.
	 * 
	 * @param factory - factory for creating the coordinate list.
	 * @param coordsID - id within the factory for the coordinate list.
	 * @param coordCount - the number of coordinates.
	 * @param dimensions - the number of dimensions for each coordinate.
	 * @param numClusters - the number of clusters.
	 * @param standardDev - the standard deviation for the gaussian distribution
	 *   of the coordinates in each cluster.
	 * @param seed - the random number generator seed.
	 * 
	 * @return - an array of length 2 containing a <tt>SimpleCoordinateSet</tt> 
	 *   in element 0 and a <tt>ClusterSet</tt> in element 1.
	 *   
	 * @throws IOException if the <tt>CoordinateListFactory</tt> encounters an
	 *   IO problem.
	 */
	public static Object[] generateGaussianCoordinatesAndClusters(
			CoordinateListFactory factory, String coordsID,
			int coordCount, int dimensions, int numClusters,
			double standardDev, long seed) throws IOException {

		CoordinateList cs = factory.createCoordinateList(coordsID, dimensions, coordCount);
		double[][] exemplars = new double[numClusters][dimensions];
		IntArrayList[] clusterIDLists = new IntArrayList[numClusters];

		RandEngine random = new RandEngine(seed);

		for (int i = 0; i < numClusters; i++) {
			for (int j = 0; j < dimensions; j++) {
				exemplars[i][j] = random.nextDouble();
			}
			clusterIDLists[i] = new IntArrayList();
		}

		double[] buf = new double[dimensions];

		Normal normal = new Normal(0.0, standardDev, random);

		for (int i = 0; i < coordCount; i++) {
			int cluster = random.nextInt(numClusters);
			double[] exemplar = exemplars[cluster];
			for (int j = 0; j < dimensions; j++) {
				buf[j] = normal.nextDouble(exemplar[j], standardDev);
			}
			cs.setCoordinates(i, buf);
			clusterIDLists[cluster].add(i);
		}

		Cluster[] clusters = new Cluster[numClusters];
		for (int i = 0; i < numClusters; i++) {
			clusterIDLists[i].trimToSize();
			clusters[i] = new Cluster(clusterIDLists[i].elements(), cs);
		}

		ClusterList clusterSet = new ClusterList(clusters);

		return new Object[] { cs, clusterSet };
	}

	public static CoordinateList generateGaussianCoordinates(int coordCount,
			int dimensions, int numClusters, double standardDev, long seed) {

		CoordinateList cs = new SimpleCoordinateList(dimensions, coordCount);
		return generateGaussianCoordinates(cs, numClusters, standardDev, seed);
	}

	public static CoordinateList generateGaussianCoordinates(int coordCount,
			int dimensions, int numClusters, double standardDev, long seed,
			File file) throws IOException {

		CoordinateList cs = FileMappedCoordinateList.createNew(file,
				dimensions, coordCount);
		return generateGaussianCoordinates(cs, numClusters, standardDev, seed);
	}

	public static CoordinateList generateGaussianCoordinates(
			int[] coordsInEachCluster, double[] standDevEachCluster,
			int dimensions, long seed) {

		int numClusters = coordsInEachCluster.length;
		if (numClusters != standDevEachCluster.length) {
			throw new IllegalArgumentException(
					"coordsInEachCluster.length != standDevEachCluster.length");
		}

		IntArrayList clustList = new IntArrayList();
		for (int i = 0; i < numClusters; i++) {
			int numInCluster = coordsInEachCluster[i];
			// Add i to the list numInCluster times.
			for (int j = 0; j < numInCluster; j++) {
				clustList.add(i);
			}
		}

		int coordCount = clustList.size();
		clustList.trimToSize();

		int[] clusterIDs = clustList.elements();
		clustList = null;

		RandEngine random = new RandEngine(seed);

		// Now shuffle clusterIDs, so as the coordinates are generated,
		// the clusters they're in are picked in random order.
		for (int i = 0; i < coordCount; i++) {
			int j = i + random.nextInt(coordCount - i);
			if (j != i) {
				clusterIDs[i] ^= clusterIDs[j];
				clusterIDs[j] ^= clusterIDs[i];
				clusterIDs[i] ^= clusterIDs[j];
			}
		}

		CoordinateList cs = new SimpleCoordinateList(dimensions, coordCount);

		double[][] exemplars = new double[numClusters][dimensions];

		for (int i = 0; i < numClusters; i++) {
			for (int j = 0; j < dimensions; j++) {
				exemplars[i][j] = random.nextDouble();
			}
		}

		double[] buf = new double[dimensions];

		Normal normal = new Normal(0.0, 0.1, random);

		for (int i = 0; i < coordCount; i++) {
			int cluster = clusterIDs[i];
			double[] exemplar = exemplars[cluster];
			double sdev = standDevEachCluster[cluster];
			for (int j = 0; j < dimensions; j++) {
				buf[j] = normal.nextDouble(exemplar[j], sdev);
			}
			cs.setCoordinates(i, buf);
		}

		return cs;
	}

	public static CoordinateList generateGaussianCoordinates(
			int[] coordsInEachCluster, double[] standDevEachCluster,
			int dimensions, long seed, File file) throws IOException {

		int numClusters = coordsInEachCluster.length;
		if (numClusters != standDevEachCluster.length) {
			throw new IllegalArgumentException(
					"coordsInEachCluster.length != standDevEachCluster.length");
		}

		IntArrayList clustList = new IntArrayList();
		for (int i = 0; i < numClusters; i++) {
			int numInCluster = coordsInEachCluster[i];
			// Add i to the list numInCluster times.
			for (int j = 0; j < numInCluster; j++) {
				clustList.add(i);
			}
		}

		int coordCount = clustList.size();
		clustList.trimToSize();

		int[] clusterIDs = clustList.elements();
		clustList = null;

		RandEngine random = new RandEngine(seed);

		// Now shuffle clusterIDs, so as the coordinates are generated,
		// the clusters they're in are picked in random order.
		for (int i = 0; i < coordCount; i++) {
			int j = i + random.nextInt(coordCount - i);
			if (j != i) {
				clusterIDs[i] ^= clusterIDs[j];
				clusterIDs[j] ^= clusterIDs[i];
				clusterIDs[i] ^= clusterIDs[j];
			}
		}

		CoordinateList cs = FileMappedCoordinateList.createNew(file,
				dimensions, coordCount);

		double[][] exemplars = new double[numClusters][dimensions];

		for (int i = 0; i < numClusters; i++) {
			for (int j = 0; j < dimensions; j++) {
				exemplars[i][j] = random.nextDouble();
			}
		}

		double[] buf = new double[dimensions];

		Normal normal = new Normal(0.0, 0.1, random);

		for (int i = 0; i < coordCount; i++) {
			int cluster = clusterIDs[i];
			double[] exemplar = exemplars[cluster];
			double sdev = standDevEachCluster[cluster];
			for (int j = 0; j < dimensions; j++) {
				buf[j] = normal.nextDouble(exemplar[j], sdev);
			}
			cs.setCoordinates(i, buf);
		}

		return cs;
	}
}
