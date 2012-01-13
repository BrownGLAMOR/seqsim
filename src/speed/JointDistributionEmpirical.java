package speed;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
	
	int avg_sum;
	double[][] avg_prob;		// [good_id][pmf idx]
	
	int no_bins;

	HashMap<List<Integer>, double[]>[] prob; // hash map from realized prices (as bins) to freq distribution, one per good
	HashMap<List<Integer>, Integer>[] sum; // hash map from realized prices (as bins) to freq dist. sums, one per good
	
	// no_goods is the number of goods/auctions
	public JointDistributionEmpirical(int no_goods, double precision, double max_price) {
		this.no_goods = no_goods;
		this.precision = precision;
		this.max_price = max_price;
		
		empty_array = new double[0];

		reset(no_goods, precision, max_price);
	}
		
	// Call this to reset the internal state. This is automatically
	// called for you when you instantiate a new JointDistribution.
	@SuppressWarnings("unchecked")
	public void reset(int no_goods, double precision, double max_price) {	
		this.no_goods = no_goods;
		this.precision = precision;
		this.max_price = max_price;
		
		reset();
	}
	
	public void reset() {
		this.ready = false;

		this.no_bins = bin(max_price, precision) + 1;
		
		this.avg_prob = new double[this.no_goods][this.no_bins];
		this.avg_sum = 0;
		
		int max_realizations;
		
		// allocate a new probability hash map if we do not already have one
		// -or- if the current one is not the right length
		if (this.prob == null || this.prob.length != no_goods) {
			this.prob = new HashMap[no_goods];
			
			max_realizations = 1; 
			for (int i = 0; i<no_goods; i++) {
				max_realizations *= no_bins;
				this.prob[i] = new HashMap<List<Integer>, double[]>(max_realizations);
			}
		} else {
			// note that our capacity estimates may be off; not sure of the potential performance impact. 
			for (HashMap<List<Integer>, double[]> p : this.prob)
				p.clear();
		}

		// allocate a new sum hash map if we do not already have one
		// -or- if the current one is not the right length
		if (this.sum == null || this.sum.length != no_goods)
			this.sum = new HashMap[no_goods];
		else
			for (HashMap<List<Integer>, Integer> s : this.sum)
				s.clear();
		
		max_realizations = 1; 
		for (int i = 0; i<no_goods; i++) {
			max_realizations *= no_bins;
			this.sum[i] = new HashMap<List<Integer>, Integer>(max_realizations);
		}
	}
	
	// Call this once per realized price vector to populate the joint distribution
	// realized.length must == no_goods from last reset()
	public void populate(double[] realized) {
		if (realized.length != no_goods)
			throw new RuntimeException("length of realized price vector must == no_goods");

		// for each good
		int binned[] = new int[no_goods];
		
		for (int i = 0; i<no_goods; i++) {
			// ensure we do not exceed the maximum price, or we will exceed array bounds later.
			// note that this bins all prices above max as the max, which could produce a skewed
			// distribution.
			if (realized[i] > max_price)
				realized[i] = max_price;

			// bin the realized price for this good
			binned[i] = bin(realized[i], precision);
			
			// create array with binned conditional prices only
			Integer[] r_tmp = new Integer[i];
			for (int j = 0; j<i; j++) 
				r_tmp[j] = binned[j];
			
			List<Integer> r = Arrays.asList(r_tmp);

			// get the distribution conditioned on earlier prices
			double[] p = prob[i].get(r);
						
			if (p == null) {
				// this is our first entry into the distribution; create it
				p = new double[no_bins];
			
				p[binned[i]]++;
				
				prob[i].put(r, p);
				sum[i].put(r, 1);
			} else {
				p[binned[i]]++;
				
				sum[i].put(r, sum[i].get(r) + 1);
			}

			// record unconditional probability
			avg_prob[i][binned[i]]++;
		}

		avg_sum++;
	}
	
	// Call this to normalize the collected data into a joint distribution
	public void normalize() {
		for (int i = 0; i<no_goods; i++) {			
			for (List<Integer> r : prob[i].keySet()) {				
				double[] p = prob[i].get(r);
				int s = sum[i].get(r);
				
				for (int j = 0; j<p.length; j++)
					p[j] /= s;
			}

			for (int j = 0; j<no_bins; j++)
				avg_prob[i][j] /= avg_sum;
		}
		
		ready = true;
	}
	
	@Override
	public double getProb(double price, double[] realized) {
		return getPMF(realized)[ bin(price, precision) ];
	}

	@Override
	// Gets the probability mass function for good numbered "realized.length", conditional
	// on realized prices
	public double[] getPMF(double[] realized) {
		if (!ready)
			throw new RuntimeException("must normalize first");
		
		if (realized == null)
			realized = empty_array;
		
		if (realized.length == no_goods)
			throw new IllegalArgumentException("no more goods");
		
		// bin the realized prices
		Integer[] r_tmp = new Integer[realized.length];

		for (int i = 0; i<realized.length; i++)
			r_tmp[i] = bin(realized[i], precision);
		
		List<Integer> r = Arrays.asList(r_tmp);
		
		// get the price distribution conditional on realized prices
		double[] p = prob[realized.length].get(r);
		
		if (p == null) {
			// ut-oh, return the unconditional pmf for this good since we have no data)
			// todo: we return this when no samples == 0, but maybe we need logic to 
			//       return this when no samples < viable threshold
			p = avg_prob[realized.length];
		}
		
		return p;
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
	public double[] getSample(Random rng) {
		ArrayList<Integer> realized = new ArrayList<Integer>(no_goods);
		double prices[] = new double[no_goods];
		
		for (int i = 0; i<no_goods; i++) {
			double[] pmf = prob[realized.size()].get(realized);
	
			// choose a random spot on the cdf
			double random = rng.nextDouble();
			
			// compute cdf. todo: maybe we should precompute inverse of cdf in normalize() so that
			// we can avoid a loop here?
			double cdf = 0.0;
			for (int j = 0; j<pmf.length; j++) {
				cdf += pmf[j];
				
				if (cdf >= random) {
					prices[i] = j;
					
					// add index to realized so that in next round we get the pmf conditional
					// on our result for this round
					realized.add(bin(prices[i], precision));
					
					// go onto next good
					break;
				}
			}
			
			// sanity check: make sure we picked something. 
		}
		
		return prices;
	}
	
	@Override
	public void output() {
		int total_act = 0;
		int total_exp = 0;
		
		for (int i = 0; i<no_goods; i++) {
			int max_realizations = MathOps.ipow(this.no_bins, i);
			
			total_exp += max_realizations;
			total_act += prob[i].size();
			
			System.out.println("prob[" + i + "].size() == " + prob[i].size() + ", max_realizations=" + max_realizations);
			
			for (Entry<List<Integer>, double[]> e : prob[i].entrySet()) {
				System.out.print("pr(" + i + " | {");
				
				for (Integer p : e.getKey())
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
		System.out.println("sampled prices:");
		for (int i = 0; i<20; i++) {
			double[] sample = jde.getSample(rng);

			System.out.print(i + ": {");
			
			for (double d : sample) 
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
