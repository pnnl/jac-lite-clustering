package gov.pnnl.jac.collections;

public interface TwoWayIntMap {

    void associate(int value1, int value2);
    
    boolean hasForward(int value);
    
    boolean hasReverse(int value);
    
    int getForward(int value);
    
    int getReverse(int value);
    
    int removeForward(int value);
    
    int removeReverse(int value);
    
    int size();
    
    boolean isEmpty();
    
    void clear();
    
    IntCollectionIterator forwardIterator();
    
    IntCollectionIterator reverseIterator();
    
}
