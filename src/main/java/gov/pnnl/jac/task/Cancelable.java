package gov.pnnl.jac.task;

/**
 * Interface which objects implement to indicate that their processes can be canceled.
 * 
 * @author R. Scarberry
 * @since 3.0.0
 *
 */
public interface Cancelable {

	boolean cancel(boolean force);
	
	boolean isCanceled();

}
