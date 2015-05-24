package gov.pnnl.jac.task;

/**
 * Defines an entity which supplies one or more <tt>Task</tt> to be run in 
 * sequence.  When no more tasks are to be run, the entity returns null.
 * 
 * @author D3J923
 *
 */
public interface TaskSource {

	/**
	 * Returns the next task.
	 * 
	 * @return - a task instance or null, if there are no more tasks from this source.
	 */
	Task<?> nextTask();
	
	/**
	 * A name for the overall sequence of tasks.
	 * 
	 * @return
	 */
	String overallTaskName();
	
	/**
	 * A prefix to be prepended to messages forwarded to listeners.
	 */
	String subtaskPrefix();
	
	double getOverallBeginProgress();
	
	double getOverallEndProgress();
	
    void setOverallProgressEndpoints(double begin, double end);        
}
