package gov.pnnl.jac.geom.distance;

import gov.pnnl.jac.util.DataConverter;

import java.io.*;

public class FileDistanceCache implements DistanceCache {

	private File mFile;
	private RandomAccessFile mRAFile;
	private int mIndexCount;
	private long mDistanceCount;
	
	public FileDistanceCache(int indexCount, File f) throws IOException {
		
		if (indexCount < 0) {
			throw new IllegalArgumentException("number of indices < 0: " + indexCount);
		}

		if (f == null) {
			throw new NullPointerException();
		}
		
		mIndexCount = indexCount;
		mFile = f;

		mDistanceCount = ((long)mIndexCount * ((long) mIndexCount - 1L))/2L;

		openFile();
		
		// In order to restore from a file, need to write the index count.
		mRAFile.writeInt(mIndexCount);
		
		// Write 0.0 as the last distance, to expand the file to its complete size.
		// O/W, if not all distances are set before the object is done with, the
		// file will not be large enough to be used by DistanceCacheFactory.read()
		// to restore a DistanceCache object.
		long offset = 8L * (getNumDistances() - 1) + 4L;
		mRAFile.seek(offset);
		mRAFile.writeDouble(0.0);
	}
	
	FileDistanceCache(File f) throws IOException {
	
		mFile = f;
		
		openFile();
		
		mIndexCount = mRAFile.readInt();
		mDistanceCount = ((long)mIndexCount * ((long) mIndexCount - 1L))/2L;

	}
	
	public boolean isOpen() {
		return mRAFile != null;
	}
	
	private synchronized void openFile() throws IOException {
		if (mRAFile == null) {
			mRAFile = new RandomAccessFile(mFile, "rw");
		}
	}
	
	public synchronized void closeFile() throws IOException {
		if (mRAFile != null) {
			mRAFile.close();
			mRAFile = null;
		}
	}
	
	protected void finalize() {
		if (isOpen()) {
			try {
				closeFile();
			} catch (IOException ioe) {
			}
		}
	}
	
	public File getFile() {
		return mFile;
	}
	
	private void checkIndex(int index) {
		if (index < 0 || index >= mIndexCount) {
			throw new IllegalArgumentException("index not in [0 - (" + mIndexCount + " - 1)]: " + index);
		}
	}

	public long distancePos(int index1, int index2) {
		if (index1 == index2) {
			throw new IllegalArgumentException("indices are equal: " + index1);
		}
        if (index1 > index2) { // Swap them
            index1 ^= index2;
            index2 ^= index1;
            index1 ^= index2;
        }
        long n = mIndexCount - index1;
        return mDistanceCount - n *(n - 1)/2 + index2 - index1 - 1;	
	}
	
	private long fileOffset(int index1, int index2) {
		// 8L is the sizeof a double, 4L accounts for the indexCount written to 
		// the start of the file.
		return 8L * distancePos(index1, index2) + 4L;
	}
	
	private boolean contiguousIndices(int[] indices1, int[] indices2) {
		int n = indices1.length;
		if (n > 0) {
			long lastPos = distancePos(indices1[0], indices2[0]);
			for (int i=1; i<n; i++) {
				long curPos = distancePos(indices1[i], indices2[i]);
				if (curPos != lastPos + 1L) {
					return false;
				}
				lastPos = curPos;
			}
		}
		return true;
	}
	
	/**
	 * Get the number of indices, N.  Valid indices for the other methods are
	 * then [0 - (N-1)].
	 * @return - the number of indices.
	 */
	public int getNumIndices() {
		return mIndexCount;
	}
	
	public long getNumDistances() {
		return mDistanceCount;
	}
	
	public synchronized double getDistance(long n) throws IOException {
		if (!isOpen()) openFile();
		long offset = 8L * n + 4L;
		mRAFile.seek(offset);
		return mRAFile.readDouble();
	}

	/**
	 * Get the distance between the entities represented by index1 and index2.
	 * @param index1
	 * @param index2
	 * @return
	 */
	public synchronized double getDistance(int index1, int index2) throws IOException {
		checkIndex(index1);
		checkIndex(index2);
		if (!isOpen()) openFile();
		mRAFile.seek(fileOffset(index1, index2));
		return mRAFile.readDouble();
	}
	
	/**
	 * Get distances in bulk.  Element i of the returned array will contain the
	 * distance between indices1[i] and indices2[i].  Therefore, indices1 and indices2
	 * must be the same length.  If distances is non-null, it must be the same length
	 * as indices1 and indices2.  If it's null, a new distances array is allocated and
	 * returned.
	 * @param indices1
	 * @param indices2
	 * @param distances
	 * @return
	 */
	public synchronized double[] getDistances(int[] indices1, int[] indices2, double[] distances) throws IOException {
		int n = indices1.length;
		if (n != indices2.length) {
			throw new IllegalArgumentException(String.valueOf(n) + " != " + indices2.length);
		}
		double[] d = distances;
		if (distances != null) {
			if (distances.length != n) {
				throw new IllegalArgumentException("distance buffer length not equal to number of indices");
			}
		} else {
			d = new double[n];
		}
		if (n > 1 && contiguousIndices(indices1, indices2)) {
			// If they're continuous, only have to check the beginning and ending indices.
			checkIndex(indices1[0]);
			checkIndex(indices2[0]);
			checkIndex(indices1[n-1]);
			checkIndex(indices2[n-1]);
			// Everything can be read in one gulp, which is much faster than reading one
			// at a time.
			byte[] readBuffer = new byte[n*8]; // 8 bytes per distance.
			if (!isOpen()) openFile();
			mRAFile.seek(fileOffset(indices1[0], indices2[0]));
			mRAFile.readFully(readBuffer);
			DataConverter.fromBytes(readBuffer, 0, d, 0, readBuffer.length);
		} else { // Indices aren't continuous, or have less than 2 to read.
			// Get 'em one at a time.
			for (int i=0; i<n; i++) {
				d[i] = getDistance(indices1[i], indices2[i]);
			}
		}
		return d;
		
	}
	
	/**
	 * Set the distance between the identities identified by index1 and index2.
	 * @param index1
	 * @param index2
	 * @param distance
	 * @throws IOException 
	 */
	public synchronized void setDistance(int index1, int index2, double distance) throws IOException {
		checkIndex(index1);
		checkIndex(index2);
		if (!isOpen()) openFile();
		mRAFile.seek(fileOffset(index1, index2));
		mRAFile.writeDouble(distance);
	}
	
	/**
	 * Set distances in bulk.  All three arrays must be the same length.
	 * @param indices1
	 * @param indices2
	 * @param distances
	 */
	public synchronized void setDistances(int[] indices1, int[] indices2, double[] distances) 
	  throws IOException {
		int n = indices1.length;
		if (n != indices2.length) {
			throw new IllegalArgumentException(String.valueOf(n) + " != " + indices2.length);
		}
		if (n != distances.length) {
			throw new IllegalArgumentException("distance buffer length not equal to number of indices");
		}
		if (n > 1 && contiguousIndices(indices1, indices2)) {
			// If they're continuous, only have to check the beginning and ending indices.
			checkIndex(indices1[0]);
			checkIndex(indices2[0]);
			checkIndex(indices1[n-1]);
			checkIndex(indices2[n-1]);
			// Everything can be written in one schmeer, which is much faster than writing one
			// at a time.
			byte[] writeBuffer = new byte[n*8]; // 8 bytes per distance.
			DataConverter.toBytes(distances, 0, writeBuffer, 0, n);
			if (!isOpen()) openFile();
			mRAFile.seek(fileOffset(indices1[0], indices2[0]));
			mRAFile.write(writeBuffer);
		} else {
			for (int i=0; i<n; i++) {
				setDistance(indices1[i], indices2[i], distances[i]);
			}
		}
	}

}
