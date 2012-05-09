package LMSR;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import speed.IntegerArray;

// Stores transition probabilities P and rewards R for all agents in the game. (\theta,(S,A)) is a state.  
public class MDP{
	
	boolean ready = false;
	int no_agents, no_rounds, total_rounds;
	
	// One per agent: differentiate if signal is T (-->1) or F (-->0)
	HashMap<IntegerArray, ArrayList<IntegerArray>>[] NextStates;	// [round]: (S,A) ==> further S. A list of further S
	HashMap<IntegerArray, double[]>[][] P;							// [round][T/F]: (S,A) ==> further S. Transition probs
	HashMap<IntegerArray, Integer>[][] counts;						// [round][T/F]: S. num of times S has been reached
	HashMap<IntegerArray, Double>[][] R;							// [round][T/F]: (S,A). expected rewards 
	
	// holder of (S,A)s
	IntegerArray SA_tmp[];
	
	@SuppressWarnings("unchecked")
	public MDP(int no_agents, int no_rounds) {

		// game parameters
		this.no_agents = no_agents;
		this.no_rounds = no_rounds;
		this.total_rounds = no_rounds*no_agents;
		
		// Possible actions - HARDCODED
		IntegerArray ActionSpace = new IntegerArray(new int[] {-1,0,1});
				
		// MDP mappings: initiate
		this.NextStates = new HashMap[total_rounds];
		this.counts = new HashMap[total_rounds][2];
		this.P = new HashMap[total_rounds][2];
		this.R = new HashMap[total_rounds][2];
		
		for (int i = 0; i < total_rounds; i++) {	
			int max_states = (int) java.lang.Math.pow(ActionSpace.d.length, i+1);	// max no. of states in round i 
			
			this.NextStates[i] = new HashMap<IntegerArray, ArrayList<IntegerArray>>(max_states); 
			
			this.P[i][0] = new HashMap<IntegerArray, double[]>(max_states);
			this.P[i][1] = new HashMap<IntegerArray, double[]>(max_states);
			this.counts[i][0] = new HashMap<IntegerArray, Integer>(max_states);
			this.counts[i][1] = new HashMap<IntegerArray, Integer>(max_states);
			this.R[i][0] = new HashMap<IntegerArray, Double>(max_states);
			this.R[i][1] = new HashMap<IntegerArray, Double>(max_states);
		}
		
		// initiate: temporary state holder
		this.SA_tmp = new IntegerArray[total_rounds];
		for (int i = 0; i < total_rounds; i++)
			this.SA_tmp[i] = new IntegerArray(i+1);
		
	}
		
	// Call this to reset the internal state. This is automatically called when instantiated. 
	public void reset() {
		ready = false;		// not normalized yet
		
		// clean HashMaps
		for (HashMap<IntegerArray, ArrayList<IntegerArray>> a : this.NextStates)
			a.clear();
		for (HashMap<IntegerArray, double[]> a : this.P[0])
			a.clear();
		for (HashMap<IntegerArray, double[]> a : this.P[1])
			a.clear();
		for (HashMap<IntegerArray, Integer> a : this.counts[0])
			a.clear();
		for (HashMap<IntegerArray, Integer> a : this.counts[1])
			a.clear();
		for (HashMap<IntegerArray, Double> a : this.R[0])
			a.clear();
		for (HashMap<IntegerArray, Double> a : this.R[1])
			a.clear();
	}
	
	// Call this once per realized game
	public void populate(IntegerArray history, double[] reward, Signal theta) {
		if (history.d.length != total_rounds || reward.length != total_rounds)
			throw new RuntimeException("length game history must equal reward vector length");

		// signals into 0/1 format 
		int[] s = new int[no_agents];
		for (int k = 0; k < no_agents; k++){
			if (theta.getSignal(k) == true)
				s[k] = 1;
		}

		// Store counts: no. of times a state is reached
		for (int i = 0; i < total_rounds; i++){
			IntegerArray SA = SA_tmp[i];
			for (int j = 0; j <= i; j++)
				SA.d[j] = history.d[j];
			
			int agent_id = i % no_agents;
			
			if (!counts[i][s[agent_id]].containsKey(SA)){
				// create this instance
				counts[i][s[agent_id]].put(new IntegerArray(Arrays.copyOf(SA.d, SA.d.length)), 1);
				R[i][s[agent_id]].put(new IntegerArray(Arrays.copyOf(SA.d, SA.d.length)), reward[i]);

//				System.out.println("put R(SA" + SA.print() + ") = " + R[i].get(SA));
//				System.out.println("put counts(SA" + SA.print() + ") = " + counts[i].get(SA));
			} 
			else {
				R[i][s[agent_id]].put(new IntegerArray(Arrays.copyOf(SA.d, SA.d.length)), R[i][s[agent_id]].get(SA) + reward[i]);
				counts[i][s[agent_id]].put(new IntegerArray(Arrays.copyOf(SA.d, SA.d.length)), counts[i][s[agent_id]].get(SA) + 1);
				
//				System.out.println("create new, put R(SA" + SA.print() + ") = " + R[i].get(SA));
//				System.out.println("create new, put counts(SA" + SA.print() + ") = " + counts[i].get(SA));
			}

		}
	}
	
	
	// Call this to normalize/compute P and R
	public void normalize() {

		// Normalize P and R for first half of the game
		for (int i = 0; i < total_rounds-no_agents; i++) {
			for (int k = 0; k <= 1; k++){
			
	//			System.out.println("normalizing for round i = " + i + ". counts[i].keySet.size() = " + counts[i].keySet().size());
				for (IntegerArray SA : counts[i][k].keySet()) {				
	
					// Normalize Rewards
					R[i][k].put(SA,R[i][k].get(SA)/counts[i][k].get(SA));	// modify values, so don't need "copies of"
	//				System.out.println("R[" + i + "].put(" + SA.print() + "," + R[i].get(SA) + ")");
					
					// Probabilities: Figure out possible next round states
					ArrayList<IntegerArray> NS = Cache.ExtensionStates(SA, no_agents);
					
					// turn counts into pmf form
					double[] p = new double[NS.size()];
					for (int j = 0; j < p.length; j++)
						p[j] = 0.0;
	
					Iterator<IntegerArray> it = NS.iterator();
					int sum = 0;
					while (it.hasNext()){
						IntegerArray nextS = it.next();
						if (counts[i+no_agents-1][k].containsKey(nextS)) {
							p[NS.indexOf(nextS)] = counts[i+no_agents-1][k].get(nextS);						
							sum += counts[i+no_agents-1][k].get(nextS);
						}
					}
					// Normalize
					for (int j = 0; j < p.length; j++)
						p[j] /= sum;				
	
					// Put into memory
	//				NextStates[i].put(new IntegerArray(Arrays.copyOf(SA.d, SA.d.length)), NS);
	//				P[i].put(new IntegerArray(Arrays.copyOf(SA.d, SA.d.length)), p);				
					NextStates[i].put(SA, NS);
					P[i][k].put(SA, p);
				}
			}
		}
			// Normalize R for second half of the game
			for (int i = total_rounds-no_agents; i < total_rounds; i++) {
				for (int k = 0; k <= 1; k++){
					// Normalize Rewards
					for (IntegerArray SA : counts[i][k].keySet())
						R[i][k].put(SA,R[i][k].get(SA)/counts[i][k].get(SA));
				}
			}
			ready = true;	// normalized
}

	// get Possible states to transition into from (S,A)
	public ArrayList<IntegerArray> getNextStates(IntegerArray SA) {
		return NextStates[SA.d.length-1].get(SA);
	}

	// get transition probabilities from (S,A) to further states
	public double[] getP(IntegerArray SA, boolean theta) {

		// Sanity check
		if (!ready)
			throw new RuntimeException("must normalize first");
		if (SA.d.length > total_rounds - no_agents)
			throw new IllegalArgumentException("no more rounds");
		
		// convert signal into index
		int id;
		if (theta == false)
			id = 0;
		else
			id = 1;
		
		// Get transition probabilities
		double[] p = P[SA.d.length-1][id].get(SA);		
		
		// TODO: Should use "near by states" when a (state,action) is not observed, not just an arbitrary one
		if (p == null){
			System.out.println("warning: (state,action) = " + SA.print() + " not observed");

			// Use the first one that comes up
			if (P[SA.d.length-1][id].entrySet().size() > 0){
				Iterator<IntegerArray> it = P[SA.d.length][id].keySet().iterator();
				p = P[SA.d.length-1][id].get(it.next());
				System.out.println("used next one");
			}else{
				throw new IllegalArgumentException("Nothing observed at all for the round of (state,action) = " + SA.print());
			}
				
		}
		return p;
	}
	
	// Get rewards
	public double getR(IntegerArray SA, int theta_id) {
		
		// Sanity check
		if (!ready)
			throw new RuntimeException("must normalize first");
		if (SA.d.length > total_rounds)
			throw new IllegalArgumentException("no more rounds. total_rounds = " + total_rounds + ", SA.d.length = " + SA.d.length);
		
		if (R[SA.d.length-1][theta_id].containsKey(SA))
			return R[SA.d.length-1][theta_id].get(SA);
		else
			return 0.0;		// TODO: something to tweak. Set it artificially high so that all states are explored? 
	}

	// Convert signal from boolean to int before getting reward
	public double getR(IntegerArray SA, boolean theta) {

		// convert signal into index
		int theta_id;
		if (theta == false)
			theta_id = 0;
		else
			theta_id = 1;
		
		return getR(SA, theta_id);
	}


	// Testing: epsilonTruthfulAgent
	public static void main(String args[]) throws IOException {

		Cache.init();
		
		// parameters
		int no_rounds = 2;
		int no_agents = 2;
		int total_rounds = no_rounds*no_agents;
		
		double p0 = 0.5;
		double rho = 0.9;
		Random rng = new Random();

		// Manually populate MDP
		Set<IntegerArray> histories = Cache.genStates(total_rounds);
		double[] reward = new double[total_rounds];
		Signal theta = new SimpleSignal(p0, rho, no_agents, rng);
		
		MDP mdp = new MDP(no_agents, no_rounds);
		for (IntegerArray hist: histories){
			theta.reset();
			// rewards = simply cumulative sum of action history
			reward[0] = hist.d[0];
			for (int i = 1; i < reward.length; i++)
				reward[i] = reward[i-1] + hist.d[i];
			mdp.populate(hist, reward, theta);
		}

		// create bias of self-transition between 0s
		int bias = 100;
		for (int i = 0; i < bias; i++){
			theta.reset();
			mdp.populate(new IntegerArray(new int[] {0,0,0,0}), new double[] {0,0,0,0}, theta);
		}
		
		mdp.normalize();
		
		// Test get next states, getP, and getR
		ArrayList<IntegerArray> SAs = new ArrayList<IntegerArray>();
		SAs.add(new IntegerArray(new int[] {-1}));
		SAs.add(new IntegerArray(new int[] {0}));
		SAs.add(new IntegerArray(new int[] {1}));
		SAs.add(new IntegerArray(new int[] {1,1}));
		
		IntegerArray SA;
		Iterator<IntegerArray> it = SAs.iterator();
		while (it.hasNext()){
			SA = it.next();

			// Get stuff
			ArrayList<IntegerArray> NSs = mdp.getNextStates(SA);
			
			// print SA and next round states
			System.out.println("\nSA = " + SA.print() + ", NSs = ");
			for (int i = 0; i < NSs.size()-1; i++)
				System.out.print(NSs.get(i).print() + "  ");
			System.out.println(NSs.get(NSs.size()-1).print());
			
			// print transitions probs and rewards (if true)
			double[] p = mdp.getP(SA,true);
			double r = mdp.getR(SA,true);
			System.out.print("signal = true: r = " + r + ", p = [");
			for (int i = 0; i < p.length-1; i++)
				System.out.print(p[i] + ",");
			System.out.println(p[p.length-1] + "]");

			// print transitions probs and rewards (if false)
			p = mdp.getP(SA,false);
			r = mdp.getR(SA,false);
			System.out.print("signal = false: r = " + r + ", p = [");
			for (int i = 0; i < p.length-1; i++)
				System.out.print(p[i] + ",");
			System.out.println(p[p.length-1] + "]");

		}
		
	}	
}

