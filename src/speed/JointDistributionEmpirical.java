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

public class JointDistributionEmpirical extends JointDistribution {
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
	
	IntegerArray r_tmp[];

	HashMap<IntegerArray, double[]>[] prob; // hash map from realized prices (as bins) to freq distribution, one per good
	HashMap<IntegerArray, Integer>[] sum; // hash map from realized prices (as bins) to freq dist. sums, one per good
	
	ArrayList<IntegerArray> log; 	// log of all realized prices fed into the JDE
	
	// no_goods is the number of goods/auctions
	@SuppressWarnings("unchecked")
	public JointDistributionEmpirical(int no_goods, double precision, double max_price) {
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
			
			this.prob[i] = new HashMap<IntegerArray, double[]>(max_realizations);			
			this.sum[i] = new HashMap<IntegerArray, Integer>(max_realizations);
		}
				
		this.marg_prob = new double[no_goods][no_bins];
		
		this.r_tmp = new IntegerArray[no_goods+1];
		
		for (int i = 0; i<no_goods+1; i++)
			this.r_tmp[i] = new IntegerArray(i);
		
		this.log = new ArrayList<IntegerArray>();
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
		
		for (HashMap<IntegerArray, double[]> p : this.prob)
			p.clear();

		for (HashMap<IntegerArray, Integer> s : this.sum)
			s.clear();
		
		log.clear();
	}
	
	// Call this once per realized price vector to populate the joint distribution
	// realized.length must == no_goods from last reset()
	public void populate(IntegerArray realized) {
		if (realized.d.length != no_goods)
			throw new RuntimeException("length of realized price vector must == no_goods");

		log.add(realized);
		
		// for each good		
		for (int i = 0; i<no_goods; i++) {			
			// create array with binned conditional prices only
			IntegerArray r = r_tmp[i];
			for (int j = 0; j<i; j++) 
				r.d[j] = realized.d[j];

			// get the distribution conditioned on earlier prices
			double[] p = prob[i].get(r);
						
			if (p == null) {
				// this is our first entry into the distribution; create it
				p = new double[no_bins];
			
				p[realized.d[i]]++;
				
				// Make a copy of IntegerArray since we are PUTing a new copy to the HashMap.
				r = new IntegerArray(Arrays.copyOf(r.d, r.d.length));
				
				prob[i].put(r, p);
				sum[i].put(r, 1);
			} else {
				p[realized.d[i]]++;
				
				// We don't need to make a copy of IntegerArray here because when put() overwrites an existing entry
				// in the HashMap, it keeps the existing key.
				sum[i].put(r, sum[i].get(r) + 1);
			}

			// record unconditional probability
			marg_prob[i][realized.d[i]]++;
		}

		marg_sum++;
	
	}
	
	// Call this once per realized price vector to populate the joint distribution
	// realized.length must == no_goods from last reset()
	public void populate(double[] realized) {
		IntegerArray binned = new IntegerArray(realized.length);

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
			binned.d[i] = bin(realized[i], precision);
		}
		
		populate(binned);
	}
	
	// Call this to normalize the collected data into a joint distribution
	public void normalize() {
		for (int i = 0; i<no_goods; i++) {			
			for (IntegerArray r : prob[i].keySet()) {				
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
	
	// gets the probability of price of good "realized.length" being "price" given realized prices "realized"
	@Override
	public double getProb(double price, double[] realized) {
		return getPMF(realized)[ bin(price, precision) ];
	}

	// get the probability mass function of the good numbered "realized.d.length",
	// given the list of binned realized prices.
	@Override
	public double[] getPMF(IntegerArray realized) {
		if (!ready)
			throw new RuntimeException("must normalize first");
		
		if (realized.d.length >= no_goods)
			throw new IllegalArgumentException("no more goods");
		
		double[] p = prob[realized.d.length].get(realized);
		
		if (p == null) {
			// ut-oh, return the unconditional (marginal) pmf for this good since we have no data
			// todo: we return this when no samples == 0, but maybe we need logic to 
			//       return this when no samples < viable threshold
			p = marg_prob[realized.d.length];

//// print. TODO: conditional distribution is an issue for MDP Agent			
//			System.out.print("previous price not observed, outputting PMF = \t [");
//			for (int i = 0 ; i < p.length; i++)
//				System.out.print(p[i] + ", ");
//			System.out.println("]");
			
		}
		
		return p;
	}
	
	// Gets the probability mass function for good numbered "realized.length", conditional
	// on realized prices
	@Override
	public double[] getPMF(double[] realized) {
		if (realized == null)
			realized = empty_array;
		
		// bin the realized prices
		IntegerArray r = r_tmp[realized.length];

		for (int i = 0; i<realized.length; i++)
			r.d[i] = bin(realized[i], precision);
				
		// get the price distribution conditional on realized prices
		return getPMF(r);
	}
	
	@Override
	// return the expected final price for good "realized.length", conditional on realized prices
	public double getExpectedFinalPrice(double[] realized) {	
		// get the price distribution conditional on realized prices
		double[] p = getPMF(realized);
				
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
						r_tmp[i+1].d[k] = bins[k];
					
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
		for (IntegerArray a : log) {
			int len = a.d.length - 1;
			
			// print out [0, a.d.length-2]
			for (int i = 0; i<len; i++)
				fw.write(a.d[i] + ",");
			
			// print out final value, [a.d.length-1]
			if (a.d.length > 0)
				fw.write(a.d[len] + "\n");
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
			
			for (Entry<IntegerArray, double[]> e : prob[i].entrySet()) {
				System.out.print("pr(" + i + " | {");
				
				for (int p : e.getKey().d)
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
		
		JointDistributionEmpirical jde = new JointDistributionEmpirical(2, 1, 5);
		
		jde.populate(new double[] {1, 1});
		jde.populate(new double[] {1, 2});
		jde.populate(new double[] {1, 2});
		jde.populate(new double[] {1, 5});
		
		jde.populate(new double[] {2, 2});
		jde.populate(new double[] {2, 3});
		jde.populate(new double[] {2, 3});
		jde.populate(new double[] {2, 5});

		jde.populate(new double[] {3, 5});
		
		jde.populate(new double[] {3, 5});
		
		jde.normalize();
		
		// get the PMF of good 0
		double[] pmf;

		System.out.print("pmf(0 | {}): ");
		pmf = jde.getPMF(new double[] {});
		
		for (double p : pmf)
			System.out.print(p + " ");
		
		System.out.println("");

		// get PMF of good 1, cond. on price of good 0 being $1
		System.out.print("pmf(1 | {1}): ");
		pmf = jde.getPMF(new double[] {1});
		
		for (double p : pmf)
			System.out.print(p + " ");
		
		System.out.println("");

		// get PMF of good 1, cond. on price of good 0 being $2
		System.out.print("pmf(1 | {2}): ");
		pmf = jde.getPMF(new double[] {2});
		
		for (double p : pmf)
			System.out.print(p + " ");
		
		System.out.println("");
		
		// get PMF of good 1, cond. on price of good 0 being $3
		System.out.print("pmf(1 | {3}): ");
		pmf = jde.getPMF(new double[] {3});
		
		for (double p : pmf)
			System.out.print(p + " ");
		
		System.out.println("");
		
		// get the PMF of good 1, conf on price of good 0 being $0
		System.out.print("pmf(1 | {0}): ");
		pmf = jde.getPMF(new double[] {0});
		
		for (double p : pmf)
			System.out.print(p + " ");
		
		System.out.println("");
		
		//
		System.out.println("");
		
		// get the expected final price of good 0; the list of conditional prices necessarily empty
		System.out.println("efp(0 | {}) = " + jde.getExpectedFinalPrice(new double[] {}));
		
		// get the expected final price of good 1 for various values of good 0.
		System.out.println("efp(1 | {0}) = " + jde.getExpectedFinalPrice(new double[] {0}));
		System.out.println("efp(1 | {1}) = " + jde.getExpectedFinalPrice(new double[] {1}));
		System.out.println("efp(1 | {2}) = " + jde.getExpectedFinalPrice(new double[] {2}));
		System.out.println("efp(1 | {3}) = " + jde.getExpectedFinalPrice(new double[] {3}));
		
		System.out.println("");
		System.out.println("sampled price **BINS INDEXES**:");
		for (int i = 0; i<20; i++) {
			int[] sample = jde.getSample(rng);

			System.out.print(i + ": {");
			
			for (int d : sample) 
				System.out.print(d + ", ");
			
			System.out.println("}");			
		}
		
		/*System.out.println("");
		System.out.println("Testing binning routine: ");
		
		for (double x = 0; x<11; x += 0.1)
			System.out.println("bin(" + x + ", 10) = " + bin(x, 1));
			*/
	}
}
