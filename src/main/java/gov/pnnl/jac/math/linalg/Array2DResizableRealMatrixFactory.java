package gov.pnnl.jac.math.linalg;

public class Array2DResizableRealMatrixFactory implements
		ResizableRealMatrixFactory {

	@Override
	public ResizableRealMatrix createResizableMatrix(int rows, int columns) {
		return new Array2DResizableRealMatrix(rows, columns);
	}

}
