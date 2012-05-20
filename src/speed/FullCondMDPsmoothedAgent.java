package speed;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

// Adapted from FullCondMDPAgent4.java, bids a smooth distribution accordingly to Boltzman distribution from tmp_Q-values. 
// Can handle discretized types, save MDP computed for each type into Cache, and reuse them later on. 
public class FullCondMDPsmoothedAgent extends SeqAgent {

	Random rng = new Random();
	
	// auction variables
	SeqAuction auction;
	JointCondDistributionEmpirical jcde;
	
	// agent variables
	int agent_idx;
	int no_goods_won;
	int no_goods;
	int idx;
	
	// computational variables
	Value valuation;
	int v_id;				// a number that summarizes valuation 
	int price_length, t;
	double optimal_bid, temp, winning_prob, v_precision, value_if_win, value_if_lose;
	IntegerArray realized, realized_plus;
	BooleanArray winner, winner_plus;

	// storing devices
	ArrayList<Integer> indices = new ArrayList<Integer>();			// The set of indices to consider when choosing the optimal bid
	HashMap<WinnerAndRealized, Double>[] pi; 	// [t].get([winner] [realized]) ==> pi
	HashMap<WinnerAndRealized, double[]>[] Q;	// [t].get([winner] prealized]) ==> Q-values
	HashMap<WinnerAndRealized, Double>[] V; 	// [t].get([winner] [realized]) ==> V

	double[] Reward;
	double[] b;
	
	// temporary storages
	double[] tmp_Q, cum_value;
	IntegerArray[] tmp_r;
	BooleanArray[] tmp_w;
	WinnerAndRealized[] tmp_wr;
	
	WinnerAndRealized wr, wr_plus;

	// preference variables (used for discretizing values and smoothing bids)
	double gamma;
	boolean discretize_value;
	
	// The same as FullCondMDPAgent.java, except has different tie breaking rules when comparing bids giving similar utility. 
	// preference = {-1,0,1,2}, each representing: favor lower bound, choose highest, favoring upper bound, favor mixing.  
	// The agent will choose bid from the bids generating the highest utility, or within epsilon of the highest utility
	// 
	// discretize_value --> whether to discretize types or not
	// and if yes, v_precision --> precision
	public FullCondMDPsmoothedAgent(Value valuation, int agent_idx, double gamma, boolean discretize_value, double v_precision) {
		super(agent_idx, valuation);
		this.v_precision = v_precision;
		this.gamma = gamma;
		this.discretize_value = discretize_value;
	}
	
	// Allocate memory for MDP calculation
	@SuppressWarnings("unchecked")
	private void allocate() {
		// list of possible bids (to maximize over)
		b = new double[price_length];

		for (int i = 0; i < b.length; i++)
			b[i] = jcde.precision*i;		// XXX: can play around with this
		
		tmp_Q = new double[price_length];		
		cum_value = new double[price_length];		
		Reward = new double[price_length];		
		V = new HashMap[jcde.no_goods]; // Value function V
		Q = new HashMap[jcde.no_goods];
		pi = new HashMap[jcde.no_goods]; // optimal bidding function \pi
		
		for (int i = 0; i<jcde.no_goods; i++) {
				V[i] = new HashMap<WinnerAndRealized, Double>();
				Q[i] = new HashMap<WinnerAndRealized, double[]>();
				pi[i] = new HashMap<WinnerAndRealized, Double>();
		}
		
		// Create a temporary area for getBids() to retrieve results
		// with having to perform a "new" operation (speed improvement)
		tmp_r = new IntegerArray[no_goods+1];
		tmp_w = new BooleanArray[no_goods+1];
		tmp_wr = new WinnerAndRealized[no_goods+1];
		
		for (int i = 0; i<no_goods+1; i++) {
			tmp_r[i] = new IntegerArray(i);
			tmp_w[i] = new BooleanArray(i);
			tmp_wr[i] = new WinnerAndRealized(i);
		}
		
	}
	
	// Ask the agent to computes optimal bidding policy \pi((WinnerAndRealized,X,t)) using MDP	
	public void computeFullMDP() {
		// Reset MDP state vars
		for (int i = 0; i<jcde.no_goods; i++) {
				V[i].clear();
				pi[i].clear();
				Q[i].clear();
		}
		

		// 1) ******************************** The last round of auction: truthful bidding is optimal
		
		t = jcde.no_goods-1;
		
		for (BooleanArray winner : Cache.getWinningHistory(t)) {
			for (IntegerArray realized : Cache.getCartesianProduct(jcde.bins, t)) {				
				wr = new WinnerAndRealized(winner, realized); 	    		
				
				value_if_win = v.getValue(winner.getSum()+1);
				value_if_lose = v.getValue(winner.getSum());
				
				if (discretize_value == true){
					value_if_win = v_precision*legacy.DiscreteDistribution.bin(value_if_win,v_precision);
					value_if_lose = v_precision*legacy.DiscreteDistribution.bin(value_if_lose,v_precision);
				}
				
				optimal_bid = value_if_win - value_if_lose;	    		
				pi[t].put(wr, optimal_bid);
				double[] condDist = jcde.getPMF(wr);
				
				// Compute Reward and F(p)
				temp = 0;
	    		winning_prob = 0;
	    		for (int j = 0; j*jcde.precision < optimal_bid && j < condDist.length; j++) {	// TODO: take into account tie breaking, ie, optimal bid = j*jcde.precision?
	    			temp += -(j*jcde.precision) * condDist[j];	// add -condDist*f(p): incur lose due to bidding
	    			winning_prob += condDist[j];
	    		}

    			temp += winning_prob*value_if_win + (1-winning_prob)*value_if_lose;	    		
	    		V[t].put(wr, temp);
			}
		}
		// 2) ******************************** Recursively assign values for t = no_slots-1,...,1
		
		// > Loop over auction t
		for (t = jcde.no_goods-2; t>-1; t--) {

    		// > Loop over possible price histories
			for (IntegerArray realized : Cache.getCartesianProduct(jcde.bins, t)) {
				
				winner_plus = new BooleanArray(t+1);	// Needs to redeclare since size changing as t changes
				realized_plus = new IntegerArray(t+1);
				
				// copy realized prices (to append later)
				for (int k = 0; k < realized.d.length; k++)
					realized_plus.d[k] = realized.d[k];
				
	    		// > Loop over possible winning histories 
	    		for (BooleanArray winner : Cache.getWinningHistory(t)) {
	    			
	    			wr = new WinnerAndRealized(winner, realized);	

	    			// copy winner array (to append later)
		    		for (int k = 0; k < winner.d.length; k++)
						winner_plus.d[k] = winner.d[k];

	    			// get conditional HOB distribution
					double[] condDist = jcde.getPMF(wr);				

					// Compute Reward
					double temp = 0;								// Sum
		    		for (int j = 0; j < b.length; j++){				// TODO: take into account tie breaking
		    			temp += -(j*jcde.precision) * condDist[j];	// add -condDist*f(p)
		    			Reward[j] = temp;
		    		}

	    			
	    			// Compute tmp_Q(b,state) for each bid b
		    		double max_value = Double.MIN_VALUE;		// Value of largest tmp_Q((X,t),b)
			    	int max_idx = -1;							// Index of largest tmp_Q((X,t),b)			    	
	    			for (int i = 0; i < b.length; i++) {
		    			double temp2 = Reward[i];

		    			// if agent wins round t
		    			for (int j = 0; j <= i; j++) {			    			// XXX: j <= i or j < i? Do we assume winning if bids are the same? 
		    				realized_plus.d[realized.d.length] = j;
		    				winner_plus.d[winner.d.length] = true;
		    				temp2 += condDist[j] * V[t+1].get(new WinnerAndRealized(winner_plus, realized_plus));  
		    			}
		    			
		    			// if agent doesn't win round t
		    			for (int j = i+1; j < condDist.length; j++) {
		    				realized_plus.d[realized.d.length] = j;
		    				winner_plus.d[winner.d.length] = false;
		    				temp2 += condDist[j] * V[t+1].get(new WinnerAndRealized(winner_plus, realized_plus));
		    			}
		    			
			    		if (temp2 > max_value || i == 0) { 
			    			max_value = temp2;
			    			max_idx = i;
			    		}

			    		tmp_Q[i] = temp2;
		    		}
	    			
	    			// Keep track of Q values
					V[t].put(wr, tmp_Q[max_idx]);
					Q[t].put(wr, tmp_Q.clone());
		    		pi[t].put(wr, b[max_idx]);
	    			
//	    			// just choose the best one
//	    			if (preference == 0){
//	    					V[t].put(wr, tmp_Q[max_idx]);
//				    		pi[t].put(wr, b[max_idx]);
//				    	}
//	    			else{
//	    				// find all the choosable bids, and select according to preference
//	    				indices.clear();
//	    				for (int i = 0; i < tmp_Q.length; i++){
//		    				if (tmp_Q[i] > max_value - epsilon){
//		    					indices.add(i);		    					
//		    				}
//		    			}
//		    			if (preference == -1){	// prefer lowerbound
//		    				V[t].put(wr,tmp_Q[indices.get(0)]);
//		    				pi[t].put(wr, b[indices.get(0)]);
//		    			}
//		    			else if (preference == 1){ // prefer upperbound
//			    				V[t].put(wr,tmp_Q[indices.get(indices.size()-1)]);
//			    				pi[t].put(wr, b[indices.get(indices.size()-1)]);
//		    			}
//		    			else{
//			    				idx = indices.get(rng.nextInt(indices.size()));	// pick one randomly
//			    				V[t].put(wr,tmp_Q[idx]);
//			    				pi[t].put(wr, b[idx]);
//		    			}
//	    				
//		    		}
//	    				
	    		}
    		}		    	
		}
	}
	
	// Prints out V values calculated by MDP
	public void printV() {
		for (int t = jcde.no_goods-1; t > -1; t--){
			for (BooleanArray winner : Cache.getWinningHistory(t)) {
				for (IntegerArray realized : Cache.getCartesianProduct(jcde.bins, t)) {
					wr = new WinnerAndRealized(winner,realized);
					if (winner.getSum() == 0)
						System.out.println("V[" + t + "](" + wr.print() + ") = " + V[t].get(wr));
				}
			}
		}
	}

	// Prints out pi values calculated by MDP
	public void printpi() {
		for (int t = jcde.no_goods-1; t > -1; t--){
			for (BooleanArray winner : Cache.getWinningHistory(t)) {
				for (IntegerArray realized : Cache.getCartesianProduct(jcde.bins, t)) {
					wr = new WinnerAndRealized(winner,realized);
					if (winner.getSum() == 0)
						System.out.println("pi[" + t + "](" + wr.print() + ") = " + pi[t].get(wr));
				}
			}
		}
	}
	
	@Override
	public void reset(SeqAuction auction) {
		this.auction = auction;

		if (discretize_value == true) {
			// Get strategy calculated in Cache if exist, or compute it and store it into Cache
			this.v_id = legacy.DiscreteDistribution.bin(v.getValue(1), v_precision);
			if (Cache.hasMDPpolicy(v_id) == true)
				pi = Cache.getMDPpolicy(v_id);
			else {
				computeFullMDP();
				Cache.storeMDPpolicy(v_id, pi);
//				System.out.println("v_id = " + v_id + ", calculate MDP... ");
			}
		}
		else{
			computeFullMDP();
		}
	}

	public void setCondJointDistribution(JointCondDistributionEmpirical jcde) {
		this.jcde = jcde;
		
		// If this JCDE does not have the same parameters as our last jcde, then (re)allocate memory
		if (pi == null || jcde.no_bins != price_length || jcde.no_goods != no_goods) {
			price_length = jcde.no_bins;
			no_goods = jcde.no_goods;
			
			allocate();
		}
	}
		
	// Outputting bids by inputting past winner and realized price sequence
	public double getBid(int good_id, boolean[] input_winner, double[] input_realized) {	

		// Sanity check
		if (input_winner.length != good_id || input_realized.length != good_id)
			System.out.println("length not matching... ");

		winner = tmp_w[good_id];
		winner.d = input_winner;
		
		// figure out realized prices
		realized = tmp_r[good_id];
		for (int i = 0; i < input_realized.length; i++)
			realized.d[i] = JointDistributionEmpirical.bin(input_realized[i], jcde.precision);
		
		// Get optimal bid
		// if last round, Get optimal bid
		if (good_id == no_goods-1)
			return pi[good_id].get(new WinnerAndRealized(winner, realized));
		// Otherwise, smoothed bids 
		else{
			
			tmp_Q = Q[good_id].get(new WinnerAndRealized(winner, realized));

//			System.out.print("tmp_Q = [");
//			for (int i = 0; i < tmp_Q.length; i++)
//				System.out.print(tmp_Q[i] + ",");
//			System.out.println("]");

			// compute cumulative exp(\gamma*Q) 
			cum_value[0] = java.lang.Math.exp(gamma*tmp_Q[0]);
			for (int i = 1; i < tmp_Q.length; i++)
				cum_value[i] = cum_value[i-1] + java.lang.Math.exp(gamma*tmp_Q[i]);
			
//			System.out.print("cum_value = [");
//			for (int i = 0; i < cum_value.length; i++)
//				System.out.print(cum_value[i] + ",");
//			System.out.println("]");
						
			// Randomly choose
			double r = Math.random()*cum_value[cum_value.length-1];
			int idx = 0;
			boolean reached = false;
			while (reached == false){
				if (r <= cum_value[idx])
					reached = true;
				else
					idx++;
			}
//			System.out.println("r = " + r + ", so idx = " + idx + ", b[idx] = " + b[idx]);
			return b[idx];
		}
	}
	
	@Override
	public double getBid(int good_id) {	
		// Figure out which ones we have won;
		winner = tmp_w[good_id];
		if (good_id > 0) {
			for (int i = 0; i < good_id; i++){
				if (auction.winner[good_id-1] == agent_idx)
					winner.d[i] = true;
				else
					winner.d[i] = false;
			}
		}
		
		// figure out realized prices
		double[] realized = new double[good_id];		
		for (int i = 0; i < realized.length; i++)
			realized[i] = auction.price[i];
		
		return getBid(good_id, winner.d, realized);
	}
	
	// helpers. these may cheat.
	public double getFirstRoundBid() {
		// no goods won. good_id == 0. tmp_r[0] is a special global for good 0. 
//		return pi[0].get(new WinnerAndRealized(new BooleanArray(new boolean[] {}) {},new IntegerArray(new int[] {}) ));
		return getBid(0, new boolean[] {}, new double[] {});
	}
	
	public double getIntermediateRoundBid(WinnerAndRealized wr) {
		return pi[0].get(wr); 
	}
	
	public double getLastRoundBid(int no_goods_won) {
		// truthful bidding. realized prices don't matter.
		return v.getValue(no_goods_won+1) - v.getValue(no_goods_won);
	}
	
	
	
	
	
	
	
	
	
	// Test it using Katzman setup
	public static void main(String args[]) throws IOException {
		Cache.init();
		Random rng = new Random();

		// tie breaking preferences
		double epsilon = 0.000001;
		int preference = 0;

		double max_value = 1.0;
		double precision = 0.02;
		double max_price = max_value;
		
		int no_goods = 2;
		int no_agents = 3;
		int nth_price = 2;

		int no_simulations = 10000000/no_agents;		// run how many games to generate PP. this gets multiplied by no_agents later.		
		int max_iterations = 10000;
		
		JointCondFactory jcf = new JointCondFactory(no_goods, precision, max_price);

		// Generate initial condition
		KatzmanUniformAgent[] katz_agents = new KatzmanUniformAgent[no_agents];
		for (int i = 0; i<no_agents; i++)
			katz_agents[i] = new KatzmanUniformAgent(new KatzHLValue(no_agents-1, max_value, rng), no_agents, i);
		SeqAuction katz_auction = new SeqAuction(katz_agents, nth_price, no_goods);

		System.out.print("Generating initial PP from katzman agents...");
//		JointCondDistributionEmpirical pp = jcf.simulAllAgentsOneRealPP(katz_auction, no_simulations,false,false,false);
		JointCondDistributionEmpirical pp = jcf.simulAllAgentsOnePP(katz_auction,no_simulations,false,false,false);
		pp.outputNormalized();

		System.out.println("done");		
		System.out.println("Generating " + max_iterations + " first-round bids...");
		
		KatzHLValue value = new KatzHLValue(no_agents-1, max_value, rng);
		
		// initial agents for comparison
		KatzmanUniformAgent katz_agent = new KatzmanUniformAgent(value, no_agents, 0);		
		double gamma = 10.0;
//		FullCondMDPAgent4 mdp_agent = new FullCondMDPAgent4(value, 1, preference, epsilon, false, 0.01);
		FullCondMDPsmoothedAgent mdp_agent = new FullCondMDPsmoothedAgent(value, 1, gamma, false, 0.01);		

		mdp_agent.setCondJointDistribution(pp);
		
		FileWriter fw_play = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/paper/june1st/uniform/testsmooth_" + gamma + ".csv");
		
		
		mdp_agent.reset(null);		
		
		for (int iteration_idx = 0; iteration_idx < max_iterations; iteration_idx++) {			
			// Have agents create their bidding strategy using the provided valuation
			// (both agents share the SAME valuation)
			
//			katz_agent.reset(null);
//			mdp_agent.reset(null);

			fw_play.write(value.getValue(1) + "," + value.getValue(2) + "," + katz_agent.getFirstRoundBid() + "," + mdp_agent.getFirstRoundBid() + "\n");
			
			// Draw new valuation for the next round
//			value.reset();
		}
		
		fw_play.close();
		System.out.println("done done");
	}

	

}
