package gov.pnnl.jac.util;

import java.util.Random;

public class MersenneTwisterRandom extends Random {

	private static final long serialVersionUID = 1L;

	private static final int INDICES = 624;
	
	private int[] mMT = new int[INDICES];
	private int mMTIndex;
	
	public MersenneTwisterRandom() {
		this(System.nanoTime());
	}
	
	public MersenneTwisterRandom(long seed) {
		setSeed(seed);
	}
	
	public synchronized void setSeed(long seed) {
		// Call the superclass' method, so the haveNextNextGaussian flag
		// is reset.
		super.setSeed(seed);
		if (mMT != null) {
			initMT(DataConverter.intsFromBytes(DataConverter.longToBytes(seed)));
		}
	}
	
	private void initMT(int[] seeds) {
		
		// First of all, seed with 19650218.
		mMT[0] = 19650218;
		for (int i=1; i<INDICES; i++) {
			mMT[i] = (1812433253 * (mMT[i-1] ^ (mMT[i-1] >> 30)) + i);
		}
		
		int i=1, j=0;
		for (int k=INDICES; k>0; k--) {
			mMT[i] = ((mMT[i] ^ (mMT[i-1] ^ mMT[i-1] >>> 30) * 1664525) + seeds[j] + j);
			i++; j++;
			if (i == INDICES) {
				mMT[0] = mMT[INDICES-1];
				i = 1;
			}
			if (j == seeds.length) {
				j = 0;
			}
		}
		
		for (int k=INDICES-1; k>0; k--) {
			mMT[i] = ((mMT[i] ^ (mMT[i-1] ^ mMT[i-1] >>> 30) * 1566083941) - i);
			i++;
			if (i == INDICES) {
				i = 1;
				mMT[0] = mMT[INDICES-1];
			}
		}
		
		mMT[0] = 0x80000000;
		mMTIndex = 0;
	}
	
	protected int next(int bits) {
		
		int y = 0;
		
		synchronized (this) {
			if (mMTIndex == 0) {
				generateNumbers();
			}
			y = mMT[mMTIndex++];
			if (mMTIndex == INDICES) {
				mMTIndex = 0;
			}
		}
		
		y ^= (y >>> 11);
		y ^= (y << 7 & 0x9d2c5680);
		y ^= (y << 15 & 0xefc60000);
		y ^= (y >>> 18);
				
		return (y >>> 32 - bits);
	}
	
	private void generateNumbers() {
		int y = 0;
		int i = 0;
		for (i=0; i<227; i++) {
			y = mMT[i] & 0x80000000 | mMT[i+1] & 0x7fffffff;
			mMT[i] = mMT[i+397] ^ y >>> 1;
			if ((y & 0x01) > 0) {
				mMT[i] ^= 0x9908b0df;
			}
		}
		int lim = INDICES - 1;
		for (; i<lim; i++) {
			y = mMT[i] & 0x80000000 | mMT[i+1] & 0x7fffffff;
			mMT[i] = mMT[i-227] ^ y >>> 1;
			if ((y & 0x01) > 0) {
				mMT[i] ^= 0x9908b0df;
			}
		}
		y = mMT[lim] & 0x80000000 | mMT[0] & 0x7fffffff;;
		mMT[lim] = mMT[396] ^ y >>> 1;
		if ((y & 0x01) > 0) {
			mMT[lim] ^= 0x9908b0df;
		}
		mMTIndex = 0;
	}	
}
