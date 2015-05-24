package gov.pnnl.jac.geom;

import gov.pnnl.jac.util.ExceptionUtil;

import java.util.ArrayList;
import java.util.List;

public class DecoratorKeyedCoordinateList<E> 
implements KeyedCoordinateList<E> {

	private Object mDummy = new Object();
	
    private CoordinateList mWrappedCoords;
    private List<Object> mKeyList;
    
    public DecoratorKeyedCoordinateList(CoordinateList wrappedCoords) {
    	ExceptionUtil.checkNotNull(wrappedCoords);
    	mWrappedCoords = wrappedCoords;
        int numCoords = wrappedCoords.getCoordinateCount();
        mKeyList = new ArrayList<Object>(numCoords);
    }
    
    public Comparable<E> getCoordinateKey(int ndx) {
        Comparable<E> key = null;
        if (ndx < mKeyList.size()) {
        	Object o = mKeyList.get(ndx);
        	if (o != mDummy) {
        		@SuppressWarnings("unchecked")
				Comparable<E> o2 = (Comparable<E>) o;
				key = o2;
        	}
        }
        return key;
    }

    public void setCoordinateKey(int ndx, Comparable<E> key) {
    	final int sz = mKeyList.size();
    	if (ndx >= sz) {
    		final int padNum = ndx - mKeyList.size();
    		for (int i=0; i<padNum; i++) {
    			mKeyList.add(mDummy);
    		}
    		mKeyList.add(key);
    	} else {
    		mKeyList.set(ndx, key);
    	}
    }

    public void setCoordinates(int ndx, Comparable<E> key, double[] coords) {
        mWrappedCoords.setCoordinates(ndx, coords);
        setCoordinateKey(ndx, key);
    }

    public double[] computeAverage(int[] indices, double[] avg) {
        return mWrappedCoords.computeAverage(indices, avg);
    }

    public double computeAverage(int[] indices, int dim) {
        return mWrappedCoords.computeAverage(indices, dim);
    }

    public double[] computeMaximum(int[] indices, double[] max) {
        return mWrappedCoords.computeMaximum(indices, max);
    }

    public double computeMaximum(int[] indices, int dim) {
        return mWrappedCoords.computeMaximum(indices, dim);
    }

    public double[] computeMedian(int[] indices, double[] med) {
        return mWrappedCoords.computeMedian(indices, med);
    }

    public double computeMedian(int[] indices, int dim) {
        return mWrappedCoords.computeMedian(indices, dim);
    }

    public double[] computeMinimum(int[] indices, double[] min) {
        return mWrappedCoords.computeMinimum(indices, min);
    }

    public double computeMinimum(int[] indices, int dim) {
        return mWrappedCoords.computeMinimum(indices, dim);
    }

    public double getCoordinate(int ndx, int dim) {
        return mWrappedCoords.getCoordinate(ndx, dim);
    }

    public int getCoordinateCount() {
        return mWrappedCoords.getCoordinateCount();
    }

    public double getCoordinateQuick(int ndx, int dim) {
        return mWrappedCoords.getCoordinateQuick(ndx, dim);
    }

    public double[] getCoordinates(int ndx, double[] coords) {
        return mWrappedCoords.getCoordinates(ndx, coords);
    }

    public int getDimensionCount() {
        return mWrappedCoords.getDimensionCount();
    }

    public double[] getDimensionValues(int dim, double[] values) {
        return mWrappedCoords.getDimensionValues(dim, values);
    }

    public void setCoordinate(int ndx, int dim, double coord) {
        mWrappedCoords.setCoordinate(ndx, dim, coord);
    }

    public void setCoordinateQuick(int ndx, int dim, double coord) {
        mWrappedCoords.setCoordinateQuick(ndx, dim, coord);
    }

    public void setCoordinates(int ndx, double[] coords) {
        mWrappedCoords.setCoordinates(ndx, coords);
    }
}
