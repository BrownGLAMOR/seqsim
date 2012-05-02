package LMSR;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;

import speed.IntegerArray;

import legacy.P_X_t;

// TODO: This class still needs to be optimized for speed in populate() and getPMF().
//       In particular, get rid of the need to make a new Integer[] for each call.
//       Maybe use thread local storage?

public class TransitionProb{
	
	boolean take_log, ready = false;
	int no_agents, no_rounds, total_times;
	
	// Each state is an IntegerArray
	
	// One per agent
	HashMap<IntegerArray, ArrayList<IntegerArray>>[] P;	// Transition probabilities: SA ==> Further states
	HashMap<IntegerArray, ArrayList<Integer>>[] Pcounts;	// tallying the number of transitions happened
	HashMap<IntegerArray, Double>[] R;			// expected rewards
	
	// holder of things
	IntegerArray SA_tmp[];
	
	@SuppressWarnings("unchecked")
	public TransitionProb(int no_agents, int no_rounds, boolean take_log) {
		Cache.init();	// XXX: need this? 
		this.take_log = take_log;
		this.no_agents = no_agents;
		this.no_rounds = no_rounds;
		this.total_times = no_rounds*no_agents;
		
		// Possible actions
		IntegerArray ActionSpace = new IntegerArray(new int[] {-1,0,1});
				
		// init this.P, this.Pcounts, this.R
		this.P = new HashMap[total_times];
		this.Pcounts = new HashMap[total_times];
		this.R = new HashMap[total_times];
		
		// Tally total number of possible histories... want to see them all hit
		int max_realizations = 1; 
		for (int i = 0; i < total_times; i++) {	
			if (i != 0)
				max_realizations *= ActionSpace.d.length;
			this.P[i] = new HashMap<IntegerArray, ArrayList<IntegerArray>>(max_realizations);
			this.Pcounts[i] = new HashMap<IntegerArray, ArrayList<Integer>>(max_realizations);
			this.R[i] = new HashMap<IntegerArray, Double>(max_realizations);
		}
		
		// For temporarily holding states 
		this.SA_tmp = new IntegerArray[total_times+1];
		for (int i = 0; i < total_times + 1; i++)
			this.SA_tmp[i] = new IntegerArray(i);
		
	}
		
	// Call this to reset the internal state. This is automatically called when instantiated. 
	public void reset() {
		ready = false;
		
		// clean HashMaps
		for (HashMap<IntegerArray, ArrayList<IntegerArray>> a : this.P)
			a.clear();
		for (HashMap<IntegerArray, ArrayList<Integer>> a : this.Pcounts)
			a.clear();
		for (HashMap<IntegerArray, Double> a : this.R)
			a.clear();

	}
	
	// Call this once per realized price vector to populate the joint distribution
	// war.d.length must == total_times from last reset()
	public void populate(IntegerArray history, double[] reward) {
		if (history.d.length != total_times || reward.length != total_times)
			throw new RuntimeException("length game history must equal reward vector length");
		
		// TODO: stopped here
		
		// Store indices, not WRs
		if (take_log == true)
			log_indices.add(Cache.getWRidx(history));
		
		// for each good		
		for (int i = 0; i<total_times; i++) {
			// create array with binned conditional prices only
			IntegerArray wr = SA_tmp[i];
			for (int j = 0; j<i; j++) { 
				wr.w.d[j] = history.w.d[j];
				wr.r.d[j] = history.r.d[j];
			}
			// get the distribution conditioned on earlier prices
			double[] p = prob[i].get(wr);
						
			if (p == null) {
				// this is our first entry into the distribution; create it
				p = new double[no_bins];

				// also create the corresponding cdf
				double[] temp = CDF[i].get(wr);
				temp = new double[no_bins];
			
				p[history.r.d[i]]++;
				
				// Make a copy of IntegerArray since we are PUTing a new copy to the HashMap.
				wr = new IntegerArray(new BooleanArray(Arrays.copyOf(wr.w.d, wr.w.d.length)), new IntegerArray(Arrays.copyOf(wr.r.d, wr.r.d.length)));
				
				prob[i].put(wr, p);
				CDF[i].put(wr, temp);
				sum[i].put(wr, 1);
				
			} else {
				p[history.r.d[i]]++;
				
				// We don't need to make a copy of IntegerArray here because when put() overwrites an existing entry
				// in the HashMap, it keeps the existing key.
				sum[i].put(wr, sum[i].get(wr) + 1);
			}

			// record unconditional probability
			marg_prob[i][history.r.d[i]]++;
		}

		marg_sum++;
	
	}

	
	// Distinguish b/w prices & highest opponent bids. 
	public void populateReal(boolean[] winner, double[] prices, double[] hob) {
//		if (history.r.d.length != total_times || history.w.d.length != total_times)
//			throw new RuntimeException("length of realized price/winner vector must == total_times");
		
//		// Store indices, not WRs
//		if (take_log == true)
//			log_indices.add(Cache.getWRidx(history));
		
		// for each good		
		for (int i = 0; i<total_times; i++) {
			// create array with binned conditional prices only
			IntegerArray wr = SA_tmp[i];
			for (int j = 0; j<i; j++) { 
				wr.w.d[j] = winner[j];
				wr.r.d[j] = bin(prices[j],precision);
			}
			// get the distribution conditioned on earlier prices
			double[] p = prob[i].get(wr);
						
			if (p == null) {
				// this is our first entry into the distribution; create it
				p = new double[no_bins];

				// also create the corresponding cdf
				double[] temp = CDF[i].get(wr);
				temp = new double[no_bins];
			
				p[bin(hob[i],precision)]++;
				
				// Make a copy of IntegerArray since we are PUTing a new copy to the HashMap.
				wr = new IntegerArray(new BooleanArray(Arrays.copyOf(wr.w.d, wr.w.d.length)), new IntegerArray(Arrays.copyOf(wr.r.d, wr.r.d.length)));
				
				prob[i].put(wr, p);
				CDF[i].put(wr, temp);
				sum[i].put(wr, 1);
				
			} else {
				p[bin(hob[i],precision)]++;

				// TODO: Include some addition to near by bins as well, or to similar conditional situations as well
				
				// We don't need to make a copy of IntegerArray here because when put() overwrites an existing entry
				// in the HashMap, it keeps the existing key.
				sum[i].put(wr, sum[i].get(wr) + 1);
			}
			// record unconditional probability
			marg_prob[i][bin(hob[i],precision)]++;
		}
		marg_sum++;	
	}
	
	// Call this once per realized price vector to populate the joint distribution
	// realized.length must == total_times from last reset()
	public void populate(boolean[] winner, double[] realized) {
		
//		IntegerArray history = new IntegerArray(realized.length);
		int[] temp = new int[realized.length];
		for (int i = 0; i<total_times; i++) {
			// record max price witnessed
			if (realized[i] > witnessed_max_price)
				witnessed_max_price = realized[i];
			
			// ensure we do not exceed the maximum price, or we will exceed array bounds later.
			// note that this bins all prices above max as the max, which could produce a skewed
			// distribution.
			if (realized[i] > max_price)
				realized[i] = max_price;
			
			// bin the realized price for this good
//			r_tmp[i] = bin(realized[i], precision);
			temp[i] = bin(realized[i], precision);
		}
		
		// XXX: has to NEW a IntegerArray every time. Put it in Cache? 
		populate(new IntegerArray(new BooleanArray(winner),new IntegerArray(temp)));
	}
	
	// Call this to add another jcde to the current one, with a weight. 
	//	Basically, PP[0] --> w*PP[0] + (1-w)*PP[1] 
	public void addJcde(JointCondDistributionEmpirical jcde1, double w) {
		
		// for all goods
		for (int i = 0; i<total_times; i++) {			

			// normalize conditional pdfs and update cdfs XXX: should we do this for all possible WRs? 
			for (IntegerArray wr : prob[i].keySet()) {
				double[] p0 = prob[i].get(wr);
				double[] p1 = jcde1.getPMF(wr);
				// take weighted average

//				if (p0.length == p1.length){
//					System.out.println("length is the same.");
//				}
//				
//				System.out.print("p0 before = [");
//				for (int j = 0; j < p0.length; j++)
//					System.out.print(p0[j] + ",");
//				System.out.println("]");
//				
//				System.out.print("p1 before = [");
//				for (int j = 0; j < p1.length; j++)
//					System.out.print(p1[j] + ",");
//				System.out.println("]");

				for (int j = 0; j < p0.length; j++)
					p0[j] = (w*p0[j] + p1[j])/(1+w);

//				System.out.print("p0 after = [");
//				for (int j = 0; j < p0.length; j++)
//					System.out.print(p0[j] + ",");
//				System.out.println("]\n");
			}
			

			}
//		normalize();
		}
	
	// Call this to normalize the collected data into a joint distribution
	public void normalize() {
				
		for (int i = 0; i<total_times; i++) {			
			
			// normalize conditional pdfs and update cdfs
			for (IntegerArray r : prob[i].keySet()) {
				double[] p = prob[i].get(r);
				double[] cdf = CDF[i].get(r);
				int s = sum[i].get(r);
				
				// cumulative tally
				cdf[0] = p[0];
				for (int j = 1; j < p.length; j++)
					cdf[j] = cdf[j-1] + p[j];
				
				// normalize wrt sum
				for (int j = 0; j < p.length; j++) {
					p[j] /= s;
					cdf[j] /= s;
				}				
			}
			
			// compute marginal pdf and cdf
			marg_prob[i][0] /= marg_sum;
			marg_cdf[i][0] = marg_prob[i][0];
			for (int j = 1; j<no_bins; j++) {
				marg_prob[i][j] /= marg_sum;
				marg_cdf[i][j] = marg_cdf[i][j-1] + marg_prob[i][j];
			}
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
	public double[] getPMF(IntegerArray history) {
		// Do these if statements make it slow? 
		if (!ready)
			throw new RuntimeException("must normalize first");
		
		if (history.r.d.length >= total_times)
			throw new IllegalArgumentException("no more goods");
		
		// return marginal if first round, and conditional pmf if later rounds
		if (history.r.d.length == 0)
			return marg_prob[0];
		else {
			double[] p = prob[history.r.d.length].get(history);
//			System.out.println("getPMF, history.r.d.length = " + history.r.d.length);
			
			if (p == null) {
				// ut-oh, return the unconditional (marginal) pmf for this good since we have no data
				// todo: we return this when no samples == 0, but maybe we need logic to 
				//       return this when no samples < viable threshold
				p = marg_prob[history.r.d.length];
			}
			return p;
		}
	}
	
	// Gets the probability mass function for good numbered "realized.length", conditional
	// on history winner and realized prices
	@Override
	public double[] getPMF(boolean[] winner, double[] realized) {
		if (realized == null)
			realized = empty_array;
		
		// bin the realized prices
		history = SA_tmp[realized.length];
		history.w.d = winner;
		for (int i = 0; i<realized.length; i++)
			history.r.d[i] = bin(realized[i], precision);
		
		// get the price distribution conditional on realized prices
		return getPMF(history);
	}
	
	public double[] getCDF(IntegerArray history) {
		// Do these if statements make it slow? 
		if (!ready)
			throw new RuntimeException("must normalize first");
		
		if (history.r.d.length >= total_times)
			throw new IllegalArgumentException("no more goods");
		
		if (history.r.d.length == 0)
			return marg_cdf[0];
		else {
			double[] p = CDF[history.r.d.length].get(history);
//			System.out.println("getCDF, history.r.d.length = " + history.r.d.length);
	
			if (p == null) {
	//			System.out.println("cdf detected null");
				// ut-oh, return the unconditional (marginal) cdf for this good since we have no data
				// todo: we return this when no samples == 0, but maybe we need logic to 
				//       return this when no samples < viable threshold
				p = marg_cdf[history.r.d.length];
			}
			return p;
		}
	}
	
	public double[] getCDF(boolean[] winner, double[] realized) {
		if (realized == null)
			realized = empty_array;
		
		// bin the realized prices
		history = SA_tmp[realized.length];
		history.w.d = winner;
		for (int i = 0; i<realized.length; i++)
			history.r.d[i] = bin(realized[i], precision);
		
		return getCDF(history);
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
	public double sampleCondPrices(Random rng, boolean[] winner, double[] realized) {
		if (!ready)
			throw new RuntimeException("must normalize first");
		
		// Put into WR form
		int round = winner.length;		
		IntegerArray wr = SA_tmp[round]; 
		wr.w.d = winner;
		for (int i = 0; i < round; i++)
			wr.r.d[i] = bin(realized[i], precision);
		
		double[] cdf = getCDF(wr);

		int return_idx = 0;		// the bin index for return value
		
		// choose a random spot on the cdf
		double random = rng.nextDouble();			
		for (int i = 0; i < cdf.length; i++) {
			if (cdf[i] >= random) {
				return_idx = i;
				break;
			}
		}
		return (return_idx*precision);
	}
	
	// sample the probability distribution, and returns an array of prices, one per good
	/*
	public int[] getSample(Random rng) {
		if (!ready)
			throw new RuntimeException("must normalize first");
		
		int bins[] = new int[total_times];
		
		for (int i = 0; i<total_times; i++) {
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
						SA_tmp[i+1].r.d[k] = bins[k];
					
					// go onto next good
					break;
				}
			}
			
			// sanity check: make sure we picked something. 
		}
		
		return bins;
	}
	*/

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
		if (take_log == false)
				System.out.println("didn't take price records, can't output");
		else {
			IntegerArray history;
			for (Integer history_idx : log_indices) {
				history = Cache.getWRfromidx(history_idx);
				int len = history.r.d.length - 1;
				
				// print out winning history [0, a.d.length-1]
				for (int i = 0; i < len+1; i++) {
					if (history.w.d[i])
						fw.write("1,");
					else
						fw.write("0,");
				}
				
				// print out [0, a.d.length-2]
				for (int i = 0; i<len; i++)
					fw.write(history.r.d[i]*precision + ",");
				
				// print out final value, [a.d.length-1]
				if (history.r.d.length > 0)
					fw.write(history.r.d[len]*precision + "\n");
			}
		}
	}
	
	@Override
	public void outputNormalized() {
		int total_act = 0;
		int total_exp = 0;
		
		System.out.println("max_price witnessed = " + witnessed_max_price);
		
		for (int i = 0; i<total_times; i++) {
			int max_realizations = MathOps.ipow(this.no_bins, i);
			
			total_exp += max_realizations;
			total_act += prob[i].size();
			
			System.out.println("prob[" + i + "].size() == " + prob[i].size() + ", max_realizations=" + max_realizations);
			
			for (Entry<IntegerArray, double[]> e : prob[i].entrySet()) {
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
	
//	// Output hits for each possible WR history (inspect holes)
//	public void outputHits(FileWriter fw) throws IOException {
//		int total_act = 0;
//		int total_exp = 0;
//		
////		System.out.println("max_price witnessed = " + witnessed_max_price);
//		
//		
//		for (int i = 0; i<total_times; i++) {
//			int max_realizations = MathOps.ipow(this.no_bins, i);
//			
//			total_exp += max_realizations;
//			total_act += prob[i].size();
//			
////			System.out.println("prob[" + i + "].size() == " + prob[i].size() + ", max_realizations=" + max_realizations);
//			
//			int[][] hits;
//			hits = new int[bins.d.length][bins.d.length];
//			for (BooleanArray winner : Cache.getWinningHistory(i)) {
//				
////				for (IntegerArray realized : Cache.getCartesianProduct(bins, i)) {
//					wr = new IntegerArray(winner, realized); 	    		
// 
////				}
//			}
//			
////			for (IntegerArray wr : Cache.allWR){
////				
////			}
//			
//			
//			for (Entry<IntegerArray, double[]> e : prob[i].entrySet()) {
//				System.out.print("pr(" + i + " | w = {");
//
//				for (boolean p : e.getKey().w.d) {
//					if (p == true)
//						System.out.print("1 ,");
//					else
//						System.out.print("0 ,");
//				}
//				System.out.print("}, p = {");
//
//				for (int p : e.getKey().r.d)
//					System.out.print(val(p, precision) + ", ");
//				
//				System.out.print("}) [hits=" + sum[i].get(e.getKey()) + "] ==> {");
//				
//				for (double p : e.getValue())
//					System.out.print(p + ", ");
//				
//				System.out.println("}");				
//			}
//		}
//		
//		System.out.println("Actual realizations == " + total_act + " of a maximum == " + total_exp);
//	}

	
	public static void main(String args[]) {
		// TESTING / EXAMPLE
		Random rng = new Random();
		
		int total_times = 2;
		double precision = 1.0;
		double max_price = 5.0;
		
		boolean aggregate_prices = true;		// whether to test aggregating prices (addJcde)
		boolean output_before = true;			// output PP before aggregating
		boolean output_after = true;			// output after aggregating
		double weight = 1;
		boolean sample_prices = false;

		JointCondDistributionEmpirical jcde = new JointCondDistributionEmpirical(total_times, precision, max_price ,false);
		
		// Populate distribution
		jcde.populate(new boolean[] {false, false}, new double[] {0, 5});
		
		jcde.populate(new boolean[] {false, false}, new double[] {1, 0});
//		jcde.populate(new boolean[] {false, false}, new double[] {1, 2});
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

		if (output_before == true) {
		
			System.out.println(" ----- if first round loses ----- ");
			
			double[] pmf;
			
			// get the PMF of good 0
			System.out.print("pmf(0 | { }, { }): ");		
			for (double p : jcde.getPMF(new boolean[] {}, new double[] {}))
				System.out.print(p + " ");
			System.out.println("");
	
			System.out.print("cdf(0 | { }, { }): ");
			for (double p : jcde.getCDF(new boolean[] {}, new double[] {}))
				System.out.print(p + " ");
			System.out.println("");
	
			// get PMF of good 1, cond. on price of good 0 being $1
			System.out.print("pmf(1 | {0}, {1}): ");
			for (double p : jcde.getPMF(new boolean[] {false}, new double[] {1}))
				System.out.print(p + " ");		
			System.out.println("");
	
			System.out.print("cdf(1 | {0}, {1}): ");
			for (double p : jcde.getCDF(new boolean[] {false}, new double[] {1}))
				System.out.print(p + " ");		
			System.out.println("");
	
			// get PMF of good 1, cond. on price of good 0 being $2
			System.out.print("pmf(1 | {0}, {2}): ");
			for (double p : jcde.getPMF(new boolean[] {false}, new double[] {2}))
				System.out.print(p + " ");
			System.out.println("");
	
			System.out.print("cdf(1 | {0}, {2}): ");
			for (double p : jcde.getCDF(new boolean[] {false}, new double[] {2}))
				System.out.print(p + " ");
			System.out.println("");
	
			
			// get PMF of good 1, cond. on price of good 0 being $3
			System.out.print("pmf(1 | {0}, {3}): ");
			for (double p : jcde.getPMF(new boolean[] {false}, new double[] {3}))
				System.out.print(p + " ");		
			System.out.println("");
			
			System.out.print("cdf(1 | {0}, {3}): ");
			for (double p : jcde.getCDF(new boolean[] {false}, new double[] {3}))
				System.out.print(p + " ");		
			System.out.println("");
	
			
			// get the PMF of good 1, conf on price of good 0 being $0
			System.out.print("pmf(1 | {0}, {0}): ");
			for (double p : jcde.getPMF(new boolean[] {false}, new double[] {0}))
				System.out.print(p + " ");		
			System.out.println("");
	
			System.out.print("cdf(1 | {0}, {0}): ");
			for (double p : jcde.getCDF(new boolean[] {false}, new double[] {0}))
				System.out.print(p + " ");		
	
			System.out.println("\n");
	
			
	//		// get the expected final price of good 0; the list of conditional prices necessarily empty
	//		System.out.println("efp(0 | { }, { }) = " + jcde.getExpectedFinalPrice(new boolean[] {}, new double[] {}));
	//		
	//		// get the expected final price of good 1 for various values of good 0.
	//		System.out.println("efp(1 | {0}, {1}) = " + jcde.getExpectedFinalPrice(new boolean[] {false}, new double[] {1}));
	//		System.out.println("efp(1 | {0}, {2}) = " + jcde.getExpectedFinalPrice(new boolean[] {false}, new double[] {2}));
	//		System.out.println("efp(1 | {0}, {3}) = " + jcde.getExpectedFinalPrice(new boolean[] {false}, new double[] {3}));
	//		System.out.println("efp(1 | {0}, {0}) = " + jcde.getExpectedFinalPrice(new boolean[] {false}, new double[] {0}));
			
			
			
			System.out.println(" ----- if first round wins ----- ");
	
			// get PMF of good 1, cond. on price of good 0 being $1
			System.out.print("pmf(1 | {1}, {1}): ");
			for (double p : jcde.getPMF(new boolean[] {true}, new double[] {1}))
				System.out.print(p + " ");		
			System.out.println("");
	
			System.out.print("cdf(1 | {1}, {1}): ");
			for (double p : jcde.getCDF(new boolean[] {true}, new double[] {1}))
				System.out.print(p + " ");		
			System.out.println("");
	
			// get PMF of good 1, cond. on price of good 0 being $2
			System.out.print("pmf(1 | {1}, {2}): ");
			for (double p : jcde.getPMF(new boolean[] {true}, new double[] {2}))
				System.out.print(p + " ");		
			System.out.println("");
			
			System.out.print("cdf(1 | {1}, {2}): ");
			for (double p : jcde.getCDF(new boolean[] {true}, new double[] {2}))
				System.out.print(p + " ");		
			System.out.println("");
			
			// get PMF of good 1, cond. on price of good 0 being $3
			System.out.print("pmf(1 | {1}, {3}): ");
			for (double p : jcde.getPMF(new boolean[] {true}, new double[] {3}))
				System.out.print(p + " ");		
			System.out.println("");
			
			System.out.print("cdf(1 | {1}, {3}): ");
			for (double p : jcde.getCDF(new boolean[] {true}, new double[] {3}))
				System.out.print(p + " ");		
			System.out.println("");
	
			// get the PMF of good 1, conf on price of good 0 being $0
			System.out.print("pmf(1 | {1}, {0}): ");
			for (double p : jcde.getPMF(new boolean[] {true}, new double[] {0}))
				System.out.print(p + " ");		
			System.out.println("");
	
			System.out.print("cdf(1 | {1}, {0}): ");
			for (double p : jcde.getCDF(new boolean[] {true}, new double[] {0}))
				System.out.print(p + " ");		
			System.out.println("");
			
			System.out.println("");
			
	//		// get the expected final price of good 0; the list of conditional prices necessarily empty
	//		System.out.println("efp(0 | { }, { }) = " + jcde.getExpectedFinalPrice(new boolean[] {}, new double[] {}));
	//		
	//		// get the expected final price of good 1 for various values of good 0.
	//		System.out.println("efp(1 | {1}, {1}) = " + jcde.getExpectedFinalPrice(new boolean[] {true}, new double[] {1}));
	//		System.out.println("efp(1 | {1}, {2}) = " + jcde.getExpectedFinalPrice(new boolean[] {true}, new double[] {2}));
	//		System.out.println("efp(1 | {1}, {3}) = " + jcde.getExpectedFinalPrice(new boolean[] {true}, new double[] {3}));
	//		System.out.println("efp(1 | {1}, {4}) = " + jcde.getExpectedFinalPrice(new boolean[] {true}, new double[] {4}));
	//		System.out.println("efp(1 | {1}, {0}) = " + jcde.getExpectedFinalPrice(new boolean[] {true}, new double[] {0}));
	
		}
		
		if (sample_prices == true){
		
			System.out.println(" ---------- Sample prices ---------- ");
			
			int sample_size = 10000;
			double[] sample = new double[sample_size];
			double[] epmf = new double[jcde.no_bins];		// Empirical pmf		
	
			// first round
			// > Case 1
			System.out.print("sampling first round prices. pmf(0 | {}, {}): ");
			for (double p : jcde.getPMF(new boolean[] {}, new double[] {}))
				System.out.print(p + " ");		
			System.out.println("");
	
			// sample, tally, and normalize
			for (int i = 0; i < epmf.length; i++)
				epmf[i] = 0;
			for (int i = 0; i < sample_size; i++) {
				sample[i] = jcde.sampleCondPrices(rng, new boolean[] {}, new double[] {});
				epmf[JointCondDistribution.bin(sample[i],jcde.precision)] ++;
			}		
			// normalize
			for (int j = 0; j < epmf.length; j++)
				epmf[j] /= sample_size;
	
			// output
			System.out.print("sampling first round prices.epmf(0 | {}, {}): ");
			for (int i = 0; i < epmf.length; i++)
				System.out.print(epmf[i] + " ");
			System.out.println();
			
			// second round
			// > Case 2
			System.out.print("sampling second round prices. pmf(0 | {false}, {1}): ");
			for (double p : jcde.getPMF(new boolean[] {false}, new double[] {1.0}))
				System.out.print(p + " ");		
			System.out.println("");
	
			// sample, tally, and normalize
			for (int i = 0; i < epmf.length; i++)
				epmf[i] = 0;
			for (int i = 0; i < sample_size; i++) {
				sample[i] = jcde.sampleCondPrices(rng, new boolean[] {false}, new double[] {1.0});
				epmf[JointCondDistribution.bin(sample[i],jcde.precision)] ++;
			}		
			// normalize
			for (int j = 0; j < epmf.length; j++)
				epmf[j] /= sample_size;
	
			// output
			System.out.print("sampling second round prices.epmf(0 | {false}, {1}): ");
			for (int i = 0; i < epmf.length; i++)
				System.out.print(epmf[i] + " ");
			System.out.println();
	
	
			// > Case 3
			System.out.print("sampling second round prices. pmf(0 | {true}, {1}): ");
			for (double p : jcde.getPMF(new boolean[] {true}, new double[] {1.0}))
				System.out.print(p + " ");		
			System.out.println("");
			
			// sample, tally, and normalize
			for (int i = 0; i < epmf.length; i++)
				epmf[i] = 0;
			for (int i = 0; i < sample_size; i++) {
				sample[i] = jcde.sampleCondPrices(rng, new boolean[] {true}, new double[] {1.0});
				epmf[JointCondDistribution.bin(sample[i],jcde.precision)] ++;
			}		
			// normalize
			for (int j = 0; j < epmf.length; j++)
				epmf[j] /= sample_size;
	
			// output
			System.out.print("sampling second round prices.epmf(0 | {true}, {1}): ");
			for (int i = 0; i < epmf.length; i++)
				System.out.print(epmf[i] + " ");
			System.out.println();
		}
		
		if (aggregate_prices == true) {
			JointCondFactory jcf = new JointCondFactory(total_times, precision, max_price);
			JointCondDistributionEmpirical jcde1 = jcf.makeUniform(false);
			
			jcde.addJcde(jcde1, weight);
		}
		
		if (output_after == true) {
			
			System.out.println(" ----- if first round loses ----- ");
			
			double[] pmf;
			
			// get the PMF of good 0
			System.out.print("pmf(0 | { }, { }): ");		
			for (double p : jcde.getPMF(new boolean[] {}, new double[] {}))
				System.out.print(p + " ");
			System.out.println("");
	
			System.out.print("cdf(0 | { }, { }): ");
			for (double p : jcde.getCDF(new boolean[] {}, new double[] {}))
				System.out.print(p + " ");
			System.out.println("");
	
			// get PMF of good 1, cond. on price of good 0 being $1
			System.out.print("pmf(1 | {0}, {1}): ");
			for (double p : jcde.getPMF(new boolean[] {false}, new double[] {1}))
				System.out.print(p + " ");		
			System.out.println("");
	
			System.out.print("cdf(1 | {0}, {1}): ");
			for (double p : jcde.getCDF(new boolean[] {false}, new double[] {1}))
				System.out.print(p + " ");		
			System.out.println("");
	
			// get PMF of good 1, cond. on price of good 0 being $2
			System.out.print("pmf(1 | {0}, {2}): ");
			for (double p : jcde.getPMF(new boolean[] {false}, new double[] {2}))
				System.out.print(p + " ");
			System.out.println("");
	
			System.out.print("cdf(1 | {0}, {2}): ");
			for (double p : jcde.getCDF(new boolean[] {false}, new double[] {2}))
				System.out.print(p + " ");
			System.out.println("");
	
			
			// get PMF of good 1, cond. on price of good 0 being $3
			System.out.print("pmf(1 | {0}, {3}): ");
			for (double p : jcde.getPMF(new boolean[] {false}, new double[] {3}))
				System.out.print(p + " ");		
			System.out.println("");
			
			System.out.print("cdf(1 | {0}, {3}): ");
			for (double p : jcde.getCDF(new boolean[] {false}, new double[] {3}))
				System.out.print(p + " ");		
			System.out.println("");
	
			
			// get the PMF of good 1, conf on price of good 0 being $0
			System.out.print("pmf(1 | {0}, {0}): ");
			for (double p : jcde.getPMF(new boolean[] {false}, new double[] {0}))
				System.out.print(p + " ");		
			System.out.println("");
	
			System.out.print("cdf(1 | {0}, {0}): ");
			for (double p : jcde.getCDF(new boolean[] {false}, new double[] {0}))
				System.out.print(p + " ");		
	
			System.out.println("\n");
	
			
	//		// get the expected final price of good 0; the list of conditional prices necessarily empty
	//		System.out.println("efp(0 | { }, { }) = " + jcde.getExpectedFinalPrice(new boolean[] {}, new double[] {}));
	//		
	//		// get the expected final price of good 1 for various values of good 0.
	//		System.out.println("efp(1 | {0}, {1}) = " + jcde.getExpectedFinalPrice(new boolean[] {false}, new double[] {1}));
	//		System.out.println("efp(1 | {0}, {2}) = " + jcde.getExpectedFinalPrice(new boolean[] {false}, new double[] {2}));
	//		System.out.println("efp(1 | {0}, {3}) = " + jcde.getExpectedFinalPrice(new boolean[] {false}, new double[] {3}));
	//		System.out.println("efp(1 | {0}, {0}) = " + jcde.getExpectedFinalPrice(new boolean[] {false}, new double[] {0}));
			
			
			
			System.out.println(" ----- if first round wins ----- ");
	
			// get PMF of good 1, cond. on price of good 0 being $1
			System.out.print("pmf(1 | {1}, {1}): ");
			for (double p : jcde.getPMF(new boolean[] {true}, new double[] {1}))
				System.out.print(p + " ");		
			System.out.println("");
	
			System.out.print("cdf(1 | {1}, {1}): ");
			for (double p : jcde.getCDF(new boolean[] {true}, new double[] {1}))
				System.out.print(p + " ");		
			System.out.println("");
	
			// get PMF of good 1, cond. on price of good 0 being $2
			System.out.print("pmf(1 | {1}, {2}): ");
			for (double p : jcde.getPMF(new boolean[] {true}, new double[] {2}))
				System.out.print(p + " ");		
			System.out.println("");
			
			System.out.print("cdf(1 | {1}, {2}): ");
			for (double p : jcde.getCDF(new boolean[] {true}, new double[] {2}))
				System.out.print(p + " ");		
			System.out.println("");
			
			// get PMF of good 1, cond. on price of good 0 being $3
			System.out.print("pmf(1 | {1}, {3}): ");
			for (double p : jcde.getPMF(new boolean[] {true}, new double[] {3}))
				System.out.print(p + " ");		
			System.out.println("");
			
			System.out.print("cdf(1 | {1}, {3}): ");
			for (double p : jcde.getCDF(new boolean[] {true}, new double[] {3}))
				System.out.print(p + " ");		
			System.out.println("");
	
			// get the PMF of good 1, conf on price of good 0 being $0
			System.out.print("pmf(1 | {1}, {0}): ");
			for (double p : jcde.getPMF(new boolean[] {true}, new double[] {0}))
				System.out.print(p + " ");		
			System.out.println("");
	
			System.out.print("cdf(1 | {1}, {0}): ");
			for (double p : jcde.getCDF(new boolean[] {true}, new double[] {0}))
				System.out.print(p + " ");		
			System.out.println("");
			
			System.out.println("");
			
	//		// get the expected final price of good 0; the list of conditional prices necessarily empty
	//		System.out.println("efp(0 | { }, { }) = " + jcde.getExpectedFinalPrice(new boolean[] {}, new double[] {}));
	//		
	//		// get the expected final price of good 1 for various values of good 0.
	//		System.out.println("efp(1 | {1}, {1}) = " + jcde.getExpectedFinalPrice(new boolean[] {true}, new double[] {1}));
	//		System.out.println("efp(1 | {1}, {2}) = " + jcde.getExpectedFinalPrice(new boolean[] {true}, new double[] {2}));
	//		System.out.println("efp(1 | {1}, {3}) = " + jcde.getExpectedFinalPrice(new boolean[] {true}, new double[] {3}));
	//		System.out.println("efp(1 | {1}, {4}) = " + jcde.getExpectedFinalPrice(new boolean[] {true}, new double[] {4}));
	//		System.out.println("efp(1 | {1}, {0}) = " + jcde.getExpectedFinalPrice(new boolean[] {true}, new double[] {0}));
	
		}

	}	
}

