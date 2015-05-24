package gov.pnnl.jac.collections;

import java.util.Arrays;
import java.util.Random;

/**
 * <p>Contains various static utility methods for
 * use with arrays.</p>
 * 
 * @author R. Scarberry
 *
 */
public final class ArrayUtil {

	private ArrayUtil() {}
	
	/**
	 * Returns a copy of the specified section of the specified array.
	 * NOTE: <tt>Arrays.copyOfRange()</tt> in Java 1.6 performs a
	 * similar operation.
	 * 
	 * @param source - the array from which the section is to be copied.
	 * @param sourcePos - the initial index of the section to be copied.
	 * @param len - the number of elements to be copied.
	 * 
	 * @return - a new array containing the copied section.
	 * 
	 * @throws IllegalArgumentException - if copying triggers an out of 
	 *   bounds access of the source array.
	 * @throws NullPointerException - if source is null.
	 */
	public static int[] section(int[] source, int sourcePos, int len) {
	    return Arrays.copyOfRange(source, sourcePos, sourcePos + len);
	}

	/**
	 * Returns a copy of the specified section of the specified array.
	 * NOTE: <tt>Arrays.copyOfRange()</tt> in Java 1.6 performs a
	 * similar operation.
	 * 
	 * @param source - the array from which the section is to be copied.
	 * @param sourcePos - the initial index of the section to be copied.
	 * @param len - the number of elements to be copied.
	 * 
	 * @return - a new array containing the copied section.
	 * 
	 * @throws IllegalArgumentException - if copying triggers an out of 
	 *   bounds access of the source array.
	 * @throws NullPointerException - if source is null.
	 */
	public static double[] section(double[] source, int sourcePos, int len) {
        return Arrays.copyOfRange(source, sourcePos, sourcePos + len);
	}
	
	/**
	 * Returns a copy of the specified section of the specified array.
	 * NOTE: <tt>Arrays.copyOfRange()</tt> in Java 1.6 performs a
	 * similar operation.
	 * 
	 * @param source - the array from which the section is to be copied.
	 * @param sourcePos - the initial index of the section to be copied.
	 * @param len - the number of elements to be copied.
	 * 
	 * @return - a new array containing the copied section.
	 * 
	 * @throws IllegalArgumentException - if copying triggers an out of 
	 *   bounds access of the source array.
	 * @throws NullPointerException - if source is null.
	 */
	public static long[] section(long[] source, int sourcePos, int len) {
        return Arrays.copyOfRange(source, sourcePos, sourcePos + len);
	}
	
	/**
	 * Returns a copy of the specified section of the specified array.
	 * NOTE: <tt>Arrays.copyOfRange()</tt> in Java 1.6 performs a
	 * similar operation.
	 * 
	 * @param source - the array from which the section is to be copied.
	 * @param sourcePos - the initial index of the section to be copied.
	 * @param len - the number of elements to be copied.
	 * 
	 * @return - a new array containing the copied section.
	 * 
	 * @throws IllegalArgumentException - if copying triggers an out of 
	 *   bounds access of the source array.
	 * @throws NullPointerException - if source is null.
	 */
	public static float[] section(float[] source, int sourcePos, int len) {
        return Arrays.copyOfRange(source, sourcePos, sourcePos + len);
	}
	
	/**
	 * Returns a copy of the specified section of the specified array.
	 * NOTE: <tt>Arrays.copyOfRange()</tt> in Java 1.6 performs a
	 * similar operation.
	 * 
	 * @param source - the array from which the section is to be copied.
	 * @param sourcePos - the initial index of the section to be copied.
	 * @param len - the number of elements to be copied.
	 * 
	 * @return - a new array containing the copied section.
	 * 
	 * @throws IllegalArgumentException - if copying triggers an out of 
	 *   bounds access of the source array.
	 * @throws NullPointerException - if source is null.
	 */
	public static byte[] section(byte[] source, int sourcePos, int len) {
        return Arrays.copyOfRange(source, sourcePos, sourcePos + len);
	}

	/**
	 * Returns a copy of the specified section of the specified array.
	 * NOTE: <tt>Arrays.copyOfRange()</tt> in Java 1.6 performs a
	 * similar operation.
	 * 
	 * @param source - the array from which the section is to be copied.
	 * @param sourcePos - the initial index of the section to be copied.
	 * @param len - the number of elements to be copied.
	 * 
	 * @return - a new array containing the copied section.
	 * 
	 * @throws IllegalArgumentException - if copying triggers an out of 
	 *   bounds access of the source array.
	 * @throws NullPointerException - if source is null.
	 */
	public static short[] section(short[] source, int sourcePos, int len) {
        return Arrays.copyOfRange(source, sourcePos, sourcePos + len);
	}

	/**
	 * Returns a copy of the specified section of the specified array.
	 * NOTE: <tt>Arrays.copyOfRange()</tt> in Java 1.6 performs a
	 * similar operation.
	 * 
	 * @param source - the array from which the section is to be copied.
	 * @param sourcePos - the initial index of the section to be copied.
	 * @param len - the number of elements to be copied.
	 * 
	 * @return - a new array containing the copied section.
	 * 
	 * @throws IllegalArgumentException - if copying triggers an out of 
	 *   bounds access of the source array.
	 * @throws NullPointerException - if source is null.
	 */
	public static char[] section(char[] source, int sourcePos, int len) {
        return Arrays.copyOfRange(source, sourcePos, sourcePos + len);
	}
	
	/**
	 * Returns a copy of the specified section of the specified array.
	 * NOTE: <tt>Arrays.copyOfRange()</tt> in Java 1.6 performs a
	 * similar operation.
	 * 
	 * @param source - the array from which the section is to be copied.
	 * @param sourcePos - the initial index of the section to be copied.
	 * @param len - the number of elements to be copied.
	 * 
	 * @return - a new array containing the copied section.
	 * 
	 * @throws IllegalArgumentException - if copying triggers an out of 
	 *   bounds access of the source array.
	 * @throws NullPointerException - if source is null.
	 */
	public static <T> T[] section(T[] source, int sourcePos, int len) {
        return Arrays.copyOfRange(source, sourcePos, sourcePos + len);
	}
	
    /**
     * Returns a copy of the specified section of the specified array.
     * The resulting array is of the class <tt>newType</tt>.
     * NOTE: <tt>Arrays.copyOfRange()</tt> in Java 1.6 performs a
     * similar operation.
     *
     * @param source - the array from which the section is to be copied.
     * @param sourcePos - the initial index of the section to be copied.
     * @param len - the number of elements to be copied.
     * @param newType the class of the copy to be returned
     * 
     * @return - a new array containing the copied section.
     *
     * @throws ArrayIndexOutOfBoundsException if <tt>from &lt; 0</tt>
     *     or <tt>from &gt; source.length()</tt>
     * @throws IllegalArgumentException if <tt>from &gt; to</tt>
     * @throws NullPointerException if <tt>source</tt> is null
     * @throws ArrayStoreException if an element copied from
     *     <tt>source</tt> is not of a runtime type that can be stored in
     *     an array of class <tt>newType</tt>.
     */
    public static <T,U> T[] section(U[] source, int sourcePos, int len, 
            Class<? extends T[]> newType) {
        return Arrays.copyOfRange(source, sourcePos, sourcePos + len, newType);
    }
    
    /**
     * Create a copy of an array as into an array of a different type.
     * 
     * @param source the source array
     * @param newType class of the new array type
     *
     * @return
     */
    public static <T,U> T[] copy(U[] source, Class<? extends T[]> newType) {
        return Arrays.copyOfRange(source, 0, source.length, newType);
    }
    
	/**
	 * Shuffle all of the elements of the specified array using
	 * the specified source of randomness.
	 * 
	 * @param array
	 * @param rnd
	 */
	public static <T> void shuffle(T[] array, Random rnd) {
		int size = array.length;
		for (int i=size; i>1; i--) {
			int j = rnd.nextInt(i);
			T tmp = array[i-1];
			array[i-1] = array[j];
			array[j] = tmp;
		}
	}

	/**
	 * Shuffle all of the elements of the specified array using
	 * the specified source of randomness.
	 * 
	 * @param array
	 * @param rnd
	 */
	public static void shuffle(int[] array, Random rnd) {
	    int size = array.length;
	    for (int i=size; i>1; i--) {
	        int j = rnd.nextInt(i);
	        array[i-1] ^= array[j];
	        array[j] ^= array[i-1];
	        array[i-1] ^= array[j];
	    }
	}
}
