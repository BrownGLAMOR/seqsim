package speed;

import java.util.Arrays;

public class WinnerAndRealized {
	public BooleanArray w;
	public IntegerArray r;
	
	public WinnerAndRealized(int n) {
		w = new BooleanArray(n);			// Winner
		r = new IntegerArray(n);				// realized prices (binned)
	}

	public WinnerAndRealized(BooleanArray w, IntegerArray r) {
		if (w.d.length != r.d.length)
			System.out.println("Error: w.d.length has to equal r.d.length!");
		this.w = w;
		this.r = r;
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
		if (this.r == aThat.r && this.w == aThat.w)
			return true;
		
		// else, do pairwise compare
		return (Arrays.equals(this.w.d, aThat.w.d) && Arrays.equals(this.r.d, aThat.r.d));
	}
	
	@Override
	public int hashCode() {
		return Arrays.hashCode(r.d)*Arrays.hashCode(w.d);
	}

}
