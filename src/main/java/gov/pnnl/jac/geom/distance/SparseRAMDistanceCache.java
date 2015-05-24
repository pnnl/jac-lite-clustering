package gov.pnnl.jac.geom.distance;

import java.io.IOException;

public class SparseRAMDistanceCache implements DistanceCache {

	public SparseRAMDistanceCache(int indexCount) {
		
	}
	
	public double[] getDistances(int[] indices1, int[] indices2,
			double[] distances) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public void setDistance(int index1, int index2, double distance)
			throws IOException {
		// TODO Auto-generated method stub

	}

	public void setDistances(int[] indices1, int[] indices2, double[] distances)
			throws IOException {
		// TODO Auto-generated method stub

	}

	public long distancePos(int index1, int index2) {
		// TODO Auto-generated method stub
		return 0;
	}

	public double getDistance(int index1, int index2) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	public double getDistance(long n) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	public long getNumDistances() {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getNumIndices() {
		// TODO Auto-generated method stub
		return 0;
	}

}
