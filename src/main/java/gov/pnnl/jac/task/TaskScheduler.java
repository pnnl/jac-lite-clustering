package gov.pnnl.jac.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A utility class for scheduling tasks to be run in the background.
 * To run a task, use code similar to the following:
 * <code>
 *   // Assuming task is an implementation of Task<V> (usually an extension of AbstractTask<V>),
 *   // and that you've already added a task listener to task:
 *   TaskScheduler.schedule(task);
 * </code>
 * 
 * @author D3J923
 *
 */
public final class TaskScheduler {
		
	// A thread pool for running the tasks.
	private static AbstractExecutorService mExecutor;
	
	// Settings for the thread pool.
	private static int mCorePoolSize = 0;
	private static int mMaxPoolSize = Integer.MAX_VALUE;
	private static long mKeepAliveTime = 60L;
	private static TimeUnit mKeepAliveTimeUnit = TimeUnit.SECONDS;
	
	// The currently-running tasks.  Keys and values are the same.  Used as a set
	// even though it's a map, since IdentityHashSets do not exist.
	private final static Map<Task<?>, Task<?>> mRunningTasks = 
                new IdentityHashMap<Task<?>, Task<?>> ();
	
	private static TaskListener mListener = new TaskListener.Adapter() {
                @Override
		public void taskBegun(TaskEvent e) {
			synchronized (mRunningTasks) {
				mRunningTasks.put(e.getTask(), e.getTask());
			}
		}
                @Override
		public void taskEnded(TaskEvent e) {
			synchronized (mRunningTasks) {
				mRunningTasks.remove(e.getTask());
				e.getTask().removeTaskListener(this);
			}
		}
	};
	
	// Not meant to be instantiated.
	private TaskScheduler() {}
	
	public static boolean multithreadingEnabled() {
		return Math.max(mCorePoolSize, mMaxPoolSize) > 1;
	}
	
    /**
     * Sets the core number of threads for the internal thread pool.
     *
     * @param corePoolSize the core size
     * @throws IllegalArgumentException if <tt>corePoolSize</tt>
     * less than zero
     * @see #getCorePoolSize
     */
	public synchronized static void setCorePoolSize(int corePoolSize) {
		if (corePoolSize < 0) {
			throw new IllegalArgumentException("corePoolSize cannot be negative");
		}
		mCorePoolSize = corePoolSize;
		mExecutor = null;
	}
	
    /**
     * Returns the core number of threads for the thread pool used to execute tasks.
     *
     * @return the core number of threads
     * @see #setCorePoolSize
     */
	public synchronized int getCorePoolSize() {
		return mCorePoolSize;
	}
	
    /**
     * Sets the maximum number of threads allowed to exist in the thread pool. 
     *
     * @param maximumPoolSize the new maximum
     * 
     * @throws IllegalArgumentException if the new maximum is
     *         less than or equal to zero, or
     *         less than the {@linkplain #getCorePoolSize core pool size}
     * @see #getMaximumPoolSize
     */
	public synchronized static void setMaximumPoolSize(int maxPoolSize) {
		if (maxPoolSize <= 0) {
			throw new IllegalArgumentException("maxPoolSize must be positive");
		}
		mMaxPoolSize = maxPoolSize;
		mExecutor = null;
	}
	
    /**
     * Returns the maximum allowed number of threads in the thread pool that executes
     * tasks.
     *
     * @return the maximum allowed number of threads
     * @see #setMaximumPoolSize
     */
	public synchronized int getMaximumPoolSize() {
		return mMaxPoolSize;
	}
	
    /**
     * Sets the time limit for which threads in the thread pool may remain idle before
     * being terminated.  This is only valid if the thread pool is set up as a
     * cached thread pool, with the core pool size less than the maximum pool size.
     * 
     * @param time the time to wait.  A time value of zero will cause
     *   excess threads to terminate immediately after executing tasks.
     * @param unit the time unit of the time argument
     * @throws IllegalArgumentException if time less than zero or
     *   if time is zero and allowsCoreThreadTimeOut
     *   
     * @see #getKeepAliveTime
     */
	public synchronized static void setKeepAliveTime(long keepAliveTime, TimeUnit timeUnit) {
		if (timeUnit == null) {
			throw new NullPointerException();
		}
		mKeepAliveTime = keepAliveTime;
		mKeepAliveTimeUnit = timeUnit;
		mExecutor = null;
	}
	
    /**
     * Returns the keep-alive time for the threads in the thread pool.
     * This is the amount of time that threads in excess of the core pool size may remain
     * idle before being terminated.    
     *
     * @param unit the desired time unit of the result
     * @return the time limit
     * 
     * @see #setKeepAliveTime
     */
	public synchronized static long getKeepAliveTime(TimeUnit timeUnit) {
        long keepAliveNS = mKeepAliveTimeUnit.toNanos(mKeepAliveTime);
		return timeUnit.convert(keepAliveNS, TimeUnit.NANOSECONDS);
	}
	
	/**
	 * Configure the internal thread pool to have a fixed number of threads
	 * which do not time out.
	 * 
	 * @param numThreads the number of threads to exist in the pool.
	 * 
	 * @throws IllegalArgumentException if numThreads is less than or equal to zero.
	 */
	public synchronized static void configureFixedThreadPool(int numThreads) {
		if (numThreads <= 0) {
			throw new IllegalArgumentException("number of threads must be positive");
		}
		mCorePoolSize = mMaxPoolSize = numThreads;
		mKeepAliveTime = 0L;
		mKeepAliveTimeUnit = TimeUnit.MILLISECONDS;
		mExecutor = null;
	}
	
	/**
	 * Configure the internal thread pool to be a cached thread pool.  Generally,
	 * the core pool size is less than the maximum pool size.  Threads in 
	 * excess of the core pool size then die when idle longer than the keep alive 
	 * limit.
	 * 
     * @param corePoolSize the number of threads to keep in the
     * pool, even if they are idle.
     * @param maxPoolSize the maximum number of threads to allow in the
     * pool.
     * @param keepAliveTime when the number of threads is greater than
     * the core, this is the maximum time that excess idle threads
     * will wait for new tasks before terminating.
     * @param timeUnit the time unit for the keepAliveTime
     * argument.
     * 
     * @throws IllegalArgumentException if corePoolSize is negative or
     *   if maxPoolSize is less than corePoolSize.
	 */
	public synchronized static void configureCachedThreadPool(
			int corePoolSize,
			int maxPoolSize,
			long keepAliveTime,
			TimeUnit timeUnit) {
		if (maxPoolSize < corePoolSize) {
			throw new IllegalArgumentException("maxPoolSize < corePoolSize");
		}
		setCorePoolSize(corePoolSize);
		setMaximumPoolSize(maxPoolSize);
		setKeepAliveTime(keepAliveTime, timeUnit);
		mExecutor = null;
	}
	
	/**
	 * Shuts down the internal thread pool.  Tasks currently in its queue continue to
	 * be executed, but no new tasks may be submitted.
	 */
	public synchronized static void shutdown() {
		if (mExecutor instanceof ThreadPoolExecutor) {
			((ThreadPoolExecutor) mExecutor).shutdown();
		}
		mExecutor = null;
	}
	
	/**
     * Attempts to stop all actively executing tasks, halts the
     * processing of waiting tasks, and returns a list of the tasks
     * that were awaiting execution. These tasks are drained (removed)
     * from the task queue upon return from this method.
     *
     * <p>There are no guarantees beyond best-effort attempts to stop
     * processing actively executing tasks.  Currently-running tasks are
     * cancelled and interrupt is called on the threads running them.</p>
     *
     * @return list of tasks that never commenced execution
     * 
     * @throws SecurityException if a security manager exists and
     * shutting down the thread pooly may manipulate threads that
     * the caller is not permitted to modify because it does not hold
     * {@link java.lang.RuntimePermission}<tt>("modifyThread")</tt>,
     * or the security manager's <tt>checkAccess</tt> method denies access.
	 */
	public static List<Task<?>> shutdownNow() {
		
		// Cancel all currently-running tasks before calling
		// shutdownNow() on the thread pool.
		Task<?>[] running = null;
		synchronized (mRunningTasks) {
			Set<Task<?>> tset = mRunningTasks.keySet();
			if (tset.size() > 0) {
				running = tset.toArray(new Task<?>[tset.size()]);
			}
		}
		if (running != null) {
			for (Task<?> t: running) {
				if (!t.isDone()) {
					t.cancel(true);
				}
			}
		}
		
		List<Runnable> list = null;
		if (mExecutor instanceof ThreadPoolExecutor) {
			// Still will call interrupt on the threads still running.
			list = ((ThreadPoolExecutor) mExecutor).shutdownNow();
		}
		
		// Copy the list of runnables to a list of tasks.
		final int sz = list != null ? list.size() : 0;
		List<Task<?>> tlist = new ArrayList<Task<?>> (sz);
		if (sz > 0) {
			for (Runnable r: list) {
				tlist.add((Task<?>) r);
			}
		}
		
		return tlist;
	}	
	
	/**
	 * Schedule a task for execution.  Tasks are executed in the order they are
	 * scheduled.
	 * 
	 * @param task
	 */
	public static void schedule(Task<?> task) {
		task.addTaskListener(mListener);
		getExecutor().execute(task);
	}
	
	public static <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) 
		throws InterruptedException {
		return getExecutor().invokeAll(tasks);
	}
	
	// Called by the monitor thread to get the thread pool executor.
	private static AbstractExecutorService getExecutor() {
		
		// Lazily instantiated.
		//
		if (mExecutor == null) {
			
			synchronized (TaskScheduler.class) {
		
				if (mExecutor == null) {
					
					BlockingQueue<Runnable> queue = null;
					long keepAliveTime = 0L;
					TimeUnit tu = TimeUnit.MILLISECONDS;
					
					// Fixed thread pool.
					if (mCorePoolSize == mMaxPoolSize) {
						// The code in Executors uses a LinkedBlockingQueue for a
						// fixed thread pool and a SynchronouseQueue for a cached 
						// thread pool.  
						queue = new LinkedBlockingQueue<Runnable>();
					} else { // Cached thread pool.
						queue = new SynchronousQueue<Runnable>();
						// The keep alive time is only relevant when
						// corePoolSize < maxPoolSize.  If they're equal,
						// leave the keepAliveTime == 0, so the threads never
						// time out.
						keepAliveTime = mKeepAliveTime;
						tu = mKeepAliveTimeUnit;
					}
					
					mExecutor = new ThreadPoolExecutor(
							mCorePoolSize, Math.max(mCorePoolSize, mMaxPoolSize),
							keepAliveTime, tu,
							queue, new TaskSchedulerThreadFactory());
					
				}
			}
		}
		
		return mExecutor;
	}
	
    /**
     * This is used in place of a DefaultThreadFactory so the threads in the pool
     * are daemons.
     */
    static class TaskSchedulerThreadFactory implements ThreadFactory {
    	
        static final AtomicInteger poolNumber = new AtomicInteger(1);
        final ThreadGroup group;
        final AtomicInteger threadNumber = new AtomicInteger(1);
        final String namePrefix;

        TaskSchedulerThreadFactory() {
            SecurityManager s = System.getSecurityManager();
            group = (s != null)? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            namePrefix = "taskscheduler_pool-" +
                          poolNumber.getAndIncrement() +
                         "-thread-";
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                                  namePrefix + threadNumber.getAndIncrement(),
                                  0);
            t.setDaemon(true);
            if (t.getPriority() != Thread.NORM_PRIORITY) {
            	t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }

}
