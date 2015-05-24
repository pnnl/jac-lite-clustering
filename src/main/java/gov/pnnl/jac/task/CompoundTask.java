package gov.pnnl.jac.task;

import java.util.Arrays;

/**
 * <p>CompoundTask -- when a group of smaller Tasks comprises the
 * overall Task. This abstract class makes no assumption of order 
 * dependence between the subtasks.  Concrete subclasses must figure
 * out whether to run the subtasks in sequence, in parallel, or in
 * a combination of the two.</p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: Battelle Memorial Institute</p>
 *
 * @author R. Scarberry
 * @version 1.0
 */
public abstract class CompoundTask extends AbstractTask<Object> 
	implements TaskListener {

    protected static final String DEFAULT_SUBTASK_PREFIX = "  ... ";

    protected String mTaskName;
    // The subtasks launched by this task.
    protected Task<?>[] mSubtasks;
    // The share of the total progress range given to each subtask.
    protected double[] mSubtaskFractions;
    // The prefix added to all subtask messages before forwarding them
    // to the registered listeners for the overall task.
    protected String mSubtaskPrefix = DEFAULT_SUBTASK_PREFIX;

    /**
     * Constructor which takes both the subtasks and their progress fractions.
     * See the remarks for <code>setSubtasks</code>.
     * @param taskName - the name to be returned by taskName().
     * @param subtasks Task[]
     * @param subtaskFractions double[]
     */
    protected CompoundTask(String taskName, Task<?>[] subtasks, double[] subtaskFractions) {
        if (taskName == null) {
            throw new NullPointerException();
        }
        mTaskName = taskName;
        setSubtasks(subtasks, subtaskFractions);
    }
    
    /**
     * Set the subtasks and their associated progress fractions.  The number
     * of subtasks should equal the number of subtask progress fractions.
     * If not, the subtask fractions will be ignored and each subtask will
     * get the an equal share of the progress spread.  If the subtask progress
     * fractions do not sum to 1.0, they are scaled so that they do.
     * @param subtasks Task[] - an array of subtasks to execute in sequence.
     * @param subtaskFractions double[] - the fraction of the overall
     *   progress spread to be alotted to each subtask.
     */
    protected void setSubtasks(Task<?>[] subtasks, double[] subtaskFractions) {
        if (subtasks == null) {
            throw new NullPointerException();
        }
        int n = subtasks.length;
        mSubtasks = new Task[n];
        System.arraycopy(subtasks, 0, mSubtasks, 0, n);
        int m = subtaskFractions != null ? subtaskFractions.length : 0;
        double equalFrac = n > 0 ? 1.0/n : 0.0;
        if (n == m) {
            mSubtaskFractions = new double[m];
            System.arraycopy(subtaskFractions, 0, mSubtaskFractions, 0, m);
            double sum = 0.0;
            for (int i=0; i<m; i++) {
                sum += mSubtaskFractions[i];
            }
            if (sum != 1.0) {
                // Need to normalize, so that the sum equals 1.0.
                if (sum > 0.0) {
                    for (int i=0; i<m; i++) {
                        mSubtaskFractions[i] /= sum;
                    }
                } else {
                    // Must've passed in an array of 0s.  Set all to equalFrac.
                    Arrays.fill(mSubtaskFractions, equalFrac);
                }
            }
        } else { // n != m.  Either passed in null or just did something dumb.

            // Set all the subtask progress fractions to an equal share.
            mSubtaskFractions = new double[n];
            Arrays.fill(mSubtaskFractions, equalFrac);
        }
    }

    /**
     * Define a string to be prepended to messages from the subtasks before
     * they are forwarded to listeners of the SequentialTask.  The default
     * prefix is two spaces, three periods, and another space. ("  ... ").
     * @param prefix String
     */
    public void setSubtaskPrefix(String prefix) {
        mSubtaskPrefix = (prefix != null ? prefix : "");
    }

    /**
     * Returns a name for the overall task.
     * @return String
     */
    public String taskName() {
        return mTaskName;
    }

    public final boolean cancel(boolean mayInterruptIfRunning) {
    	if (super.cancel(mayInterruptIfRunning)) {
    		cancelSubtasks();
    		return true;
    	}
    	return false;
    }

    /**
     * Subclasses must figure out how to cancel their subtasks 
     * after cancellation has begun.
     */
    protected abstract void cancelSubtasks();
}
