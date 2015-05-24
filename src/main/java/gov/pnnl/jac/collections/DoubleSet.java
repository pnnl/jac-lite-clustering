// ** Notice:
// **     This computer software was prepared by Battelle Memorial Institute,
// **     hereinafter the Contractor, under Contract No. DE-AC06-76RL0 1830 with
// **     the Department of Energy (DOE).  All rights in the computer software
// **     are reserved by DOE on behalf of the United States Government and the
// **     Contractor as provided in the Contract.  You are authorized to use
// **     this computer software for Governmental purposes but it is not to be
// **     released or distributed to the public. NEITHER THE GOVERNMENT NOR THE
// **     CONTRACTOR MAKES ANY WARRANTY, EXPRESS OR IMPLIED, OR ASSUMES ANY
// **     LIABILITY FOR THE USE OF THIS SOFTWARE.  This notice including this
// **     sentence must appear on any copies of this computer software.
package gov.pnnl.jac.collections;

/**
 * <tt>DoubleSet</tt> is an interface defining entities that store doubles
 * (primitive <tt>double</tt>) values in a set.  No duplicate entries are
 * permitted.  Each unique double value is contained once or not at all.
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: Battelle Memorial Institute</p>
 *
 * @author R. Scarberry, Vern Crow
 * @version 1.0
 */
public interface DoubleSet extends DoubleCollection {

    /**
     * Returns a new <tt>DoubleSet</tt> containing the union of the values
     * in this set and the specified set.
     * @param other - the other set.
     * @return - a new <tt>DoubleSet</tt> object with the same class as the
     *   receiver.
     */
    DoubleSet unionWith(DoubleSet other);

    /**
     * Returns a new <tt>DoubleSet</tt> containing the intersection of the values
     * in this set and the specified set.
     * @param other - the other set.
     * @return - a new <tt>DoubleSet</tt> object with the same class as the
     *   receiver.
     */
    DoubleSet intersectionWith(DoubleSet other);

    /**
     * Returns a new <tt>DoubleSet</tt> containing the values
     * contained either in the receiver or the specified set, but not both.
     * @param other - the other set.
     * @return - a new <tt>DoubleSet</tt> object with the same class as the
     *   receiver.
     */
    DoubleSet xorWith(DoubleSet other);

}
