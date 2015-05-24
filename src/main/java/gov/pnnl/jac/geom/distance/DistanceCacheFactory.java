package gov.pnnl.jac.geom.distance;

import gov.pnnl.jac.geom.CoordinateList;

import java.io.*;

import java.nio.channels.FileChannel;

public class DistanceCacheFactory {

	private DistanceCacheFactory() {}
	
	public static DistanceCache newDistanceCache(
		int coordinateCount,
		long memoryThreshold, 
		long fileThreshold, 
		File cacheFile) throws IOException {
		
		long size = distanceCacheSize(coordinateCount);
		if (size <= memoryThreshold) {
			return new RAMDistanceCache(coordinateCount);
		} else if (size <= fileThreshold) {
			return new FileDistanceCache(coordinateCount, cacheFile);
		}
		
		return null;
	}
	
	public static ReadOnlyDistanceCache asReadOnly(DistanceCache cache) {
		if (cache instanceof ReadOnlyDistanceCache) {
			return (ReadOnlyDistanceCache) cache;
		} else {
			return new ReadOnlyWrapper(cache);
		}
	}
	
	public static ReadOnlyDistanceCache asReadOnlyDistanceCache(
			CoordinateList cs, DistanceFunc func) {
		return new ReadOnlyWrapper2(cs, func);
	}
	
	public static long distanceCacheSize(int coordinateCount) {
		return 4L + 4L*coordinateCount*((long)coordinateCount - 1);
	}
	
	public static int coordinateLimit(long byteThreshold) {
		return (int) ((Math.sqrt(16.0 + 16.0 * (byteThreshold - 4L)) + 4.0)/8.0);
	}
	
	public static int[] getIndicesForDistance(long pos, ReadOnlyDistanceCache cache) {

	    if (pos < 0 || pos >= cache.getNumDistances()) {
	        throw new IndexOutOfBoundsException("pos not in [0 - (" 
	                + cache.getNumDistances() + " - 1)]: " + pos);
	    }

	    int coordCount = cache.getNumIndices();       
	    double b = 2.0*coordCount - 1;

	    int i = (int)(-(Math.sqrt(b*b - 8.0*pos) - b)/2.0);
	    int j = i+1;

	    j += (int) (pos - cache.distancePos(i, j));

	    return new int[] { i, j };
	}
	
	public static void save(DistanceCache cache, File f) throws IOException {

		if (cache instanceof FileDistanceCache) {

			FileDistanceCache fileCache = (FileDistanceCache) cache;
			if (fileCache.isOpen()) {
				fileCache.closeFile();
			}
			
			File src = fileCache.getFile();
			FileInputStream fis = null;
			FileOutputStream fos = null;

			try {
				
				// The first version of this used the nio method of
				// getting the source FileChannel and the destination
				// FileChannel and using the FileChannel method 
				// transferTo.  But this failed on large files ( > 2GB), 
				// so I changed it to the more traditional copy method.
				
				long flen = src.length();
				fis = new FileInputStream(src);
				
				// Found this buffer size to give the speediest 
				// performance on my Windows XP laptop.  May want
				// to make the buffer size a static class member
				// initialized to an optimum value for the OS.
				byte[] ioBuffer = new byte[16*1024];
								
				fos = new FileOutputStream(f);				
				
				long transferred = 0L;
				while (transferred < flen) {					
					int bytesRead = fis.read(ioBuffer);
					if (bytesRead > 0) {
						fos.write(ioBuffer, 0, bytesRead);
					}
					transferred += bytesRead;
				}
				
			} finally {
				
				if (fos != null) {
					// Don't trap IOException, because if this file
					// doesn't close successfully, the cache probably didn't
					// save successfully.
					fos.close();
				}
				
				if (fis != null) {
					try {
						fis.close();
					} catch (IOException ioe) {
						// Ignore, since this probably didn't hose the save.
					}
				}
				
			}
			
		} else { // Some other kind, probably a RAMDistanceCache
			
			DataOutputStream dos = null;
			
			try {

				dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
				dos.writeInt(cache.getNumIndices());
				long numDistances = cache.getNumDistances();
				
				for (long d=0L; d<numDistances; d++) {
					dos.writeDouble(cache.getDistance(d));
				}
				
			} finally {
				
				if (dos != null) {
					// Don't trap IOException, because if this file
					// doesn't close successfully, the cache probably didn't
					// save successfully.
					dos.close();
				}
			}
			
		}
	}
	
	public static DistanceCache read(File f, 
			long memoryThreshold, 
			long fileThreshold) throws IOException {

		DistanceCache cache = null;
		
		DataInputStream dis = null;
		
		try {
			
			long flen = f.length();

			dis = new DataInputStream(new BufferedInputStream(new FileInputStream(f)));
			
			int numIndices = dis.readInt();
			long expectedLen = distanceCacheSize(numIndices);
			
			if (numIndices < 0 || flen != expectedLen) {
				throw new IOException("invalid distance cache file");
			}
			
			if (numIndices <= RAMDistanceCache.MAX_INDEX_COUNT && flen <= memoryThreshold) {
				
				int numDistances = numIndices*(numIndices - 1)/2;
				
				double[] distances = new double[numDistances];
				for (int i=0; i<numDistances; i++) {
					distances[i] = dis.readDouble();
				}
				
				cache = new RAMDistanceCache(numIndices, distances);
				
			} else if (flen <= fileThreshold) {
				
				try {
					dis.close();
				} catch (IOException x) {	
				}
				
				cache = new FileDistanceCache(f);
				
			} else {
				
				throw new IOException("cache file is too large: " + flen + " > " + fileThreshold);
				
			}
			
			
		} finally {
		
			if (dis != null) {
				try {
					dis.close();
				} catch (IOException ioe) {
					
				}
			}
			
		}

		return cache;
	}
	
	private static class ReadOnlyWrapper implements ReadOnlyDistanceCache {
		
		private DistanceCache mCache;
		
		ReadOnlyWrapper(DistanceCache cache) {
			mCache = cache;
		}
		
		public int getNumIndices() {
			return mCache.getNumIndices();
		}
		
		public double getDistance(int index1, int index2) throws IOException {
			return mCache.getDistance(index1, index2);
		}
		
		public double[] getDistances(int[] indices1, int[] indices2, double[] distances) 
		throws IOException {
			return mCache.getDistances(indices1, indices2, distances);
		}

		public long getNumDistances() {
			return mCache.getNumDistances();
		}
		
		public double getDistance(long n) throws IOException {
			return mCache.getDistance(n);
		}

		public long distancePos(int index1, int index2) {
			return mCache.distancePos(index1, index2);
		}
		
	}
	
	private static class ReadOnlyWrapper2 implements ReadOnlyDistanceCache {
		
		private CoordinateList mCS;
		private DistanceFunc mDistFunc;
		private double[] mCoordBuf1, mCoordBuf2;
		
		ReadOnlyWrapper2(CoordinateList cs, DistanceFunc distFunc) {
			mCS = cs;
			mDistFunc = distFunc;
			int dim = mCS.getDimensionCount();
			mCoordBuf1 = new double[dim];
			mCoordBuf2 = new double[dim];
		}

		public int getNumIndices() {
			return mCS.getCoordinateCount();
		}
		
		public double getDistance(int index1, int index2) throws IOException {
			mCS.getCoordinates(index1, mCoordBuf1);
			mCS.getCoordinates(index2, mCoordBuf2);
			return mDistFunc.distanceBetween(mCoordBuf1, mCoordBuf2);
		}
		
		public double[] getDistances(int[] indices1, int[] indices2, double[] distances) 
		throws IOException {
			int len = indices1.length;
			if (distances == null || distances.length < len) {
				distances = new double[len];
			}
			for (int i=0; i<len; i++) {
				distances[i] = getDistance(indices1[i], indices2[i]);
			}
			return distances;
		}

		public long getNumDistances() {
			long coordCount = mCS.getCoordinateCount();
			return coordCount*(coordCount - 1L)/2L;
		}
		
		public double getDistance(long n) throws IOException {
			int[] ids = new int[2];
			DistanceCacheFactory.getIndicesForDistance(n, this);
			return getDistance(ids[0], ids[1]);
		}

		public long distancePos(int index1, int index2) {
			if (index1 > index2) { // Swap them
	            index1 ^= index2;
	            index2 ^= index1;
	            index1 ^= index2;
	        }
	        int n = mCS.getCoordinateCount() - index1;
	        return getNumDistances() - n *(n - 1)/2 + index2 - index1 - 1;	
		}
	
	}
	
	public static void main(String[] args) {
		System.out.println("coordinate limit: " + coordinateLimit(128L * 1024L * 1024L));
		System.out.println("coordinate limit: " + coordinateLimit(2L * 1024L * 1024L * 1024L));
	}
}
