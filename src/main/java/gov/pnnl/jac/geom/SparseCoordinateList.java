package gov.pnnl.jac.geom;

import gov.pnnl.jac.collections.DoubleArrayList;
import gov.pnnl.jac.collections.DoubleList;
import gov.pnnl.jac.collections.IntArrayList;
import gov.pnnl.jac.collections.IntList;
import gov.pnnl.jac.collections.ListUtils;
import gov.pnnl.jac.util.ExceptionUtil;
import gov.pnnl.jac.util.SortUtils;

import java.util.ArrayList;
import java.util.List;

public class SparseCoordinateList extends AbstractCoordinateList {

	private List<IntList> mNonZeroIndices;
	private List<DoubleList> mNonZeroValues;
	
	public SparseCoordinateList() {
        mNonZeroIndices = new ArrayList<IntList>();
        mNonZeroValues = new ArrayList<DoubleList>();
	}
	
	@Override
	public void setCoordinates(int ndx, double[] coords) {
		ExceptionUtil.checkNonNegative(ndx);
		final int sz = mNonZeroIndices.size();
		boolean add = false;
		IntList nonZeroIndices = new IntArrayList();
		DoubleList nonZeroValues = new DoubleArrayList();
		if (ndx >= sz) {
			add = true;
			final int padCount = ndx - sz;
			for (int i=0; i<padCount; i++) {
				mNonZeroIndices.add(new IntArrayList());
				mNonZeroValues.add(new DoubleArrayList());
			}
			for (int i=0; i<coords.length; i++) {
				double d = coords[i];
				if (d != 0.0) {
					nonZeroIndices.add(i);
					nonZeroValues.add(d);
				}
			}
		}
		if (add) {
			mNonZeroIndices.add(nonZeroIndices);
			mNonZeroValues.add(nonZeroValues);
		} else {
			mNonZeroIndices.set(ndx, nonZeroIndices);
			mNonZeroValues.set(ndx, nonZeroValues);
		}
		if (coords.length > mDim) {
			mDim = coords.length;
		}
		if (ndx >= mCount) {
			mCount = ndx + 1;
		}
	}
	
	public void setCoordinates(int ndx, int[] indices, double[] values) {
		SortUtils.parallelSort(indices, values);
		final int sz = mNonZeroIndices.size();
		if (ndx >= sz) {
			final int padCount = ndx - sz;
			for (int i=0; i<padCount; i++) {
				mNonZeroIndices.add(new IntArrayList());
				mNonZeroValues.add(new DoubleArrayList());
			}
			mNonZeroIndices.add(new IntArrayList(indices));
			mNonZeroValues.add(new DoubleArrayList(values));
		} else {
			mNonZeroIndices.set(ndx, new IntArrayList(indices));
			mNonZeroValues.set(ndx, new DoubleArrayList(values));
		}
		if (ndx >= mCount) {
			mCount = ndx + 1;
		}
		if (indices.length > 0) {
			int max = indices[indices.length - 1];
			if (max >= mDim) {
				mDim = max + 1;
			}
		}
	}

	@Override
	public double[] getCoordinates(int ndx, double[] coords) {
		checkIndex(ndx);
		double[] rtn = coords != null && coords.length >= mDim ? coords : new double[mDim];
		if (ndx < mNonZeroIndices.size()) {
			IntList nonZeroIndices = mNonZeroIndices.get(ndx);
			DoubleList nonZeroValues = mNonZeroValues.get(ndx);
			final int sz = nonZeroIndices.size();
			for (int i=0; i<sz; i++) {
				rtn[nonZeroIndices.get(i)] = nonZeroValues.get(i);
			}
		}
		return rtn;
	}

	@Override
	public double getCoordinateQuick(int ndx, int dim) {
		checkIndex(ndx);
		checkDimension(dim);
		double rtn = 0.0;
		if (ndx < mNonZeroIndices.size()) {
			IntList nonZeroIndices = mNonZeroIndices.get(ndx);
			int n = ListUtils.binarySearch(nonZeroIndices, ndx);
			if (n >= 0) {
				rtn = mNonZeroValues.get(ndx).get(n);
			}
		}
		return rtn;
	}

	@Override
	public void setCoordinateQuick(int ndx, int dim, double coord) {
		double[] coords = new double[Math.max(dim, mDim)];
		getCoordinates(ndx, coords);
		coords[dim] = coord;
		setCoordinates(ndx, coords);
	}
}
