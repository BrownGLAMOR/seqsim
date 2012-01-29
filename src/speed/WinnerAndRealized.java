package speed;

import java.util.Arrays;

public class WinnerAndRealized {
	public boolean[] w;
	public int[] d;
	
	public WinnerAndRealized(int n) {
		w = new boolean[n];			// Winner
		d = new int[n];				// realized prices (binned)
	}

	public WinnerAndRealized(boolean[] w, int[] d) {
		if (w.length != d.length)
			System.out.println("Error: w.length has to equal d.length!");
		this.w = w;
		this.d = d;
	}
	
	@Override
	public boolean equals(Object that) {
		// same object
		if (this == that)
			return true;

		// not same class
		if (! (that instanceof WinnerAndRealized))
			return false;
		
		WinnerAndRealized aThat = (WinnerAndRealized) that;
		
		// underlying data points to same array?
		if (this.d == aThat.d && this.w == aThat.w)
			return true;
		
		// else, do pairwise compare
		return (Arrays.equals(this.w, aThat.w) && Arrays.equals(this.d, aThat.d));
	}
	
	@Override
	public int hashCode() {
		return Arrays.hashCode(d)*Arrays.hashCode(w);
	}

}
