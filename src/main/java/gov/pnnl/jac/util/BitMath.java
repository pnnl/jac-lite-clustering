package gov.pnnl.jac.util;

public final class BitMath {

	private BitMath() {}
	
	public static boolean isPowerOf2(int n) {
		return (n & (n - 1)) == 0;
	}
	
	public static int nextHigherPowerOf2(int n) {
		return 1 << (32 - Integer.numberOfLeadingZeros(n));
	}
}
