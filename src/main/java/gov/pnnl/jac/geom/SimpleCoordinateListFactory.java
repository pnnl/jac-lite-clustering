package gov.pnnl.jac.geom;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

public class SimpleCoordinateListFactory implements CoordinateListFactory {
	
	private Map<String, CoordinateList> mCoordListMap = new HashMap<String, CoordinateList> ();
	
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

	public synchronized CoordinateList createCoordinateList(String id, int dimensions,
			int coordCount) throws IOException {
		CoordinateList coords = new SimpleCoordinateList(dimensions, coordCount);
		mCoordListMap.put(id, coords);
		return coords;
	}

	public synchronized CoordinateList openCoordinateList(String id) throws IOException {
		return mCoordListMap.get(id);
	}

	public synchronized void saveCoordinateList(String id, CoordinateList coordList)
			throws IOException {
		mCoordListMap.put(id, coordList);
	}

	@Override
	public synchronized Set<String> coordinateListNames() {
		return new TreeSet<String>(mCoordListMap.keySet());
	}

	@Override
	public synchronized boolean hasCoordinatesFor(String name) {
		return mCoordListMap.containsKey(name);
	}

	@Override
	public synchronized void saveCoordinateList(CoordinateList coordList) throws IOException {
		String id = idAssociatedWithCoordinates(coordList);
		if (id == null) {
			throw new IOException("coordinates are not associated with this factory");
		}
	}

	@Override
	public synchronized void deleteCoordinateList(CoordinateList coordList)
			throws IOException {
		String id = idAssociatedWithCoordinates(coordList);
		if (id == null) {
			throw new IOException("coordinates are not associated with this factory");
		}
		mCoordListMap.remove(id);
	}

	@Override
	public synchronized void closeCoordinateList(CoordinateList coordList)
			throws IOException {
		String id = idAssociatedWithCoordinates(coordList);
		if (id == null) {
			throw new IOException("coordinates are not associated with this factory");
		}
	}

	@Override
	public synchronized void closeAll() throws IOException {
		// noop
	}
	
    // Finds the name associated with the specified tuple list.
    //
    private String idAssociatedWithCoordinates(CoordinateList coords) {
        
            Iterator<Entry<String, CoordinateList>> it = mCoordListMap.entrySet().iterator();
            
            while(it.hasNext()) {
                Entry<String, CoordinateList> entry = it.next();
                if (entry.getValue() == coords) {
                    return entry.getKey();
                }
            }

            return null;
    }
}
