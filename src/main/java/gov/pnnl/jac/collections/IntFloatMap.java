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

import java.util.*;

/**
 * Interface to define objects that map integer keys to float values
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: Battelle Memorial Institute</p>
 *
 * @author R. Scarberry, Vern Crow
 * @version 1.0
 */
public interface IntFloatMap {

  /**
   * Returns the number of key-value pairs contained in the map.
   *
   * @return the number of mappings.
   */
  int size();

  /**
   * Returns <tt>true</tt> if this map contains no key-value pairs.
   *
   * @return <tt>true</tt> if the number of mappings is zero.
   */
  boolean isEmpty();

  /**
   * Returns <tt>true</tt> if the receiver maps the specifed integer key
   * to a float value.
   *
   * @param key an integer key.
   * @return <tt>true</tt> if this map contains a mapping for the key.
   */
  boolean containsKey(int key);

  /**
   * Returns <tt>true</tt> if the receiver maps at least one key to the
   * specified value.
   *
   * @param value the value.
   * @return <tt>true</tt> if at least one key maps to the value.
   */
  boolean containsValue(float value);

  /**
   * Returns the value to which the receiver maps the given key, or
   * -1 if the key is not contained.  Since -1 is a perfectly valid value
   * for storage in the map, its return does not
   * <i>necessarily</i> prove that the map does not contain the key.
   * Use <tt>containsKey</tt> to conclusively determine if the receiver
   * contains a mapping for the given key.
   *
   * @param key the key.
   * @return the value to which the receiver maps the key, or
   *	       -1 if the map does not contain the key.
   *
   * @see #containsKey(int)
   */
  float get(int key);

  // Modification Operations

  /**
   * Maps the specified integer key to the specified value in this map.
   * (This is an optional operation to permit read-only implementations.)
   * The previous mapping for the key, if present, is overwritten.
   *
   * @param key the key.
   * @param value value to be associated with the key.
   * @return the previous value paired to the given key, or -1 if no
   *   previous mapping was present.
   *
   * @throws UnsupportedOperationException if the <tt>put</tt> operation is
   *	          not supported by this map.
   * @throws IllegalArgumentException if some aspect of this key or value
   *	          prevents it from being stored in this map.
   */
  float put(int key, float value);

  /**
   * Remove the mapping for the specified key from the receiver. (To permit
   * read-only implementations, this is an optional operation.)
   *
   * @param key the key.
   * @return the previous value paired to the given key, or -1 if no
   *   previous mapping was present.  (A -1 return does not
   *   <i>necessarily</i> prove that no mapping was present: the key may have
   *   been explicitly mapped to -1.)
   * @throws UnsupportedOperationException if the <tt>remove</tt> method is
   *         not supported by this map.
   */
  float remove(int key);

  // Bulk Operations
  /**
   * Adds all key-value mapping from the specified <tt>IntFloatMap</tt> to the
   * receiver.  (This is an optional operation, since some implementations
   * may be read-only.)  These mappings may replace mappings already present
   * in the receiver.
   *
   * @param map contains mappings to be copied to this map.
   *
   * @throws UnsupportedOperationException if the <tt>putAll</tt> method is
   * 		  not supported by this map.
   *
   * @throws IllegalArgumentException some aspect of a key or value in the
   *	          specified map prevents it from being stored in this map.
   */
  void putAll(IntFloatMap map);

  /**
   * Remove all key-value mappings from the receiver. (Optional operation.)
   *
   * @throws UnsupportedOperationException if this operation is not allowed
   *   by the implementation.
   */
  void clear();

  // Views

  /**
   * Returns the integer keys for which the receiver contains mappings.  The
   * order of the returned keys is undefined.  In the event the receiver contains
   * no mappings, the returned array should be length zero, not <tt>null</tt>.
   *
   * @return an array containing the keys with mappings.
   */
  int[] keys();

  /**
   * Returns an iterator over the keys of this map.
   * @return IntIterator
   */
  IntCollectionIterator keyIterator();

  /**
   * Returns an array of the values contained in the receiver.  The order of
   * the values in the array is undefined.
   *
   * @return an array containing the values contained in the map.
   */
  float[] values();

  // Comparison and hashing
  /**
   * Compares the receiver with the specified object for equality.  Returns
   * <tt>true</tt> if the specified object is also an <tt>IntFloatMap</tt> and the two
   * represent the same mappings.  They may not be the same implementation class.
   *
   * @param o object to be compared with this map.
   * @return <tt>true</tt> if the specified object contains the same
   *   key-value mappings as this map.
   */
  boolean equals(Object o);

  /**
   * Returns the hash code for this map.
   *
   * @return the hash code for this map.
   * @see Object#hashCode()
   * @see Object#equals(Object)
   * @see #equals(Object)
   */
  int hashCode();

}
