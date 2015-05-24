package gov.pnnl.jac.collections;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;

import gov.pnnl.jac.util.*;

public class ArrayIntDeque extends AbstractIntCollection 
	implements IntDeque, Cloneable, Serializable {

	private static final long serialVersionUID = 3096277341478628194L;

	// Always keep the length of values a power of 2 greater than 1.
	private transient int[] values;
	// Always keep this values.length - 1.  If values.length == 16,
	// then bitMask will be 0..00001111.
	private transient int bitMask;
	// Index of the head and tail.  The number of elements is always (tail - head)&(values.length-1)
	private transient int head;
	private transient int tail;
	
	public ArrayIntDeque() {
		this(16);
	}
	
	public ArrayIntDeque(int initialCapacity) {
		allocateInitialStorage(initialCapacity);
	}
	
	public ArrayIntDeque(IntCollection values) {
		allocateInitialStorage(values.size());
		addAll(values);
	}
	
	private void allocateInitialStorage(int n) {
		int cap = 16;
		if (n > 8) {
			cap = BitMath.isPowerOf2(n) ? n : BitMath.nextHigherPowerOf2(n);
		}
		this.values = new int[cap];
		// Since cap is always a power of 2, cap - 1 is a bitMask of all 1s below
		// the leading 0s. Ex: cap = 8, cap - 1 == 00000111
		this.bitMask = cap - 1;
	}
	
	private void doubleCapacity() {
		
		assert head == tail;
		
		int p = head;
		int n = values.length;
	    int r = n - p; // Number of elements to the right of p
	    int newCap = n << 1;
	    
	    if (newCap < 0) {
	      throw new IllegalStateException("Sorry, int deque is too big to expand");
	    }
	    
	    int[] newValues = new int[newCap];
	    
	    System.arraycopy(values, p, newValues, 0, r);
	    System.arraycopy(values, 0, newValues, r, p);
	    
	    this.values = newValues;
	    this.bitMask = newCap - 1;
	    
	    this.head = 0;
	    this.tail = n;
	}
	
	@Override
	public boolean add(int value) {
		addLast(value);
		return true;
	}

	@Override
	public int element() {
		return getFirst();
	}

	@Override
	public boolean offer(int value) {
		return offerLast(value);
	}

	@Override
	public int peek() {
		return peekFirst();
	}

	@Override
	public int poll() {
		return pollFirst();
	}

	@Override
	public int remove() {
		return removeFirst();
	}

	@Override
	public int size() {
		return (tail - head) & bitMask;
	}

	@Override
	public boolean isEmpty() {
		return head == tail;
	}

	@Override
	public boolean contains(int value) {
		for (int j=head; j != tail; j= (j+1) & bitMask) {
			if (values[j] == value) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int[] toArray() {
		final int sz = size();
		int[] result = new int[sz];
		if (sz > 0) {
			if (head < tail) {
				System.arraycopy(values, head, result, 0, sz);
			} else { // head > tail (if head == tail, sz == 0)
				int backLen = values.length - head;
				System.arraycopy(values, head, result, 0, backLen);
				System.arraycopy(values, 0, result, backLen, tail);
			}
		}
		return result;
	}

	@Override
	public boolean remove(int value) {
		return removeFirstOccurrence(value);
	}

	@Override
	public void clear() {
		if (head != tail) {
			head = tail = 0;
		}
	}

	@Override
	public IntCollectionIterator iterator() {
		return new AscendingIterator();
	}

	@Override
	public void addFirst(int value) {
		head = (head - 1) & bitMask;
		values[head] = value;
		if (head == tail) {
			doubleCapacity();
		}
	}

	@Override
	public void addLast(int value) {
		values[tail] = value;
		tail = (tail + 1) & bitMask;
		if (tail == head) {
			doubleCapacity();
		}
	}

	@Override
	public boolean offerFirst(int value) {
		addFirst(value);
		return true;
	}

	@Override
	public boolean offerLast(int value) {
		addLast(value);
		return true;
	}

	@Override
	public int removeFirst() {
		if (isEmpty()) {
			throw new NoSuchElementException();
		}
		return pollFirst();
	}

	@Override
	public int removeLast() {
		if (isEmpty()) {
			throw new NoSuchElementException();
		}
		return pollLast();
	}

	@Override
	public int pollFirst() {
		if (isEmpty()) {
			return -1;
		}
		int rtn = values[head];
		head = (head + 1) & bitMask;
		return rtn;
	}

	@Override
	public int pollLast() {
		if (isEmpty()) {
			return -1;
		}
		tail = (tail - 1) & bitMask;
		return values[tail];
	}

	@Override
	public int getFirst() {
		if (isEmpty()) {
			throw new NoSuchElementException();
		}
		return values[head];
	}

	@Override
	public int getLast() {
		if (isEmpty()) {
			throw new NoSuchElementException();
		}
		return values[(tail - 1) & bitMask];
	}

	@Override
	public int peekFirst() {
		return isEmpty() ? -1 : values[head];
	}

	@Override
	public int peekLast() {
		return isEmpty() ? -1 : values[(tail - 1) & bitMask];
	}

	@Override
	public boolean removeFirstOccurrence(int value) {
		return remove(value);
	}

	@Override
	public boolean removeLastOccurrence(int value) {
		IntCollectionIterator it = descendingIterator();
		while(it.hasNext()) {
			if (value == it.next()) {
				it.remove();
				return true;
			}
		}
		return false;
	}

	@Override
	public void push(int value) {
		addFirst(value);
	}

	@Override
	public int pop() {
		return removeFirst();
	}

	@Override
	public IntCollectionIterator descendingIterator() {
		return new DescendingIterator();
	}

	private boolean delete(int i) {
		
		final int[] vals = values;
		
		final int mask = bitMask;
		final int h = head;
		final int t = tail;
		
		// Number in queue prior to i.
		final int front = (i - h) & mask;
		// Number in queue after i.
		final int back = (t - i) & mask;
		
		// ((t - h) & mask) gives the size when this method was started.
		if (front >= ((t - h) & mask)) {
			throw new ConcurrentModificationException();
		}
		
		// Do the fewest copies possible.
		if (front < back) {
			if (h <= i) {
				System.arraycopy(vals, h, vals, h+1, front);
			} else {
				System.arraycopy(vals, 0, vals, 1, i);
				vals[0] = vals[mask];
				System.arraycopy(vals, h, vals, h+1, mask-h);
			}
			head = (h+1) & mask;
			return false;
		} else {
			if (i < t) {
				System.arraycopy(vals, i+1, vals, i, back);
				tail = t - 1;
			} else {
				System.arraycopy(vals, i+1, vals, i, mask-i);
				vals[mask] = vals[0];
				System.arraycopy(vals, 1, vals, 0, t);
				tail = (t - 1) & mask;
			}
			return true;
		}
	}
	
	public ArrayIntDeque clone() {
		try {
			ArrayIntDeque result = (ArrayIntDeque) super.clone();
			result.values = Arrays.copyOf(values, values.length);
			return result;
		} catch (CloneNotSupportedException e) {
			throw new AssertionError();
		}
	}
	
	private void writeObject(ObjectOutputStream os) throws IOException {
		os.defaultWriteObject();
		os.writeInt(size());
		for (int i=head; i!=tail; i = (i+1)&bitMask) {
			os.writeInt(values[i]);
		}
	}
	
	private void readObject(ObjectInputStream is) 
			throws IOException, ClassNotFoundException {
		is.defaultReadObject();
		final int sz = is.readInt();
		allocateInitialStorage(sz);
		for (int i=0; i<sz; i++) {
			values[i] = is.readInt();
		}
		head = 0;
		tail = sz;
	}
		
	private class AscendingIterator implements IntCollectionIterator {
		
		// Index of the value returned in the next call to next.
		private int cursor = head;
		// Used to stop the iterator and to check for comodification.
		private int limit = tail;
		// Index of the last returned element.  -1 indicates no return.
		// It's also set to -1 on remove().
		private int lastNdx = -1;
		
		@Override
		public boolean hasNext() {
			return cursor != limit;
		}

		@Override
		public int next() {
			if (tail != limit) {
				throw new ConcurrentModificationException();
			}
			if (cursor == limit) {
				throw new NoSuchElementException();
			}
			int result = values[cursor];
			lastNdx = cursor;
			// Advance the cursor.
			cursor = (cursor + 1) & bitMask;
			return result;
		}
		
		@Override
		public void remove() {
			if (lastNdx < 0) {
				throw new IllegalStateException();
			}
			if (delete(lastNdx)) {
				cursor = (cursor - 1) & bitMask;
				limit = tail;
			}
			lastNdx = -1;
		}
		
	}
	
	private class DescendingIterator implements IntCollectionIterator {
		
		// Index of the value returned in the next call to next.
		private int cursor = tail;
		// Used to stop the iterator and to check for comodification.
		private int limit = head;
		// Index of the last returned element.  -1 indicates no return.
		// It's also set to -1 on remove().
		private int lastNdx = -1;
		
		@Override
		public boolean hasNext() {
			return cursor != limit;
		}
		
		@Override
		public int next() {
			if (head != limit) {
				throw new ConcurrentModificationException();
			}
			if (cursor == limit) {
				throw new NoSuchElementException();
			}
			cursor = (cursor - 1) & bitMask;
			int result = values[cursor];
			lastNdx = cursor;
			return result;
		}
		
		@Override
		public void remove() {
			if (lastNdx < 0) {
				throw new IllegalStateException();
			}
			if (!delete(lastNdx)) {
				cursor = (cursor + 1) & bitMask;
				limit = head;
			}
			lastNdx = -1;
		}
		
		
	}
}
