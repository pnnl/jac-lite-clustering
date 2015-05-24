package gov.pnnl.jac.geom;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

public class FSCoordinateListFactory implements CoordinateListFactory {

	private static final Logger LOGGER = Logger.getLogger(FSCoordinateListFactory.class);
	
	public static final long DEFAULT_RAM_THRESHOLD = 256L*1024L*1024L; // 256mb
	public static final long DEFAULT_FILE_THRESHOLD = 4L*DEFAULT_RAM_THRESHOLD; // 1gb
	
	public static final String FILENAME_PREFIX = "coords_";
	public static final String FILENAME_SUFFIX = ".dat";
	public static final String MULTI_DIRECTORY = "multi";
	
	private File mDir;
	private long mRAMThreshold, mFileThreshold;
	
	private Map<String, Object> mCoordListMap = new HashMap<String, Object> ();
	private Object mSingleFileSentinel = new Object();
	private Object mMultiFileSentinel = new Object();
	
	public FSCoordinateListFactory(File dir, long ramThreshold, long fileThreshold)
	throws IOException {
		
		if (dir.exists() && !dir.isDirectory()) {
			throw new IOException("not a directory: " + dir);
		}
		
		if (ramThreshold > fileThreshold) {
			throw new IllegalArgumentException("ramThreshold > fileThreshold");
		}
		
		mDir = dir;
		mRAMThreshold = ramThreshold;
		mFileThreshold = fileThreshold;
		
		if (!mDir.exists()) {
			if (!mDir.mkdir()) {
				throw new IOException("could not create directory: " + mDir.getAbsolutePath());
			}
		}
		
		File multiDir = multiDirectory();
		
		if (!multiDir.exists()) {
			if (!multiDir.mkdir()) {
                throw new IOException("could not create directory for multiple file coordinate lists: " + multiDir.getAbsolutePath());
			}
		} else if (!multiDir.isDirectory()) {
            throw new IOException("file for multiple file coordinate lists exists but is not a directory: " + multiDir.getAbsolutePath());
		}
		
		loadExistingCoordinateListNames();
	}
	
	public FSCoordinateListFactory(File dir) throws IOException {
		this(dir, DEFAULT_RAM_THRESHOLD, DEFAULT_FILE_THRESHOLD);
	}
	
    private void loadExistingCoordinateListNames() throws IOException {
    	
    	File[] singleFiles = mDir.listFiles(new FileFilter() {
    		@Override
    		public boolean accept(File f) {
    			if (f.isFile()) {
    				String name = f.getName();
    				return name.startsWith(FILENAME_PREFIX) && name.endsWith(FILENAME_SUFFIX);
    			}
    			return false;
    		}
    	});
    
    	for (int i=0; i<singleFiles.length; i++) {
    		File f = singleFiles[i];
    		try {
    			if (FileMappedCoordinateList.validateFile(f)) {
    				String fname = f.getName();
    				String tupleName = fname.substring(FILENAME_PREFIX.length(), 
                        fname.length() - FILENAME_SUFFIX.length());
    				mCoordListMap.put(tupleName, mSingleFileSentinel);
    			}
    		} catch (IOException ioe) {
    			LOGGER.error(ioe);
    		}
    	}
    
    	File[] multiDirs = multiDirectory().listFiles();
    	for (int i=0; i<multiDirs.length; i++) {
    		File dir = multiDirs[i];
    		try {
    			if (MultiFileMappedCoordinateList.validateDirectory(dir)) {
    				mCoordListMap.put(dir.getName(), mMultiFileSentinel);
    			}
    		} catch (IOException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
    	}
    }

    public synchronized CoordinateList createCoordinateList(
			String id, 
			int dimensions,
			int coordCount) throws IOException {
    	
		if (id == null) throw new NullPointerException();
		
		if (id.length() == 0) throw new IllegalArgumentException("id must not be blank");
		
		if (mCoordListMap.containsKey(id)) {
			throw new IOException("coordinates already exist for id " + id);
		}
		
		CoordinateList coords = null;
		long dataSize = 8L*coordCount*dimensions;
		if (dataSize <= mRAMThreshold) {
			try {
				coords = new SimpleCoordinateList(dimensions, coordCount);
			} catch (OutOfMemoryError me) {
				// Attempt to recover from the out of memory error and try 
				// a file-based coordinate list instead.
				coords = null;
				System.gc();
				String filename = getFilename(id);
				coords = FileMappedCoordinateList.createNew(new File(mDir, filename), dimensions, coordCount);
			}
		} else if (dataSize <= mFileThreshold) {
			String filename = getFilename(id);
			coords = FileMappedCoordinateList.createNew(new File(mDir, filename), dimensions, coordCount);
		} else {
			String dirname = getDirname(id);
			coords = MultiFileMappedCoordinateList.createNew(new File(mDir, dirname), dimensions, coordCount);
		}
		
		mCoordListMap.put(id, coords);
		
		return coords;
	}

	public File getFileFor(String id) {
		return new File(mDir, getFilename(id));
	}
	
	public synchronized CoordinateList openCoordinateList(String id) throws IOException {
		
		CoordinateList coords = null;
		
		if (!mCoordListMap.containsKey(id)) {
			throw new IOException("coordinates do not exist for id " + id);
		}
		
		Object o = mCoordListMap.get(id);
		
		if (o == mSingleFileSentinel) {
			File f = getFileFor(id);
			if (f.length() <= mRAMThreshold) {
				coords = SimpleCoordinateList.load(f);
			} else {
				coords = FileMappedCoordinateList.openExisting(f);
			}
		} else if (o == mMultiFileSentinel) {
			File dir = new File(mDir, getDirname(id));
			if (dir.isDirectory()) {
				coords = MultiFileMappedCoordinateList.openExisting(dir);
			}
		} else if (o instanceof CoordinateList) {
			coords = (CoordinateList) o;
			if (coords instanceof FileMappedCoordinateList) {
				((FileMappedCoordinateList) coords).openFile();
			} else if (coords instanceof MultiFileMappedCoordinateList) {
				((MultiFileMappedCoordinateList) coords).open();
			}
		}

		if (o != coords) {
			mCoordListMap.put(id, coords);
		}
		
		return coords;
	}
	
	
//	// TODO:  Add more error checking.  Make it handle the case where f is a directory
//	// containing multiple files.
//	public CoordinateList openCoordinateList(File f) throws IOException {
//
//	    CoordinateList coords = null;
//
//	    if (f.exists() && f.isFile()) {
//
//	        DataInputStream dis = null;
//	        try {
//
//	            dis = new DataInputStream(new FileInputStream(f));
//	            int dimensions = dis.readInt();
//	            int coordCount = dis.readInt();
//	            long dataSize = 8L*coordCount*dimensions;
//
//	            dis.close();
//	            dis = null;
//
//	            if (dataSize <= mRAMThreshold) {
//	                dis = new DataInputStream(new BufferedInputStream(new FileInputStream(f)));
//	                coords = SimpleCoordinateList.load(dis);
//	            } else if (dataSize <= mFileThreshold) {
//	                coords = FileMappedCoordinateList.openExisting(f);
//	            }
//
//	        } finally {
//	            if (dis != null) {
//	                try {
//	                    dis.close();
//	                } catch (IOException ioe) {
//	                }
//	            }
//	        }
//	    }
//
//	    return coords;
//	}
	
//	public void saveCoordinateList(String id, CoordinateList coordList) throws IOException {
//		if (coordList instanceof SimpleCoordinateList) {
//			((SimpleCoordinateList) coordList).save(new File(mDir, getFilename(id)));
//		}
//	}

	private String getFilename(final String id) {
		return FILENAME_PREFIX + id + FILENAME_SUFFIX;
	}
	
//	private String getID(final String filename) {
//	    if (filename.startsWith(FILENAME_PREFIX) && filename.endsWith(FILENAME_SUFFIX)) {
//	        return filename.substring(FILENAME_PREFIX.length(), filename.length() - FILENAME_SUFFIX.length());
//	    }
//	    return null;
//	}
	
	private String getDirname(String id) {
		return FILENAME_PREFIX + id;
	}

	public synchronized CoordinateList copyCoordinateList(String id,
			CoordinateList sourceCoordList) throws IOException {
		
		int dimensions = sourceCoordList.getDimensionCount();
		int coordinateCount = sourceCoordList.getCoordinateCount();
		
		CoordinateList coords = createCoordinateList(id, dimensions, coordinateCount);
		
		double[] buffer = new double[dimensions];
		for (int i=0; i<coordinateCount; i++) {
			sourceCoordList.getCoordinates(i, buffer);
			coords.setCoordinates(i, buffer);
		}
		
		return coords;
	}
	
	private File multiDirectory() {
		return new File(mDir, MULTI_DIRECTORY);
	}

	@Override
	public synchronized Set<String> coordinateListNames() {
		return new TreeSet<String> (mCoordListMap.keySet());
	}

	@Override
	public synchronized boolean hasCoordinatesFor(String name) {
		return mCoordListMap.containsKey(name);
	}

	@Override
	public synchronized void deleteCoordinateList(CoordinateList coordList)
			throws IOException {
		
        String id = idAssociatedWithCoordinates(coordList);
        
        if (id == null) {
            throw new IOException("coordinates not associated with this factory");
        }
        
        if (coordList instanceof FileMappedCoordinateList) {
        	FileMappedCoordinateList fmCoordList = (FileMappedCoordinateList) coordList;
        	File f = fmCoordList.getBackingFile();
        	fmCoordList.closeFile();
        	if (!f.delete()) {
        		throw new IOException("could not delete file for coordinates associated with id " + id);
        	}
        } else if (coordList instanceof MultiFileMappedCoordinateList) {
        	MultiFileMappedCoordinateList mfmCoordList = (MultiFileMappedCoordinateList) coordList;
        	File dir = mfmCoordList.getDirectory();
        	mfmCoordList.close();
        	if (!dir.delete()) {
        		throw new IOException("could not delete directory for coordinates associated with id " + id);
        	}
        } else {
        	File f = getFileFor(id);
        	if (f.isFile()) {
        		if (!f.delete()) {
            		throw new IOException("could not delete file for coordinates associated with id " + id);
        		}
        	}
        }
        
        mCoordListMap.remove(id);
	}

	@Override
	public synchronized void closeCoordinateList(CoordinateList coordList)
			throws IOException {

		String id = idAssociatedWithCoordinates(coordList);
        
		if (id == null) {
            throw new IOException("coordinates not associated with this factory");
        }
		
		if (coordList instanceof FileMappedCoordinateList) {
			((FileMappedCoordinateList) coordList).closeFile();
			mCoordListMap.put(id, mSingleFileSentinel);
		} else if (coordList instanceof MultiFileMappedCoordinateList) {
			((MultiFileMappedCoordinateList) coordList).close();
			mCoordListMap.put(id, mMultiFileSentinel);
		} else {
			mCoordListMap.put(id, mSingleFileSentinel);
		}
	}

	@Override
	public synchronized void closeAll() throws IOException {
		Set<String> keys = mCoordListMap.keySet();
		String[] ids = keys.toArray(new String[keys.size()]);
		for (int i=0; i<ids.length; i++) {
			Object o = mCoordListMap.get(ids[i]);
			if (o instanceof FileMappedCoordinateList) {
				((FileMappedCoordinateList) o).closeFile();
				mCoordListMap.put(ids[i], mSingleFileSentinel);
			} else if (o instanceof MultiFileMappedCoordinateList) {
				((MultiFileMappedCoordinateList) o).close();
				mCoordListMap.put(ids[i], mMultiFileSentinel);
			} else if (o instanceof SimpleCoordinateList) {
				mCoordListMap.put(ids[i], mSingleFileSentinel);
			}
		}
	}
	
    // Finds the name associated with the specified tuple list.
    //
    private String idAssociatedWithCoordinates(CoordinateList coords) {
        
            Iterator<Entry<String, Object>> it = mCoordListMap.entrySet().iterator();
            
            while(it.hasNext()) {
                Entry<String, Object> entry = it.next();
                if (entry.getValue() == coords) {
                    return entry.getKey();
                }
            }

            return null;
    }

	@Override
	public synchronized void saveCoordinateList(CoordinateList coordList) throws IOException {
		String id = idAssociatedWithCoordinates(coordList);
        
		if (id == null) {
            throw new IOException("coordinates not associated with this factory");
        }
		
		// The other kinds write their changes to files as you use them. Closing them
		// is sure to save the data.
		if (coordList instanceof SimpleCoordinateList) {
			File backingFile = getFileFor(id);
			((SimpleCoordinateList) coordList).save(backingFile);
		}
	}
}
