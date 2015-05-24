package gov.pnnl.jac.util;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

/**
 * Contains static utility methods pertaining to memory.
 * 
 * @author D3J923
 *
 */
public final class MemUtils {

	// Use the initialization-on-demand holder idiom.  instance isn't set until the holder is accessed.
	private static class MXBeanHolder {
		private static final MemoryMXBean instance = ManagementFactory.getMemoryMXBean();
	}
	
	private MemUtils() {}
	
	/**
	 * Returns the maximum available heap memory in bytes.  The result takes into account
	 * the ability of the jvm to expand the heap.  
	 * 
	 * @return
	 */
	public static long maximumFreeHeap() {
		MemoryUsage heapUsage = getHeapMemoryUsage();
		return (heapUsage.getMax() - heapUsage.getUsed());
	}
	
	/**
	 * Returns the <tt>MemoryUsage</tt> object containing information about
	 * the current state of the heap.
	 * 
	 * @return
	 */
	public static MemoryUsage getHeapMemoryUsage() {
		return getMemoryMXBean().getHeapMemoryUsage();
	}
	
	public static MemoryMXBean getMemoryMXBean() {
		return MXBeanHolder.instance;
	}
}
