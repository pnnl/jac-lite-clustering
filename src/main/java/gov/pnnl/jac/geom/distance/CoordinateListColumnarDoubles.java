package gov.pnnl.jac.geom.distance;

import gov.pnnl.jac.geom.CoordinateList;

public class CoordinateListColumnarDoubles implements ColumnarDoubles {

    private CoordinateList mCoordinates;
    
    public CoordinateListColumnarDoubles(CoordinateList coordinates) {
        if (coordinates == null) throw new NullPointerException();
        mCoordinates = coordinates;
    }
    
    public int getColumnCount() {
        return mCoordinates.getDimensionCount();
    }

    public double[] getColumnValues(int column, double[] buffer) {
        return mCoordinates.getDimensionValues(column, buffer);
    }

    public int getRowCount() {
        return mCoordinates.getCoordinateCount();
    }

}
