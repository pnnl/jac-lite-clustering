package gov.pnnl.jac.geom;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

public class MultiFileMappedCoordinateList
extends AbstractCoordinateList {

    public static final long MAX_BYTES_PER_FILE_DEFAULT = 1024L*1024L*1024L; // 2 GB
    public static final String SUBSET_FILENAME = "_scs_.";
    
    private File mDir;
    private int mCoordsPerFile;//, mCoordsInLastFile;
    private FileMappedCoordinateList[] mCoordinateSets;
    private boolean mOpen;
    
    public static MultiFileMappedCoordinateList createNew(File file, 
            int dimensions, int coordinateCount,
            long maxBytesPerFile) throws IOException {
        return new MultiFileMappedCoordinateList(file, dimensions, coordinateCount, maxBytesPerFile);
    }
    
    public static MultiFileMappedCoordinateList createNew(File file, 
            int dimensions, int coordinateCount) throws IOException {
        return new MultiFileMappedCoordinateList(file, dimensions, coordinateCount, 
                MAX_BYTES_PER_FILE_DEFAULT);
    }
    
    public static MultiFileMappedCoordinateList openExisting(File file)
    throws IOException {
        return new MultiFileMappedCoordinateList(file);
    }    

    private MultiFileMappedCoordinateList(File file, 
            int dimensions, int coordinateCount, 
            long maxBytesPerFile) 
    throws IOException {
        
        if (dimensions < 0) {
            throw new IllegalArgumentException("dimensions < 0: " + dimensions);
        }
        if (coordinateCount < 0) {
            throw new IllegalArgumentException("coordinateCount < 0: " + coordinateCount);
        }
        long bytesPerCoord = 8L*dimensions;
        if (maxBytesPerFile < bytesPerCoord) {
            throw new IllegalArgumentException("maxBytesPerFile < number of bytes necessary for 1 coordinate");
        }
        
        if (file.exists()) {
            if (!file.isDirectory()) {
                throw new IOException("non-directory file exists: " + file);
            }
        } else { // directory doesn't exist, so create it.
            if (!file.mkdir()) {
                throw new IOException("could not create directory: " + file);
            }
        }
        
        mDir = file;
        mCount = coordinateCount;
        mDim = dimensions;
        
        long totalSize = 8L*dimensions*coordinateCount;
        int fileCount = 1;
        int coordsInLastFile = 0;
        if (totalSize > maxBytesPerFile) {
            // How many coordinates can we fit in one file?
            mCoordsPerFile = (int) (maxBytesPerFile/(8L*dimensions));
            fileCount = coordinateCount/mCoordsPerFile;
            int leftOver = coordinateCount%mCoordsPerFile;
            if (leftOver > 0) {
                fileCount++;
                coordsInLastFile = leftOver;
            } else {
                // mCoordsPerFile divided evenly into coordinateCount. All
                // backing files will be of the same length.
                coordsInLastFile = mCoordsPerFile;
            }
        } else { // Everything will fit into one file, so only need one FileMappedCoordinateSet
            mCoordsPerFile = coordinateCount;
            coordsInLastFile = coordinateCount;
        }
        
        int coordSum = 0;
        
        mCoordinateSets = new FileMappedCoordinateList[fileCount];
        
        for (int i=0; i<fileCount-1; i++) {
            mCoordinateSets[i] = FileMappedCoordinateList.createNew(
                    new File(file, SUBSET_FILENAME + i), 
                    dimensions, mCoordsPerFile);
            coordSum += mCoordsPerFile;
        }
        
        mCoordinateSets[fileCount-1] = FileMappedCoordinateList.createNew(
                new File(file, SUBSET_FILENAME + (fileCount-1)),
                dimensions, coordsInLastFile);
        coordSum += coordsInLastFile;
        
        mOpen = true;
        
        assert coordSum == coordinateCount;
    }
    
    private MultiFileMappedCoordinateList(File file) throws IOException {
        if (!file.exists()) {
            throw new FileNotFoundException("not found: " + file);
        } else if (!file.isDirectory()) {
            throw new IOException("not a directory: " + file);
        }

        File[] subFiles = existingCoordinateListFiles(file);

        mDir = file;
        mCount = 0;
        mCoordinateSets = new FileMappedCoordinateList[subFiles.length];
        
        for (int i=0; i<subFiles.length; i++) {
            FileMappedCoordinateList cs = FileMappedCoordinateList.openExisting(subFiles[i]);
            int dimensions = cs.getDimensionCount();
            int coordCount = cs.getCoordinateCount();
            if (i == 0) {
                mDim = dimensions;
                mCoordsPerFile = coordCount;
            } else {
            	if ((i+1) < subFiles.length && coordCount != mCoordsPerFile) {
            		throw new IOException("inconsistent number of coordinates in file: " + subFiles[i]);
            	}
            	if (dimensions != mDim) {
            		throw new IOException("inconsistent number of dimensions in file: " + subFiles[i]);
            	}
            }
            mCount += coordCount;
            mCoordinateSets[i] = cs;
        }
        
        mOpen = true;
    }

    public synchronized File getDirectory() {
    	return mDir;
    }
    
    public synchronized void open() throws IOException {
    	if (!mOpen) {
    		try {
    			for (int i=0; i<mCoordinateSets.length; i++) {
    				mCoordinateSets[i].openFile();
    			}
    			mOpen = true;
    		} finally {
    			if (!mOpen) {
    				for (int i=0; i<mCoordinateSets.length; i++) {
    					if (mCoordinateSets[i].isOpen()) {
    						try {
    							mCoordinateSets[i].closeFile();
    						} catch (IOException ioe) {
    							// Ignore this one.
    						}
    					}
    				}
    			}
    		}
    	}
    }
    
    public synchronized void close() throws IOException {
    	if (mOpen) {
    		for (int i=0; i<mCoordinateSets.length; i++) {
    			mCoordinateSets[i].closeFile();
    		}
    		mOpen = false;
    	}
    }
    
    private static File[] existingCoordinateListFiles(File dir) throws IOException {
    	
    	File[] files = dir.listFiles(new FileFilter() {
    		public boolean accept(File f) {
    			if (f.isFile()) {
    				String name = f.getName();
    				if (name.startsWith(SUBSET_FILENAME)) {
    					String suffix = name.substring(SUBSET_FILENAME.length());
    					try {
    						int seqno = Integer.parseInt(suffix);
    						if (seqno >= 0) {
    							return true;
    						}
    					} catch (NumberFormatException nfe) {
    					}
    				}
    			}
    			return false;
    		}
    	});
    	
    	final int prefixLen = SUBSET_FILENAME.length();
    	
        Arrays.sort(files, new Comparator<File>() {
            public int compare(File f1, File f2) {
                String fname1 = f1.getName();
                String fname2 = f2.getName();
                int v1 = Integer.parseInt(fname1.substring(prefixLen));
                int v2 = Integer.parseInt(fname2.substring(prefixLen));
                return v1 < v2 ? -1 : (v1 > v2 ? 1 : 0);
            }
        });
        
        return files;
    }
    
    private long transformNdx(int ndx) {
        long whichCoordSet = 0, coordSetNdx = ndx;
        if (ndx >= mCoordsPerFile) {
            whichCoordSet = ndx/mCoordsPerFile;
            coordSetNdx = ndx%mCoordsPerFile;
        }
        return (whichCoordSet << 32)|coordSetNdx;
    }
    
    /**
     * Set the coordinate values for the coordinate with the
     * specified index.
     * @param ndx - the coordinate index which must be in the range
     *   <code>[0 - getCoordinateCount()-1]</code>.
     * @param coords - the coordinate values.
     * @throws IndexOutOfBoundsException - if <code>ndx</code> is out of range.
     * @throws IllegalArgumentException - if <code>coords</code> is not
     *   of length <code>getDimensions()</code>.
     */
    public synchronized void setCoordinates(int ndx, double[] coords) {
        checkIndex(ndx);
        checkDimensions(coords.length);
        checkOpen();
        long tndx = transformNdx(ndx);
        mCoordinateSets[(int)(tndx>>32)].setCoordinates((int)(tndx & 0xffffffff), coords);
    }

    public synchronized void setCoordinateQuick(int ndx, int dim, double coord) {
        checkOpen();
    	long tndx = transformNdx(ndx);
        mCoordinateSets[(int)(tndx>>32)].setCoordinate((int)(tndx & 0xffffffff), dim, coord);
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
     * @throws IndexOutOfBoundsException - if <code>ndx</code> 
     *   is not in the valid range.
     * @throws IllegalArgumentException - if the array passed in is
     *   non-null but of incorrect length.  
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
        long tndx = transformNdx(ndx);
        return mCoordinateSets[(int)(tndx>>32)].getCoordinates((int)(tndx & 0xffffffff), c);
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
    	long tndx = transformNdx(ndx);
        return mCoordinateSets[(int)(tndx>>32)].getCoordinateQuick((int)(tndx & 0xffffffff), dim);
    }

    /**
     * Computes and average coordinate vector for a number of indices.
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
        checkOpen();
        checkIndices(indices);
        double[] rtn = null;
        if (avg != null) {
            checkDimensions(avg.length);
            rtn = avg;
        } else {
            rtn = new double[mDim];
        }
        
        java.util.Arrays.fill(rtn, 0.0);
        int[] counts = new int[mDim];

        int n = indices.length;
        double[] coordBuffer = new double[mDim];
        
        for (int i = 0; i < n; i++) {
            getCoordinates(indices[i], coordBuffer);
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
    
    public static boolean validateDirectory(File directory) throws IOException {
    	
        File[] subFiles = existingCoordinateListFiles(directory);

        final int n = subFiles.length;
     
        if (n == 0) {
        	return false;
        }
        
        int expectedDim = 0;
        int expectedCoordCount = 0;
        
        for (int i=0; i<subFiles.length; i++) {
        	
	        DataInputStream in = null;
	    
	        int coordLen = 0;
	        int coordCount = 0;
	        
	        try {
	            in = new DataInputStream(new FileInputStream(subFiles[i]));
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
            
            if (subFiles[i].length() != expectedFileLen) {
            	return false;
            }
            
            if (i == 0) {
            	expectedDim = coordLen;
            	expectedCoordCount = coordCount;
            } else {
            	if ((i+1) < subFiles.length) {
            		if (coordCount != expectedCoordCount) {
            			return false;
            		}
            	}
            	if (coordLen != expectedDim) {
            		return false;
            	}
            }

        }
     
        return true;     
    }
    
    // Ensures file is open.
    private void checkOpen() {
    	if (!isOpen()) {
    		throw new IllegalStateException("not open");
    	}
    }
    
    public synchronized boolean isOpen() {
    	return mOpen;
    }
    
    /**
     * {@inheritDoc}
     */
    public synchronized double getCoordinate(int ndx, int dim) {
    	return super.getCoordinate(ndx, dim);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void setCoordinate(int ndx, int dim, double coord) {
    	super.setCoordinate(ndx, dim, coord);
    }
    
    /**
     * {@inheritDoc}
     */
    public synchronized double[] getDimensionValues(int dim, double[] values) {
    	return super.getDimensionValues(dim, values);
    }
    
    /**
     * {@inheritDoc}
     */
    public synchronized double computeAverage(int[] indices, int dim) {
    	return super.computeAverage(indices, dim);
    }

    /**
     * {@inheritDoc}
     */
	public synchronized double[] computeMinimum(int[] indices, double[] min) {
		return super.computeMinimum(indices, min);
	}

    /**
     * {@inheritDoc}
     */
	public synchronized double computeMinimum(int[] indices, int dim) {
		return super.computeMinimum(indices, dim);
	}

    /**
     */
	public synchronized double[] computeMaximum(int[] indices, double[] max) {
		return super.computeMaximum(indices, max);
	}

    /**
     * {@inheritDoc}
     */
	public synchronized double computeMaximum(int[] indices, int dim) {
		return super.computeMaximum(indices, dim);
	}

    /**
     * {@inheritDoc}
     */
	public synchronized double[] computeMedian(int[] indices, double[] med) {
		return super.computeMedian(indices, med);
	}

    /**
     * {@inheritDoc}
     */
	public synchronized double computeMedian(int[] indices, int dim) {
		return super.computeMedian(indices, dim);
	}

}
