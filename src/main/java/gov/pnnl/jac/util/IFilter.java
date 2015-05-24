package gov.pnnl.jac.util;

/**
 * Interface defining a general filter for determining if objects match an arbitray condition.
 * 
 * @author D3J923
 *
 * @param <E>
 */
public interface IFilter<E> {

	boolean accept(E e);
	
}
