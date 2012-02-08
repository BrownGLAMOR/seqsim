package speed;

import java.util.Arrays;

public class IntegerArray {
	public int[] d;
	
	public IntegerArray(int n) {
		d = new int[n];
	}

	public IntegerArray(int[] d) {
		this.d = d;
	}
	
	@Override
	public boolean equals(Object that) {
		// same object
		if (this == that)
			return true;

		// not same class
		if (! (that instanceof IntegerArray))
			return false;
		
		IntegerArray aThat = (IntegerArray) that;
		
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