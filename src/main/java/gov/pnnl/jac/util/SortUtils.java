package gov.pnnl.jac.util;

import java.util.*;

/**
 * <p>Contains static sorting utilities not provided by the standard
 * Java JDK.</p>
 * 
 * @author R. Scarberry
 *
 */
public class SortUtils {

	private SortUtils() {
	}

	/**
	 * Returns the indices of the provided ComparableList ordered so that:
	 * 
	 * <code>
	 * int m = compList.memberCount();
	 * compList.compareMembers(n, n+1) <= 0 for n = (0, 1, 2, ..., m-2)
	 * </code>
	 *  
	 * @param compList
	 * @return
	 */
	public static int[] sortIndices(ComparableList compList) {
		int memberCount = compList.memberCount();
		// Init them to 0, 1, 2, 3 ..., (memberCount - 1)
		int[] indices = new int[memberCount];
		for (int i=0; i<memberCount; i++) {
			indices[i] = i;
		}
		// Now sort them so compList.compareMembers(n , n+1) <= 0, 
		// for n = 0, 1, 2, ... memberCount - 2.
		_sortIndices(indices, 0, memberCount, compList);
		return indices;
	}
	
	private static void _sortIndices(int[] indices, int offset, int len, 
			ComparableList compList) {
		
		// Insertion sort on smallest arrays
		if (len < 7) {
		    for (int i=offset; i<len+offset; i++) {
			  for (int j=i; j>offset && compList.compareMembers(j-1, j) > 0; j--) {
			    swap(indices, j, j-1);
			  }
		    }
		    return;
		}

		// Choose a partition element, v
		int m = offset + (len >> 1);       // Small arrays, middle element
		if (len > 7) {
		    int l = offset;
		    int n = offset + len - 1;
		    if (len > 40) {        // Big arrays, pseudomedian of 9
			int s = len/8;
			l = med3(indices, l,     l+s, l+2*s, compList);
			m = med3(indices, m-s,   m,   m+s, compList);
			n = med3(indices, n-2*s, n-s, n, compList);
		    }
		    m = med3(indices, l, m, n, compList); // Mid-size, med of 3
		}
		int v = indices[m];

		// Establish Invariant: v* (<v)* (>v)* v*
		int a = offset, b = a, c = offset + len - 1, d = c;
		while(true) {
			int cval = b <= c ? compList.compareMembers(indices[b], v) : 0;
		    while (b <= c && cval <= 0) {
		    	if (compList.compareMembers(indices[b], v) == 0) {
		    		swap(indices, a++, b);
		    	}
		    	b++;
		    	if (b <= c) {
		    		cval = compList.compareMembers(indices[b], v);
		    	}
		    }
		    cval = c >= b ? compList.compareMembers(indices[c], v) : 0;
		    while (c >= b && cval >= 0) {
		    	if (cval == 0) {
		    		swap(indices, c, d--);
		    	}
		    	c--;
		    	if (c >= b) {
		    		cval = compList.compareMembers(indices[c], v);
		    	}
		    }
		    
		    if (b <= c) {
		    	swap(indices, b++, c--);
		    } else {
		    	break;
		    }
		}

		// Swap partition elements back to middle
		int n = offset + len;
		int s = 0;
		
		s = Math.min(a-offset, b-a  );  vecswap(indices, offset, b-s, s);
		s = Math.min(d-c,   n-d-1);  vecswap(indices, b,   n-s, s);

		// Recursively sort non-partition-elements
		if ((s = b-a) > 1) {
		    _sortIndices(indices, offset, s, compList);
		}
		if ((s = d-c) > 1) {
		    _sortIndices(indices, n-s, s, compList);
		}
	}

    /**
     * Returns the index of the median of the three indexed integers.
     */
    private static int med3(int x[], int a, int b, int c, ComparableList compList) {
	return (compList.compareMembers(a, b) < 0 ?
		(compList.compareMembers(b, c) < 0 ? b : compList.compareMembers(a, c) < 0 ? c : a) :
		(compList.compareMembers(b, c) > 0 ? b : compList.compareMembers(a, c) > 0 ? c : a));
    }

	/**
	 * Sorts a list of Comparables and an array of ints preserving the 1:1
	 * correspondence between the entries. The sort is primary on the list
	 * values, secondary on the int values.
	 * 
	 * @param ovalues -
	 *            the list of objects implementing Comparable.
	 * @param ivalues -
	 *            the array of int values.
	 * @param from -
	 *            starting index.
	 * @param to -
	 *            index of first element not included in the sort.
	 * @param ascending -
	 *            specifies the sort order.
	 * @exception ArrayIndexOutOfBoundsException -
	 *                if the arrays are not the same length.
	 */
	public static <T extends Comparable<? super T>> void parallelSort(List<T> ovalues, int[] ivalues, int from,
			int to, boolean ascending) {
		ArrayList<T> ocopy = new ArrayList<T>(ovalues.size());
		ocopy.addAll(ovalues);
		int[] icopy = (int[]) ivalues.clone();
		mergeSort(ocopy, ovalues, icopy, ivalues, from, to, ascending);
	}
	
    public static <T> void parallelSort(List<T> ovalues, Comparator<? super T> c, int[] ivalues, int from,
            int to, boolean ascending) {
        ArrayList<T> ocopy = new ArrayList<T>(ovalues.size());
        ocopy.addAll(ovalues);
        int[] icopy = (int[]) ivalues.clone();
        mergeSort(ocopy, ovalues, c, icopy, ivalues, from, to, ascending);
    }

    /**
	 * Sorts a list of Comparables and an array of ints preserving the 1:1
	 * correspondence between the entries. The sort is in ascending order, 
	 * primary on the list values, secondary on the int values.
	 * 
	 * @param ovalues -
	 *            the list of objects implementing Comparable.
	 * @param ivalues -
	 *            the array of int values.
	 * @param from -
	 *            starting index.
	 * @param to -
	 *            index of first element not included in the sort.
	 * @exception ArrayIndexOutOfBoundsException -
	 *                if the arrays are not the same length.
	 */
	public static <T extends Comparable<? super T>> void parallelSort(List<T> ovalues, int[] ivalues, int from,
			int to) {
		parallelSort(ovalues, ivalues, from, to, true);
	}

	/**
	 * Sorts a list of Comparables and an array of ints preserving the 1:1
	 * correspondence between the entries. The sort is primary on the list
	 * values, secondary on the int values.
	 * 
	 * @param ovalues -
	 *            the list of objects implementing Comparable.
	 * @param ivalues -
	 *            the array of int values.
	 * @param ascending -
	 *            specifies the sort order.
	 * @exception ArrayIndexOutOfBoundsException -
	 *                if the arrays are not the same length.
	 */
	public static <T extends Comparable<? super T>> void parallelSort(List<T> ovalues, int[] ivalues,
			boolean ascending) {
		parallelSort(ovalues, ivalues, 0, ovalues.size(), ascending);
	}

	/**
	 * Sorts a list of Comparables and an array of ints preserving the 1:1
	 * correspondence between the entries. The sort is in ascending order, 
	 * primary on the list values, secondary on the int values.
	 * 
	 * @param ovalues -
	 *            the list of objects implementing Comparable.
	 * @param ivalues -
	 *            the array of int values.
	 * @exception ArrayIndexOutOfBoundsException -
	 *                if the arrays are not the same length.
	 */
	public static <T extends Comparable<? super T>> void parallelSort(List<T> ovalues, int[] ivalues) {
		parallelSort(ovalues, ivalues, true);
	}

	/**
	 * Sorts an array of doubles and an array of ints preserving the 1:1
	 * correspondence between the entries. The sort is primary on the double
	 * values, secondary on the int values.
	 * 
	 * @param dvalues -
	 *            the array of double values.
	 * @param ivalues -
	 *            the array of int values.
	 * @param from -
	 *            starting index.
	 * @param to -
	 *            index of first element not included in the sort.
	 * @param ascending -
	 *            specifies the sort order.
	 * @exception ArrayIndexOutOfBoundsException -
	 *                if the arrays are not the same length.
	 */
	public static void parallelSort(double[] dvalues, int[] ivalues, int from,
			int to, boolean ascending) {
		double[] dcopy = (double[]) dvalues.clone();
		int[] icopy = (int[]) ivalues.clone();
		mergeSort(dcopy, dvalues, icopy, ivalues, from, to, ascending);
	}

	/**
	 * Sorts an array of doubles and an array of ints in ascending order
	 * preserving the 1:1 correspondence between the entries. The sort is
	 * primary on the double values, secondary on the int values.
	 * 
	 * @param dvalues -
	 *            the array of double values.
	 * @param ivalues -
	 *            the array of int values.
	 * @param from -
	 *            starting index.
	 * @param to -
	 *            index of first element not included in the sort.
	 * @exception ArrayIndexOutOfBoundsException -
	 *                if the arrays are not the same length.
	 */
	public static void parallelSort(double[] dvalues, int[] ivalues, int from,
			int to) {
		parallelSort(dvalues, ivalues, from, to, true);
	}

	/**
	 * Sorts an array of doubles and an array of ints preserving the 1:1
	 * correspondence between the entries. The sort is primary on the double
	 * values, secondary on the int values.
	 * 
	 * @param dvalues -
	 *            the array of double values.
	 * @param ivalues -
	 *            the array of int values.
	 * @param ascending -
	 *            specifies the sort order.
	 * @exception ArrayIndexOutOfBoundsException -
	 *                if the arrays are not the same length.
	 */
	public static void parallelSort(double[] dvalues, int[] ivalues,
			boolean ascending) {
		parallelSort(dvalues, ivalues, 0, dvalues.length, ascending);
	}

	/**
	 * Sorts an array of doubles and an array of ints in ascending order
	 * preserving the 1:1 correspondence between the entries. The sort is
	 * primary on the double values, secondary on the int values.
	 * 
	 * @param dvalues -
	 *            the array of double values.
	 * @param ivalues -
	 *            the array of int values.
	 * @exception ArrayIndexOutOfBoundsException -
	 *                if the arrays are not the same length.
	 */
	public static void parallelSort(double[] dvalues, int[] ivalues) {
		parallelSort(dvalues, ivalues, true);
	}
/**
	 * Sorts an array of doubles and an array of ints preserving the 1:1
	 * correspondence between the entries. The sort is primary on the double
	 * values, secondary on the int values.
	 * 
	 * @param dvalues -
	 *            the array of double values.
	 * @param ivalues -
	 *            the array of int values.
	 * @param from -
	 *            starting index.
	 * @param to -
	 *            index of first element not included in the sort.
	 * @param ascending -
	 *            specifies the sort order.
	 * @exception ArrayIndexOutOfBoundsException -
	 *                if the arrays are not the same length.
	 */
	public static void parallelSort(float[] dvalues, int[] ivalues, int from,
			int to, boolean ascending) {
		float[] dcopy = (float[]) dvalues.clone();
		int[] icopy = (int[]) ivalues.clone();
		mergeSort(dcopy, dvalues, icopy, ivalues, from, to, ascending);
	}

	/**
	 * Sorts an array of doubles and an array of ints in ascending order
	 * preserving the 1:1 correspondence between the entries. The sort is
	 * primary on the double values, secondary on the int values.
	 * 
	 * @param dvalues -
	 *            the array of double values.
	 * @param ivalues -
	 *            the array of int values.
	 * @param from -
	 *            starting index.
	 * @param to -
	 *            index of first element not included in the sort.
	 * @exception ArrayIndexOutOfBoundsException -
	 *                if the arrays are not the same length.
	 */
	public static void parallelSort(float[] dvalues, int[] ivalues, int from,
			int to) {
		parallelSort(dvalues, ivalues, from, to, true);
	}

	/**
	 * Sorts an array of doubles and an array of ints preserving the 1:1
	 * correspondence between the entries. The sort is primary on the double
	 * values, secondary on the int values.
	 * 
	 * @param dvalues -
	 *            the array of double values.
	 * @param ivalues -
	 *            the array of int values.
	 * @param ascending -
	 *            specifies the sort order.
	 * @exception ArrayIndexOutOfBoundsException -
	 *                if the arrays are not the same length.
	 */
	public static void parallelSort(float[] dvalues, int[] ivalues,
			boolean ascending) {
		parallelSort(dvalues, ivalues, 0, dvalues.length, ascending);
	}

	/**
	 * Sorts an array of doubles and an array of ints in ascending order
	 * preserving the 1:1 correspondence between the entries. The sort is
	 * primary on the double values, secondary on the int values.
	 * 
	 * @param dvalues -
	 *            the array of double values.
	 * @param ivalues -
	 *            the array of int values.
	 * @exception ArrayIndexOutOfBoundsException -
	 *                if the arrays are not the same length.
	 */
	public static void parallelSort(float[] dvalues, int[] ivalues) {
		parallelSort(dvalues, ivalues, true);
	}

	/**
	 * Sorts two arrays of ints in ascending order
	 * preserving the 1:1 correspondence between the entries. The sort is
	 * primary on the first array, secondary on the second.
	 * 
	 * @param pvalues -
	 *            the array of values for the primary sort.
	 * @param ivalues -
	 *            the array of values for the secondary sort.
	 * @param from -
	 *            starting index.
	 * @param to -
	 *            index of first element not included in the sort.
	 * @param ascending -
	 *            specifies the sort order.
	 * @exception ArrayIndexOutOfBoundsException -
	 *                if the arrays are not the same length.
	 */
	public static void parallelSort(int[] pvalues, int[] svalues, int from,
			int to, boolean ascending) {
		int[] pcopy = (int[]) pvalues.clone();
		int[] scopy = (int[]) svalues.clone();
		mergeSort(pcopy, pvalues, scopy, svalues, from, to, ascending);
	}

	public static void parallelSort(int[] pvalues, double[] svalues, int from,
			int to, boolean ascending) {
		int[] pcopy = (int[]) pvalues.clone();
		double[] scopy = (double[]) svalues.clone();
		mergeSort(pcopy, pvalues, scopy, svalues, from, to, ascending);
	}

	public static void parallelSort(int[] pvalues, double[] svalues, boolean ascending) {
		parallelSort(pvalues, svalues, 0, pvalues.length, ascending);
	}

	public static void parallelSort(int[] pvalues, double[] svalues) {
		parallelSort(pvalues, svalues, 0, pvalues.length, true);
	}

    public static <E> void parallelSort(int[] pvalues, E[] svalues, boolean ascending) {
        parallelSort(pvalues, svalues, 0, pvalues.length, ascending);
    }

    public static <E >void parallelSort(int[] pvalues, E[] svalues) {
        parallelSort(pvalues, svalues, 0, pvalues.length, true);
    }

    public static <E> void parallelSort(int[] pvalues, E[] svalues, int from,
            int to, boolean ascending) {
        int[] pcopy = (int[]) pvalues.clone();
        E[] scopy = (E[]) svalues.clone();
        mergeSort(pcopy, pvalues, scopy, svalues, from, to, ascending);
    }

    /**
	 * Sorts two arrays of ints in ascending order
	 * preserving the 1:1 correspondence between the entries. The sort is
	 * primary on the first array, secondary on the second.
	 * 
	 * @param pvalues -
	 *            the array of values for the primary sort.
	 * @param ivalues -
	 *            the array of values for the secondary sort.
	 * @param from -
	 *            starting index.
	 * @param to -
	 *            index of first element not included in the sort.
	 * @exception ArrayIndexOutOfBoundsException -
	 *                if the arrays are not the same length.
	 */
	public static void parallelSort(int[] pvalues, int[] svalues, int from,
			int to) {
		parallelSort(pvalues, svalues, from, to, true);
	}

	/**
	 * Sorts two arrays of ints preserving the 1:1
	 * correspondence between the entries. The sort is primary on the first
	 * array, secondary on the second.
	 * 
	 * @param pvalues -
	 *            the array of values for the primary sort.
	 * @param ivalues -
	 *            the array of values for the secondary sort.
	 * @param ascending -
	 *            specifies the sort order.
	 * @exception ArrayIndexOutOfBoundsException -
	 *                if the arrays are not the same length.
	 */
	public static void parallelSort(int[] pvalues, int[] svalues,
			boolean ascending) {
		parallelSort(pvalues, svalues, 0, pvalues.length, ascending);
	}

	/**
	 * Sorts two arrays of ints in ascending order
	 * preserving the 1:1 correspondence between the entries. The sort is
	 * primary on the first array, secondary on the second.
	 * 
	 * @param pvalues -
	 *            the array of values for the primary sort.
	 * @param ivalues -
	 *            the array of values for the secondary sort.
	 * @exception ArrayIndexOutOfBoundsException -
	 *                if the arrays are not the same length.
	 */
	public static void parallelSort(int[] pvalues, int[] svalues) {
		parallelSort(pvalues, svalues, true);
	}

	/**
	 * Sorts the specified array of values using the supplied <code>IntComparator</code>
	 * for comparing the values.  This can be used for unconventional sorts.  For example,
	 * the values may be indexes into an array or list.  The comparator could then base 
	 * its comparison on the entities contained in the array or list at the given values 
	 * instead of on the values themselves.
	 * 
	 * @param values an array of ints.
	 * @param comp the comparator object.
	 */
	public static void sort(int[] values, IntComparator comp) {
		sort(values, 0, values.length, comp);
	}
	
	/**
	 * Sorts the specified array of values over the specified range using the 
	 * supplied <code>IntComparator</code>
	 * for comparing the values.  This can be used for unconventional sorts.  For example,
	 * the values may be indexes into an array or list.  The comparator could then base 
	 * its comparison on the entities contained in the array or list at the given values 
	 * instead of on the values themselves.
	 * 
	 * @param values an array of ints.
	 * @param fromIndex the starting index of the sort, inclusive.
	 * @param toIndex the ending index of the sort, exclusive.
	 * @param comp the comparator object.
	 */
	public static void sort(int[] values, int fromIndex, int toIndex, IntComparator comp) {
	    rangeCheck(values.length, fromIndex, toIndex);
		sort1(values, fromIndex, toIndex, comp);
	}
	
	/**
	 * Performs an optimized quicksort of the specified array, using the specified comparator.
	 * This method used the same algorithm as used by java.lang.Arrays for sorting arrays of
	 * primitive types.  The java.lang.Arrays sort methods for objects, use a merge sort, which
	 * is not quite as fast as quicksort.  Note that quicksort is NOT a stable sort, but merge sort is.
	 * 
	 * @param a
	 * @param c
	 */
    public static <T> void quickSort(T[] a, Comparator<? super T> c) {
    	quickSort(a, 0, a.length, c);
    }
    
    private static void parallelInsertionSort(int maxDepth, boolean ascending, int offset, int len, int[]... arrays) {
        
        final int limit = offset + len;
        final int numArrays = arrays.length;
        final int[] colBuffer = new int[numArrays];
        
        for (int i=offset + 1; i<limit; i++) {
            for (int k=0; k<numArrays; k++) {
                colBuffer[k] = arrays[k][i];
            }
            int j=i;
            int cmp = compare(j-1, maxDepth, colBuffer, arrays);
            if (!ascending) {
                cmp = -cmp;
            }
            while(j > offset && cmp > 0) {
                for (int k=0; k<numArrays; k++) {
                    int[] array = arrays[k];
                    array[j] = array[j-1];
                }
                j--;
                if (j > offset) {
                    cmp = compare(j-1, maxDepth, colBuffer, arrays);
                    if (!ascending) {
                        cmp = -cmp;
                    }
                }
            }        
            for (int k=0; k<numArrays; k++) {
                int[] array = arrays[k];
                array[j] = colBuffer[k];
            }
        }
        
        // Done!
        return;        
    }
    
    public static void parallelQuickSort(int maxDepth, boolean ascending, int[]... arrays) {
        if (maxDepth <= 0 || maxDepth > arrays.length) {
            throw new IllegalArgumentException(String.format("maxDepth not in [1 - %d]: %d", arrays.length, maxDepth));
        }
        
        final int numValues = arrays[0].length;
        final int numArrays = arrays.length;
        
        for (int i=1; i<numArrays; i++) {
            if (arrays[i].length != numValues) {
                throw new IllegalArgumentException("arrays of unequal length");
            }
        }
        
        parallelQS(maxDepth, ascending, 0, numValues, arrays);
        
    }
    
    private static void parallelQS(int maxDepth, boolean ascending, int offset, int len, int[]... arrays) {
     
        // Just use insertion sort for the small arrays.
        if (len < 7) {
            parallelInsertionSort(maxDepth, ascending, offset, len, arrays);
            return;
        }
        
        final int limit = offset + len;
        
        // Select a partition element index, m
        int m = offset + (len >> 1);
        
        int left = offset;
        int right = limit - 1;
        
        // For arrays > 40, use the pseudomedian of 9
        //
        if (len > 40) {
            int eighthLen = len/8;
            // Index of median value of the 3 on the left.
            left = medianOf3(arrays[0], left, left + eighthLen, left + 2*eighthLen);
            // In the middle
            m = medianOf3(arrays[0], m-eighthLen, m, m+eighthLen);
            // On the right
            right = medianOf3(arrays[0], right-2*eighthLen, right-eighthLen, right);
        }
        
        // Convert to index of the median of the 3.
        m = medianOf3(arrays[0], left, m, right);
        
        final int numArrays = arrays.length;
        
        final int[] partitionValues = new int[numArrays];
        for (int k=0; k<numArrays; k++) {
            partitionValues[k] = arrays[k][m];
        }
        
        int a = offset;
        int b = offset;
        int c = limit - 1;
        int d = c;

        while (true) {
            
            int cmpValue = 0;
            if (b <= c) {
                cmpValue = compare(b, maxDepth, partitionValues, arrays);
                if (!ascending) {
                    cmpValue = -cmpValue;
                }
            }
            
            while (b <= c && cmpValue <= 0) {
                if (cmpValue == 0) {
                    swap(a++, b, arrays);
                }
                b++;
                if (b <= c) {
                    cmpValue = compare(b, maxDepth, partitionValues, arrays);
                    if (!ascending) {
                        cmpValue = -cmpValue;
                    }
                }
            }
            
            cmpValue = 0;
            if (c >= b) {
                cmpValue = compare(c, maxDepth, partitionValues, arrays);
                if (!ascending) {
                    cmpValue = -cmpValue;
                }
            }
            
            while(c >= b && cmpValue >= 0) {
                if (cmpValue == 0) {
                    swap(c, d--, arrays);
                }
                c--;
                if (c >= b) {
                    cmpValue = compare(c, maxDepth, partitionValues, arrays);
                    if (!ascending) {
                        cmpValue = -cmpValue;
                    }
                }
            }
            
            if (b > c) {
                break;
            }
            
            swap(b++, c--,  arrays);
        }
        
        int s = Math.min(a - offset, b - a);
        vecSwap(offset, b-s, s, arrays);
        
        s = Math.min(d-c, limit-d-1);
        vecSwap(b, limit-s, s, arrays);
        
        s = b - a;
        if (s > 1) {
            parallelQS(maxDepth, ascending, offset, s, arrays);
        }
        
        s = d - c;
        if (s > 1) {
            parallelQS(maxDepth, ascending, limit - s, s, arrays);
        }
        
    }
    
    public static int medianOf3(int[] values, int a, int b, int c) {
        int va = values[a];
        int vb = values[b];
        int vc = values[c];
        return va < vb ? (vb < vc ? b : (vc < va ? a : c)) : (va < vc ? a : (vc < vb ? b : c));
    }
    
	/**
	 * Performs an optimized quicksort of the specified array, using the natural ordering of the object.
	 * (They should implement Comparable in order for this method to have any effect.)
	 * This method used the same algorithm as used by java.lang.Arrays for sorting arrays of
	 * primitive types.  The java.lang.Arrays sort methods for objects, use a merge sort, which
	 * is not quite as fast as quicksort.  Note that quicksort is NOT a stable sort, but merge sort is.
	 * 
	 * @param a
	 */
    public static <T> void quickSort(T[] a) {
    	quickSort(a, 0, a.length, null);
    }
    
    public static <T> void quickSort(T[] arr, int offset, int len, Comparator<? super T> cmp) {
    
    	final int limit = offset + len;
    	
    	// Use an insertion sort for really small arrays.
    	if (len < 7) {
    		for (int i=offset + 1; i<limit; i++) {
    			T v = arr[i];
    			int j=i;
    			while(j > offset && compare(arr[j-1], v, cmp) > 0) {
    				arr[j] = arr[j-1];
    				j--;
    			}
    			arr[j] = v;
    		}
    		// Done!
    		return;
    	}
    	
    	// Select a partition element index, m
    	int m = offset + (len >> 1);
    	
    	int left = offset;
    	int right = limit - 1;
    	
    	// For arrays > 40, use the pseudomedian of 9
    	//
    	if (len > 40) {
    		int eighthLen = len/8;
    		// Index of median value of the 3 on the left.
    		left = medianOf3(arr, left, left + eighthLen, left + 2*eighthLen, cmp);
    		// In the middle
    		m = medianOf3(arr, m-eighthLen, m, m+eighthLen, cmp);
    		// On the right
    		right = medianOf3(arr, right-2*eighthLen, right-eighthLen, right, cmp);
    	}
    	
    	// Convert to index of the median of the 3.
    	m = medianOf3(arr, left, m, right, cmp);
    	
    	T partitionValue = arr[m];
    	
    	int a = offset;
    	int b = offset;
    	int c = limit - 1;
    	int d = c;

    	while (true) {
    		
    		int cmpValue = b <= c ? compare(arr[b], partitionValue, cmp) : 0;
    		
    		while (b <= c && cmpValue <= 0) {
    			if (cmpValue == 0) {
    				swap(arr, a++, b);
    			}
    			b++;
    			if (b <= c) {
    				cmpValue = compare(arr[b], partitionValue, cmp);
    			}
    		}
    		
    		cmpValue = c >= b ? compare(arr[c], partitionValue, cmp) : 0;
    		
    		while(c >= b && cmpValue >= 0) {
    			if (cmpValue == 0) {
    				swap(arr, c, d--);
    			}
    			c--;
    			if (c >= b) {
    				cmpValue = compare(arr[c], partitionValue, cmp);
    			}
    		}
    		
    		if (b > c) {
    			break;
    		}
    		
    		swap(arr, b++, c--);
    	}
    	
    	int s = Math.min(a - offset, b - a);
    	vecSwap(arr, offset, b-s, s);
    	
    	s = Math.min(d-c, limit-d-1);
    	vecSwap(arr, b, limit-s, s);
    	
    	s = b - a;
    	if (s > 1) {
    		quickSort(arr, offset, s, cmp);
    	}
    	
    	s = d - c;
    	if (s > 1) {
    		quickSort(arr, limit - s, s, cmp);
    	}
    }
    
    /**
     * Given three indexes into the specified array, this method returns the index of the
     * middle element as determined by the supplied comparator.  If the comparator is null,
     * the middle index is determined by the <tt>compareTo</tt> methods of the objects, assuming
     * they implement comparable.
     * 
     * @param a
     * @param x
     * @param y
     * @param z
     * @param c
     * @return
     */
    public static <T> int medianOf3(T[] a, int x, int y, int z, Comparator<? super T> c) {
    	if (compare(a[x], a[y], c) < 0) {
    		// x < y
    		if (compare(a[y], a[z], c) < 0) {
    			// y < z, so x < y < z
    			// y is the middle
    			return y;
    		}
    		// x < y, but y >= z
    		if (compare(a[x], a[z], c) < 0) {
    			// x < z
    			// x < z <= y
    			return z;
    		}
    		// x >= z
    		// Has to be x.
    		return x;
    	}
    	// x >= y
    	if (compare(a[y], a[z], c) > 1) {
    		// y > z
    		return y;
    	}
    	// y <= z
    	if (compare(a[x], a[z], c) > 0) {
    		// x > z
    		return z;
    	}
    	return x;
    }
    
    private static <T> void swap(T[] arr, int i, int j) {
    	T tmp = arr[i];
    	arr[i] = arr[j];
    	arr[j] = tmp;
    }
    
    private static <T> void vecSwap(T[] arr, int offset1, int offset2, int len) {
    	for (int i=0; i<len; i++, offset1++, offset2++) {
    		swap(arr, offset1, offset2);
    	}
    }
    
    private static void vecSwap(int offset1, int offset2, int len, int[]... arrays) {
        final int numArrays = arrays.length;
        for (int i=0; i<len; i++, offset1++, offset2++) {
            for (int j=0; j<numArrays; j++) {
                swap(arrays[j], offset1, offset2);
            }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
	private static <T> int compare(T v1, T v2, Comparator<? super T> c) {
    	if (c != null) {
    		return c.compare(v1, v2);
    	}
    	if (v1 instanceof Comparable) {
    		return ((Comparable) v1).compareTo(v2);
    	}
    	return 0;
    }
	
    private static void rangeCheck(int arrayLength, int from, int to) {
        if (from > to) {
            throw new IllegalArgumentException("from(" + from +
                    ") > to(" + to+")");
        }
        if (from < 0) {
            throw new ArrayIndexOutOfBoundsException(from);
        }
        if (to > arrayLength) {
            throw new ArrayIndexOutOfBoundsException(to);
        }
    }
    
    /**
     * Sorts the specified sub-array of integers into ascending order using the
     * supplied comparator.
     */
    private static void sort1(int x[], int off, int len, IntComparator comp) {
    	
    	// Insertion sort on smallest arrays
    	if (len < 7) {
    		for (int i=off; i<len+off; i++)
    			for (int j=i; j>off && comp.compare(x[j-1], x[j]) > 0; j--)
    				swap(x, j, j-1);
    		return;
    	}

    	// Choose a partition element, v
    	int m = off + (len >> 1);       // Small arrays, middle element
    	if (len > 7) {
    		int l = off;
    		int n = off + len - 1;
    		if (len > 40) {        // Big arrays, pseudomedian of 9
    			int s = len/8;
    			l = med3(x, l,     l+s, l+2*s, comp);
    			m = med3(x, m-s,   m,   m+s, comp);
    			n = med3(x, n-2*s, n-s, n, comp);
    		}
    		m = med3(x, l, m, n, comp); // Mid-size, med of 3
    	}
    	
    	int v = x[m];

    	// Establish Invariant: v* (<v)* (>v)* v*
    	int a = off, b = a, c = off + len - 1, d = c;
    	while(true) {
    		while (b <= c && comp.compare(x[b], v) <= 0) {
    			if (comp.compare(x[b], v) == 0)
    				swap(x, a++, b);
    			b++;
    		}
    		while (c >= b && comp.compare(x[c], v) >= 0) {
    			if (comp.compare(x[c], v) == 0)
    				swap(x, c, d--);
    			c--;
    		}
    		if (b > c)
    			break;
    		swap(x, b++, c--);
    	}

    	// Swap partition elements back to middle
    	int s, n = off + len;
    	s = Math.min(a-off, b-a  );  vecswap(x, off, b-s, s);
    	s = Math.min(d-c,   n-d-1);  vecswap(x, b,   n-s, s);

    	// Recursively sort non-partition-elements
    	if ((s = b-a) > 1)
    		sort1(x, off, s, comp);
    	if ((s = d-c) > 1)
    		sort1(x, n-s, s, comp);
    }
	
    /**
     * Swaps x[a] with x[b].
     */
    private static void swap(int x[], int a, int b) {
    	int t = x[a];
    	x[a] = x[b];
    	x[b] = t;
    }

    /**
     * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
     */
    private static void vecswap(int x[], int a, int b, int n) {
    	for (int i=0; i<n; i++, a++, b++)
    		swap(x, a, b);
    }

    /**
     * Returns the index of the median of the three indexed integers.
     */
    private static int med3(int x[], int a, int b, int c, IntComparator comp) {
	return (comp.compare(x[a], x[b]) < 0 ?
		(comp.compare(x[b], x[c]) < 0 ? b : comp.compare(x[a], x[c]) < 0 ? c : a) :
		(comp.compare(x[b], x[c]) > 0 ? b : comp.compare(x[a], x[c]) > 0 ? c : a));
    }

    private static void mergeSort(double[] dsrc, double[] ddest, int[] isrc,
			int[] idest, int low, int high, boolean ascending) {
		int length = high - low;
		// Small arrays just use an insertion sort.
		if (length < 7) {
			for (int i = low; i < high; i++) {
				for (int j = i; j > low
						&& compare(ddest[j - 1], ddest[j], idest[j - 1],
								idest[j], ascending) > 0; j--) {
					swap(ddest, idest, j, j - 1);
				}
			}
			return;
		}
		// On larger arrays, recursively sort halves.
		int mid = (low + high) / 2;
		mergeSort(ddest, dsrc, idest, isrc, low, mid, ascending);
		mergeSort(ddest, dsrc, idest, isrc, mid, high, ascending);
		// If list is already sorted, just copy from srcs to desst. This
		// optimization results in faster sorts for nearly ordered lists.
		if (compare(dsrc[mid - 1], dsrc[mid], isrc[mid - 1], isrc[mid],
				ascending) <= 0) {
			System.arraycopy(dsrc, low, ddest, low, length);
			System.arraycopy(isrc, low, idest, low, length);
			return;
		}
		// Merge sorted halves (now in src) into dest
		for (int i = low, p = low, q = mid; i < high; i++) {
			if (q >= high
					|| p < mid
					&& compare(dsrc[p], dsrc[q], isrc[p], isrc[q], ascending) <= 0) {
				ddest[i] = dsrc[p];
				idest[i] = isrc[p++];
			} else {
				ddest[i] = dsrc[q];
				idest[i] = isrc[q++];
			}
		}
	}

	private static int compare(double d1, double d2, int n1, int n2,
			boolean ascending) {
		int rtn;
		if (Double.doubleToLongBits(d1) == Double.doubleToLongBits(d2)) {
			rtn = 0;
		} else {
			// NaNs are considered to be > than anything else.
			boolean d1NaN = Double.isNaN(d1);
			boolean d2NaN = Double.isNaN(d2);
			if (d1NaN && !d2NaN) {
				rtn = 1;
			} else if (d2NaN && !d1NaN) {
				rtn = -1;
			} else { // Neither can possibly be NaN.
				rtn = d1 < d2 ? -1 : (d1 > d2 ? 1 : 0);
			}
		}
		if (rtn == 0) {
			rtn = (n1 < n2 ? -1 : (n1 > n2 ? 1 : 0));
		}
		if (!ascending)
			rtn = -rtn;
		return rtn;
	}

	private static void swap(double x[], int[] n, int a, int b) {
		double t = x[a];
		x[a] = x[b];
		x[b] = t;
		int tn = n[a];
		n[a] = n[b];
		n[b] = tn;
	}

        private static void mergeSort(float[] dsrc, float[] ddest, int[] isrc,
			int[] idest, int low, int high, boolean ascending) {
		int length = high - low;
		// Small arrays just use an insertion sort.
		if (length < 7) {
			for (int i = low; i < high; i++) {
				for (int j = i; j > low
						&& compare(ddest[j - 1], ddest[j], idest[j - 1],
								idest[j], ascending) > 0; j--) {
					swap(ddest, idest, j, j - 1);
				}
			}
			return;
		}
		// On larger arrays, recursively sort halves.
		int mid = (low + high) / 2;
		mergeSort(ddest, dsrc, idest, isrc, low, mid, ascending);
		mergeSort(ddest, dsrc, idest, isrc, mid, high, ascending);
		// If list is already sorted, just copy from srcs to desst. This
		// optimization results in faster sorts for nearly ordered lists.
		if (compare(dsrc[mid - 1], dsrc[mid], isrc[mid - 1], isrc[mid],
				ascending) <= 0) {
			System.arraycopy(dsrc, low, ddest, low, length);
			System.arraycopy(isrc, low, idest, low, length);
			return;
		}
		// Merge sorted halves (now in src) into dest
		for (int i = low, p = low, q = mid; i < high; i++) {
			if (q >= high
					|| p < mid
					&& compare(dsrc[p], dsrc[q], isrc[p], isrc[q], ascending) <= 0) {
				ddest[i] = dsrc[p];
				idest[i] = isrc[p++];
			} else {
				ddest[i] = dsrc[q];
				idest[i] = isrc[q++];
			}
		}
	}

	private static int compare(float d1, float d2, int n1, int n2,
			boolean ascending) {
		int rtn;
		if (Double.doubleToLongBits(d1) == Double.doubleToLongBits(d2)) {
			rtn = 0;
		} else {
			// NaNs are considered to be > than anything else.
			boolean d1NaN = Double.isNaN(d1);
			boolean d2NaN = Double.isNaN(d2);
			if (d1NaN && !d2NaN) {
				rtn = 1;
			} else if (d2NaN && !d1NaN) {
				rtn = -1;
			} else { // Neither can possibly be NaN.
				rtn = d1 < d2 ? -1 : (d1 > d2 ? 1 : 0);
			}
		}
		if (rtn == 0) {
			rtn = (n1 < n2 ? -1 : (n1 > n2 ? 1 : 0));
		}
		if (!ascending)
			rtn = -rtn;
		return rtn;
	}

	private static void swap(float x[], int[] n, int a, int b) {
		float t = x[a];
		x[a] = x[b];
		x[b] = t;
		int tn = n[a];
		n[a] = n[b];
		n[b] = tn;
	}

    private static void mergeSort(int[] dsrc, int[] ddest, int[] isrc,
			int[] idest, int low, int high, boolean ascending) {
		int length = high - low;
		// Small arrays just use an insertion sort.
		if (length < 7) {
			for (int i = low; i < high; i++) {
				for (int j = i; j > low
						&& compare(ddest[j - 1], ddest[j], idest[j - 1],
								idest[j], ascending) > 0; j--) {
					swap(ddest, idest, j, j - 1);
				}
			}
			return;
		}
		// On larger arrays, recursively sort halves.
		int mid = (low + high) / 2;
		mergeSort(ddest, dsrc, idest, isrc, low, mid, ascending);
		mergeSort(ddest, dsrc, idest, isrc, mid, high, ascending);
		// If list is already sorted, just copy from srcs to desst. This
		// optimization results in faster sorts for nearly ordered lists.
		if (compare(dsrc[mid - 1], dsrc[mid], isrc[mid - 1], isrc[mid],
				ascending) <= 0) {
			System.arraycopy(dsrc, low, ddest, low, length);
			System.arraycopy(isrc, low, idest, low, length);
			return;
		}
		// Merge sorted halves (now in src) into dest
		for (int i = low, p = low, q = mid; i < high; i++) {
			if (q >= high
					|| p < mid
					&& compare(dsrc[p], dsrc[q], isrc[p], isrc[q], ascending) <= 0) {
				ddest[i] = dsrc[p];
				idest[i] = isrc[p++];
			} else {
				ddest[i] = dsrc[q];
				idest[i] = isrc[q++];
			}
		}
	}

    private static void mergeSort(int[] dsrc, int[] ddest, double[] isrc,
			double[] idest, int low, int high, boolean ascending) {
		int length = high - low;
		// Small arrays just use an insertion sort.
		if (length < 7) {
			for (int i = low; i < high; i++) {
				for (int j = i; j > low
						&& compare(ddest[j - 1], ddest[j], idest[j - 1],
								idest[j], ascending) > 0; j--) {
					swap(ddest, idest, j, j - 1);
				}
			}
			return;
		}
		// On larger arrays, recursively sort halves.
		int mid = (low + high) / 2;
		mergeSort(ddest, dsrc, idest, isrc, low, mid, ascending);
		mergeSort(ddest, dsrc, idest, isrc, mid, high, ascending);
		// If list is already sorted, just copy from srcs to desst. This
		// optimization results in faster sorts for nearly ordered lists.
		if (compare(dsrc[mid - 1], dsrc[mid], isrc[mid - 1], isrc[mid],
				ascending) <= 0) {
			System.arraycopy(dsrc, low, ddest, low, length);
			System.arraycopy(isrc, low, idest, low, length);
			return;
		}
		// Merge sorted halves (now in src) into dest
		for (int i = low, p = low, q = mid; i < high; i++) {
			if (q >= high
					|| p < mid
					&& compare(dsrc[p], dsrc[q], isrc[p], isrc[q], ascending) <= 0) {
				ddest[i] = dsrc[p];
				idest[i] = isrc[p++];
			} else {
				ddest[i] = dsrc[q];
				idest[i] = isrc[q++];
			}
		}
	}

    private static <E> void mergeSort(int[] dsrc, int[] ddest, E[] isrc,
            E[] idest, int low, int high, boolean ascending) {
        int length = high - low;
        // Small arrays just use an insertion sort.
        if (length < 7) {
            for (int i = low; i < high; i++) {
                for (int j = i; j > low
                        && compare(ddest[j - 1], ddest[j], ascending) > 0; j--) {
                    swap(ddest, idest, j, j - 1);
                }
            }
            return;
        }
        // On larger arrays, recursively sort halves.
        int mid = (low + high) / 2;
        mergeSort(ddest, dsrc, idest, isrc, low, mid, ascending);
        mergeSort(ddest, dsrc, idest, isrc, mid, high, ascending);
        // If list is already sorted, just copy from srcs to desst. This
        // optimization results in faster sorts for nearly ordered lists.
        if (compare(dsrc[mid - 1], dsrc[mid],
                ascending) <= 0) {
            System.arraycopy(dsrc, low, ddest, low, length);
            System.arraycopy(isrc, low, idest, low, length);
            return;
        }
        // Merge sorted halves (now in src) into dest
        for (int i = low, p = low, q = mid; i < high; i++) {
            if (q >= high
                    || p < mid
                    && compare(dsrc[p], dsrc[q], ascending) <= 0) {
                ddest[i] = dsrc[p];
                idest[i] = isrc[p++];
            } else {
                ddest[i] = dsrc[q];
                idest[i] = isrc[q++];
            }
        }
    }

    private static int compare(int d1, int d2, int n1, int n2,
			boolean ascending) {
		int rtn;
		if (d1 == d2) {
			rtn = (n1 < n2 ? -1 : (n1 > n2 ? 1 : 0));
		} else {
			rtn = d1 < d2 ? -1 : (d1 > d2 ? 1 : 0);
		}
		if (!ascending)
			rtn = -rtn;
		return rtn;
	}

    private static int compare(int d1, int d2, double n1, double n2,
			boolean ascending) {
		int rtn;
		if (d1 == d2) {
			rtn = (n1 < n2 ? -1 : (n1 > n2 ? 1 : 0));
		} else {
			rtn = d1 < d2 ? -1 : (d1 > d2 ? 1 : 0);
		}
		if (!ascending)
			rtn = -rtn;
		return rtn;
	}

    private static int compare(int n1, int n2,
            boolean ascending) {
        int rtn = (n1 < n2 ? -1 : (n1 > n2 ? 1 : 0));
        if (!ascending)
            rtn = -rtn;
        return rtn;
    }

    private static void swap(int x[], int[] n, int a, int b) {
		int t = x[a];
		x[a] = x[b];
		x[b] = t;
		t = n[a];
		n[a] = n[b];
		n[b] = t;
	}
    
    private static <E> void swap(int[] x, E[] n, int a, int b) {
        int t = x[a];
        x[a] = x[b];
        x[b] = t;
        E tmp = n[a];
        n[a] = n[b];
        n[b] = tmp;
    }

    private static void swap(int x[], double[] n, int a, int b) {
		int t = x[a];
		x[a] = x[b];
		x[b] = t;
		double d = n[a];
		n[a] = n[b];
		n[b] = d;
	}

    private static <T extends Comparable<? super T>> void mergeSort(List<T> osrc, List<T> odest, int[] isrc,
			int[] idest, int low, int high, boolean ascending) {
		int length = high - low;
		// Small arrays just use an insertion sort.
		if (length < 7) {
			for (int i = low; i < high; i++) {
				for (int j = i; j > low && compare(odest.get(j-1), odest.get(j), idest[j-1], idest[j], ascending) > 0; j--) {
					swap(odest, idest, j, j - 1);
				}
			}
			return;
		}
		// On larger arrays, recursively sort halves.
		int mid = (low + high) / 2;
		mergeSort(odest, osrc, idest, isrc, low, mid, ascending);
		mergeSort(odest, osrc, idest, isrc, mid, high, ascending);
		// If list is already sorted, just copy from srcs to desst. This
		// optimization results in faster sorts for nearly ordered lists.
		if (compare(osrc.get(mid - 1), osrc.get(mid), isrc[mid - 1], isrc[mid],
				ascending) <= 0) {
			int lim = low + length;
			for (int i=low; i<lim; i++) {
				odest.set(i, osrc.get(i));
				idest[i] = isrc[i];
			}
			return;
		}
		// Merge sorted halves (now in src) into dest
		for (int i = low, p = low, q = mid; i < high; i++) {
			if (q >= high
					|| p < mid
					&& compare(osrc.get(p), osrc.get(q), isrc[p], isrc[q], ascending) <= 0) {
				odest.set(i, osrc.get(p));
				idest[i] = isrc[p++];
			} else {
				odest.set(i, osrc.get(q));
				idest[i] = isrc[q++];
			}
		}
	}

    private static <T> void mergeSort(List<T> osrc, List<T> odest, Comparator<? super T> c, int[] isrc,
            int[] idest, int low, int high, boolean ascending) {

        int length = high - low;
        
        // Small arrays just use an insertion sort.
        if (length < 7) {
            for (int i = low; i < high; i++) {
                for (int j = i; j > low && compare(odest.get(j-1), odest.get(j), c, idest[j-1], idest[j], ascending) > 0; j--) {
                    swap(odest, idest, j, j - 1);
                }
            }
            return;
        }
        
        // On larger arrays, recursively sort halves.
        int mid = (low + high) / 2;
        mergeSort(odest, osrc, c, idest, isrc, low, mid, ascending);
        mergeSort(odest, osrc, c, idest, isrc, mid, high, ascending);
        
        // If list is already sorted, just copy from srcs to desst. This
        // optimization results in faster sorts for nearly ordered lists.
        if (compare(osrc.get(mid - 1), osrc.get(mid), c, isrc[mid - 1], isrc[mid],
                ascending) <= 0) {
            int lim = low + length;
            for (int i=low; i<lim; i++) {
                odest.set(i, osrc.get(i));
                idest[i] = isrc[i];
            }
            return;
        }
        // Merge sorted halves (now in src) into dest
        for (int i = low, p = low, q = mid; i < high; i++) {
            if (q >= high
                    || p < mid
                    && compare(osrc.get(p), osrc.get(q), c, isrc[p], isrc[q], ascending) <= 0) {
                odest.set(i, osrc.get(p));
                idest[i] = isrc[p++];
            } else {
                odest.set(i, osrc.get(q));
                idest[i] = isrc[q++];
            }
        }
    }
    
	private static <T extends Comparable<? super T>> int compare(T c1, T c2, int n1, int n2,
			boolean ascending) {
		int rtn = c1.compareTo((T) c2);
		if (rtn == 0) {
			rtn = (n1 < n2 ? -1 : (n1 > n2 ? 1 : 0));
		}
		if (!ascending)
			rtn = -rtn;
		return rtn;
	}

    private static <T> int compare(T c1, T c2, Comparator<? super T> c, int n1, int n2,
            boolean ascending) {
        int rtn = c.compare(c1, c2);
        if (rtn == 0) {
            rtn = (n1 < n2 ? -1 : (n1 > n2 ? 1 : 0));
        }
        if (!ascending)
            rtn = -rtn;
        return rtn;
    }

    private static <T> void swap(List<T> lst, int[] n, int a, int b) {
		T c = lst.get(a);
		lst.set(a, lst.get(b));
		lst.set(b, c);
		int tn = n[a];
		n[a] = n[b];
		n[b] = tn;
	}
    
    private static void swap(int i, int j, int[]... arrays) {
        if (i != j) {
            final int arrCount = arrays.length;
            for (int a=0; a<arrCount; a++) {
                int[] array = arrays[a];
                array[i] ^= array[j];
                array[j] ^= array[i];
                array[i] ^= array[j];
            }
        }
    }
    
    private static int compare(int i, int j, int maxDepth, int[]... arrays) {
        for (int d=0; d<maxDepth; d++) {
            int[] array = arrays[d];
            if (array[i] < array[j]) return -1;
            if (array[i] > array[i]) return +1;
        }
        return 0;
    }
    
    private static int compare(int i, int maxDepth, int[] colBuffer, int[]... arrays) {
        for (int d=0; d<maxDepth; d++) {
            int[] array = arrays[d];
            int bv = colBuffer[d];
            int v = array[i];
            if (bv > v) return -1;
            if (bv < v) return +1;
        }
        return 0;
    }
}
