package gov.pnnl.jac.cluster.similarity;

/**
 * Defines a container of pairwise similarity measures between a number of entities.
 * 
 * @author D3J923
 *
 */
public interface Similarities {

    /**
     * Returns the number of entities for which pairwise similarities are maintained.
     * 
     * @return a nonnegative integer.
     */
    int getRecordCount();
    
    /**
     * Returns the maximum similarity possible, generally the same as
     * the similarities between identical records (or each record and itself).
     * 
     * @return the maximum value, or NaN, if no maximum is defined.
     */
    double getMaxPossible();
    
    /**
     * Returns the minimum similarity possible, generally but not necessarily 0.0.
     * 
     * @return the minimum value, or NaN, if no minimum is defined.
     */
    double getMinPossible();
    
    /**
     * Returns the similarity between the specified records.
     * @param i
     * @param j
     * 
     * @return the similarity measure.
     * 
     * @throws IndexOutOfBoundException if i or j are out of range.
     */
    double getSimilarity(int i, int j);
    
}
