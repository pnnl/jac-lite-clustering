/*
 * PreassignedSeeder.java
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

import cern.colt.map.HashFunctions;
import gov.pnnl.jac.geom.CoordinateList;

public class PreassignedSeeder implements ClusterSeeder {

	private CoordinateList mSeeds;
	
	public PreassignedSeeder(CoordinateList seeds) {
		if (seeds == null) throw new NullPointerException();
		mSeeds = seeds;
	}
	
	public int hashCode() {
		int hc = 17;
		int coordLen = mSeeds.getDimensionCount();
		int numCoords = mSeeds.getCoordinateCount();
		double[] buffer = new double[coordLen];
		for (int i=0; i<numCoords; i++) {
			mSeeds.getCoordinates(i, buffer);
			for (int j=0; j<coordLen; j++) {
				hc = 31 * hc + HashFunctions.hash(buffer[j]);
			}
		}
		return hc;
	}
	
	public boolean equals(Object o) {
		if (o == this) return true;
		if (o instanceof PreassignedSeeder) {
			PreassignedSeeder other = (PreassignedSeeder) o;
			if (other.mSeeds == this.mSeeds) return true;
			int coordLen = this.mSeeds.getDimensionCount();
			int numCoords = this.mSeeds.getCoordinateCount();
			if (coordLen == other.mSeeds.getDimensionCount() &&
					numCoords == other.mSeeds.getCoordinateCount()) {
				double[] buffer1 = new double[coordLen];
				double[] buffer2 = new double[coordLen];
				for (int i=0; i<numCoords; i++) {
					this.mSeeds.getCoordinates(i, buffer1);
					other.mSeeds.getCoordinates(i, buffer2);
					for (int j=0; j<coordLen; j++) {
						if (Double.doubleToLongBits(buffer1[j]) != Double.doubleToLongBits(buffer2[j])) {
							return false;
						}
					}
				}
				
				return true;
			}
		}
		return false;
	}
	
	public CoordinateList generateSeeds(final CoordinateList coords, final int numSeeds) {
		if (coords.getDimensionCount() != mSeeds.getDimensionCount()) {
			throw new IllegalArgumentException("dimensionality of coordinates != dimensionality of seeds");
		}
		return mSeeds;
	}
}
