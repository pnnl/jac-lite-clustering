/*
 * ClusterSeeder.java
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

/**
 * <p>Interface defining entities which generate cluster seeds, such as
 * those used to initialize K-Means clusters.</p>
 * 
 * @author d3j923
 *
 */
public interface ClusterSeeder {

        /**
         * Returns a list of coordinates containing the generated seeds.
         * 
         * @param coords a <tt>CoordinateList</tt> containing the coordinates to
         *   be clustered.
         * @param numSeeds the number of seeds to generate.
         * 
         * @return a new <tt>CoordinateList</tt> containing the seeds.
         */
	CoordinateList generateSeeds(CoordinateList coords, int numSeeds);
	
}
