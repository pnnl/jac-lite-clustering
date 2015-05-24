package gov.pnnl.jac.task;

public class TaskSourceTask extends AbstractTask<Void> 
	implements TaskListener {

	// A source of tasks to run in sequence.
	private TaskSource mSource;
	
	// Whatever task is currently running.  It's obtained from mSource.
	private Task mCurrentTask;
	
	public TaskSourceTask(TaskSource source) {
		if (source == null) throw new NullPointerException();
		mSource = source;
	}
	
	@Override
	protected Void doTask() throws Exception {
		
		if (mSource.getOverallBeginProgress() != this.getBeginProgress() ||
				mSource.getOverallEndProgress() != this.getEndProgress()) {
			super.setProgressEndpoints(mSource.getOverallBeginProgress(),
					mSource.getOverallEndProgress());
		}
		
		ProgressHandler ph = new ProgressHandler(this, 
				getBeginProgress(), 
				getEndProgress());
		
		ph.postBegin();
		
		while((mCurrentTask = mSource.nextTask()) != null) {
            // Check for cancel just in case cancel() was before mCurrentTask
            // could be set and started or AFTER it had finished.
            checkForCancel();
			mCurrentTask.addTaskListener(this);
			mCurrentTask.run();
			mCurrentTask.removeTaskListener(this);
            TaskOutcome outcome = mCurrentTask.getTaskOutcome();
            if (outcome != TaskOutcome.SUCCESS) {
                if (outcome == TaskOutcome.CANCELLED) {
                    // Will cause the run method (inherited from Task) for this
                    // object to get a CancellationException and properly set
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
            }
		}
		
		ph.postEnd();
		
		return null;
	}

    public final boolean cancel(boolean mayInterruptIfRunning) {
    	if (super.cancel(mayInterruptIfRunning)) {
        	// Cancel the currently executing subtask.  This will likely result
        	// in a CancellationException which the run() method of Task will
        	// catch.
        	if (mCurrentTask != null) {
        		// Force it to cancel, since this is called by
        		// CompoundTask's cancel method after cancellation
        		// has been irreversibly begun.
        		mCurrentTask.cancel(true);
        	}
    		return true;
    	}
    	return false;
    }

    public String taskName() {
		return mSource.overallTaskName();
	}

    public void setProgressEndpoints(double begin, double end) {        
    	super.setProgressEndpoints(begin, end);
    	mSource.setOverallProgressEndpoints(begin, end);
    }
    
    // TaskListener methods -- received from current subtask; forwarded to
    // registered listeners of this SequentialTask.
    //

    // Forward begun events of subtasks as message events for the overall
    // task.
    public void taskBegun(TaskEvent e) {
        // Listeners have already received a taskBegun event for the
        // overall sequence.  Convert the subtask taskBegun to a message event.
        postMessage(mSource.subtaskPrefix() + e.getMessage());
    }

    public void taskMessage(TaskEvent e) {
        postMessage(mSource.subtaskPrefix() + e.getMessage());
    }

    public void taskProgress(TaskEvent e) {
        postProgress(e.getTask().getProgress());
    }

    public void taskEnded(TaskEvent e) {
        // Listeners will receive a taskEnded method for the overall sequence
        // when all the subtasks are done.  Convert to a message event.
        postMessage(mSource.subtaskPrefix() + e.getMessage());
    }
}
