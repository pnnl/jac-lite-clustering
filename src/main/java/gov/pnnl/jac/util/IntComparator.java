package gov.pnnl.jac.util;

/**
 * Interface for comparing two integers for sorting purposes,
 * when a simple comparison of values may not be sufficient.
 * 
 * @author R. Scarberry
 *
 */
public interface IntComparator {

	/**
	 * Return -1 if a should be placed before b, +1 if a should be
	 * after b, 0 if they should be regarded as equal by the sort.
	 * @param a
	 * @param b
	 * @return
	 */
	public int compare(int a, int b);
	
}
