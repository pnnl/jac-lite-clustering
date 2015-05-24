/*
 * GMeansSplitter.java
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
import gov.pnnl.jac.geom.CoordinateMath;
import gov.pnnl.jac.geom.FilteredCoordinateList;
import gov.pnnl.jac.geom.SimpleCoordinateList;
import gov.pnnl.jac.geom.distance.BasicDistanceMethod;
import gov.pnnl.jac.geom.distance.DistanceFunc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Splitting criterion for use by {@link GMeansClusterTask}.
 * This splits a cluster if it is non-Gaussian, as described in
 * the paper "Learning the k in k-means", Hamerly and Elkan, 2003.
 *
 * @author Grant Nakamura; July 2005 (original), October 2005 (port to IN-SPIRE)
 *      R. Scarberry; March 2009, adapted Grant's code for JAC.
 */
public class GMeansSplitter extends ClusterSplitter {

    private CoordinateList mCoordinates;
    private KMeansSplittingClusterTaskParams mParams;

    public GMeansSplitter(CoordinateList coordinates,
            KMeansSplittingClusterTaskParams params) {
        if (coordinates == null || params == null) {
            throw new NullPointerException();
        }
        mCoordinates = coordinates;
        mParams = params;
    }

    @Override
    protected boolean preferSplit(Cluster cluster, Collection<Cluster> children) {
        return !CoordinateMath
                .andersonDarlingGaussianTest(projectToLineBetweenChildren(
                        cluster, children));
    }

    @Override
    protected Collection<Cluster> splitCluster(Cluster cluster) {
        CoordinateList seeds = createTwoSeeds(cluster);
        return runLocalKMeans(cluster, seeds);
    }

    /**
     * Projects the data in a cluster to the line connecting its two children's
     * centers.
     */
    private double[] projectToLineBetweenChildren(Cluster cluster,
            Collection<Cluster> children) {
        double[] projectedData = null;
        if (children.size() == 2) {
            Iterator<Cluster> it = children.iterator();
            double[] center1 = it.next().getCenterDirect();
            double[] center2 = it.next().getCenterDirect();
            int dim = center1.length;
            double[] projection = new double[dim];
            for (int i = 0; i < dim; i++) {
                projection[i] = center1[i] - center2[i];
            }
            projectedData = projectToVector(cluster, projection);
        }
        return projectedData;
    }

    /**
     * Projects all data in a cluster to one dimension, via the dot product with
     * a projection vector.
     */
    private double[] projectToVector(Cluster cluster, double[] projection) {
        int n = cluster.getSize();
        int dim = mCoordinates.getDimensionCount();
        double[] projectedData = new double[n];
        double[] coords = new double[dim];
        for (int i = 0; i < n; i++) {
            mCoordinates.getCoordinates(cluster.getMember(i), coords);
            projectedData[i] = CoordinateMath.dotProduct(coords, projection);
        }
        return projectedData;
    }

    /**
     * Create two cluster seeds by going +/- one standard deviation from the
     * cluster's center.
     * 
     * @return List of two VectorClusters
     */
    private CoordinateList createTwoSeeds(Cluster cluster) {
        int dim = mCoordinates.getDimensionCount();

        double[][] stats = ClusterStats.computeMeanAndVariance(mCoordinates,
                cluster);

        CoordinateList seeds = new SimpleCoordinateList(dim, 2);

        for (int i = 0; i < dim; i++) {
            double center = stats[i][0];
            double sdev = Math.sqrt(stats[i][1]);
            seeds.setCoordinate(0, i, center - sdev);
            seeds.setCoordinate(1, i, center + sdev);
        }

        return seeds;
    }

    protected Collection<Cluster> runLocalKMeans(Cluster cluster,
            CoordinateList seeds) {

        int[] members = cluster.getMembership();

        FilteredCoordinateList fcs = new FilteredCoordinateList(members,
                mCoordinates);
        
        DistanceFunc distFunc = mParams.getDistanceFunc();

        KMeansClusterTaskParams.Builder builder = new KMeansClusterTaskParams.Builder(seeds.getCoordinateCount())
            .maxIterations(1000)
            .movesGoal(0)
            .numWorkerThreads(1)
            .replaceEmptyClusters(false)
            .distanceFunc(distFunc)
            .seeder(new PreassignedSeeder(seeds));
        
        KMeansClusterTask kmeans = new KMeansClusterTask(fcs, builder.build());
        kmeans.run();
        
        ClusterList clusters = kmeans.getClusterList();
        
        int n = clusters.getClusterCount();
        List<Cluster> clusterList = new ArrayList<Cluster>(n);
        for (int i = 0; i < n; i++) {
            Cluster c = clusters.getCluster(i);
            int[] indexes = c.getMembership();
            for (int j = 0; j < indexes.length; j++) {
                indexes[j] = fcs.getWrappedIndex(indexes[j]);
            }
            clusterList.add(new Cluster(indexes, c.getCenter()));
        }
        return clusterList;
    }
}
