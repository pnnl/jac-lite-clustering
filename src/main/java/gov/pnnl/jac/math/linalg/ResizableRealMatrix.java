package gov.pnnl.jac.math.linalg;

import org.apache.commons.math3.linear.RealMatrix;

public interface ResizableRealMatrix extends RealMatrix {

	void insertRow(int row);
	
	void removeRow(int row);
	
	void insertColumn(int column);
	
	void removeColumn(int column);
	
}
