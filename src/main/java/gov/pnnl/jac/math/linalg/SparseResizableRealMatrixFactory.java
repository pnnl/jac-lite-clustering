package gov.pnnl.jac.math.linalg;

public class SparseResizableRealMatrixFactory implements
		ResizableRealMatrixFactory {

	@Override
	public ResizableRealMatrix createResizableMatrix(int rows, int columns) {
		return new OpenLongMapRealMatrix(rows, columns);
	}

}
