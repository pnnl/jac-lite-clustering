// ** Notice:
// **     This computer software was prepared by Battelle Memorial Institute,
// **     hereinafter the Contractor, under Contract No. DE-AC06-76RL0 1830 with
// **     the Department of Energy (DOE).  All rights in the computer software
// **     are reserved by DOE on behalf of the United States Government and the
// **     Contractor as provided in the Contract.  You are authorized to use
// **     this computer software for Governmental purposes but it is not to be
// **     released or distributed to the public. NEITHER THE GOVERNMENT NOR THE
// **     CONTRACTOR MAKES ANY WARRANTY, EXPRESS OR IMPLIED, OR ASSUMES ANY
// **     LIABILITY FOR THE USE OF THIS SOFTWARE.  This notice including this
// **     sentence must appear on any copies of this computer software.
package gov.pnnl.jac.collections;

import java.util.Arrays;

/**
 * <p>Utility class for use with lists of primitives.</p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: Battelle Memorial Institute</p>
 *
 * @author R. Scarberry
 * @version 1.0
 */
public final class ListUtils {

  // Make noninstantiable, since all methods are static.
  private ListUtils() {
  }

  /**
   * Computes hash code for a list of integers.
   * @param intList IntList
   * @return int
   */
  public static int computeHash(IntList intList) {
    int hc = 17;
    int sz = intList.size();
    for (int i=0; i<sz; i++) {
      hc = 37*hc + intList.get(i);
    }
    return hc;
  }

  /**
   * Computes hash code for a list of longs.
   * @param longList LongList
   * @return int
   */
  public static int computeHash(LongList longList) {
    int hc = 17;
    int sz = longList.size();
    for (int i=0; i<sz; i++) {
        long v = longList.get(i);
        hc = 37*hc + (int)(v ^ (v >> 32));
    }
    return hc;
  }

  /**
   * Computes hash code for a list of doubles.
   * @param doubleList DoubleList
   * @return int
   */
  public static int computeHash(DoubleList doubleList) {
    int hc = 17;
    int sz = doubleList.size();
    for (int i=0; i<sz; i++) {
      // Colt's HashFunctions.hash(f) uses Float.floatToIntBits(f*663608941.737f),
      // but I think this will be fine here.
      long bits = Double.doubleToLongBits(doubleList.get(i));
      hc = 37*hc + (int)(bits ^ (bits >> 32));
    }
    return hc;
  }

  /**
   * Computes hash code for a list of floats.
   * @param floatList FloatList
   * @return int
   */
  public static int computeHash(FloatList floatList) {
    int hc = 17;
    int sz = floatList.size();
    for (int i=0; i<sz; i++) {
      // Colt's HashFunctions.hash(f) uses Float.floatToIntBits(f*663608941.737f),
      // but I think this will be fine here.
      hc = 37*hc + Float.floatToIntBits(floatList.get(i));
    }
    return hc;
  }
  /**
   * Check two <tt>IntList</tt>s for equality.  They are considered equal if
   * <tt>list1.size() == list2.size()</tt> and for every element index i,
   * <tt>list1.get(i) == list2.get(i)</tt>.
   * @param list1
   * @param list2
   * @return boolean
   */
  public static boolean checkEqual(IntList list1, IntList list2) {
    int sz = list1.size();
    if (sz == list2.size()) {
      for (int i=0; i<sz; i++) {
        if (list1.get(i) != list2.get(i)) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  /**
   * Check two <tt>LongList</tt>s for equality.  They are considered equal if
   * <tt>list1.size() == list2.size()</tt> and for every element index i,
   * <tt>list1.get(i) == list2.get(i)</tt>.
   * @param list1
   * @param list2
   * @return boolean
   */
  public static boolean checkEqual(LongList list1, LongList list2) {
    int sz = list1.size();
    if (sz == list2.size()) {
      for (int i=0; i<sz; i++) {
        if (list1.get(i) != list2.get(i)) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  /**
   * Check two <tt>FloatList</tt>s for equality.  They are considered equal if
   * <tt>list1.size() == list2.size()</tt> and for every element index i,
   * <tt>list1.get(i) == list2.get(i)</tt>.
   * @param list1
   * @param list2
   * @return boolean
   */
  public static boolean checkEqual(FloatList list1, FloatList list2) {
    int sz = list1.size();
    if (sz == list2.size()) {
      for (int i=0; i<sz; i++) {
        if (Float.floatToIntBits(list1.get(i)) != Float.floatToIntBits(list2.get(i))) {
          return false;
        }
      }
      return true;
    }
    return false;
  }
  
  /**
   * Check two <tt>DoubleList</tt>s for equality.  They are considered equal if
   * <tt>list1.size() == list2.size()</tt> and for every element index i,
   * <tt>list1.get(i) == list2.get(i)</tt>.
   * @param list1
   * @param list2
   * @return boolean
   */
  public static boolean checkEqual(DoubleList list1, DoubleList list2) {
    int sz = list1.size();
    if (sz == list2.size()) {
      for (int i=0; i<sz; i++) {
        if (Double.doubleToLongBits(list1.get(i)) != Double.doubleToLongBits(list2.get(i))) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  /**
   * Checks if the given index is in range.  If not, throws an 
   * IndexOutOfBoundsException.
   */
  public static void rangeCheck(int index, int size) {
		if (index < 0 || index >= size)
		    throw new IndexOutOfBoundsException(
			"Index: "+index+", Size: "+size);
  }
  
  public static int binarySearch(IntList list, int value) {
    return _binarySearch(list, 0, list.size(), value);
  }
  
  public static int binarySearch(IntList list, int fromIndex, int toIndex, int value) {
    rangeCheck(list.size(), fromIndex, toIndex);
    return _binarySearch(list, fromIndex, toIndex, value);
  }
  
  private static int _binarySearch(IntList list, int fromIndex, int toIndex, int value) {
    int low = fromIndex;
    int high = toIndex - 1;

    while (low <= high) {
      int mid = (low + high) >>> 1;
      int midVal = list.get(mid);

      if (midVal < value)
        low = mid + 1;
      else if (midVal > value)
        high = mid - 1; 
      else
        return mid; // key found
    }
    
    return -(low + 1);  // key not found.
  }
    
    /**
     * Check that fromIndex and toIndex are in range, and throw an
     * appropriate exception if they aren't.
     */
    private static void rangeCheck(int arrayLen, int fromIndex, int toIndex) {
        if (fromIndex > toIndex)
            throw new IllegalArgumentException("fromIndex(" + fromIndex +
                       ") > toIndex(" + toIndex+")");
        if (fromIndex < 0)
            throw new ArrayIndexOutOfBoundsException(fromIndex);
        if (toIndex > arrayLen)
            throw new ArrayIndexOutOfBoundsException(toIndex);
    }
}
