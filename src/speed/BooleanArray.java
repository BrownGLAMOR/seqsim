package speed;

import java.util.Arrays;

public class BooleanArray {
	public boolean[] d;
	private int sum;
	
	public BooleanArray(int n) {
		d = new boolean[n];
	}

	public BooleanArray(boolean[] d) {
		this.d = d;
	}
	
	public int getSum() {
		sum = 0;
		for (int i = 0; i < d.length; i++) {
			if (d[i] == true)
				sum++;
		}
		return sum;
	}
	
	@Override
	public boolean equals(Object that) {
		// same object
		if (this == that)
			return true;

		// not same class
		if (! (that instanceof BooleanArray))
			return false;
		
		BooleanArray aThat = (BooleanArray) that;
		
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
	
	// Testing (getSum function)
	public static void main(String args[]){
		boolean[] b0 = new boolean[] {false, true, true};
		BooleanArray b = new BooleanArray(b0);
		System.out.print("b = {");
		for (int i = 0; i < 3; i++)
			System.out.print(b.d[i] + ",");
		System.out.println("}");
		System.out.println("num of winners = " + b.getSum());
		
		// -------- Copying (successful)
		BooleanArray c = new BooleanArray(Arrays.copyOf(b0, b0.length));
		b0[0] = true;
		System.out.println("c.d[0] = " + c.d[0]);
	}
	
}
