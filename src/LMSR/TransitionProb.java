package LMSR;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import speed.IntegerArray;

// TODO: This class still needs to be optimized for speed in populate() and getPMF().
//       In particular, get rid of the need to make a new Integer[] for each call.
//       Maybe use thread local storage?

public class TransitionProb{
	
	boolean take_log, ready = false;
	int no_agents, no_rounds, total_rounds;
	
	// Each state is an IntegerArray
	
	// One per agent
	HashMap<IntegerArray, ArrayList<IntegerArray>>[] NextStates;	// A list of SA ==> further states
//	HashMap<IntegerArray, double[]>[] P;							// Corresponding transition probs: SA ==> further states
	HashMap<IntegerArray, Double>[] counts;							// num of times each state has been reached
	HashMap<IntegerArray, Double>[] R;								// expected rewards
	
	// holder of things
	IntegerArray SA_tmp[];
	
	@SuppressWarnings("unchecked")
	public TransitionProb(int no_agents, int no_rounds, boolean take_log) {
		Cache.init();	// XXX: need this? 
		this.take_log = take_log;
		this.no_agents = no_agents;
		this.no_rounds = no_rounds;
		this.total_rounds = no_rounds*no_agents;
		
		// Possible actions
		IntegerArray ActionSpace = new IntegerArray(new int[] {-1,0,1});
				
		// init this.P, this.P, this.R
		this.NextStates = new HashMap[total_rounds];
//		this.P = new HashMap[total_rounds];
		this.counts = new HashMap[total_rounds];
		this.R = new HashMap[total_rounds];
		
		// Tally total number of possible histories... want to see them all hit
		for (int i = 0; i < total_rounds; i++) {	
			int max_states = (int) java.lang.Math.pow(ActionSpace.d.length, i); 
			
			this.NextStates[i] = new HashMap<IntegerArray, ArrayList<IntegerArray>>(max_states); 
//			this.P[i] = new HashMap<IntegerArray, double[]>(max_realizations);
			this.counts[i] = new HashMap<IntegerArray, Double>(max_states);
			this.R[i] = new HashMap<IntegerArray, Double>(max_states);
		}
		
		// For temporarily holding states 
		this.SA_tmp = new IntegerArray[total_rounds+1];
		for (int i = 0; i < total_rounds + 1; i++)
			this.SA_tmp[i] = new IntegerArray(i);
		
	}
		
	// Call this to reset the internal state. This is automatically called when instantiated. 
	public void reset() {
		ready = false;
		
		// clean HashMaps
		for (HashMap<IntegerArray, ArrayList<IntegerArray>> a : this.NextStates)
			a.clear();
//		for (HashMap<IntegerArray, double[]> a : this.P)
//			a.clear();
		for (HashMap<IntegerArray, Double> a : this.counts)
			a.clear();
		for (HashMap<IntegerArray, Double> a : this.R)
			a.clear();

	}
	
	// Call this once per realized game
	public void populate(IntegerArray history, double[] reward) {
		if (history.d.length != total_rounds || reward.length != total_rounds)
			throw new RuntimeException("length game history must equal reward vector length");

		// TODO: stopped here
		
		// Store all counts
		for (int i = 0; i < total_rounds - no_agents; i++) {
			IntegerArray SA = SA_tmp[i];
			IntegerArray nextS = SA_tmp[i+no_agents-1];
			
			// SA --> nextS. Copy from "history"
			for (int j = 0; j<i; j++){
				SA.d[j] = history.d[j];
				nextS.d[j] = history.d[j];
			}
			for (int j = i; j < i+no_agents-1; j++)
				nextS.d[j] = history.d[j];
		
			// get further states conditioned on earlier prices
			ArrayList<IntegerArray> NS = NextStates[i].get(SA);
						
			if (NS == null) {
				
				// this is our first entry into the distribution. Initialize
				NS = Cache.ExtensionStates(SA, no_agents);
				double[] p = new double[NS.size()];
				
				for (int j = 0; j < p.length; j++)
					p[j] = 0;
				p[NS.indexOf(nextS)] = 1;

				// Don't have to do "new copy of" right? XXX
				NextStates[i].put(SA, NS);
				P[i].put(SA, p);
			} else {
				double[] p = P[i].get(SA);
				p[NS.indexOf(nextS)] ++;
			}			
		}
		
		// Store all R
		for (int i = 0; i < total_rounds; i++) {
			IntegerArray SA = SA_tmp[i];
			
			// SA. Copy from "history"
			for (int j = 0; j<i; j++)
				SA.d[j] = history.d[j];
		
			// If this is our first entry into the distribution, initialize						
			if (! R[i].containsKey(SA))				
				R[i].put(SA, reward[i]);
			else
				R[i].put(SA,R[i].get(SA) + reward[i]);		// XXX: does this rewrite the old entry?
		}
	}
	
	/*
	// Call this to do a weighted average of 2 MDPs, used in smoothing. TODO 
	//	Basically, PNextStates[0] --> w*PNextStates[0] + (1-w)*PNextStates[1] 
	public void addMDP(JointCondDistributionEmpirical jcde1, double w) {
		
		// for all goods
		for (int i = 0; i<total_rounds; i++) {			

			// normalize conditional pdfs and update cdfs XXX: should we do this for all possible WRs? 
			for (IntegerArray SA : prob[i].keySet()) {
				double[] p0 = prob[i].get(SA);
				double[] p1 = jcde1.getPMF(SA);
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
	*/
	
	// Call this to normalize P and R
	public void normalize() {

		// Normalize P and R for first half
		for (int i = 0; i < total_rounds - no_agents; i++) {			
			for (IntegerArray SA : NextStates[i].keySet()) {

				// Get entities
				double[] p = P[i].get(SA);
				double r = R[i].get(SA);
				
				// Get sum
				double sum = 0.0;
				for (int j = 0; j < p.length; j++)
					sum += p[j];
				
				// Divide by sum 
				r = r/sum;
				for (int j = 0; j < p.length; j++)
					p[j] /= sum;
			}
		}
		
	// Normalize R for the second half
	for (int i = total_rounds - no_agents; i < total_rounds; i++) {			
		for (IntegerArray SA : R[i].keySet()) {
			
			// Get entities
			double r = R[i].get(SA); 

			// Get sum and normalize
			double sum = 0.0;
			for (int j = 0; j < p.length; j++)
				sum += p[j];
			r = r/sum;
		}
	}
	
	ready = true;
}

	// get Possible states to transition into from SA
	public ArrayList<IntegerArray> getNextStates(IntegerArray SA) {
		return NextStates[SA.d.length].get(SA);
	}

	// get transition probabilities from SA to further states
	public double[] getP(IntegerArray SA) {

		// Sanity check
		if (!ready)
			throw new RuntimeException("must normalize first");
		if (SA.d.length >= total_rounds - no_agents)
			throw new IllegalArgumentException("no more rounds");
		
		// Get transition probabilities
		double[] p = P[SA.d.length].get(SA);  
		
		// TODO: Should use "near by states" when a (state,action) is not observed, not just an arbitrary one
		if (p == null){
			System.out.println("warning: (state,action) = " + SA.print() + " not observed");

			// Use the first one that comes up
			if (P[SA.d.length].entrySet().size() > 0){
				Iterator<IntegerArray> it = P[SA.d.length].keySet().iterator();
				p = P[SA.d.length].get(it.next());
			}else{
				throw new IllegalArgumentException("Nothing observed at all for the round of (state,action) = " + SA.print());
			}
				
		}
		return p;
	}
	
	// Get rewards
	public double getR(IntegerArray SA) {
		
		// Sanity check
		if (!ready)
			throw new RuntimeException("must normalize first");
		if (SA.d.length >= total_rounds)
			throw new IllegalArgumentException("no more rounds");

		if (R[SA.d.length].containsKey(SA))
			return R[SA.d.length].get(SA);
		else
			return 0.0;		// TODO: something to tweak. Set it artificially high so that all states are explored? 
	}
	
	

	/* might want in the future
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
						fw.SAite("1,");
					else
						fw.SAite("0,");
				}
				
				// print out [0, a.d.length-2]
				for (int i = 0; i<len; i++)
					fw.SAite(history.r.d[i]*precision + ",");
				
				// print out final value, [a.d.length-1]
				if (history.r.d.length > 0)
					fw.SAite(history.r.d[len]*precision + "\n");
			}
		}
	}
	
	@Override
	public void outputNormalized() {
		int total_act = 0;
		int total_exp = 0;
		
		System.out.println("max_price witnessed = " + witnessed_max_price);
		
		for (int i = 0; i<total_rounds; i++) {
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
		*/


	// Testing
	public static void main(String args[]) throws IOException {

		Cache.init();
		
		// parameters
		int no_rounds = 2;
		int no_agents = 2;
		int total_rounds = no_rounds*no_agents;

		// Manually populate MDP
		Set<IntegerArray> histories = Cache.genStates(total_rounds);
		double[] reward = new double[total_rounds];
		for (int i = 0; i < reward.length; i++)
			reward[i] = 1.0;
		
		TransitionProb mdp = new TransitionProb(no_agents, no_rounds, false);
		for (IntegerArray hist: histories)
			mdp.populate(hist, reward);
		mdp.normalize();
		
		// Test get next states, getP, and getR
		ArrayList<IntegerArray> SAs = new ArrayList<IntegerArray>();
		SAs.add(new IntegerArray(new int[] {-1}));
		SAs.add(new IntegerArray(new int[] {0}));
		SAs.add(new IntegerArray(new int[] {1}));
		SAs.add(new IntegerArray(new int[] {1,1}));
		SAs.add(new IntegerArray(new int[] {0,0,0}));
		SAs.add(new IntegerArray(new int[] {-1,-1,-1,-1}));
		
		IntegerArray SA;
		Iterator<IntegerArray> it = SAs.iterator();
		while (it.hasNext()){
			SA = it.next();

			// Get stuff
			ArrayList<IntegerArray> NSs = mdp.getNextStates(SA);
			double[] p = mdp.getP(SA);
			double r = mdp.getR(SA);
			
			// print SA and reward
			System.out.println("SA = " + SA.print() + ": r = " + r);
			
			// print next round states
			System.out.print("NSs = ");
			for (int i = 0; i < NSs.size()-1; i++)
				System.out.print(NSs.get(i).print() + "  ");
			System.out.println(NSs.get(NSs.size()-1).print());
			
			// print transition probs
			System.out.print("p = [");
			for (int i = 0; i < p.length-1; i++)
				System.out.print(p[i] + ",");
			System.out.println(p[p.length-1] + "]");
		}
		
	}	
}

