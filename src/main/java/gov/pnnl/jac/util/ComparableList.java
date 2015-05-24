package gov.pnnl.jac.util;

/**
 * Interface defining an entity containing members that it can compare
 * for sorting purposes.
 * 
 * @author d3j923
 *
 */
public interface ComparableList {

	/**
	 * The number of members, always greater than or equal to zero.
	 * @return
	 */
	public int memberCount();
	
	/**
	 * Compares the members with the given indexes.  Both i and
	 * j must be in the range <tt>[0 - memberCount() - 1]</tt>
	 * @param i
	 * @param j
	 * @return
	 */
	public int compareMembers(int i, int j);
	
}
