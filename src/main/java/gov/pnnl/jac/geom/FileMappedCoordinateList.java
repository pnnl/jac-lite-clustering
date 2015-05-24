package gov.pnnl.jac.geom;

import gov.pnnl.jac.util.DataConverter;
import java.io.*;

/**
 * <p><tt>FileMappedCoordinateList</tt> is an implementation of
 * <tt>CoordinateList</tt> backed by a single file of data. Use this
 * class when the coordinate data may be too much to store in 
 * memory, but is small enough to store in a single file.
 * </p>
 * <p>The data is stored to disk exactly as <tt>SimpleCoordinateList</tt>
 * stores its data in memory.  Therefore, a <tt>SimpleCoordinateList</tt>
 * instance may be instantiated from the same file as a
 * <tt>FileMappedCoordinateList</tt> as long as sufficient memory exists.
 * </p>
 * 
 * @author d3j923
 *
 */
public final class FileMappedCoordinateList extends AbstractCoordinateList {

	private File mBackingFile;
    private RandomAccessFile mRAF;
    private byte[] mIOBuffer;
    
	/**
	 * Factory method for creating a new FileMappedCoordinateSet.
	 * 
	 * @param file - the backing file, which will be overwritten if
	 *   it already exists.
	 * @param dimensions - the number of dimensions in each
	 *   coordinate.
	 * @param coordinateCount - the number of coordinates.
	 * @return - an instance of FileMappedCoordinateSet.
	 * @throws IOException - if an IO error occurs while trying to
	 *   create the file.
	 * @throws IllegalArgumentException - if either dimensions or
	 *   coordinateCount are negative.
	 * 
	 */
	public static FileMappedCoordinateList createNew(File file, 
			int dimensions, int coordinateCount) throws IOException {
		return new FileMappedCoordinateList(file, dimensions, coordinateCount);
	}
	
	/**
	 * Factory method for instantiating a FileMappedCoordinateSet
	 * from an existing file containing coordinate data.
	 * 
	 * @param file - the backing file, which should already exist.
	 * @return - an instance of FileMappedCoordinateSet.
	 * @throws IOException - if an IO error occurs while trying to
	 *   open the file.  This could occur if the file does not
	 *   contain coordinate data.
	 */
	public static FileMappedCoordinateList openExisting(File file)
	throws IOException {
		return new FileMappedCoordinateList(file);
	}
	
	/**
	 * Constructor for creating a new FileMappedCoordinateSet.  Private, so the
	 * more aptly-named factory method createNew must be used instead.
	 * @param file
	 * @param dimensions
	 * @param coordinateCount
	 * @throws IOException
	 */
	private FileMappedCoordinateList(File file, int dimensions, int coordinateCount) 
	throws IOException {
		if (dimensions < 0) {
			throw new IllegalArgumentException("dimensions < 0: " + dimensions);
		}
		if (coordinateCount < 0) {
			throw new IllegalArgumentException("coordinateCount < 0: " + coordinateCount);
		}
        mDim = dimensions;
        mCount = coordinateCount;
		// Create the file and make it the proper length.
		initEmptyFile(file, dimensions, coordinateCount);
		mBackingFile = file;
		// Now open the file.
		openFile();
	}
	
	/**
	 * Constructor for instantiating a FileMappedCoordinateSet from and
	 * existing data file.  Private, so the factory method openExisting must
	 * be used instead.
	 * @param file
	 * @throws IOException
	 */
	private FileMappedCoordinateList (File file) throws IOException {
		if (!file.exists()) {
			throw new FileNotFoundException("not found: " + file);
		}
		mBackingFile = file;
		openFile();
	}
	
	/**
	 * Ensures the backing file is closed.
	 */
	protected void finalize() {
		if (isOpen()) {
			try {
				closeFile();
			} catch (IOException ioe) {
			}
		}
	}
	
	/**
	 * Returns the file in which the data is stored.
	 * @return
	 */
	public File getBackingFile() {
		return mBackingFile;
	}
	
	/**
	 * Is the backing file for this coordinate set open?
	 * @return 
	 */
	public synchronized boolean isOpen() {
		return mRAF != null;
	}
	
	/**
	 * Check to see whether or not a appears to be a valid coordinate list file.
	 * 
	 * @param f
	 * @return
	 * @throws IOException
	 */
	public static boolean validateFile(File f) throws IOException {
		if (f.isFile()) {
	        DataInputStream in = null;
	        int coordLen = 0;
	        int coordCount = 0;
	        try {
	            in = new DataInputStream(new FileInputStream(f));
	            coordLen = in.readInt();
	            coordCount = in.readInt();
	        } finally {
	            if (in != null) {
	                try {
	                    in.close();
	                } catch (IOException e) {
	                }
	            }
	        }
            long expectedFileLen = 8L*((long) coordLen) * coordCount;
            return f.length() == expectedFileLen;
		}
		return false;
	}
	
	/**
	 * Open the backing file.  If already open, no action is
	 * taken.  The file is open in read-write mode.  Normally,
	 * you should not need to call this method, since both
	 * factory methods return instances of FileMappedCoordinateSet
	 * in the open condition.
	 * @throws IOException - if an IO error occurs.
	 */
	public synchronized void openFile() throws IOException {
		if (!isOpen()) {
			boolean ok = false;
			try {
				mRAF = new RandomAccessFile(mBackingFile, "rw");
				mDim = mRAF.readInt();
				mCount = mRAF.readInt();
                int bytesPerCoord = mDim*8;
				if (mBackingFile.length() != 8L + (long)mCount * bytesPerCoord) {
					throw new IOException("improper file format");
				}
				mIOBuffer = new byte[bytesPerCoord];
				ok = true;
			} finally {
				if (!ok) {
					try {
						closeFile();
					} catch (IOException ioe) {
                        ioe.printStackTrace();
					}
				}
			}
		}
	}
	
	/**
	 * Close the backing file, if open.  You should normally 
	 * call this method after you no longer need to use the
	 * coordinate set.  The coordinate set, however, may be 
	 * used again by calling openFile(). 
	 * @throws IOException
	 */
	public synchronized void closeFile() throws IOException {
		if (isOpen()) {
			try {
                mRAF.close();
			} finally {
                mRAF = null;
                mIOBuffer = null;
			}
		}
	}

	/**
	 * Creates a new file of the correct size packed with zero values.
	 * @param file
	 * @param dimensions
	 * @param coordinateCount
	 * @throws IOException
	 */
	private static void initEmptyFile(File file, int dimensions, int coordinateCount) 
	throws IOException {
		DataOutputStream dos = null;
		try {
			dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
			dos.writeInt(dimensions);
			dos.writeInt(coordinateCount);
			for (int coord=0; coord<coordinateCount; coord++) {
				for (int dim=0; dim<dimensions; dim++) {
					dos.writeDouble(0.0);
				}
			}
			dos.flush();
		} finally {
			if (dos != null) {
				try {
					dos.close();
				} catch (IOException ioe) {
				}
			}
		}
	}
    
	// Computes the file position for the coordinate data
	// with the given index.
    private long filePos(int ndx) {
        return 8L + 8L* mDim * ndx;
    }
	
    /**
     * Set the coordinate values for the coordinate with the
     * specified index.
     * @param ndx - the coordinate index which must be in the range
     *   <code>[0 - getCoordinateCount()-1]</code>.
     * @param coords - the coordinate values.
     * @throws IllegalStateException - if the backing file is not open.
     */
    public synchronized void setCoordinates(int ndx, double[] coords) {
    	checkIndex(ndx);
		checkDimensions(coords.length);
		checkOpen();
		DataConverter.toBytes(coords, 0, mIOBuffer, 0, mDim);
        try {
            mRAF.seek(filePos(ndx));
            mRAF.write(mIOBuffer);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * Retrieve the coordinate values for the coordinate with
     * the specified index.  
     * @param ndx - the coordinate index which must be in the range
     *   <code>[0 - getCoordinateCount()-1]</code>.
     * @param coords - an array to hold the returned coordinates. 
     *   If non-null, must be of length <code>getDimensions()</code>.
     *   If null, a new array is allocated and returned with the values.
     * @return - the array containing the values, which will be the
     *   same as the second argument if that argument is non-null.
     * @throws IllegalArgumentException - if the array passed in is
     *   non-null but of incorrect length.  Also, if <code>ndx</code> 
     *   is not in the valid range.
     * @throws IllegalStateException - if the backing file is not open.
     */
    public synchronized double[] getCoordinates(int ndx, double[] coords) {
		checkIndex(ndx);
		checkOpen();
		double[] c = null;
		if (coords != null) {
			checkDimensions(coords.length);
			c = coords;
		} else {
			c = new double[mDim];
		}
        try {
            mRAF.seek(filePos(ndx));
            mRAF.read(mIOBuffer);
            DataConverter.fromBytes(mIOBuffer, c);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return c;
    }
    
    /**
     * Retrieves a column of coordinate data for the specified dimension.
     * 
     * @param dim - the dimension of data to retrieve, which must be
     *   in the range <code>[0 - getDimensions() - 1]</code>.
     * @param values - an array to hold the values for the dimension.
     *   If non-null, must be of length <code>getCoordinateCount()</code>.
     *   If null, a new array is allocated and returned containing the values.
     * @return - the array containing the values, which will be the same
     *   as the second argument if that argument is non-null.
     * @throws IndexOutOfBoundsException - if dim is out of range.
     * @throws IllegalArgumentException - if values is
     *   non-null and of improper length.
     */
    public synchronized double[] getDimensionValues(int dim, double[] values) {
		checkDimension(dim);
		checkOpen();
		double[] v = null;
		if (values != null) {
			if (values.length != mCount) {
				throw new IllegalArgumentException(String
						.valueOf(values.length)
						+ " != " + mCount);
			}
			v = values;
		} else {
			v = new double[mCount];
		}
		int ndx = dim;
        try {
            mRAF.seek(filePos(ndx));
            int bytesToSkip = (mDim-1) * 8;
            for (int i = 0; i < mCount-1; i++) {
                v[i] = mRAF.readDouble();
                mRAF.skipBytes(bytesToSkip);
            }
            v[mCount-1] = mRAF.readDouble();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
		return v;    	
    }

    /**
     * Retrieve the value for the given index and dimension.
     * @param ndx - the coordinate index which must be in the range
     *   <code>[0 - getCoordinateCount()-1]</code>.
     * @param dim - the dimension for the value, which must be
     *   in the range <code>[0 - getDimensions() - 1]</code>.
     * @return - the value.
     * @throws IndexOutOfBoundException - either argument if 
     *   out of range.
     */
    public double getCoordinate(int ndx, int dim) {
		checkIndex(ndx);
		checkDimension(dim);
		return getCoordinateQuick(ndx, dim);
    }
    
    /**
     * Identical to <code>getCoordinate(ndx, dim)</code>, but
     * bounds checking is not performed on the arguments.  This
     * method is mandated by the interface, so other methods  
     * can retrieve coordinates in loops without having 
     * redundant bounds checking performed on every iteration.  
     * Do not call this method directly unless you are sure the
     * arguments are in range.  If they are not, the behavior is
     * determined by the implementation class.  
     * @param ndx - the coordinate index which must be in the range
     *   <code>[0 - getCoordinateCount()-1]</code>.
     * @param dim - the dimension for the value, which must be
     *   in the range <code>[0 - getDimensions() - 1]</code>.
     * @return - the value.
     */
    public synchronized double getCoordinateQuick(int ndx, int dim) {
    	checkOpen();
        double v = 0.0;
        try {
            mRAF.seek(filePos(ndx) + 8L*dim);
            v = mRAF.readDouble();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return v;
    }

    /**
     * Computes the average coordinate vector for a number of indices.
     * 
     * @param indices - an array containing the indices of the
     *   coordinates to be averaged.
     * @param avg - an array to hold the computed averages.  If 
     *   non-null, it must be of length <code>getDimensions()</code>.
     *   If null, a new array is allocated and returned with the 
     *   computed averages.
     * @return - an array containing the computed averages.
     */
    public synchronized double[] computeAverage(int[] indices, double[] avg) {
		checkIndices(indices);
		checkOpen();
		double[] rtn = null;
		if (avg != null) {
			checkDimensions(avg.length);
			rtn = avg;
		} else {
			rtn = new double[mDim];
		}
		java.util.Arrays.fill(rtn, 0.0);
		int[] counts = new int[mDim];
		double[] coordBuffer = new double[mDim];
		int n = indices.length;
		for (int i = 0; i < n; i++) {
			int ndx = indices[i];
            getCoordinates(ndx, coordBuffer);
			for (int d = 0; d < mDim; d++) {
				double dv = coordBuffer[d];
				if (!Double.isNaN(dv)) {
					rtn[d] += dv;
					counts[d]++;
				}
			}
		}
		for (int d = 0; d < mDim; d++) {
			int ct = counts[d];
			if (ct >= 1) {
				rtn[d] /= ct;
			} else {
				// No information in dimension d.
				rtn[d] = Double.NaN;
			}
		}
		return rtn;
    }
    
    /**
     * Identical to <code>setCoordinate(ndx, dim, coord)</code>, but
     * bounds checking is not performed on the arguments.  This
     * method is mandated by the interface, so other methods  
     * can quickly set values in loops without having 
     * redundant bounds checking performed on every iteration.  
     * Do not call this method directly unless you are sure the
     * arguments are in range.  If they are not, the behavior is
     * determined by the implementation class.  
     * @param ndx the index of the coordinate.
     * @param dim the dimension to be set.
     * @param coord the value to be applied.
     * 
     * @throws IndexOutOfBoundsException if either ndx or dim is out of range.
     */
    public synchronized void setCoordinateQuick(int ndx, int dim, double coord) {
    	checkOpen();
        try {
            mRAF.seek(filePos(ndx) + 8L*dim);
            mRAF.writeDouble(coord);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    // Ensures file is open.
    private void checkOpen() {
    	if (!isOpen()) {
    		throw new IllegalStateException("not open");
    	}
    }
}
