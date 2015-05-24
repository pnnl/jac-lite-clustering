package gov.pnnl.jac.task;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * ConcurrentTask -- when a sequence of smaller Tasks comprises the overall Task
 * and they may all be performed simultaneously.
 * 
 * <p>
 * Copyright: Copyright (c) 2005
 * </p>
 * 
 * <p>
 * Company: Battelle Memorial Institute
 * </p>
 * 
 * @author R. Scarberry
 * @version 1.0
 */
public class ConcurrentTask extends CompoundTask {

	// Priority of the threads which launch the subtasks.
	protected int mThreadPriority;
	// Maximum number of subtasks to be concurrently run.
	protected int mMaxThreadCount;

	// Valid only during execution of doTask.
	private CyclicBarrier mBarrier;
	private Executor mExecutor;
	private boolean mWaiting;

	/**
	 * Constructor which takes both the subtasks and their progress fractions.
	 * See the remarks for <code>setSubtasks</code>.
	 * 
	 * @param taskName
	 *            - the name to be returned by taskName().
	 * @param subtasks
	 *            Task[]
	 * @param subtaskFractions
	 *            double[]
	 */
	public ConcurrentTask(String taskName, Task<?>[] subtasks,
			double[] subtaskFractions, int threadPriority, int maxThreadCount) {
		super(taskName, subtasks, subtaskFractions);
		if (threadPriority < Thread.MIN_PRIORITY
				|| threadPriority > Thread.MAX_PRIORITY) {
			throw new IllegalArgumentException("illegal thread priority: "
					+ threadPriority);
		}
		mThreadPriority = threadPriority;
		if (maxThreadCount <= 0) {
			mMaxThreadCount = Runtime.getRuntime().availableProcessors();
		} else {
			mMaxThreadCount = maxThreadCount;
		}
	}

	/**
	 * Constructor which configures the subtasks, alotting equal shares of the
	 * overall progress spread to each.
	 * 
	 * @param taskName
	 *            - the name to be returned by taskName().
	 * @param subtasks
	 *            Task[]
	 */
	public ConcurrentTask(String taskName, Task<?>[] subtasks) {
		this(taskName, subtasks, null, Thread.NORM_PRIORITY, Runtime
				.getRuntime().availableProcessors());
	}

	/**
	 * Executes the subtasks in sequential order.
	 */
	protected final Object doTask() throws Exception {

		final int n = mSubtasks.length;

		try {

			if (n == 0) {
				error("no subtasks to execute");
			}

			for (int i = 0; i < n; i++) {
				mSubtasks[i].setProgressEndpoints(0.0, 1.0);
				mSubtasks[i].addTaskListener(this);
			}

			// No point in having more threads than subtasks.
			int maxThreads = Math.min(mMaxThreadCount, n);

			mExecutor = null;

			if (n == 1) {
				mBarrier = null;
				mExecutor = new Executor() {
					public void execute(Runnable runnable) {
						if (!Thread.interrupted()) {
							runnable.run();
						} else {
							throw new RejectedExecutionException();
						}
					}
				};
			} else {
				mWaiting = true;
				mBarrier = new CyclicBarrier(n, new Runnable() {
					public void run() {
						subtasksDone();
					}
				});
				mExecutor = Executors.newFixedThreadPool(maxThreads,
						new ConcurrentTaskThreadFactory(mThreadPriority));
			}

			for (int i = 0; i < n; i++) {
				mExecutor.execute(mSubtasks[i]);
			}

			if (mBarrier != null) {
				waitOnSubtasks();
			}

			// If cancel has been called, this should pop the exception.
			checkForCancel();

			boolean errorFlag = false;
			boolean cancelFlag = false;

			for (int i = 0; i < n; i++) {
				TaskOutcome outcome = mSubtasks[i].getTaskOutcome();
				if (outcome == TaskOutcome.ERROR
						|| outcome == TaskOutcome.NOT_FINISHED) {
					errorFlag = true;
					if (outcome == TaskOutcome.ERROR) {
						postMessage(mSubtaskPrefix + mSubtasks[i].getErrorMessage());
					} else {
						postMessage(mSubtaskPrefix + mSubtasks[i].taskName()
								+ " did not terminate properly");
					}
				} else if (outcome == TaskOutcome.CANCELLED) {
					// Somehow cancelled independently of this ConcurrentTask
					cancelFlag = true;
					postMessage(mSubtaskPrefix + mSubtasks[i].taskName()
							+ " canceled");
				}
			}

			if (cancelFlag) {
				this.cancel(true);
				// This should force the issue and cause a CancellationException.
				checkForCancel();
			}

			if (errorFlag) {
				error("failure of one or more subtasks");
			}

			return combineResults();
			
		} finally {

			if (mExecutor instanceof ThreadPoolExecutor) {
				ThreadPoolExecutor tpe = (ThreadPoolExecutor) mExecutor;
				if (!tpe.isShutdown()) {
					tpe.shutdownNow();
				}
			}
			mExecutor = null;
			mBarrier = null;

			for (int i=0; i < n; i++) {
				mSubtasks[i].removeTaskListener(this);
			}
		}
	}

	private synchronized void subtasksDone() {
		mWaiting = false;
		notify();
	}

	private synchronized void waitOnSubtasks() {
		while (mWaiting) {
			try {
				wait();
			} catch (InterruptedException ie) {
			}
		}
	}

	protected Object combineResults() {
		final int n = mSubtasks.length;
		Object[] results = new Object[n];
		for (int i = 0; i < n; i++) {
			try {
				results[i] = mSubtasks[i].get();
			} catch (Exception e) {
				// Should not happen, since all subtasks have finished
				// successfully.
			}
		}
		return results;
	}

	/**
	 * Cancel any tasks currently running.
	 */
	protected void cancelSubtasks() {
		if (mExecutor instanceof ThreadPoolExecutor) {
			((ThreadPoolExecutor) mExecutor).shutdownNow();
		}
		final int n = mSubtasks.length;
		for (int i = 0; i < n; i++) {
			Task subtask = mSubtasks[i];
			if (subtask.isBegun() && !subtask.isEnded()) {
				subtask.cancel(true);
			}
		}
	}

	// TaskListener methods -- received from subtasks; forwarded to
	// registered listeners of this ConcurrentTask.
	//

	// Forward begun events of subtasks as message events for the overall
	// task.
	public void taskBegun(TaskEvent e) {
		// Listeners have already received a taskBegun event for the
		// overall sequence. Convert the subtask taskBegun to a message event.
		postMessage(mSubtaskPrefix + e.getMessage());
	}

	public void taskMessage(TaskEvent e) {
		postMessage(mSubtaskPrefix + e.getMessage());
	}

	public void taskProgress(TaskEvent e) {
		double totalProgress = 0.0;
		final int n = mSubtasks.length;
		for (int i = 0; i < n; i++) {
			totalProgress += mSubtasks[i].getProgress() * mSubtaskFractions[i];
		}
		double p1 = this.getBeginProgress();
		double pdelt = this.getEndProgress() - p1;
		postProgress(p1 + totalProgress * pdelt);
	}

	public void taskEnded(TaskEvent e) {
		// Listeners will receive a taskEnded method for the overall sequence
		// when all the subtasks are done. Convert to a message event.
		postMessage(mSubtaskPrefix + e.getMessage());
		if (mBarrier != null) {
			try {
				mBarrier.await();
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (BrokenBarrierException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

	private static AtomicInteger mCurrentTaskNum = new AtomicInteger(1);

	class ConcurrentTaskThreadFactory implements ThreadFactory {

		final ThreadGroup mGroup;
		final AtomicInteger mThreadNum = new AtomicInteger(1);
		final String mNamePrefix;
		final int mThreadPriority;

		ConcurrentTaskThreadFactory(int threadPriority) {
			SecurityManager s = System.getSecurityManager();
			mGroup = (s != null) ? s.getThreadGroup() : Thread.currentThread()
					.getThreadGroup();
			mNamePrefix = taskName() + "-" + mCurrentTaskNum.getAndDecrement()
					+ "-pool-" + "-thread-";
			this.mThreadPriority = threadPriority;
		}

		public Thread newThread(Runnable r) {
			Thread t = new Thread(mGroup, r, mNamePrefix
					+ mThreadNum.getAndIncrement(), 0);
			if (t.isDaemon())
				t.setDaemon(false);
			if (t.getPriority() != mThreadPriority)
				t.setPriority(mThreadPriority);
			return t;
		}
	}
}
