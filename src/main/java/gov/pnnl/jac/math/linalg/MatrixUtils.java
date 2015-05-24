package gov.pnnl.jac.math.linalg;

import java.util.Arrays;

import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import cern.colt.matrix.*;
import cern.jet.stat.Descriptive;

/**
 * <p>Contains static utility methods for performing operations on matrices from
 * the colt library.
 * </p>
 * 
 * @author d3j923
 *
 */
public final class MatrixUtils {

    private MatrixUtils() {}
   
    /**
     * Constructs 2-dimensional matrix by repeating the specified column matrix.
     * 
     * @param column the 1-dimensional column matrix.
     * @param count the number of columns for the resulting matrix.
     * 
     * @return a 2-dimensional matrix.
     */
    public static DoubleMatrix2D replicateColumn(DoubleMatrix1D column, int count) {
        int nrows = column.size();
        int ncols = count;
        DoubleMatrix2D matrix = DoubleFactory2D.dense.make(nrows, ncols);
        for (int i=0; i<ncols; i++) {
            matrix.viewColumn(i).assign(column);
        }
        return matrix;
    }
    
    /**
     * Constructs a 2-dimensional matrix by repeating the specified row matrix.
     * 
     * @param row
     * @param count
     * @return
     */
    public static DoubleMatrix2D replicateRow(DoubleMatrix1D row, int count) {
        int nrows = count;
        int ncols = row.size();
        DoubleMatrix2D matrix = DoubleFactory2D.dense.make(nrows, ncols);
        for (int i=0; i<nrows; i++) {
            matrix.viewRow(i).assign(row);
        }
        return matrix;
    }
    
    /**
     * Returns a 1-dimensional matrix the same length as the input matrix, 
     * whose elements are the cumulative sums of the input matrix.  For example,
     * if the input matrix is [1, 2, 3, 4], the result matrix is [1, 1+2=3, 1+2+3=6, 1+2+3+4=10]
     * 
     * @param input
     * @return
     */
    public static DoubleMatrix1D cumulativeSums(DoubleMatrix1D input) {
        DoubleMatrix1D output = input.like();
        double sum = 0.0;
        int sz = input.size();
        for (int i=0; i<sz; i++) {
            sum += input.get(i);
            output.set(i, sum);
        }
        return output;
    }
    
    /**
     * Returns the mean of the elements in the specified 
     * 1-dimensional matrix. NaN elements are excluded from
     * the computation.
     * 
     * @param vector
     * @return
     */
    public static double mean(DoubleMatrix1D vector) {
        double mean = Double.NaN;
        int len = vector.size();
        if (len > 0) {
                double sum = 0.0;
                int n = 0;
                for (int i=0; i<len; i++) {
                        double v = vector.get(i);
                        if (!Double.isNaN(v)) {
                                sum += v;
                                n++;
                        }
                }
                if (n > 0) {
                        mean = sum/n;
                }
        }
        return mean;
    }
    
    public static double mean(RealVector vector) {
        double mean = Double.NaN;
        int len = vector.getDimension();
        if (len > 0) {
                double sum = 0.0;
                int n = 0;
                for (int i=0; i<len; i++) {
                        double v = vector.getEntry(i);
                        if (!Double.isNaN(v)) {
                                sum += v;
                                n++;
                        }
                }
                if (n > 0) {
                        mean = sum/n;
                }
        }
        return mean;
    }
    
    public static int hash(DoubleMatrix2D matrix) {
    	
    	final int rows = matrix.rows();
    	final int cols = matrix.columns();
    	
    	int hc = 31*rows + cols;
    	
    	double sum = 0.0;
    	for (int i=0; i<rows; i++) {
    		for (int j=0; j<cols; j++) {
    			sum += matrix.get(i, j);
    		}
    	}
    	
    	sum /= rows*cols;
    	
    	if (sum > 0.0) {
    		for (int i=0; i<rows; i++) {
    			for (int j=0; j<cols; j++) {
    				hc = 31*hc + (int) (1000.0 * matrix.get(i, j)/sum);
    			}
    		}
    	}
    	
    	return hc;
    }
    
    public static int hash(RealMatrix matrix) {
    	
    	final int rows = matrix.getRowDimension();
    	final int cols = matrix.getColumnDimension();
    	
    	int hc = 31*rows + cols;
    	
    	double sum = 0.0;
    	for (int i=0; i<rows; i++) {
    		for (int j=0; j<cols; j++) {
    			sum += matrix.getEntry(i, j);
    		}
    	}
    	
    	sum /= rows*cols;
    	
    	if (sum > 0.0) {
    		for (int i=0; i<rows; i++) {
    			for (int j=0; j<cols; j++) {
    				hc = 31*hc + (int) (1000.0 * matrix.getEntry(i, j)/sum);
    			}
    		}
    	}
    	
    	return hc;
    }

    /**
     * Returns a 1-dimensional matrix the same length as the input matrix, but
     * whose elements are replaced by their average ranks when the elements are
     * in sorted order.
     * 
     * @param vector
     * @return
     */
    public static DoubleMatrix1D ranks(DoubleMatrix1D vector) {
        
        int len = vector.size();
        
        double[] sortedValues = vector.toArray();
        
        Arrays.sort(sortedValues);
        
        double[] ranks = new double[len];
        
        int n = 0;
        while(n < len) {
            double v = sortedValues[n];
            int count = 1;
            double rank = n;
            int m = n + 1;
            while(m < len && Double.doubleToLongBits(v) == Double.doubleToLongBits(sortedValues[m])) {
                count++;
                rank += m;
                m++;
            }
            rank /= count;
            for (int i=0; i<count; i++) {
                ranks[n+i] = rank;
            }
            n += count;
        }
                
        DoubleMatrix1D rankVector = DoubleFactory1D.dense.make(len);
        for (int i=0; i<len; i++) {
            int ndx = Arrays.binarySearch(sortedValues, vector.get(i));
            rankVector.set(i, ranks[ndx]);
        }
        
        return rankVector;
    }
    
    public static double variance(DoubleMatrix1D vector) {
        double variance = 0.0;
        int len = vector.size();
        if (len > 0) {
            int n = 0;
            double sum = 0.0, sumOfSquares = 0.0;
            for (int i=0; i<len; i++) {
                double v = vector.get(i);
                if (!Double.isNaN(v)) {
                    sum += v;
                    sumOfSquares += v*v;
                    n++;
                }
            }
            if (n > 0) {
                variance = Descriptive.variance(n, sum, sumOfSquares);
            }
        }
        return variance;
    }
    
    public static double sum(DoubleMatrix1D vector, boolean ignoreNaNs) {
        double sum = 0.0;
        int len = vector.size();
        for (int i=0; i<len; i++) {
            double v = vector.get(i);
            if (!Double.isNaN(v)) {
                sum += v;
            } else if (!ignoreNaNs) {
                return Double.NaN;
            }
        }
        return sum;
    }
    
    public static DoubleMatrix1D rowSums(DoubleMatrix2D matrix, boolean ignoreNaNs) {
        int rows = matrix.rows();
        DoubleMatrix1D sums = DoubleFactory1D.dense.make(rows);
        for (int i=0; i<rows; i++) {
            sums.set(i, sum(matrix.viewRow(i), ignoreNaNs));
        }
        return sums;
    }

    public static DoubleMatrix1D columnSums(DoubleMatrix2D matrix, boolean ignoreNaNs) {
        int cols = matrix.columns();
        DoubleMatrix1D sums = DoubleFactory1D.dense.make(cols);
        for (int i=0; i<cols; i++) {
            sums.set(i, sum(matrix.viewColumn(i), ignoreNaNs));
        }
        return sums;
    }
    
}
