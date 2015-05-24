package gov.pnnl.jac.cluster;

import gov.pnnl.jac.collections.IntArrayList;
import gov.pnnl.jac.geom.CoordinateList;
import gov.pnnl.jac.geom.CoordinateMath;
import gov.pnnl.jac.geom.SimpleCoordinateList;
import gov.pnnl.jac.geom.distance.DistanceFunc;
import gov.pnnl.jac.task.ProgressHandler;
import gov.pnnl.jac.util.MethodTimer;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class FuzzyCMeansClusterTask extends ClusterTask {

	// Synchronize on this while changing mDegreeOfMembership
	private Object mMFLock = new Object();
	
	private double[][] mDegreeOfMembership;
	private double[][] mClusterCenters;

	private CoordinateList mCoords;
	private DistanceFunc mDistFunc;
	private int mClusterCount;
	private double mFuzziness;
	private double mFuzzyPow;
	
	private List<MembershipDegreeUpdater> mMembershipUpdaters;
	private List<ClusterCenterUpdater> mClusterCenterUpdaters;
	private List<ErrorCalculator> mErrorCalculators;
	
	private ExecutorService mThreadPool;

	public FuzzyCMeansClusterTask(CoordinateList cs,
			FuzzyCMeansClusterTaskParams params) {
		super(cs, params);
	}

	@Override
	public String getAlgorithmName() {
		return "fuzzy c-means";
	}

	@Override
	protected ClusterList doTask() throws Exception {

		try {

			final FuzzyCMeansClusterTaskParams params = (FuzzyCMeansClusterTaskParams) getParams();

			final int maxIterations = params.getMaxIterations();

			final int steps = maxIterations + 2;

			// Create the progress and message posting object and post the
			// first progress.
			ProgressHandler ph = new ProgressHandler(this, getBeginProgress(),
					getEndProgress(), steps);
			ph.postBegin();

			mCoords = getCoordinateList();
			mFuzziness = params.getFuzziness();
			mFuzzyPow = 2.0 / (mFuzziness - 1.0);
			
			mDistFunc = params.getDistanceFunc();

			final int coordCount = mCoords.getCoordinateCount();

			if (coordCount == 0) {
				error("zero coordinates");
			}
			
			mClusterCount = Math.min(params.getNumClusters(), coordCount);
			
			int numThreads = params.getNumWorkerThreads();
			if (numThreads <= 0) {
				numThreads = Runtime.getRuntime().availableProcessors();
			}
			
			mMembershipUpdaters = new ArrayList<MembershipDegreeUpdater> (numThreads);
			mErrorCalculators = new ArrayList<ErrorCalculator> (numThreads);
			
			int coordsPerUpdater = coordCount/numThreads;
			int startCoord = 0;
			for (int i=0; i<numThreads; i++) {
				int numCoords = i < (numThreads - 1) ? coordsPerUpdater : coordCount - startCoord;
				mMembershipUpdaters.add(new MembershipDegreeUpdater(startCoord, numCoords));
				mErrorCalculators.add(new ErrorCalculator(startCoord, numCoords));
				startCoord += numCoords;
			}
			
			mClusterCenterUpdaters = new ArrayList<ClusterCenterUpdater> ();
			int clustersPerUpdater = mClusterCount/numThreads;
			
			int startCluster = 0;
			for (int i=0; i<numThreads; i++) {
				int numClusters = i < (numThreads - 1) ? clustersPerUpdater : mClusterCount - startCluster;
				mClusterCenterUpdaters.add(new ClusterCenterUpdater(startCluster, numClusters));
				startCluster += numClusters;
			}
			
			if (numThreads > 0) {
				mThreadPool = Executors.newFixedThreadPool(numThreads);
			}

			initCenters(ph);
			
			// Holds the membership scores, [0 - 1]
			mDegreeOfMembership = new double[coordCount][mClusterCount];
			
			updateDegreesOfMembership();

			/** Other implementations I've seen randomly set the degrees of membership, THEN
			 *  compute cluster centers for the first time.  When that is done, I noticed many 
			 *  of the clusters falling out because of 0 membership.
			
			long randomSeed = params.getRandomSeed();
			if (randomSeed < 0L) {
				randomSeed = System.nanoTime();
			}

			// Randomly initialize the membership scores.
			Random random = new Random(randomSeed);

			for (int i = 0; i < coordCount; i++) {
				double sum = 0.0;
				for (int j = 0; j < mClusterCount; j++) {
					double m = 0.01 + random.nextDouble();
					mDegreeOfMembership[i][j] = m;
					sum += m;
				}
				for (int j = 0; j < mClusterCount; j++) {
					mDegreeOfMembership[i][j] /= sum;
				}
			}
			
			updateClusterCenters();
			 * 
			 */

			ph.postStep();

			double previousError = calculateError();

			final double epsilon = params.getEpsilon();

			int iteration = 0;

			while (iteration < maxIterations) {

				this.checkForCancel();

				updateClusterCenters();

				this.checkForCancel();

				updateDegreesOfMembership();

				double error = calculateError();

				ph.postStep();

				iteration++;

				double errorChange = Math.abs(previousError - error);

				ph.postMessage(String.format(
						"iteration %d, error change from %f to %f", iteration,
						previousError, error));

				previousError = error;

				if (errorChange < epsilon) {
					break;
				}
			}

			ph.postMessage("constructing final clusters");

			super.setClusterList(constructClusters());

			ph.postMessage(String.valueOf(mClusters.getClusterCount())
					+ " clusters generated");

			ph.postEnd();

		} finally {

			mClusterCenters = null;
			if (mThreadPool != null) {
				mThreadPool.shutdownNow();
				mThreadPool = null;
			}
			
			mMembershipUpdaters = null;
			mClusterCenterUpdaters = null;

		}

		return super.mClusters;
	}

	static MethodTimer.Marker MARKER;
	
	private void updateClusterCenters() throws Exception {

		MARKER = MethodTimer.startMethodTimer(MARKER, null);
		
		try {

			if (mThreadPool != null) {
				mThreadPool.invokeAll(mClusterCenterUpdaters);
			} else {
				mClusterCenterUpdaters.get(0).call();
			}
			
		} finally {
			MethodTimer.endMethodTimer(MARKER);
		}
	}

	private static MethodTimer.Marker UPDATE_DEGREES;
	
	private void updateDegreesOfMembership() throws Exception {
		
		synchronized (mMFLock) {
		
			UPDATE_DEGREES = MethodTimer.startMethodTimer(UPDATE_DEGREES, null);

			try {
				if (mThreadPool != null) {
					mThreadPool.invokeAll(mMembershipUpdaters);
				} else {
					mMembershipUpdaters.get(0).call();
				}
			} finally {
				MethodTimer.endMethodTimer(UPDATE_DEGREES);
			}
		}
	}

	public double[] getDegreesOfMemberShip(int coord, double[] mfBuffer) {

		synchronized (mMFLock) {
			if (mfBuffer == null || mfBuffer.length < mClusterCount) {
				mfBuffer = new double[mClusterCount];
			}
			if (mDegreeOfMembership != null) {
				System.arraycopy(mDegreeOfMembership[coord], 0, mfBuffer, 0, mClusterCount);
			} else {
				Arrays.fill(mfBuffer, 0.0);
			}
			return mfBuffer;
		}
		
	}
	
	public ClusterList constructClusters() {

		IntArrayList[] membershipList = new IntArrayList[mClusterCount];

		final int coordCount = mCoords.getCoordinateCount();

		for (int i = 0; i < coordCount; i++) {

			double maxm = 0.0;
			int c = 0;

			for (int j = 0; j < mClusterCount; j++) {
				double m = mDegreeOfMembership[i][j];
				if (j == 0 || m > maxm) {
					maxm = m;
					c = j;
				}
			}

			if (membershipList[c] == null) {
				membershipList[c] = new IntArrayList();
			}

			membershipList[c].add(i);

		}

		List<Cluster> clist = new ArrayList<Cluster>(mClusterCount);
		for (int i = 0; i < mClusterCount; i++) {

			IntArrayList mlist = membershipList[i];
			if (mlist != null && mlist.size() > 0) {
				Cluster c = new Cluster(mlist.toArray(), mCoords);
				clist.add(c);
			}
		}

		final int sz = clist.size();

		return new ClusterList(clist.toArray(new Cluster[sz]));
	}

	private static MethodTimer.Marker CALC_ERROR;
	
	// This is the objective function we wish to minimize. It should decrease
	// for every iteration.
	private double calculateError() throws Exception {

		CALC_ERROR = MethodTimer.startMethodTimer(CALC_ERROR, null);

		try {

			if (mThreadPool != null) {
				mThreadPool.invokeAll(mErrorCalculators);
			} else {
				mErrorCalculators.get(0).call();
			}
			
			double error = 0;
			final int sz = mErrorCalculators.size();
			for (int i=0; i<sz; i++) {
				error += mErrorCalculators.get(i).getError();
			}
			
			return error;
			
		} finally {

			MethodTimer.endMethodTimer(CALC_ERROR);

		}
	}

	private static MethodTimer.Marker INIT_CENTERS;
	
	private void initCenters(ProgressHandler ph) {

		INIT_CENTERS = MethodTimer.startMethodTimer(INIT_CENTERS, null);

		try {

			FuzzyCMeansClusterTaskParams params = (FuzzyCMeansClusterTaskParams) getParams();

			CoordinateList cs = getCoordinateList();

			ClusterSeeder seeder = params.getClusterSeeder();

			int clustersRequested = params.getNumClusters();

			int minUniqueCoordCount = CoordinateMath
					.checkNumberOfUniqueCoordinates(cs, clustersRequested);

			int actualClusterCount = Math.min(clustersRequested,
					minUniqueCoordCount);

			CoordinateList centers = seeder.generateSeeds(cs, actualClusterCount);
			mClusterCount = centers.getCoordinateCount();

			mClusterCenters = new double[mClusterCount][centers.getDimensionCount()];
			for (int i=0; i<mClusterCount; i++) {
				centers.getCoordinates(i, mClusterCenters[i]);
			}

			this.checkForCancel();

			if (clustersRequested > 0 && mClusterCount < clustersRequested) {
				ph.postMessage("number of requested clusters reduced to "
						+ mClusterCount + ", the number of unique coordinates");
			}

		} finally {

			MethodTimer.endMethodTimer(INIT_CENTERS);

		}
	}	
	
	class MembershipDegreeUpdater implements Callable<Void> {

		private int mStartCoord, mNumCoords;
		// Every updater needs its own distance func object.  They
		// can't be safely shared.
		private DistanceFunc mMyDistFunc;
		
		MembershipDegreeUpdater(int startCoord, int numCoords) {
			mStartCoord = startCoord;
			mNumCoords = numCoords;
			mMyDistFunc = mDistFunc.clone();
		}
		
		@Override
		public Void call() throws Exception {
			
			final int lim = mStartCoord + mNumCoords;
			final int coordLen = mCoords.getDimensionCount();

			final double[] coordBuf = new double[coordLen];
			final double[] dists = new double[mClusterCount];
			
			for (int i=mStartCoord; i<lim; i++) {
				
				mCoords.getCoordinates(i, coordBuf);
				
				for (int j=0; j<mClusterCount; j++) {
					dists[j] = mMyDistFunc.distanceBetween(coordBuf, mClusterCenters[j]);
				}
				
				for (int j=0; j<mClusterCount; j++) {
					
					double dist = dists[j];
					double denom = 0.0;
					
					for (int k=0; k<mClusterCount; k++) {
						double ratio = 1.0;
						if (j != k) {
							double dist2 = dists[k];
							ratio = dist/dist2;
						}
						denom += Math.pow(ratio, mFuzzyPow);
					}
					
					mDegreeOfMembership[i][j] = 1.0/denom;
				}
			}
						
			return null;
		}
		
	}
	
	class ClusterCenterUpdater implements Callable<Void> {
		
		private int mStartCluster;
		private int mNumClusters;
		
		ClusterCenterUpdater(int startCluster, int numClusters) {
			mStartCluster = startCluster;
			mNumClusters = numClusters;
		}

		@Override
		public Void call() throws Exception {
		
			for (int i=0; i<mNumClusters; i++) {
				Arrays.fill(mClusterCenters[i + mStartCluster], 0.0);
			}

			final int numCoords = mCoords.getCoordinateCount();
			final int coordLen = mCoords.getDimensionCount();
			
			double[] coordBuf = new double[coordLen];
			double[][] denoms = new double[mNumClusters][coordLen];
			
			final int jLim = mStartCluster + mNumClusters;
			
			for (int i=0; i<numCoords; i++) {
				
				mCoords.getCoordinates(i, coordBuf);
				
				for (int j=mStartCluster; j<jLim; j++) {
					double m = mDegreeOfMembership[i][j];
					double f = Math.pow(m, mFuzziness);
					double[] center = mClusterCenters[j];
					double[] denom = denoms[j - mStartCluster];
					for (int k=0; k<coordLen; k++) {
						center[k] += f * coordBuf[k];
						denom[k] += f;
					}
				}
			}
			
			for (int i=mStartCluster; i<jLim; i++) {
				double[] center = mClusterCenters[i];
				double[] denom = denoms[i - mStartCluster];
				for (int j=0; j<coordLen; j++) {
					center[j] /= denom[j];
				}
			}
			
			return null;
		}
		
	}

	class ErrorCalculator implements Callable<Void> {

		private int mStartCoord, mNumCoords;
		private DistanceFunc mMyDistFunc;
		
		private double mError;

		ErrorCalculator(int startCoord, int numCoords) {
			mStartCoord = startCoord;
			mNumCoords = numCoords;
			mMyDistFunc = mDistFunc.clone();
		}
		
		double getError() {
			return mError;
		}
		
		@Override
		public Void call() throws Exception {
		
			double error = 0;

			final int coordLen = mCoords.getDimensionCount();

			final double[] coordBuf = new double[coordLen];

			final int iLim = mStartCoord + mNumCoords;
			
			for (int i = mStartCoord; i < iLim; i++) {

				mCoords.getCoordinates(i, coordBuf);

				for (int j = 0; j < mClusterCount; j++) {

					double distance = mMyDistFunc.distanceBetween(coordBuf, mClusterCenters[j]);

					double m = mDegreeOfMembership[i][j];

					double multiplier = Math.pow(m, mFuzziness); 

					error += distance * multiplier;	
				}

			}

			mError = error;
			
			return null;
		}
	}

}
