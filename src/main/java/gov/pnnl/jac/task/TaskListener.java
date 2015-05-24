package gov.pnnl.jac.task;

/**
 * <p>Interface defining objects which listen for <code>TaskEvents</code>s
 * from running <code>Task</code> implementations</p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: Battelle Memorial Institute</p>
 *
 * @author R. Scarberry
 * @version 1.0
 */
public interface TaskListener {
	
	/**
	 * Utility class which you can extend if you are
	 * interested in only doing something in response
	 * to a subset of task event types.  Usage scenarios
	 * are similar to those of <code>MouseAdapter</code>, et cetera.
	 *
	 * @param <V>
	 */
	public class Adapter implements TaskListener {
		public void taskBegun(TaskEvent e) {
		}

		public void taskEnded(TaskEvent e) {
		}

		public void taskMessage(TaskEvent e) {
		}

		public void taskProgress(TaskEvent e) {
		}
	};

    /**
     * Sent to listeners when a <code>Task</code> has begun execution.
     * @param e
     */
    public void taskBegun(TaskEvent e);

    /**
     * Message events sent to listeners as a <code>Task</code> executes.
     * @param e
     */
    public void taskMessage(TaskEvent e);

    /**
     * Events sent to listeners by executing <code>Task</code>s to 
     * indicate progress.
     * @param e
     */
    public void taskProgress(TaskEvent e);

    /**
     * Final event sent to listeners when a <code>Task</code> finishes execution.
     * @param e
     */
    public void taskEnded(TaskEvent e);

}
