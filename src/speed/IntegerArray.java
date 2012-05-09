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
	
	public String print(){
		String str = "{";
		for (int i = 0; i < d.length - 1; i++)
			str += d[i] + ",";
		if (d.length > 0)
			str += d[d.length-1] + "}";
		else
			str += "}";
		return str;
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
