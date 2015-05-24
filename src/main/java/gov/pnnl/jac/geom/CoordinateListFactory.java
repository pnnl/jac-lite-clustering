package gov.pnnl.jac.geom;

import java.io.IOException;
import java.util.Set;

/**
 * <p>Interface which defines a factory for <tt>CoordinateList</tt>s.</p>
 * 
 * @author R. Scarberry
 *
 */
public interface CoordinateListFactory {

	CoordinateList createCoordinateList(String id, int dimensions, int coordCount)
	  throws IOException;
	
	CoordinateList openCoordinateList(String id) throws IOException;
		
	CoordinateList copyCoordinateList(String id, CoordinateList sourceCoordList) throws IOException;
	
	Set<String> coordinateListNames();
	
	boolean hasCoordinatesFor(String name);
	
	void saveCoordinateList(CoordinateList coordList) throws IOException;
	
	void deleteCoordinateList(CoordinateList coordList) throws IOException;
	
	void closeCoordinateList(CoordinateList coordList) throws IOException;
	
	void closeAll() throws IOException;
	
}
