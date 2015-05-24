/*
 * ClusterTaskParams.java
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

/**
 * <p>Interface defining entities that represent clustering algorithm
 * parameters.  Every extension of <tt>ClusterTask</tt> that takes parameters 
 * should have its own associated implementation of <tt>ClusterTaskParams</tt>.
 * </p>
 *
 * @author R. Scarberry
 */
public interface ClusterTaskParams extends 
    java.io.Serializable, java.lang.Cloneable {}
