package gov.pnnl.jac.util;

import java.util.Random;

/**
 * Uses algorithm from <tt>Numerical Recipes: The Art of Scientific Computing</tt> to
 * give relatively fast, high quality random number generation.  
 * 
 * @author D3J923 (Found on the site http://www.javamex.com/ and adapted.)
 *
 */
public class HighQualityRandom extends Random {

	private static final long serialVersionUID = 1L;

	private long mU;
	private long mV = 4101842887655102017L;
	private long mW = 1;

	public HighQualityRandom() {
		this(System.nanoTime());
	}

	public HighQualityRandom(long seed) {
		setSeed(seed);
	}

	public synchronized void setSeed(long seed) {
		mU = seed ^ mV;
		nextLong();
		mV = mU;
		nextLong();
		mW = mV;
		nextLong();
	}

	public synchronized long nextLong() {
		// LGC, like java.util.Random
		mU = mU * 2862933555777941757L + 7046029254386353087L;
		// xor shift
		mV ^= mV >>> 17;
		mV ^= mV << 31;
		mV ^= mV >>> 8;
		// Multiply-with-carry
		mW = 4294957665L * (mW & 0xffffffff) + (mW >>> 32);
		// xor shift
		long x = mU ^ (mU << 21);
		x ^= x >>> 35;
		x ^= x << 4;
		long ret = (x + mV) ^ mW;
		return ret;
	}

	protected int next(int bits) {
		return (int) (nextLong() >>> (64-bits));
	}
}
