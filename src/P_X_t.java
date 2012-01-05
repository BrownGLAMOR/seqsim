import java.util.Set;

// This class implements the (realized_prices,X,t) state in Full MDP iteration
// where X is the set of goods obtained, and t is the time
public class P_X_t extends X_t {

	double[] realized;
	
	public P_X_t(double[] realized, Set<Integer> X, int t) {
		super(X, t);
		this.realized = realized;
	}

	@Override
	public boolean equals(Object that) {
		// same object
		if (this == that)
			return true;

		// not same class
		if (! (that instanceof P_X_t))
			return false;
		
		// compare elementwise
		P_X_t aThat = (P_X_t) that;		
		boolean ret = true;
		if (aThat.realized.length != this.realized.length) {
			ret = false;
		} else {
			for (int i = 0; i < this.realized.length; i++) {
				if (aThat.realized[i] != this.realized[i]) {
					ret = false;
				}
			}
		}
		ret = ret && this.t == aThat.t && this.X.containsAll(aThat.X) && aThat.X.containsAll(this.X);
		
		return ret;
	}

	
	@Override
	public String toString() {
		String message = "({";
		for (double p : realized){
			message+=" ";
			message+=p;
		}
		message+="},{";
		for (int i : X){
			message+=" " ;
			message+=i;
		}
		message=message+"},"+t+")";
		return message;
	}

}
