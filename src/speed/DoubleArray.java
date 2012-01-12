package speed;

import java.util.Arrays;

public class DoubleArray {
	double[] d;
	
	DoubleArray(int n) {
		d = new double[n];
	}

	DoubleArray(double[] d) {
		this.d = d;
	}
	
	@Override
	public boolean equals(Object that) {
		// same object
		if (this == that)
			return true;

		// not same class
		if (! (that instanceof DoubleArray))
			return false;
		
		DoubleArray aThat = (DoubleArray) that;
		
		// underlying data points to same array?
		if (this.d == aThat.d)
			return true;
		
		// else, do pairwise compare
		return Arrays.equals(this.d, aThat.d);
	}
	
	@Override
	public int hashCode() {
		return Arrays.hashCode(d);
	}
}
