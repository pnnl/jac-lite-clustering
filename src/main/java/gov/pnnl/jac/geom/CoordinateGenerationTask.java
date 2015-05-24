package gov.pnnl.jac.geom;

import java.util.Arrays;

import cern.colt.list.IntArrayList;
import cern.jet.random.Normal;
import gov.pnnl.jac.cluster.*;
import gov.pnnl.jac.task.*;
import gov.pnnl.jac.util.RandEngine;

public class CoordinateGenerationTask extends AbstractTask<CoordinateList> {

    private CoordinateListFactory mFactory;
    private String mCoordsID;
    private int mNumCoords;
    private int mNumDimensions;
    private int mNumClusters;
    private double mClusterMultiplier;
    private double mMeanStandardDev;
    private double mStddevStandardDev;
    private long mRandomSeed;
    
    // The fraction of missing values (NaNs).  
    private double mFractionMissing;
    
    private CoordinateList mCoordinates;
    private ClusterList mClusters;
    
    public CoordinateGenerationTask(CoordinateListFactory factory,
            String coordsID,
            int numCoords,
            int numDimensions,
            int numClusters,
            double clusterMultiplier,
            double meanStandardDev,
            double stddevStandardDev,
            long randomSeed) {
        if (numCoords <= 0) {
            throw new IllegalArgumentException("must be positive: " + numCoords);
        }
        if (numDimensions <= 0) {
            throw new IllegalArgumentException("must be positive: " + numDimensions);
        }
        if (Double.isNaN(clusterMultiplier)) {
            throw new IllegalArgumentException("clusterMultiplier is NaN");
        }
        mFactory = factory;
        mCoordsID = coordsID;
        mNumCoords = numCoords;
        mNumDimensions = numDimensions;
        mNumClusters = numClusters;
        mClusterMultiplier = clusterMultiplier;
        mMeanStandardDev = meanStandardDev;
        mStddevStandardDev = stddevStandardDev;
        mRandomSeed = randomSeed;
    }
    
    public void setFractionMissing(double f) {
        mFractionMissing = f;
    }
    
    public double getFractionMissing() {
        return mFractionMissing;
    }
    
    protected CoordinateList doTask() throws Exception {
        
        ProgressHandler ph = new ProgressHandler(this, mNumCoords);
        ph.postBegin();
        
        CoordinateList cs = mFactory.createCoordinateList(mCoordsID, mNumDimensions, mNumCoords);
        checkForCancel();
        
        RandEngine random = new RandEngine(mRandomSeed);
        
        int numClusters = Math.max(1, Math.min(mNumClusters, mNumCoords));
        
        double mult = Math.max(1.0, mClusterMultiplier);
        double[] fracs = new double[numClusters];
        
        if (mult == 1.0) {
            Arrays.fill(fracs, 1.0/numClusters);
        } else {
            double minFrac = 1.0/((mult - 1.0) * numClusters);
            double maxFrac = mult * minFrac;
            double d = maxFrac - minFrac;
            double totalFrac = 0.0;
            for (int i=0; i<numClusters; i++) {
                fracs[i] = minFrac + random.nextDouble()*d;
                totalFrac += fracs[i];
            }
            d = 1.0/totalFrac;
            for (int i=0; i<numClusters; i++) {
                fracs[i] *= d;
            }
        }
        
        checkForCancel();
        
        double[] cdfs = new double[numClusters];
        for (int i=1; i<numClusters; i++) {
            cdfs[i] = cdfs[i-1] + fracs[i-1];
        }
        
        double stddevStandardDev = Math.max(0.0, mStddevStandardDev);
        
        Normal[] clusterNormals = new Normal[numClusters];
        
        if (stddevStandardDev > 0.0) {
            Normal normal = new Normal(mMeanStandardDev, mStddevStandardDev, random);
            for (int i=0; i<numClusters; i++) {
                clusterNormals[i] = new Normal(0.0, normal.nextDouble(), random);
            }
        } else {
            Normal normal = new Normal(0.0, mMeanStandardDev, random);
            Arrays.fill(clusterNormals, normal);
        }
        
        checkForCancel();
        
        double[][] exemplars = new double[numClusters][mNumDimensions];
        IntArrayList[] clusterIDLists = new IntArrayList[numClusters];

        for (int i = 0; i < numClusters; i++) {
                for (int j = 0; j < mNumDimensions; j++) {
                        exemplars[i][j] = random.nextDouble();
                }
                clusterIDLists[i] = new IntArrayList();
        }

        double[] buf = new double[mNumDimensions];

        boolean gotFractionMissing = mFractionMissing > 0.0;
        
        for (int i = 0; i < mNumCoords; i++) {
            double d = random.nextDouble();
            int cluster = 0;
            for (int j=numClusters-1; j>=0; j--) {
                if (d >= cdfs[j]) {
                    cluster = j;
                    break;
                }
            }
            double[] exemplar = exemplars[cluster];
            Normal cnormal = clusterNormals[cluster];
            for (int j = 0; j < mNumDimensions; j++) {
                if (gotFractionMissing && random.nextDouble() < mFractionMissing) {
                    buf[j] = Double.NaN;
                } else {
                    buf[j] = exemplar[j] + cnormal.nextDouble();
                }
            }
            
            cs.setCoordinates(i, buf);
            clusterIDLists[cluster].add(i);
            
            ph.postStep();
        }

        Cluster[] clusters = new Cluster[numClusters];
        for (int i = 0; i < numClusters; i++) {
                clusterIDLists[i].trimToSize();
                clusters[i] = new Cluster(clusterIDLists[i].elements(), cs);
        }

        mCoordinates = cs;
        mClusters = new ClusterList(clusters);
        
        ph.postEnd();
        
        return cs;
    }

    public String taskName() {
        return "coordinate generation";
    }
    
    public ClusterList getClusterList() {
        return mClusters;
    }
    
    public CoordinateList getCoordinateList() {
        return mCoordinates;
    }
}
