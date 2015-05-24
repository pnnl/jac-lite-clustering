package gov.pnnl.jac.util;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>
 * A medium quality random number generator that uses the XOR shift
 * algorithm to quickly generate random numbers.
 * </p>
 * 
 * @author D3J923
 *
 */
public class XORShiftRandom extends Random {

	private static final long serialVersionUID = 1L;

	private final AtomicLong mSeed;
	
	public XORShiftRandom() {
		this(System.nanoTime());
	}
	
	public XORShiftRandom(long seed) {
		// super() is implicitly called here... Which
		// calls setSeed() before mSeed is initialized.
		mSeed = new AtomicLong(0L);
		setSeed(seed);
	}
	
	public synchronized void setSeed(long seed) {
		// Since mSeed is not initialized when this
		// is first called by the superclass' default constructor,
		// need this null check.
		if (this.mSeed != null) this.mSeed.set(seed);
		// Call the superclass' method, so the haveNextNextGaussian flag
		// is reset.
		super.setSeed(seed);
	}
	
    protected int next(int bits) {
        long oldseed, nextseed;
        AtomicLong seed = this.mSeed;
        do {
        	nextseed = oldseed = seed.get();
        	nextseed ^= (nextseed << 21);
        	nextseed ^= (nextseed >>> 35);
        	nextseed ^= (nextseed << 4);
        } while (!seed.compareAndSet(oldseed, nextseed));
        return (int)(nextseed & ((1L << bits) - 1));
	}
    
}
