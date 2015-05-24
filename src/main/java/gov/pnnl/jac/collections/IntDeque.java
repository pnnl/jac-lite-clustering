package gov.pnnl.jac.collections;

public interface IntDeque extends IntQueue {

	void addFirst(int value);

	void addLast(int value);

	boolean offerFirst(int value);

    boolean offerLast(int value);

	int removeFirst();

	int removeLast();

	int pollFirst();

	int pollLast();

	int getFirst();

	int getLast();

	int peekFirst();

	int peekLast();

	boolean removeFirstOccurrence(int value);

	boolean removeLastOccurrence(int value);

	void push(int value);

	int pop();

	IntCollectionIterator descendingIterator();

}
