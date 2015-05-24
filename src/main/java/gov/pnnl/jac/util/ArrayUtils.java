package gov.pnnl.jac.util;

import java.util.Arrays;
import java.util.Random;

public final class ArrayUtils {
    
    private ArrayUtils() {}
    
    /**
     * Returns the fraction of the elements of the given array that are zero in value.
     * @param values
     * @return
     */
    public static double sparseness(double[] values) {
    	double rtn = Double.NaN;
    	if (values.length > 0) {
    		int zeroCount = 0;
    		for (int i=0; i<values.length; i++) {
    			if (values[i] == 0.0) zeroCount++;
    		}
    		rtn = ((double) zeroCount)/values.length;
    	}
    	return rtn;
    }
    
    /**
     * Divides an integer into nearly equal-sized divisions.
     * The values in the returned array are guaranteed to sum to the total count.
     * 
     * @param totalCount
     * @param divisions
     * @return
     */
    public static int[] apportion(int totalCount, int divisions) {
    	int[] rtn = new int[divisions];
    	int perDivision = totalCount/divisions;
    	Arrays.fill(rtn, perDivision);
    	int leftOver = totalCount - (divisions * perDivision);
    	int n = 0;
    	while(leftOver > 0) {
    		rtn[n]++;
    		leftOver--;
    		n++;
    		if (n == divisions) {
    			n = 0;
    		}
    	}
    	return rtn;
    }
    
    public static void shuffle(int[] a, Random r) {
        final int len = a.length;
        for (int i=len; i>1; i--) {
            int j = r.nextInt(i);
            swap(a, j, i-1);
        }
    }
    
    public static void shuffle(int[] a) {
    	shuffle(a, MathUtils.RANDOM);
    }
    
    public static <T> void shuffle(T[] a, Random r) {
    	final int len = a.length;
    	for (int i=len; i>1; i--) {
    		int j = r.nextInt(i);
    		swap(a, j, i-1);
    	}
    }
    
    public static <T> void shuffle(T[] a) {
    	shuffle(a, MathUtils.RANDOM);
    }
    
    public static <T> T randomPick(final T[] a) {
    	return a[MathUtils.randomInt(0, a.length - 1)];
    }

    public static void reverseOrder(int[] a) {
    	final int len = a.length;
    	for (int i=0, j=len-1; j > i; j--, i++) {
    		swap(a, i, j);
    	}
    }

    public static <T> void reverseOrder(T[] a) {
    	final int len = a.length;
    	for (int i=0, j=len-1; j > i; j--, i++) {
    		swap(a, i, j);
    	}
    }
    
    public static void addToAll(int[] a, int amount) {
    	final int len = a.length;
    	for (int i=0; i<len; i++) {
    		a[i] += amount;
    	}
    }
    
    private static void swap(int[] a, int i, int j) {
        if (i == j) return;
        a[i] ^= a[j];
        a[j] ^= a[i];
        a[i] ^= a[j];
    }
    
    private static <T> void swap(T[] a, int i, int j) {
      if (i == j) return;
    	T temp = a[i];
    	a[i] = a[j];
    	a[j] = temp;
    }
}
