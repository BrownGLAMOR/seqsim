package speed;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

// Adapted from FullCondMDPAgent4FP.java, an MDP agent that bids in first price auctions and bids a smoothed distribution of Q values.    
public class MDPAgentFP_smoothed extends SeqAgent {

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
	HashMap<WinnerAndRealized, Double>[] pi; // [t].get([winner] [realized]) ==> pi
	HashMap<WinnerAndRealized, double[]>[] Q;	// [t].get([winner] [realized]) ==> Q-values
	HashMap<WinnerAndRealized, Double>[] V; // [t].get([winner] [realized]) ==> V

	double[] tmp_Q, cum_value;
	double[] Reward;
	double[] condCDF;
	double[] b;
	
	IntegerArray[] tmp_r;
	BooleanArray[] tmp_w;
	WinnerAndRealized[] tmp_wr;
	WinnerAndRealized wr, wr_plus;

	// preference variables
	int preference;
	double epsilon;
	double[] GAMMA;		// can have different gamma values for different rounds
	boolean discretize_value;
	
	// The same as FullCondMDPAgent.java, except has different tie breaking rules when comparing bids giving similar utility. 
	// preference = {-1,0,1,2}, each representing: favor lower bound, choose highest, favoring upper bound, favor mixing.  
	// The agent will choose bid from the bids generating the highest utility, or within epsilon of the highest utility
	// 
	// discretize_value --> whether to discretize types or not
	// and if yes, v_precision --> precision
	public MDPAgentFP_smoothed(Value valuation, int agent_idx, double[] GAMMA, boolean discretize_value, double v_precision) {
		super(agent_idx, valuation);
		this.GAMMA= GAMMA;
		this.v_precision = v_precision;
		this.discretize_value = discretize_value;
	}
	
	// Allocate memory for MDP calculation
	@SuppressWarnings("unchecked")
	private void allocate() {

		// list of possible bids (to maximize over)
		b = new double[price_length];
		condCDF = new double[price_length];		// for later use

//		b[0] = 0;
		for (int i = 0; i < b.length; i++)
			b[i] = jcde.precision*i;		// bid = p_{i}	XXX: can play around with this
		
		tmp_Q = new double[price_length];		
		cum_value = new double[price_length];		
		Reward = new double[price_length];		
		V = new HashMap[jcde.no_goods]; // Value function V		
		pi = new HashMap[jcde.no_goods]; // optimal bidding function \pi
		Q = new HashMap[jcde.no_goods];
		
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
		
		// 1) ******************************** The last round of auction: truthful bidding NO LONGER optimal
		
		t = jcde.no_goods-1;
		
		// > Loop over possible price histories and winning histories
		for (IntegerArray realized : Cache.getCartesianProduct(jcde.bins, t)) {			
    		for (BooleanArray winner : Cache.getWinningHistory(t)) {    			

    			wr = new WinnerAndRealized(winner, realized);	

    			// get conditional HOB distribution
				double[] condPMF = jcde.getPMF(wr);

				// Compute HOB cdf and cost incurred by bidding
				double temp = 0;
	    		for (int j = 0; j < b.length; j++){				
	    			temp += condPMF[j];	// add -f(p)
	    			condCDF[j] = temp;
//	    			condCDF[j] = temp - condPMF[j]/2;
	    			Reward[j] = - condCDF[j]*j*jcde.precision;
	    		}
	    		
    			// Compute Q(b,state) for each bid b
	    		double max_value = Double.MIN_VALUE;		// Value of largest Q((X,t),b)
	    		int max_idx = -1;							// Index of largest Q((X,t),b)			    	
    			for (int i = 0; i < b.length; i++) {
	    			double temp2 = Reward[i];
    				temp2 += condCDF[i]*v.getValue(winner.getSum()+1) + (1-condCDF[i])*v.getValue(winner.getSum());
	    			
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

    		}    				
		}		    	
	

		// 2) ******************************** Recursively assign values for t = no_slots-1,...,1
		
		// > Loop over auction t
		for (t = jcde.no_goods-2; t>-1; t--) {

			realized_plus = new IntegerArray(t+1);
			winner_plus = new BooleanArray(t+1);

			// > Loop over possible price histories
			for (IntegerArray realized : Cache.getCartesianProduct(jcde.bins, t)) {
				
				// copy realized prices (to append later)
				for (int k = 0; k < realized.d.length; k++)
					realized_plus.d[k] = realized.d[k];
				
	    		// > Loop over possible winning histories 
	    		for (BooleanArray winner : Cache.getWinningHistory(t)) {
	    			
	    			// copy winner array (to append later)
		    		for (int k = 0; k < winner.d.length; k++)
						winner_plus.d[k] = winner.d[k];
	    			
	    			wr = new WinnerAndRealized(winner, realized);	
	    			
	    			// get conditional HOB distribution
					double[] condPMF = jcde.getPMF(wr);				

					// Compute HOB cdf and cost incurred by bidding
					double temp = 0;
		    		for (int j = 0; j < b.length; j++){
		    			temp += condPMF[j];							// add f(p)
		    			condCDF[j] = temp;							 
//		    			condCDF[j] = temp + condPMF[j]/2;
//		    			condCDF[j] = temp - condPMF[j]/2;							 
		    			Reward[j] = - condCDF[j]*j*jcde.precision;
		    		}					
	    			
	    			// Compute Q(b,state) for each bid b
		    		double max_value = Double.MIN_VALUE;		// Value of largest Q((X,t),b)
			    	int max_idx = -1;							// Index of largest Q((X,t),b)			    	
	    			for (int i = 0; i < b.length; i++) {
		    			double temp2 = Reward[i];

		    			// if agent wins
	    				realized_plus.d[realized.d.length] = i;
	    				winner_plus.d[winner.d.length] = true;
	    				temp2 += condCDF[i] * V[t+1].get(new WinnerAndRealized(winner_plus, realized_plus));
		    			
		    			for (int j = i+1; j < condPMF.length; j++) {
		    				realized_plus.d[realized.d.length] = j;
		    				winner_plus.d[winner.d.length] = false;
		    				temp2 += condPMF[j] * V[t+1].get(new WinnerAndRealized(winner_plus, realized_plus));
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
//					if (winner.getSum() == 0)
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
//					if (winner.getSum() == 0)
						System.out.println("pi[" + t + "](" + wr.print() + ") = " + pi[t].get(wr));
				}
			}
		}
	}
	
	// Prints out first round Q values
	public void printQ() {
		// get Q
		winner = new BooleanArray(new boolean[] {});
		realized = new IntegerArray(new int[] {});
		tmp_Q = Q[0].get(new WinnerAndRealized(winner,realized));
		
		// print Q
		for (int i = 0; i < tmp_Q.length-1; i++)
			System.out.print(tmp_Q[i] + ",");
		System.out.println(tmp_Q[tmp_Q.length-1]);
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

	// Reset gamma
	public void setGamma(double[] GAMMA){
		this.GAMMA = GAMMA;
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
		
	// Short cut
	public double getBid(WinnerAndRealized wr){
		int good_id = wr.r.d.length + 1;
		return pi[good_id].get(new WinnerAndRealized(winner, realized));
	}
	
	
	// Outputting bids by inputting past winner and realized price sequence
	public double getBid(int good_id, boolean[] input_winner, double[] input_realized) {	

		// Sanity check
		if (input_winner.length != good_id || input_realized.length != good_id)
			System.out.println("length not matching... ");

		winner = tmp_w[good_id];
		winner.d = input_winner;
		
		double gamma = GAMMA[good_id];		// corresponding smoothing parameter
		
		// bin realized prices
		realized = tmp_r[good_id];
		for (int i = 0; i < input_realized.length; i++)
			realized.d[i] = JointDistributionEmpirical.bin(input_realized[i], jcde.precision);
		
		tmp_Q = Q[good_id].get(new WinnerAndRealized(winner, realized));

//		System.out.print("tmp_Q = [");
//		for (int i = 0; i < tmp_Q.length; i++)
//			System.out.print(tmp_Q[i] + ",");
//		System.out.println("]");

		// compute cumulative exp(\gamma*Q) 
		cum_value[0] = java.lang.Math.exp(gamma*tmp_Q[0]);
		for (int i = 1; i < tmp_Q.length; i++)
			cum_value[i] = cum_value[i-1] + java.lang.Math.exp(gamma*tmp_Q[i]);
			
//		System.out.print("cum_value = [");
//		for (int i = 0; i < cum_value.length; i++)
//			System.out.print(cum_value[i] + ",");
//		System.out.println("]");
						
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
	
	// Called in auction simulations
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
	
	// helper: get randomized version of first round bid
	public double getFirstRoundBid() {
		// no goods won. good_id == 0. tmp_r[0] is a special global for good 0. 
		return getBid(0, new boolean[] {}, new double[] {});
	}
	
	// return the OPTIMAL first round bid, not the randomized version
	public double getFirstRoundPi() {
		return pi[0].get(new WinnerAndRealized(new BooleanArray(new boolean[] {}), new IntegerArray(new int[] {})));
	}

	public double getLastRoundBid(int no_goods_won) {
		// truthful bidding. realized prices don't matter.
		return v.getValue(no_goods_won+1) - v.getValue(no_goods_won);
	}
	
	// Test using Katzman setup
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
		int nth_price = 1;

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
		double[] GAMMA = new double[] {10.0, 10.0};
//		FullCondMDPAgent4FP mdp_agent = new FullCondMDPAgent4FP(value, 1, preference, epsilon, false, 0.01);
		MDPAgentFP_smoothed mdp_agent = new MDPAgentFP_smoothed(value, 1, GAMMA, false, 0.01);		

		mdp_agent.setCondJointDistribution(pp);
		
		FileWriter fw_play = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/paper/june1st/smoothed/testFP_smooth_" + GAMMA[0] + "," + GAMMA[1] + ".csv");
		
		
//		mdp_agent.reset(null);		
//		mdp_agent.printQ();
		
		for (int iteration_idx = 0; iteration_idx < max_iterations; iteration_idx++) {			
			// Have agents create their bidding strategy using the provided valuation
			// (both agents share the SAME valuation)
			
			katz_agent.reset(null);
			mdp_agent.reset(null);

			fw_play.write(value.getValue(1) + "," + value.getValue(2) + "," + katz_agent.getFirstRoundBid() + "," + mdp_agent.getFirstRoundBid() + "\n");
			
			// Draw new valuation for the next round
			value.reset();
		}
		
		fw_play.close();
		System.out.println("done done");
	}


}
