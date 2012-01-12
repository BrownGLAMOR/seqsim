package speed;

import java.util.Arrays;
import java.util.Random;

// Valuation function which preserves the property of decreasing marginal utility
// (actually, preserves property of non-increasing marginal utility) as the number
// of acquired goods increases. 0 goods acquired gives valuation of 0.

/*
 * Right. So it will be something like, v(2)=10, v(1)=6.
[9:12:11 PM] Jiacui Li: v(num of goods won) = utility.
[9:12:41 PM] Jiacui Li: We can, say, generate u_1, u_2 from uniform (0,10).
[9:12:51 PM] Jiacui Li: Sort them, so that u_2 >= u_1.
[9:13:04 PM] Jiacui Li: Then, assign v(1)=u_1, v(2)=u_1+u_2.
[9:13:11 PM] Jiacui Li: The case for more than 2 goods is analogous.
 */
public class DMUValue extends Value {
	int no_goods;
	double max_price;
	double[] v;
	Random rng;
	
	public DMUValue(int no_goods, double max_price, Random rng) {
		this.no_goods = no_goods;
		this.v = new double[no_goods+1];
		this.rng = rng;

		// generate a random valuation
		reset();
	}
	
	@Override
	public void reset() {
		// create random values & sort them
		double[] u = new double[no_goods];
		double max_u = max_price / no_goods; // ensures v[i] < max_price for all i
		
		for (int i = 0; i<no_goods; i++)
			u[i] = rng.nextDouble() * max_u;
		
		// sort the array in ascending order. 
		Arrays.sort(u);
		
		// create valuation list with DMU. v[0] = 0.
		// note we access u[] in descending order.
		for (int i = 1; i<v.length; i++)
			v[i] = v[i-1] + u[u.length - i];
	}

	@Override
	public double getValue(int no_goods_won) {
		return v[no_goods_won];
	}

	public static void main(String args[]) {
		// TESTING
		Random rng = new Random(); // this rng is meant to be reused across multiple valuations.
		
		int no_goods = 2;
		double max_price = 10;
		
		DMUValue v = new DMUValue(no_goods, max_price, rng);
		
		double last_mu = Double.MAX_VALUE;
		for (int i = 0; i<no_goods+1; i++) {
			System.out.print("v[" + i + "]=" + v.getValue(i));
		
			if (i > 0) {
				double mu = v.getValue(i) - v.getValue(i - 1);

				System.out.println(", mu=" + mu);
				
				if (mu >= last_mu) {
					System.out.println("ERROR! DMU violated. last_mu=" + last_mu + ", mu=" + mu);
				} else {
					last_mu = mu;
				}
			} else {
				System.out.println("");
			}
		}
	}
}
