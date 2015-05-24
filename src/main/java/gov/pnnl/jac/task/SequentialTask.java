package gov.pnnl.jac.task;


/**
 * <p>SequentialTask -- when a sequence of smaller Tasks comprises the
 * overall Task.
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: Battelle Memorial Institute</p>
 *
 * @author R. Scarberry
 * @version 1.0
 */
public class SequentialTask extends CompoundTask {

    // Currently executing subtask, since only one may
	// execute at a time.
    protected Task<?> mCurrentTask;

    /**
     * Constructor which takes both the subtasks and their progress fractions.
     * See the remarks for <code>setSubtasks</code>.
     * @param taskName - the name to be returned by taskName().
     * @param subtasks Task[]
     * @param subtaskFractions double[]
     */
    public SequentialTask(String taskName, 
    		Task<?>[] subtasks, 
    		double[] subtaskFractions) {
    	super(taskName, subtasks, subtaskFractions);
    }

    /**
     * Constructor which configures the subtasks, alotting equal shares of
     * the overall progress spread to each.
     * @param taskName - the name to be returned by taskName().
     * @param subtasks Task[]
     */
    public SequentialTask(String taskName, Task<?>[] subtasks) {
        this(taskName, subtasks, null);
    }

    /**
     * Default constructor.  The subclass should call <code>setSubtasks</code>
     * before it is executed.
     * @param taskName - the name to be returned by taskName().
     */
    public SequentialTask(String taskName) {
        this (taskName, new Task[0]);
    }

    /**
     * Executes the subtasks in sequential order.
     */
    protected final Object doTask() throws Exception {
        // Before doing anything else, loop through the subtasks and set their
        // progress endpoints.
        int n = mSubtasks.length;
        double p1 = getBeginProgress();
        double delt = getEndProgress() - p1;
        for (int i=0; i<n; i++) {
            double p2 = mSubtaskFractions[i]*delt + p1;
            mSubtasks[i].setProgressEndpoints(p1, p2);
            p1 = p2;
        }
        Object[] results = new Object[n];
        // Execute the subtasks by calling their run methods directly.
        for (int i=0; i<n; i++) {
            // Check for cancel just in case cancel() was before mCurrentTask
            // could be set and started or AFTER it had finished.
            checkForCancel();
            mCurrentTask = mSubtasks[i];
            mCurrentTask.addTaskListener(this);
            mCurrentTask.run();
            mCurrentTask.removeTaskListener(this);
            TaskOutcome outcome = mCurrentTask.getTaskOutcome();
            if (outcome != TaskOutcome.SUCCESS) {
                if (outcome == TaskOutcome.CANCELLED) {
                    // Will cause the run method (inherited from Task) for this
                    // object to get a CancelationException and properly set
                    // the outcome.
                    checkForCancel();
                } else if (outcome == TaskOutcome.ERROR) {
                    // Transfer the error message from the subtask to
                    // this object.
                    error(mCurrentTask.getErrorMessage());
                } else { // Has to be TaskOutcome.NOT_FINISHED, which makes no sense.
                         // But we'll deal with it for completeness.
                    error(mCurrentTask.taskName()
                          + " completed without properly setting its outcome");
                }
                // No need for a break statement, since checkForCancel and
                // error trigger exceptions that abort the for loop.
            } else {
                results[i] = mCurrentTask.get();
            }
        }
        return results;
    }

    /**
     * Cancel the sequence of tasks.
     */
    protected void cancelSubtasks() {
    	// Cancel the currently executing subtask.  This will likely result
    	// in a CancelationException which the run() method of Task will
    	// catch.
    	if (mCurrentTask != null) {
    		// Force it to cancel, since this is called by
    		// CompoundTask's cancel method after cancellation
    		// has been irreversibly begun.
    		mCurrentTask.cancel(true);
    	}
    }

    // TaskListener methods -- received from current subtask; forwarded to
    // registered listeners of this SequentialTask.
    //

    // Forward begun events of subtasks as message events for the overall
    // task.
    public void taskBegun(TaskEvent e) {
        // Listeners have already received a taskBegun event for the
        // overall sequence.  Convert the subtask taskBegun to a message event.
        postMessage(mSubtaskPrefix + e.getMessage());
    }

    public void taskMessage(TaskEvent e) {
        postMessage(mSubtaskPrefix + e.getMessage());
    }

    public void taskProgress(TaskEvent e) {
        postProgress(e.getTask().getProgress());
    }

    public void taskEnded(TaskEvent e) {
        // Listeners will receive a taskEnded method for the overall sequence
        // when all the subtasks are done.  Convert to a message event.
        postMessage(mSubtaskPrefix + e.getMessage());
    }
}
