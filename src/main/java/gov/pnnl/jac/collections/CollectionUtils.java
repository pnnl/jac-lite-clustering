package gov.pnnl.jac.collections;

import java.util.*;

import gov.pnnl.jac.util.IFilter;

public final class CollectionUtils {

	public <T> List<T> removeAllMatches(Collection<T> col, IFilter<T> filter) {
		List<T> removed = new ArrayList<T> ();
		Iterator<T> iter = col.iterator();
		while(iter.hasNext()) {
			T t = iter.next();
			if (filter.accept(t)) {
				iter.remove();
				removed.add(t);
			}
		}
		return removed;
	}
	
	public <T> T removeFirstMatch(List<T> list, IFilter<T> filter) {
		int n = list.size();
		for (int i=0; i<n; i++) {
			T t = list.get(i);
			if (filter.accept(t)) {
				list.remove(i);
				return t;
			}
		}
		return null;
	}
	
	public <T> T removeLastMatch(List<T> list, IFilter<T> filter) {
		int n = list.size();
		for (int i=n-1; i>=0; i--) {
			T t = list.get(i);
			if (filter.accept(t)) {
				list.remove(i);
				return t;
			}
		}
		return null;
	}
	
	public <T> List<T> getAllMatches(Collection<T> col, IFilter<T> filter) {
		List<T> matches = new ArrayList<T> ();
		Iterator<T> iter = col.iterator();
		while(iter.hasNext()) {
			T t = iter.next();
			if (filter.accept(t)) {
				matches.add(t);
			}
		}
		return matches;
	}
	
	public <T> T getFirstMatch(List<T> list, IFilter<T> filter) {
		int n = list.size();
		for (int i=0; i<n; i++) {
			T t = list.get(i);
			if (filter.accept(t)) {
				return t;
			}
		}
		return null;
	}
	
	public <T> T getLastMatch(List<T> list, IFilter<T> filter) {
		int n = list.size();
		for (int i=n-1; i>=0; i--) {
			T t = list.get(i);
			if (filter.accept(t)) {
				return t;
			}
		}
		return null;
	}
	
	public <T> int firstMatchingIndex(List<T> list, IFilter<T> filter) {
		return nextMatchingIndex(list, 0, filter);
	}
	
	public <T> int nextMatchingIndex(List<T> list, int start, IFilter<T> filter) {
		int n = list.size();
		if (start != n) {
			for (int i=start; i<n; i++) {
				T t = list.get(i);
				if (filter.accept(t)) {
					return i;
				}
			}
		}
		return -1;
	}
	
	public <T> int lastMatchingIndex(List<T> list, IFilter<T> filter) {
		return previousMatchingIndex(list, list.size() - 1, filter);
	}
	
	public <T> int previousMatchingIndex(List<T> list, int start, IFilter<T> filter) {
		if (start != -1) {
			for (int i=start; i>=0; i--) {
				T t = list.get(i);
				if (filter.accept(t)) {
					return i;
				}
			}
		}
		return -1;
	}
	
	public <T> int[] matchingIndexes(List<T> list, IFilter<T> filter) {
		IntList intList = new IntArrayList();
		int n = list.size();
		for (int i=0; i<n; i++) {
			if (filter.accept(list.get(i))) {
				intList.add(i);
			}
		}
		return intList.toArray();
	}
	
	public <T> void addAllMatches(Collection<T> source, Collection<T> dest, IFilter<T> filter) {
		Iterator<T> iter = source.iterator();
		while(iter.hasNext()) {
			T t = iter.next();
			if (filter.accept(t)) {
				dest.add(t);
			}
		}
	}

}
