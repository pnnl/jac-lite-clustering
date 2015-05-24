package gov.pnnl.jac.util;

import java.security.SecureRandom;
import java.util.Random;

/**
 * <p>
 * A factory
 * @author D3J923
 *
 */
public final class RandomFactory {

	public enum Quality {
		LOW,
		MEDIUM,
		HIGH,
		CRYPTO
	};
	
	private RandomFactory() {}
	
	public static Random createRandom(Quality quality) {
		return createRandom(quality, System.nanoTime());
	}
	
	public static Random createRandom(Quality quality, long seed) {
		if (quality == null) throw new NullPointerException();
		if (quality == Quality.LOW) {
			return new Random(seed);
		} 
		if (quality == Quality.MEDIUM) {
			return new XORShiftRandom(seed);
		}
		if (quality == Quality.HIGH) {
			return new HighQualityRandom(seed);
		}
		// Must be CRYPTO.  Have to convert the seed
		// to bytes first.
		byte[] bytes = new byte[8];
		for (int i = 0; i < 8; i++) {
		    bytes[i] = (byte) seed;
		    seed >>= 8;
		}
		return new SecureRandom(bytes);
	}
}
