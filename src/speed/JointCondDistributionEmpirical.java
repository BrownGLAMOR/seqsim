package speed;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;

// TODO: This class still needs to be optimized for speed in populate() and getPMF().
//       In particular, get rid of the need to make a new Integer[] for each call.
//       Maybe use thread local storage?

public class JointCondDistributionEmpirical extends JointCondDistribution {
	boolean ready;
	
	public int no_goods;
	public double precision;
	public double max_price;
	double[] empty_array;
	
	int marg_sum;
	double[][] marg_prob;		// [good_id][pmf idx]
	
	IntegerArray bins;					// the bins in the pmf
	DoubleArray prices;			// the value of each bin in the pmf
	
	double witnessed_max_price;
	
	int no_bins;
	
	// holder of things
	WinnerAndRealized wr_tmp[];
	int r_tmp[];
	WinnerAndRealized past;
	
	HashMap<WinnerAndRealized, double[]>[] prob; // hash map from realized prices (as bins) to freq distribution, one per good
	HashMap<WinnerAndRealized, Integer>[] sum; // hash map from realized prices (as bins) to freq dist. sums, one per good
	
	ArrayList<WinnerAndRealized> log; 	// log of all past history fed into the JDE
	
	// no_goods is the number of goods/auctions
	@SuppressWarnings("unchecked")
	public JointCondDistributionEmpirical(int no_goods, double precision, double max_price) {
		this.no_goods = no_goods;
		this.precision = precision;
		this.max_price = max_price;
				
		this.no_bins = bin(max_price, precision) + 1;
		this.empty_array = new double[0];

		this.prices = new DoubleArray(this.no_bins);
		this.bins = new IntegerArray(this.no_bins);
		for (int i = 0; i<this.no_bins; i++) {
			this.bins.d[i] = i;
			this.prices.d[i] = bin(i, precision);
		}
		
		
		// init this.prob and this.sum
		this.prob = new HashMap[no_goods];
		this.sum = new HashMap[no_goods];
		
		int max_realizations = 1; 
		for (int i = 0; i<no_goods; i++) {
			max_realizations *= no_bins;
			
			this.prob[i] = new HashMap<WinnerAndRealized, double[]>(max_realizations);			
			this.sum[i] = new HashMap<WinnerAndRealized, Integer>(max_realizations);
		}
				
		this.marg_prob = new double[no_goods][no_bins];
		
		// For temporarily holding WR 
		this.wr_tmp = new WinnerAndRealized[no_goods+1];
		for (int i = 0; i<no_goods+1; i++)
			this.wr_tmp[i] = new WinnerAndRealized(i);
		this.log = new ArrayList<WinnerAndRealized>();

		// For temporarily holding price binds 
		this.r_tmp = new int [no_goods];

	}
		
	// Call this to reset the internal state. This is automatically
	// called for you when you instantiate a new JointDistribution.
	public void reset() {
		ready = false;
		witnessed_max_price = 0.0;
		
		for (int i = 0; i<no_goods; i++)
			for (int j = 0; j<no_bins; j++)
				marg_prob[i][j] = 0.0;
		
		marg_sum = 0;
		
		for (HashMap<WinnerAndRealized, double[]> p : this.prob)
			p.clear();

		for (HashMap<WinnerAndRealized, Integer> s : this.sum)
			s.clear();
		
		log.clear();
	}
	
	// Call this once per realized price vector to populate the joint distribution
	// war.d.length must == no_goods from last reset()
	public void populate(WinnerAndRealized past) {
		if (past.r.d.length != no_goods || past.w.d.length != no_goods)
			throw new RuntimeException("length of realized price/winner vector must == no_goods");
		
		log.add(past);
		
		// for each good		
		for (int i = 0; i<no_goods; i++) {			
			// create array with binned conditional prices only
			WinnerAndRealized wr = wr_tmp[i];
			for (int j = 0; j<i; j++) { 
				wr.w.d[j] = past.w.d[j];
				wr.r.d[j] = past.r.d[j];
			}
			// get the distribution conditioned on earlier prices
			double[] p = prob[i].get(wr);
						
			if (p == null) {
				// this is our first entry into the distribution; create it
				p = new double[no_bins];
			
				p[past.r.d[i]]++;
				
				// Make a copy of IntegerArray since we are PUTing a new copy to the HashMap.
				wr = new WinnerAndRealized(new BooleanArray(Arrays.copyOf(wr.w.d, wr.w.d.length)), new IntegerArray(Arrays.copyOf(wr.r.d, wr.r.d.length)));
				
				prob[i].put(wr, p);
				sum[i].put(wr, 1);
			} else {
				p[past.r.d[i]]++;
				
				// We don't need to make a copy of IntegerArray here because when put() overwrites an existing entry
				// in the HashMap, it keeps the existing key.
				sum[i].put(wr, sum[i].get(wr) + 1);
			}

			// record unconditional probability
			marg_prob[i][past.r.d[i]]++;
		}

		marg_sum++;
	
	}
	
	// Call this once per realized price vector to populate the joint distribution
	// realized.length must == no_goods from last reset()
	public void populate(boolean[] winner, double[] realized) {
		
//		WinnerAndRealized past = new WinnerAndRealized(realized.length);
//		int[] temp = new int[realized.length];
		for (int i = 0; i<no_goods; i++) {
			// record max price witnessed
			if (realized[i] > witnessed_max_price)
				witnessed_max_price = realized[i];
			
			// ensure we do not exceed the maximum price, or we will exceed array bounds later.
			// note that this bins all prices above max as the max, which could produce a skewed
			// distribution.
			if (realized[i] > max_price)
				realized[i] = max_price;
			
			// bin the realized price for this good
			r_tmp[i] = bin(realized[i], precision);
		}		
		
		// XXX: has to NEW a WinnerAndRealized every time. Put it in Cache? 
		populate(new WinnerAndRealized(new BooleanArray(winner),new IntegerArray(r_tmp)));
	}
	
	// Call this to normalize the collected data into a joint distribution
	public void normalize() {
		for (int i = 0; i<no_goods; i++) {			
			for (WinnerAndRealized r : prob[i].keySet()) {				
				double[] p = prob[i].get(r);
				int s = sum[i].get(r);
				
				for (int j = 0; j<p.length; j++)
					p[j] /= s;
			}
			
			// compute the marginal distribution for this good
			for (int j = 0; j<no_bins; j++)
				marg_prob[i][j] /= marg_sum;
		}
		
		ready = true;
	}
	
	// gets the probability of price of good "realized.length" being "price" given realized prices "realized" TODO: not worked on yet
	@Override
	public double getProb(boolean[] winner, double price, double[] realized) {
		return getPMF(winner, realized)[ bin(price, precision) ];
	}

	// get the probability mass function of the good numbered "realized.d.length",
	// given the list of binned realized prices.
	@Override
	public double[] getPMF(WinnerAndRealized past) {
		if (!ready)
			throw new RuntimeException("must normalize first");
		
		if (past.r.d.length >= no_goods)
			throw new IllegalArgumentException("no more goods");
		
		double[] p = prob[past.r.d.length].get(past);
		
		if (p == null) {
			// ut-oh, return the unconditional (marginal) pmf for this good since we have no data
			// todo: we return this when no samples == 0, but maybe we need logic to 
			//       return this when no samples < viable threshold
			p = marg_prob[past.r.d.length];

		}
		
		return p;
	}
	
	// Gets the probability mass function for good numbered "realized.length", conditional
	// on realized prices
	@Override
	public double[] getPMF(boolean[] winner, double[] realized) {
		if (realized == null)
			realized = empty_array;
		
		// bin the realized prices
		past = wr_tmp[realized.length];

		past.w.d = winner;
		for (int i = 0; i<realized.length; i++)
			past.r.d[i] = bin(realized[i], precision);
		
				
		// get the price distribution conditional on realized prices
		return getPMF(past);
	}
	
	@Override
	// return the expected final price for good "realized.length", conditional on realized prices
	public double getExpectedFinalPrice(boolean[] winner, double[] realized) {	
		// get the price distribution conditional on realized prices
		double[] p = getPMF(winner, realized);
				
		// compute expected final price
		double efp = 0.0;
		
		for (int i = 0; i<p.length; i++)
			efp += val(i, precision) * p[i];
		
		return efp;
	}

	// sample the probability distribution, and returns an array of prices, one per good
	@Override
	public int[] getSample(Random rng) {
		if (!ready)
			throw new RuntimeException("must normalize first");
		
		int bins[] = new int[no_goods];
		
		for (int i = 0; i<no_goods; i++) {
			double[] pmf = prob[i].get(r_tmp[i]);
	
			// choose a random spot on the cdf
			double random = rng.nextDouble();
			
			// compute cdf. todo: maybe we should precompute inverse of cdf in normalize() so that
			// we can avoid a loop here?
			double cdf = 0.0;
			for (int j = 0; j<pmf.length; j++) {
				cdf += pmf[j];
				
				if (cdf >= random) {
					bins[i] = j;
					
					// add index to realized so that in next round we get the pmf conditional
					// on our result for this round
					for (int k = 0; k<=i; k++)
						wr_tmp[i+1].r.d[k] = bins[k];
					
					// go onto next good
					break;
				}
			}
			
			// sanity check: make sure we picked something. 
		}
		
		return bins;
	}
	
	// get the marginal distribution for good id, assuming independent prices
	public double[] getMarginalDist(int good_id) {
		if (!ready)
			throw new RuntimeException("must normalize first");
		
		return marg_prob[good_id];
	}
	
	// get the maximum price witnessed across all goods
	public double getWitnessedMaxPrice() {	
		return witnessed_max_price;
	}
	
	@Override
	public void outputRaw(FileWriter fw) throws IOException {
		for (WinnerAndRealized past : log) {
			int len = past.r.d.length - 1;
			
			// print out winning history [0, a.d.length-1]
//			System.out.println("past in log = [" + past.w.d[0] + ", " + past.w.d[1] +  ", " + past.r.d[0] + ", " + past.r.d[1] + " ]");
			for (int i = 0; i < len+1; i++) {
				if (past.w.d[i])
					fw.write("1,");
				else
					fw.write("0,");
			}
			
			// print out [0, a.d.length-2]
			for (int i = 0; i<len; i++)
				fw.write(past.r.d[i]*precision + ",");
			
			// print out final value, [a.d.length-1]
			if (past.r.d.length > 0)
				fw.write(past.r.d[len]*precision + "\n");
		}
	}
	
	@Override
	public void outputNormalized() {
		int total_act = 0;
		int total_exp = 0;
		
		System.out.println("max_price witnessed = " + witnessed_max_price);
		
		for (int i = 0; i<no_goods; i++) {
			int max_realizations = MathOps.ipow(this.no_bins, i);
			
			total_exp += max_realizations;
			total_act += prob[i].size();
			
			System.out.println("prob[" + i + "].size() == " + prob[i].size() + ", max_realizations=" + max_realizations);
			
			for (Entry<WinnerAndRealized, double[]> e : prob[i].entrySet()) {
				System.out.print("pr(" + i + " | w = {");

				for (boolean p : e.getKey().w.d) {
					if (p == true)
						System.out.print("1 ,");
					else
						System.out.print("0 ,");
				}
				System.out.print("}, p = {");

				for (int p : e.getKey().r.d)
					System.out.print(val(p, precision) + ", ");
				
				System.out.print("}) [hits=" + sum[i].get(e.getKey()) + "] ==> {");
				
				for (double p : e.getValue())
					System.out.print(p + ", ");
				
				System.out.println("}");				
			}
		}
		
		System.out.println("Actual realizations == " + total_act + " of a maximum == " + total_exp);
	}
	
	public static void main(String args[]) {
		// TESTING / EXAMPLE
		Random rng = new Random();
		
		JointCondDistributionEmpirical jcde = new JointCondDistributionEmpirical(2, 1, 5);
		
		jcde.populate(new boolean[] {false, false}, new double[] {1, 1});
		jcde.populate(new boolean[] {false, false}, new double[] {1, 2});
		jcde.populate(new boolean[] {false, false}, new double[] {1, 2});
		jcde.populate(new boolean[] {false, false}, new double[] {1, 5});
		
		jcde.populate(new boolean[] {false, false}, new double[] {2, 2});
		jcde.populate(new boolean[] {false, false}, new double[] {2, 3});
		jcde.populate(new boolean[] {false, false}, new double[] {2, 3});
		jcde.populate(new boolean[] {false, false}, new double[] {2, 5});

		jcde.populate(new boolean[] {false, false}, new double[] {3, 5});		
		jcde.populate(new boolean[] {false, false}, new double[] {3, 5});
		
		// ------
		
		jcde.populate(new boolean[] {true, false}, new double[] {2, 1});
		jcde.populate(new boolean[] {true, false}, new double[] {2, 2});
		jcde.populate(new boolean[] {true, false}, new double[] {2, 2});
		jcde.populate(new boolean[] {true, false}, new double[] {2, 5});
		
		jcde.populate(new boolean[] {true, false}, new double[] {3, 2});
		jcde.populate(new boolean[] {true, false}, new double[] {3, 3});
		jcde.populate(new boolean[] {true, false}, new double[] {3, 3});
		jcde.populate(new boolean[] {true, false}, new double[] {3, 5});
		
		jcde.populate(new boolean[] {true, false}, new double[] {4, 5});		
		jcde.populate(new boolean[] {true, false}, new double[] {4, 5});
		
		jcde.normalize();
		
		System.out.println(" ----- if first round loses ----- ");
		
		// get the PMF of good 0
		double[] pmf;
		System.out.print("pmf(0 | { }, { }): ");
		pmf = jcde.getPMF(new boolean[] {}, new double[] {});
		
		for (double p : pmf)
			System.out.print(p + " ");
		
		System.out.println("");

		// get PMF of good 1, cond. on price of good 0 being $1
		System.out.print("pmf(1 | {0}, {1}): ");
		pmf = jcde.getPMF(new boolean[] {false}, new double[] {1});
		
		for (double p : pmf)
			System.out.print(p + " ");
		
		System.out.println("");

		// get PMF of good 1, cond. on price of good 0 being $2
		System.out.print("pmf(1 | {0}, {2}): ");
		pmf = jcde.getPMF(new boolean[] {false}, new double[] {2});
		
		for (double p : pmf)
			System.out.print(p + " ");
		
		System.out.println("");
		
		// get PMF of good 1, cond. on price of good 0 being $3
		System.out.print("pmf(1 | {0}, {3}): ");
		pmf = jcde.getPMF(new boolean[] {false}, new double[] {3});
		
		for (double p : pmf)
			System.out.print(p + " ");
		
		System.out.println("");
		
		// get the PMF of good 1, conf on price of good 0 being $0
		System.out.print("pmf(1 | {0}, {0}): ");
		pmf = jcde.getPMF(new boolean[] {false}, new double[] {0});		
		for (double p : pmf)
			System.out.print(p + " ");
		
		System.out.println("");
		
		//
		System.out.println("");
		
		// get the expected final price of good 0; the list of conditional prices necessarily empty
		System.out.println("efp(0 | { }, { }) = " + jcde.getExpectedFinalPrice(new boolean[] {}, new double[] {}));
		
		// get the expected final price of good 1 for various values of good 0.
		System.out.println("efp(1 | {0}, {1}) = " + jcde.getExpectedFinalPrice(new boolean[] {false}, new double[] {1}));
		System.out.println("efp(1 | {0}, {2}) = " + jcde.getExpectedFinalPrice(new boolean[] {false}, new double[] {2}));
		System.out.println("efp(1 | {0}, {3}) = " + jcde.getExpectedFinalPrice(new boolean[] {false}, new double[] {3}));
		System.out.println("efp(1 | {0}, {0}) = " + jcde.getExpectedFinalPrice(new boolean[] {false}, new double[] {0}));
		
		
		
		System.out.println(" ----- if first round wins ----- ");

		// get PMF of good 1, cond. on price of good 0 being $1
		System.out.print("pmf(1 | {1}, {1}): ");
		pmf = jcde.getPMF(new boolean[] {true}, new double[] {1});
		
		for (double p : pmf)
			System.out.print(p + " ");
		
		System.out.println("");

		// get PMF of good 1, cond. on price of good 0 being $2
		System.out.print("pmf(1 | {1}, {2}): ");
		pmf = jcde.getPMF(new boolean[] {true}, new double[] {2});
		
		for (double p : pmf)
			System.out.print(p + " ");
		
		System.out.println("");
		
		// get PMF of good 1, cond. on price of good 0 being $3
		System.out.print("pmf(1 | {1}, {3}): ");
		pmf = jcde.getPMF(new boolean[] {true}, new double[] {3});
		
		for (double p : pmf)
			System.out.print(p + " ");
		
		System.out.println("");
		
		// get the PMF of good 1, conf on price of good 0 being $0
		System.out.print("pmf(1 | {1}, {0}): ");
		pmf = jcde.getPMF(new boolean[] {true}, new double[] {0});		
		for (double p : pmf)
			System.out.print(p + " ");
		
		System.out.println("");
		System.out.println("");
		
		// get the expected final price of good 0; the list of conditional prices necessarily empty
		System.out.println("efp(0 | { }, { }) = " + jcde.getExpectedFinalPrice(new boolean[] {}, new double[] {}));
		
		// get the expected final price of good 1 for various values of good 0.
		System.out.println("efp(1 | {1}, {1}) = " + jcde.getExpectedFinalPrice(new boolean[] {true}, new double[] {1}));
		System.out.println("efp(1 | {1}, {2}) = " + jcde.getExpectedFinalPrice(new boolean[] {true}, new double[] {2}));
		System.out.println("efp(1 | {1}, {3}) = " + jcde.getExpectedFinalPrice(new boolean[] {true}, new double[] {3}));
		System.out.println("efp(1 | {1}, {4}) = " + jcde.getExpectedFinalPrice(new boolean[] {true}, new double[] {4}));
		System.out.println("efp(1 | {1}, {0}) = " + jcde.getExpectedFinalPrice(new boolean[] {true}, new double[] {0}));

//		System.out.println("sampled price **BINS INDEXES**:");
//		for (int i = 0; i<20; i++) {
//			int[] sample = jcde.getSample(rng);
//
//			System.out.print(i + ": {");
//			
//			for (int d : sample) 
//				System.out.print(d + ", ");
//			
//			System.out.println("}");			
//		}
		
		/*System.out.println("");
		System.out.println("Testing binning routine: ");
		
		for (double x = 0; x<11; x += 0.1)
			System.out.println("bin(" + x + ", 10) = " + bin(x, 1));
			*/
	}
}
