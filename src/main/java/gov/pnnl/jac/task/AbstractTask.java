package gov.pnnl.jac.task;

import java.util.*;
import java.util.concurrent.*;
import javax.swing.ProgressMonitor;
import org.apache.log4j.Logger;

/**
 * <p>The <code>AbstractTask</code> class is an abstract base implementation of 
 * <code>Task</code> on which to build extensions that perform time-consuming chores.
 * <p>Extensions of this class must implement a method <code>doTask()</code> in
 * which to perform their work.  They cannot define a
 * <code>run()</code> method, since AbstractTask's run() method is final.  This is done
 * to ensure proper handling of exceptions, event handling, and cleanup.</p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: Battelle Memorial Institute</p>
 *
 * @author R. Scarberry
 * @version 1.0
 */
public abstract class AbstractTask<V> implements Task<V> {

        private static final Logger LOGGER = Logger.getLogger(AbstractTask.class);
        
        // The outcome of the AbstractTask.  Never null, but NOT_FINISHED means what
        // it sounds like.
        private TaskOutcome mOutcome = TaskOutcome.NOT_FINISHED;
        
        // Stores the results of error conditions.  If <code>error(String mesg)</code>
        // is called by the subclass' <code>doTask()</code> method,
        // the string is set to the message and the throwable is set to a RuntimeException
        // which is immediately thrown.
        private Throwable mError;
        private String mErrorMsg;
        
        // The owning thread.  Only one thread may ever execute a given
        // AbstractTask<V> at a time.  This is set near the beginning of the
        // run() method.
        private volatile Thread mOwner;
        
        // Cancel flag set by cancel().  Attempting to post a message or
        // progress triggers a CancellationException, which interrupts the
        // run method.
        private volatile boolean mCancelFlag;
        // Set to true in postBegun(), which can only be called once.
        private volatile boolean mHasBegun;
        // Set to true in postEnded(), which also can only be called once.
        private volatile boolean mHasEnded;
        
        // Set to true by pause;
        private volatile boolean mPauseFlag;

        // Endpoints for progress reporting.  To have the expected effect,
        // these must be set before the task is started.
        private double mBeginProgress = 0.0;
        private double mEndProgress = 1.0;
        
        // The last reported progress.
        private double mProgress;
        
        // The result returned by get().
        private V mResult;
     
        // Optional ProgressMonitor object to set via setProgressMonitor().
        // Calls to checkForCancel() then check the isCanceled() method of 
        // the progress monitor.
        private ProgressMonitor mPM;
        
        // List of TaskListeners to which TaskEvents are posted.
        //
        private class TaskListenerList extends ArrayList<TaskListener> implements
        	TaskListener {

			private static final long serialVersionUID = 1L;
			
			// Get all the listeners in an array, then send the events.
			// Don't iterate through the listeners and send events.  A listener
			// might remove itself, which cause the iterator to throw a
                        // ConcurrentModificationException.
			private TaskListener[] getAll() {
				TaskListener [] rtn = new TaskListener [this.size()];
				this.toArray(rtn);
				return rtn;
			}

			public synchronized void taskBegun(TaskEvent e) {
				for (TaskListener l: getAll()) {
					l.taskBegun(e);
				}
			}

			public synchronized void taskEnded(TaskEvent e) {
				for (TaskListener l: getAll()) {
					l.taskEnded(e);
				}
			}

			public synchronized void taskMessage(TaskEvent e) {
				for (TaskListener l: getAll()) {
					l.taskMessage(e);
				}
			}

			public synchronized void taskProgress(TaskEvent e) {
				for (TaskListener l: getAll()) {
					l.taskProgress(e);
				}
			}
        	
        };
        
        private final TaskListenerList mListeners = new TaskListenerList();

        /**
         * Add a listener to the receiver's list of listeners.  The listener
         * is normally added before the thread executing the AbstractTask is started.  As
         * the AbstractTask executes, registered listeners receive event notifications
         * when the AbstractTask starts, when it ends, messages, and progress indications.
         * (Subclasses of <code>AbstractTask</code> are responsible for messages and
         * progress, but <code>AbstractTask</code> ensures the propagation of start and
         * finish events in its run method.)
         * @param l - an object which implements <code>TaskListener</code>.
         */
        public void addTaskListener(TaskListener l) {
            synchronized (mListeners) {
                if (!mListeners.contains(l)) {
                    mListeners.add(l);
                }
            }
        }

        /**
         * Remove a registered listener.  Normally called after the AbstractTask is
         * finished.
         * @param l - a <code>TaskListener</code> previously added via
         * <code>addTaskListener(l)</code>.
         */
        public void removeTaskListener(TaskListener l) {
            synchronized (mListeners) {
                mListeners.remove(l);
            }
        }

        /**
         * Set the begining and ending progress endpoints.  This method should be
         * called before starting the AbstractTask.  If not called, the endpoints default
         * to 0.0 and 1.0.
         * @param begin - the beginning progress.
         * @param end - the ending progress.
         */
        public void setProgressEndpoints(double begin, double end) {        
            if (begin < 0.0 || end < 0.0 || (begin > end)) {
                throw new IllegalArgumentException(
                        "invalid progress endpoints (begin == " + begin
                        + ", end == " + end + ")");
            }
            if (mHasBegun) {
                throw new IllegalStateException(
                        "endpoints must be set before running the AbstractTask"
                );
            } else {
                mBeginProgress = begin;
                mEndProgress = end;
            }
        }

        /**
         * Get the begining progress.
         * @return double
         */
        protected double getBeginProgress() {
            return mBeginProgress;
        }

        /**
         * Get the ending progress.
         * @return double
         */
        protected double getEndProgress() {
            return mEndProgress;
        }
        
        /**
         * Returns the result of the computation, waiting if necessary until the
         * computation is finished.
         * 
         * @return the completed result, if the computation completed successfully.
         *
         * @throws CancellationException - if the computation was cancelled.
         * @throws ExecutionException - the the computation encountered an exception.
         * @throws InterruptedException - if the current thread was interrupted 
         *   while waiting.
         */
        public V get() throws InterruptedException, ExecutionException {
            while(!mHasEnded) {
                synchronized (this) {
                   wait();
                }
            }
            return getResultAfterHasEnded();
        }
        
        /**
         * Returns the result of the computation, waiting if necessary for at most
         * the specified amount of time for the computation to finish.
         * 
         * @param timeout maximum time to wait
         * @param unit the unit for the timeout parameter
         * 
         * @return the completed result, if the computation completed successfully.
         *
         * @throws CancellationException - if the computation was cancelled.
         * @throws ExecutionException - the the computation encountered an exception.
         * @throws InterruptedException - if the current thread was interrupted 
         *   while waiting.
         */
        public V get(long timeout, TimeUnit unit) 
            throws InterruptedException, ExecutionException, TimeoutException {
            long timeLimit = System.currentTimeMillis() + unit.toMillis(timeout);
            while(!mHasEnded) {
                long timeToWait = timeLimit - System.currentTimeMillis();
                if (timeToWait > 0) {
                    synchronized (this) {
                        wait(timeToWait);
                    }
                } else if (!mHasEnded) {
                    throw new TimeoutException();
                }
            }
            return getResultAfterHasEnded();
        }
        
        // Called by the get methods after their wait has been 
        // terminated.
        private V getResultAfterHasEnded() throws ExecutionException {
            if (mOutcome == TaskOutcome.SUCCESS) {
            	// Everything is ok, so just return the goods.
                return mResult;
            } 
            if (mOutcome == TaskOutcome.CANCELLED) {
            	// Was cancelled.
                throw new CancellationException();
            }
            if (mOutcome == TaskOutcome.ERROR) {
            	// Some sort of exception was encountered during the computation.
                throw new ExecutionException(mError);
            }
            return mResult;
        }

        /** Returns true if this task has been paused. 
         */
        public boolean isPaused() {
        	return mPauseFlag;
        }
        
        /**
         * Pause this task if it is running.
         */
        public synchronized void pause() {
        	if (!mPauseFlag && this.isBegun() && !this.isEnded()) {
        		mPauseFlag = true;
        		notifyAll();
        	}
        }
        
        /**
         * Resumes this task if it is paused.
         */
        public synchronized void play() {
        	if (mPauseFlag) {
        		mPauseFlag = false;
        		notifyAll();
        	}
        }
        
        /**
         * Resets the task object, so it can be used again.  Never call this method 
         * on a task that has started but not finished.
         * 
         * @throws IllegalStateException - if the task is currently ongoing.
         */
        public void reset() {
            if (mHasBegun) {
                if (mHasEnded) {
                    mOutcome = TaskOutcome.NOT_FINISHED;
                    mError = null;
                    mErrorMsg = null;
                    mCancelFlag = false;
                    mHasBegun = mHasEnded = false;
                    mResult = null;
                } else {
                    throw new IllegalStateException("cannot reset while running");
                }
            }
        }
        
        /**
         * Ensures posting of beginning and ending events to registered listeners
         * and calls <code>doTask()</code>, which must be defined by the
         * subclass.
         */
        public final void run() {
            
        	// Set mOwner to the current thread.  If another thread has already
            // gained the ownership, this will throw a RejectedExecutionException.
            obtainOwnership();
            
            try {
                
            	// Reset in case the task is being reused.
                mError = null;
                
                // Post an initial TaskEvent and set mHasBegun to true.
                postBegun();
                mHasBegun = true;
                
                // Perform the work of the task by calling the 
                // subclass' doTask method.
                //
                mResult = doTask();
                
                // In case error() was called off from off the owning thread,
                // possibly by a subtask thread.  (If called on the owning thread,
                // say directly from code in doTask(), error() tosses a RuntimeException.)
                //
                if (mError != null) {
                    mOutcome = TaskOutcome.ERROR;
                    mErrorMsg = mError.getMessage();
                    if (mErrorMsg == null) {
                        mErrorMsg = mError.toString();
                    }
                }
                
            } catch (CancellationException ce) {
                
            	// Not an error -- this event is the result of cancellation.
                mOutcome = TaskOutcome.CANCELLED;
            
            } catch (InterruptedException ie) { 
                
            	// Methods such as sleep(), wait(), BlockingQueue.put(), BlockingQueue.take()
                // throw InterruptedExceptions if the execution thread is interrupted.  If
                // the cancel flag is true, then the task was probably canceled while blocking
                // in one of these methods.    
                if (isCancelled()) {
                    mOutcome = TaskOutcome.CANCELLED;
                }
                if (mOwner != null) {
                    // Since the methods that throw the InterruptedException clear the
                    // interrupt status, reset it so code farther up the call stack will
                    // see that the thread was interrupted.
                    mOwner.interrupt();
                }
            
            } catch (Throwable t) {
            
            	if (!(t instanceof TaskErrorException)) {
            		
            		t.printStackTrace();
            		
            		// Stack traces for exceptions not thrown deliberately by
            		// error are quite useful for debugging.
                    LOGGER.error("Exception in task " + taskName(), t);
            	}
            	
            	mError = t;
 
            	// Every exception or error other than a CancellationException
                // is regarded as an error.
                mErrorMsg = t.getMessage();
                
                if (mErrorMsg == null) {
                    mErrorMsg = t.toString();
                }
                
                mOutcome = TaskOutcome.ERROR;
            
            } finally {
            
            	if (mOutcome == TaskOutcome.NOT_FINISHED) {
                    mOutcome = TaskOutcome.SUCCESS;
                }
                // Post the final event.
                postEnded();
                
                // Set mOwner back to null.
                releaseOwnership();            
            }
        }

        // Logically, only one thread should be executing the task at a time.  
        // If multiple threads were, they'd trample the data unless we encapsulated
        // everything in ThreadLocals.  Would this be worth the trouble? Nah.
        //
        private synchronized void obtainOwnership() {
            if (mOwner != null) {
                throw new RejectedExecutionException();
            }
            mOwner = Thread.currentThread();
        }
        
        // Release a thread's ownership after finishing up a run.
        //
        private synchronized void releaseOwnership() {
            if (Thread.currentThread() == mOwner) {
                mOwner = null;
            }
        }
        
        /**
         * Cancel the AbstractTask.
         */
        public boolean cancel(boolean mayInterruptIfRunning) {
            if (!isBegun() || mayInterruptIfRunning) {
                if (!(mCancelFlag || isEnded())) {
                    mCancelFlag = true;
                    // Break from the hang in checkForCancel() if
                    // paused.
                    if (mPauseFlag) {
                    	synchronized (this) {
                    		notifyAll();
                    	}
                    }
                    if (mPM != null) {
                        mPM.close();
                    }
                    if (mOwner != null) {
                        synchronized (this) {
                            if (mOwner != null) {
                                // The thread executing the task may
                                // be in a blocking operation of some kind
                                // which interrupt might unblock.
                                mOwner.interrupt();
                            }
                        }
                    }
                    return true;
                }
            }
            return false;
        }
        
        public boolean isCancelled() {
            return mCancelFlag;
        }
        
        
        public boolean isCanceled() {
        	return isCancelled();
        }

        /**
         * Same as isEnded().
         * 
         * @return true if the task was started and finished.
         *    How it finished is determined by call getTaskOutcome().
         */
        public boolean isDone() {
            return mHasEnded;
        }

        /**
         * Subclasses error out by calling this method.  It emits a <code>
         * RuntimeException</code> which is trapped in the final run method.  The run method's
         * catch takes care of setting the error message.
         * 
         * When subclasses call this method on the thread executing run(), the
         * controller thread, the controller thread is immediately interrupted by a 
         * run time exception which it catches and posts to TaskListeners via a 
         * TaskEvent with an error outcome.  But if this method
         * is called off of the controlling thread, perhaps by a subtask thread,
         * the controller is not immediately interrupted.  However, the controller still 
         * reports an error outcome at the termination of the task.
         * 
         * @param errorMsg String
         */
        protected void error(String errorMsg) {
            if (Thread.currentThread() == mOwner) {
                throw new TaskErrorException(errorMsg);
            } else {
                mError = new TaskErrorException(errorMsg);
            }
        }

        /**
         * Returns the error message associated with an outcome of TaskOutcome.ERROR.
         * Null is returned if the outcome is anything else or if the AbstractTask is not
         * finished.
         * @return String
         */
        public String getErrorMessage() {
            return mErrorMsg;
        }
        
        /**
         * Returns the throwable (error or exception) which aborted execution,
         * if such a condition terminated the run.
         * 
         * @return
         */
        public Throwable getError() {
        	return mError;
        }

        /**
         * Get the current progress.
         * @return double
         */
        public double getProgress() {
            return mProgress;
        }

        /**
         * Subclasses define this method to do their work.
         */
        protected abstract V doTask() throws Exception;

        /**
         * Returns true if the AbstractTask has begun, false otherwise.
         * @return boolean
         */
        public boolean isBegun() {
            return mHasBegun;
        }

        /**
         * Returns true if the AbstractTask has finished, false otherwise.
         * @return boolean
         */
        public boolean isEnded() {
            return isDone();
        }

        /**
         * Called after the final event to get the outcome of the AbstractTask.  Possible
         * return values are:
         * <ul>
         * <li>TaskOutcome.NOT_FINISHED
         * <li>TaskOutcome.CANCELED
         * <li>TaskOutcome.ERROR
         * <li>TaskOutcome.SUCCESS
         * </ul>
         * @return TaskOutcome
         */
        public final TaskOutcome getTaskOutcome() {
            return mOutcome;
        }

        // Posts the first TaskEvent to registered listeners by calling their
        // taskBegun() methods.
        private void postBegun() {
        	if (!mHasBegun) {
        		if (mListeners.size() > 0) {
        			mListeners.taskBegun(new TaskEvent(this, this.taskName() + " has begun"));
        		}
            }
        }

        // Posts a message to registered listeners by calling their taskMessage() methods,
        // but first checks status flags and for cancellation.
        protected void postMessage(String msg) {
            if (mHasBegun && !mHasEnded) {
                checkForCancel();
            }
            transmitMessage(msg);
        }
        
        // Post a message to listeners with no cancellation check.
        private void transmitMessage(String msg) {
            if (mListeners.size() > 0) {
            	mListeners.taskMessage(new TaskEvent(this, msg));
            }
            if (mPM != null) {
                mPM.setNote(msg);
            }
        }

        // Notifies registered listeners of the current progress by calling
        // their taskProgress() methods.
        protected void postProgress(double progress) {
            if (mHasBegun && !mHasEnded) {
                checkForCancel();
                mProgress = progress;
                if (mListeners.size() > 0) {
                	mListeners.taskProgress(new TaskEvent(this, null));
                }
                if (mPM != null) {
                    int min = mPM.getMinimum();
                    int max = mPM.getMaximum();
                    int p = min + (int)((max - min)*(mProgress - mBeginProgress)/(mEndProgress - mBeginProgress));
                    mPM.setProgress(p);
                }
            }
        }

        private void postEnded() {
            if (mHasBegun && !mHasEnded) {
                mHasEnded = true;
                synchronized (this) {
                    notifyAll();
                }
                if (mListeners.size() > 0) {
                	String msg = null;
                	if (mOutcome == TaskOutcome.SUCCESS) {
                		msg = this.taskName() + " has ended successfully";
                	} else if (mOutcome == TaskOutcome.CANCELLED) {
                		msg = this.taskName() + " has been canceled";
                	} else if (mOutcome == TaskOutcome.ERROR) {
                		msg = this.taskName() + " has ended with an error";
                	}
                	mListeners.taskEnded(new TaskEvent(this, msg));
                }
                if (mPM != null) {
                    mPM.close();
                }
            }
        }
        
        /**
         * Associate a ProgressMonitor with this AbstractTask.  If you click the cancel
         * button on the ProgressMonitor dialog, the dialog closes, and the
         * isCancel() method of the ProgressMonitor returns true.  Subsequent attempts
         * of the AbstractTask to post messages or progress check the cancellation status of
         * the monitor and also cancel the AbstractTask.  
         * @param pm
         */
        public void setProgressMonitor(ProgressMonitor pm) {
            mPM = pm;
        }

        protected void checkForCancel() {
        	if (!mCancelFlag && mPM != null) {
        		// Cancelled via a ProgressMonitor dialog
        		if (mPM.isCanceled()) {
        			cancel(true);
        		}
        	}
        	// Whenever cancelled, pop a CancellationException
        	if (mCancelFlag) {
        		throw new CancellationException();
        	}
        	// But if paused, hang here until play() is called.
        	if (mPauseFlag) {
        		// Post a pause message, but not through postMessage(), since
        		// it calls this method.
        		transmitMessage(this.taskName() + " has been paused");
        		// Hang in this loop until either play() or cancel() is called.
        		synchronized (this) {
        			while (mPauseFlag && !mCancelFlag) {
        				try {
        					wait(1000L);
        				} catch (InterruptedException ie) {
        				}
        			}
        		}
            	// Whenever cancelled, pop a CancellationException
            	if (mCancelFlag) {
            		throw new CancellationException();
            	}
        		// Post the resume message.
            	transmitMessage(this.taskName() + " has resumed");
        	}
        }
        
        /**
         * Thrown by the error() method of AbstractTask.  The subclassing is done
         * just so the catch in the run method can differient exceptions thrown by 
         * error from NullPointerExceptions and the like.
         * 
         * @author D3J923
         *
         */
        public static class TaskErrorException extends RuntimeException {
        	
        	public TaskErrorException(String message) {
        		super(message);
        	}
        	
        }
}
