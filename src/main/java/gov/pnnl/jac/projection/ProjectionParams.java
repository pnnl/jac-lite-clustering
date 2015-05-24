package gov.pnnl.jac.projection;

import cern.colt.map.HashFunctions;

public class ProjectionParams {

	private double mGravityFactor = 0.2;
	private boolean mNormalizeDimensions = true;
	private boolean mNormalizeCoordinates = true;
	private int mNumDimensions = 2;
	
	public ProjectionParams(int numDimensions, double gravityFactor,
			boolean normalizeDimensions,
			boolean normalizeCoordinates) {
		if (gravityFactor < 0.0 || gravityFactor > 1.0) {
			throw new IllegalArgumentException("invalid tolerance: " + gravityFactor);
		}
		if (numDimensions <= 0) {
			throw new IllegalArgumentException("number of dimensions <= 0: " + numDimensions);
		}
		mGravityFactor = gravityFactor;
		mNormalizeDimensions = normalizeDimensions;
		mNormalizeCoordinates = normalizeCoordinates;
		mNumDimensions = numDimensions;
	}
	
	public ProjectionParams(int numDimensions) {
	    this(numDimensions, 0.2, true, true);
	}
	
	public ProjectionParams() {
		this(2, 0.2, true, true);
	}
	
	public double getGravityFactor() {
		return mGravityFactor;
	}
	
	public boolean getNormalizeDimensions() {
		return mNormalizeDimensions;
	}
	
	public boolean getNormalizeCoordinates() {
		return mNormalizeCoordinates;
	}
	
	public int getNumDimensions() {
		return mNumDimensions;
	}
	
	public int hashCode() {
		int hc = HashFunctions.hash(mGravityFactor);
		hc = hc*37 + HashFunctions.hash(mNormalizeDimensions);
		hc = hc*37 + HashFunctions.hash(mNormalizeCoordinates);
		hc = hc*37 + HashFunctions.hash(mNumDimensions);
		return hc;
	}
	
	public boolean equals(Object o) {
		if (o == this) return true;
		if (o instanceof ProjectionParams) {
			ProjectionParams other = (ProjectionParams) o;
			return other.getGravityFactor() == this.getGravityFactor() &&
			  other.getNormalizeCoordinates() == this.getNormalizeCoordinates() &&
			  other.getNormalizeDimensions() == this.getNormalizeDimensions() &&
			  other.getNumDimensions() == this.getNumDimensions();
		}
		return false;
	}
}
