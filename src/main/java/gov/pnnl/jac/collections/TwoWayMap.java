package gov.pnnl.jac.collections;

import java.util.Iterator;
import java.util.Set;

public interface TwoWayMap<K, V> {

    void associate(K k, V v);

    boolean disassociate(K k, V v);
    
    boolean hasForward(K k);
    
    boolean hasReverse(V v);
    
    V getForward(K k);
    
    K getReverse(V v);
    
    V removeForward(K k);
    
    K removeReverse(V v);
    
    int size();
    
    boolean isEmpty();
    
    void clear();
    
    Set<K> forwardKeySet();
    
    Set<V> reverseKeySet();
    
    Iterator<K> forwardIterator();
    
    Iterator<V> reverseIterator();
    
    TwoWayMap<K, V> copy();

}
