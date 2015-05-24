package gov.pnnl.jac.util;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Benchmarking class for tallying the amount of time spent in various methods.
 * You should call <tt>startMethodTimer()</tt> near the beginning of the method
 * and call <tt>endMethodTimer()</tt> before the method returns, ideally in a
 * finally clause, so that it is certain to execute.
 * 
 * @author D3J923
 *
 */
public final class MethodTimer {

    // Timing maps keyed by thread, so printStats() can output them all.
    private static Map<Thread, Map<Marker, long[]>> sMapsByThread = 
        new IdentityHashMap<Thread, Map<Marker, long[]>> ();
    
    // So every thread has a unique timing map.
    private static final ThreadLocal<Map<Marker, long[]>> sTimingMaps = 
        new ThreadLocal<Map<Marker, long[]>> () {
        @Override protected Map<Marker, long[]> initialValue() {
            Map<Marker, long[]> timingMap = new HashMap<Marker, long[]> ();
            // Also place it here, so printStats() can print info for
            // all threads instead of just the calling thread.
            sMapsByThread.put(Thread.currentThread(), timingMap);
            return timingMap;
        }
    };
    
    // To keep track of places in the code from which startMethodTimer() has
    // been called with null before, so you can't get away with it more than
    // once from the same thread.
    private static final ThreadLocal<Set<StackTraceElement>> mCalledFromWithNullPreviously = 
    		new ThreadLocal<Set<StackTraceElement>> () {
    	@Override
    	protected Set<StackTraceElement> initialValue() {
    		return new HashSet<StackTraceElement> ();
    	}
    };
    
    // Must enable in order to use the methods.
    private static boolean sEnabled;
    
    private MethodTimer() {}
    
    /**
     * Enable the methods.  There is no disable method.
     */
    public static void enable() {
        sEnabled = true;
    }
        
    /**
     * Start timing the calling method for the calling thread.
     */
    public static Marker startMethodTimer(Marker from, String message) {
        if (sEnabled) {
            Map<Marker, long[]> timingMap = sTimingMaps.get();
            if (from == null) {
                from = callingFrom(message);
                Set<StackTraceElement> calledWithNullPrev = mCalledFromWithNullPreviously.get();
                if (calledWithNullPrev.contains(from.getStack())) {
                	throw new NullPointerException("you can only call this method from any given place with null once!");
                } else {
                	calledWithNullPrev.add(from.getStack());
                }
            }
            long[] timeHolder = timingMap.get(from);
            // The method is not being timed and has never been timed before.
            if (timeHolder == null) {
                // 0th element is the start time, 1th element is the
                // countdown, 2nd element is the accumulated time.
                timeHolder = new long[] { System.nanoTime(), 1L, 0L };
                timingMap.put(from, timeHolder);
            } else {
                // If starting when the countdown is 0, reinitialize the
                // start time.
                if (timeHolder[1] == 0) {
                    timeHolder[0] = System.nanoTime();
                }
                // Increment the countdown.
                timeHolder[1]++;
            }
        }
        return from;
    }
    
    /**
     * Ends the current timing of the method and increments the
     * accumulated time.
     */
    public static void endMethodTimer(Marker startedFrom) {
        if (sEnabled && startedFrom != null) {
            long currentTime = System.nanoTime();
            Map<Marker, long[]> timingMap = sTimingMaps.get();
            long[] timeHolder = timingMap.get(startedFrom);
            if (timeHolder != null) {
                long countdown = timeHolder[1];
                if (countdown > 0L) {
                    countdown--;
                    if (countdown == 0L) {
                        timeHolder[2] += currentTime - timeHolder[0];
                    }
                    timeHolder[1] = countdown;
                }
            }
        }
    }
    
    public static void printStats(PrintStream out) {
        
        if (sEnabled) {
            Iterator<Thread> threadIt = sMapsByThread.keySet().iterator();
            while(threadIt.hasNext()) {

                Thread t = threadIt.next();
                Map<Marker, long[]> timingMap = sMapsByThread.get(t);

                List<MapElement> elements = new ArrayList<MapElement> (timingMap.size());

                Iterator<Marker> it = timingMap.keySet().iterator();
                while(it.hasNext()) {
                    Marker from = it.next();
                    long[] timeHolder = timingMap.get(from);
                    elements.add(new MapElement(from, timeHolder));
                }

                Collections.sort(elements);

                out.println("\nTIMING DATA FOR THREAD: " + t);

                Iterator<MapElement> elementIt = elements.iterator();
                while(elementIt.hasNext()) {
                    out.print(elementIt.next().toString());
                }
            }
        }
    }
    
    public static Marker callingFrom(String message) {
        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        return new Marker(stackTrace[2], message);
    }
    
    private static class MapElement implements Comparable<MapElement> {

        public Marker mFrom;
        public long[] mTimeHolder;
        
        MapElement(Marker from, long[] timeHolder) {
            mFrom = from;
            mTimeHolder = timeHolder;
        }
        
        public int compareTo(MapElement o) {
            long tm1 = this.mTimeHolder[2];
            long tm2 = o.mTimeHolder[2];
            if (tm1 > tm2) {
                return -1;
            } else if (tm1 < tm2) {
                return +1;
            } else {
                return this.mFrom.toString().compareTo(o.mFrom.toString());
            }
        }
        
        public String toString() {
            long msec = Math.round(((double) mTimeHolder[2])/1000000.0);
            double sec = ((double) mTimeHolder[2])/(1000000000.0);
            return String.format("%d nanoseconds (%d msec, %f sec) from %s\n",
                    mTimeHolder[2], msec, sec, mFrom.toString());
        }
        
    }
    
    public static class Marker {
    	
    	private StackTraceElement mStack;
    	private String mMessage;
    	
    	private Marker (StackTraceElement stack, String message) {
    		mStack = stack;
    		mMessage = message;
    	}
    	
    	public StackTraceElement getStack() {
    		return mStack;
    	}
    	
    	public String getMessage() {
    		return mMessage;
    	}
    	
    	public String toString() {
    		String msg = mMessage != null && mMessage.length() > 0 ? mMessage : "null";
    		return mStack.toString() + "[" + msg + "]";
    	}
    }
}
