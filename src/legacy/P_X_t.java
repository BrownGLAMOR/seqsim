package legacy;
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
		
		P_X_t aThat = (P_X_t) that;		

		// check to see if realized price vectors are the same?
		if (this.realized != aThat.realized) {
			// compare elementwise
			if (aThat.realized.length != this.realized.length) {
				return false;
			} else {
				for (int i = 0; i < this.realized.length; i++) {
					if (aThat.realized[i] != this.realized[i]) {
						return false;
					}
				}
			}
		} 
	
		return this.t == aThat.t && this.X.size() == aThat.X.size() && this.X.containsAll(aThat.X);
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
