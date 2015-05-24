package gov.pnnl.jac.util;

import java.util.Random;
import cern.jet.random.engine.RandomEngine;

/**
 * <p><tt>RandEngine</tt> is a simple extension of the abstract class
 * <tt>cern.jet.random.engine.RandomEngine</tt>.  It uses an instance of
 * <tt>java.util.Random</tt> to generate random values.</p>
 * 
 * @author R. Scarberry
 *
 */
public class RandEngine extends RandomEngine  {

    private static final long serialVersionUID = -6854776099750434811L;
    private Random mRand;
    
    /**
     * Constructor.
     * 
     * @param seed seed for the random number generator.
     */
    public RandEngine(RandomFactory.Quality quality, long seed) {
        mRand = RandomFactory.createRandom(quality, seed);
    }
    
    public RandEngine(long seed) {
    	this(RandomFactory.Quality.HIGH, seed);
    }
    
    /**
     * Contructor which uses the system time to seed the random number generator.
     */
    public RandEngine() {
        this(RandomFactory.Quality.HIGH, System.nanoTime());
    }
    
    /**
     * Returns the next pseudorandom, uniformly distributed {@code int}
     * value from the random number generator's sequence.
     *
     * @return the next pseudorandom, uniformly distributed {@code int}
     *         value from this random number generator's sequence
     */
    public int nextInt() {
        return mRand.nextInt();
    }
    
    /**
     * Returns a pseudorandom, uniformly distributed {@code int} value
     * between 0 (inclusive) and the specified value (exclusive), drawn from
     * this random number generator's sequence.
     *
     * @param n the bound on the random number to be returned.  Must be
     *	      positive.
     * @return the next pseudorandom, uniformly distributed {@code int}
     *         value between {@code 0} (inclusive) and {@code n} (exclusive)
     *         from this random number generator's sequence
     *
     * @throws IllegalArgumentException if n is not positive
     */
    public int nextInt(int n) {
        return mRand.nextInt(n);
    }

}
