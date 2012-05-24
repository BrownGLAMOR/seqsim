package speed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class MDPAgentSP extends SeqAgent {

	Random rng = new Random();
	
	// auction variables
	SeqAuction auction;
	JointCondDistributionEmpirical jcde;
	
	int agent_idx;
	int no_goods_won;
	int no_goods;
	int idx;
	
	// computational variables
	int price_length;
	int v_id;				// a number that summarizes valuation 
	double optimal_bid, temp, winning_prob;
	IntegerArray realized, realized_plus;
	BooleanArray winner, winner_plus;
	
	// Storage
	ArrayList<Integer> indices = new ArrayList<Integer>();			// The set of indices to consider when choosing the optimal bid
	HashMap<WinnerAndRealized, Double>[] pi; // [t].get([winner] [realized]) ==> pi
	HashMap<WinnerAndRealized, double[]>[] Q;	// [t].get([winner] [realized]) ==> Q value for each bid
	HashMap<WinnerAndRealized, Double>[] V; // [t].get([winner] [realized]) ==> V

	// Temporary storage
	double[] tmp_Q;
	double[] Reward;
	double[] b;
	double[] cum_value;		// used in softmaxing
	IntegerArray[] tmp_r;
	BooleanArray[] tmp_w;
	WinnerAndRealized[] tmp_wr;
	WinnerAndRealized wr, wr_plus;

	// preference variables
	boolean discretize_value;
	int preference;
	double epsilon, rho, v_precision;
	double[] GAMMA;
	
	// make sure things are inputted
	boolean epsilon_ready = false, gamma_ready = false, rho_ready = false;
	
	// greedy + epsilon:
	// preference = {-1,0,1,2}, each representing: favor lower bound, choose highest, favoring upper bound, favor random mixing.
	// preference = {3,4}: Boltzman softmaxing, add uniform mixture
	public MDPAgentSP(Value valuation, int agent_idx, int preference, boolean discretize_value, double v_precision) {
		super(agent_idx, valuation);
		this.preference = preference;
		this.discretize_value = discretize_value;
		this.v_precision = v_precision;
	}
	
	// Allocate memory for MDP calculation
	@SuppressWarnings("unchecked")
	private void allocate() {
		// list of possible bids (to maximize over)
		b = new double[price_length];
		cum_value = new double[price_length];
		
		for (int i = 0; i < b.length; i++)
			b[i] = jcde.precision*((i+(i+1))/2.0 - 0.5);		// bid = (p_{i}+p_{i+1})/2 - 0.49999*precision
		
		tmp_Q = new double[price_length];		
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

		// The last entry of GAMMA will be ignored since there is no smoothing in final round
		GAMMA = new double[jcde.no_goods];	
		
	}
	
	// Input parameter epsilon (for preference = -1, 1, 2)
	public void inputEpsilon(double epsilon){
		this.epsilon = epsilon;
		this.epsilon_ready = true;
	}
	
	// Input parameter gamma (for preference = 3)
	public void inputGamma(double[] GAMMA){
		
		this.GAMMA = GAMMA;
		this.gamma_ready = true;
	}
	
	// Input parameter rho (for preference = 4)
	public void inputRho(double rho){
		
		// Sanity check
		if (rho < 0 || rho > 1)
			throw new RuntimeException("rho must be between 0 and 1");

		this.rho = rho;
		this.rho_ready = true; 
	}
	
	
	// Compute optimal bidding	
	public void computeFullMDP() {
		
		// Sanity check: are parameters inputted? 
		if ((preference == -1 || preference == 1 || preference == 2) && (!epsilon_ready))
			throw new RuntimeException("must input epsilon first");
		else if (preference == 4 && !rho_ready)
			throw new RuntimeException("must input rho first");
		
		// Reset MDP state vars
		for (int i = 0; i<jcde.no_goods; i++) {
				V[i].clear();
				pi[i].clear();
				Q[i].clear();
		}
		
		// 1) ******************************** The last round of auction: truthful bidding is optimal
		
		int t = jcde.no_goods-1;
		
		for (BooleanArray winner : Cache.getWinningHistory(t)) {
			for (IntegerArray realized : Cache.getCartesianProduct(jcde.bins, t)) {				
				wr = new WinnerAndRealized(winner, realized);  	    		
				
				double value_if_win = v.getValue(winner.getSum()+1);
				double value_if_lose = v.getValue(winner.getSum());
				
				// 
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
	    		for (int j = 0; j*jcde.precision < optimal_bid && j < condDist.length; j++) {
	    			temp += -(j*jcde.precision) * condDist[j];	// add -condDist*f(p)
	    			winning_prob += condDist[j];
	    		}

	    		temp += winning_prob*v.getValue(winner.getSum()+1) + (1-winning_prob)*v.getValue(winner.getSum());	    		
	    		V[t].put(wr, temp);
			}
		}
		// 2) ******************************** Recursively assign values for t = no_slots-1,...,1
		
		// > Loop over auction t
		for (t = jcde.no_goods-2; t>-1; t--) {

    		// > Loop over possible realized historical prices
			for (IntegerArray realized : Cache.getCartesianProduct(jcde.bins, t)) {
				
				winner_plus = new BooleanArray(t+1);	// Needs to redeclare since size changing as t changes
				realized_plus = new IntegerArray(t+1);
				
				// copy realized prices (to append later)
				for (int k = 0; k < realized.d.length; k++)
					realized_plus.d[k] = realized.d[k];
				
	    		// > Loop over past histories 
	    		for (BooleanArray winner : Cache.getWinningHistory(t)) {
	    			
	    			wr = new WinnerAndRealized(winner, realized); 
	
					// copy winner array (to append later)
		    		for (int k = 0; k < winner.d.length; k++)
						winner_plus.d[k] = winner.d[k];

	    			// get conditional Distribution
					double[] condDist = jcde.getPMF(wr);				

					// Compute Reward
					double temp = 0;								// Sum
		    		for (int j = 0; j < b.length; j++){
		    			temp += -(j*jcde.precision) * condDist[j];	// add -condDist*f(p)
//		    			Reward[j] = temp + 0.5*(j*jcde.precision)*condDist[j];	// XXX handling...
		    			Reward[j] = temp;
		    		}

	    			
	    			// Compute tmp_Q(b,state) for each bid b
		    		double max_value = Double.MIN_VALUE;		// Value of largest tmp_Q((X,t),b)
			    	int max_idx = -1;							// Index of largest tmp_Q((X,t),b)			    	
	    			for (int i = 0; i < b.length; i++) {
		    			double temp2 = Reward[i];

		    			// if agent wins round t
		    			for (int j = 0; j <= i; j++) {
		    				realized_plus.d[realized.d.length] = j;
		    				winner_plus.d[winner.d.length] = true;
		    				temp2 += condDist[j] * V[t+1].get(new WinnerAndRealized(winner_plus, realized_plus));  
		    			}
		    			
//		    			// XXX: handle tie breaking: assume half chance of winning
//	    				realized_plus.d[realized.d.length] = i;
//	    				winner_plus.d[winner.d.length] = true;
//	    				temp2 += 0.5*condDist[i] * V[t+1].get(new WinnerAndRealized(winner_plus, realized_plus));
//	    				winner_plus.d[winner.d.length] = false;
//	    				temp2 += 0.5*condDist[i] * V[t+1].get(new WinnerAndRealized(winner_plus, realized_plus));
		    			
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
	    			
	    			// store Q values if need to Boltzman smooth later
	    			if (preference == 3)
	    				Q[t].put(wr, tmp_Q);
	    			
	    			// just choose the best one
	    			if (preference == 0 || preference == 3 || preference == 4){
	    					V[t].put(wr, tmp_Q[max_idx]);
				    		pi[t].put(wr, b[max_idx]);
				    	}
	    			else{
	    				// find all the choosable bids, and select according to preference
	    				indices.clear();
	    				for (int i = 0; i < tmp_Q.length; i++){
		    				if (tmp_Q[i] > max_value - epsilon){
		    					indices.add(i);		    					
		    				}
		    			}
		    			if (preference == -1){	// prefer lowerbound
		    				V[t].put(wr,tmp_Q[indices.get(0)]);
		    				pi[t].put(wr, b[indices.get(0)]);
		    			}
		    			else if (preference == 1){ // prefer upperbound
		    				V[t].put(wr,tmp_Q[indices.get(indices.size()-1)]);
		    				pi[t].put(wr, b[indices.get(indices.size()-1)]);
		    			}
		    			else if (preference == 2) { // randomly choose one 
	    					idx = indices.get(rng.nextInt(indices.size()));	// pick one randomly
		    				V[t].put(wr,tmp_Q[idx]);
		    				pi[t].put(wr, b[idx]);
		    			}
		    		}
	    		}
    		}		    	
		}
	}
	
	// Prints out V values calculated by MDP
	public void printV() {
		for (int t = jcde.no_goods; t > -1; t--){
			for (BooleanArray winner : Cache.getWinningHistory(t)) {
				for (IntegerArray realized : Cache.getCartesianProduct(jcde.bins, t)) {
					wr = new WinnerAndRealized(winner,realized);
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
					System.out.println("pi[" + t + "](" + wr.print() + ") = " + pi[t].get(wr));
				}
			}
		}
	}

	// Prints out Q values for first round
	public void printQ(){
		// TODO: to add
	}
	
	@Override
	public void reset(SeqAuction auction) {
		this.auction = auction;		
		
		if (discretize_value == true) {
			
			// Get strategy calculated in Cache if exist, or compute it and store it into Cache
			this.v_id = legacy.DiscreteDistribution.bin(v.getValue(1), v_precision);
			if (Cache.hasMDPpolicy(v_id) == true){
				pi = Cache.getMDPpolicy(v_id);
				
				// print first round bids
//				System.out.println("v_id = " + v_id + ", pi[0] = " + pi[0].get(new WinnerAndRealized(new BooleanArray(new boolean[] {}), new IntegerArray(new int[] {}))));
				
//				if (preference == 3)	// TODO: to also Cache
//					Q = Cache.getQmap(v_id);
				
			}
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
		
	// Output bids conditional on past information
	public double getBid(int good_id, boolean[] input_winner, double[] input_realized) {	

		// Sanity check
		if (input_winner.length != good_id || input_realized.length != good_id)
			System.out.println("length not matching... ");
		if (preference == 3 && !gamma_ready)
			throw new RuntimeException("must input gamma first");

		// Put conditioning information into WR format
		winner = tmp_w[good_id];
		winner.d = input_winner;		
		realized = tmp_r[good_id];
		for (int i = 0; i < input_realized.length; i++)
			realized.d[i] = JointDistributionEmpirical.bin(input_realized[i], jcde.precision);
		
		// Bid truthfully in the last round
		if (good_id == jcde.no_goods - 1 || preference == -1 || preference == 0 || preference == 1 || preference == 2)
			return pi[good_id].get(new WinnerAndRealized(winner, realized));
		// In non-terminal rounds, can do softmaxing
		else if (preference == 3){
			tmp_Q = Q[good_id].get(new WinnerAndRealized(winner, realized));

			// compute cumulative exp(\gamma*Q)
			double gamma = GAMMA[good_id];
			cum_value[0] = java.lang.Math.exp(gamma*tmp_Q[0]);
			for (int i = 1; i < tmp_Q.length; i++)
				cum_value[i] = cum_value[i-1] + java.lang.Math.exp(gamma*tmp_Q[i]);
			
			// Randomly choose
			double r = rng.nextDouble()*cum_value[cum_value.length-1];
			int idx = 0;
			boolean reached = false;
			while (reached == false){
				if (r <= cum_value[idx])
					reached = true;
				else
					idx++;
			}
			return b[idx];
		}
		else{	// (preference == 4)
			// TODO: to add
			return 0.0;
		}
	}
	
	// Collect past information and input into getBid
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
		
		// figure out realized prices (hobs)
		double[] realized = new double[good_id];
		for (int i = 0; i < realized.length; i++)
//			realized[i] = auction.price[i];	// XXX
			realized[i] = auction.hob[agent_idx][i];
		
		// Get optimal bid
		return getBid(good_id, winner.d, realized);
	}
	
	// Different from "getFirstRoundPi"
	public double getFirstRoundBid() {
		return getBid(0, new boolean[] {}, new double[] {});
	}

	// Get optimal first round bid
	public double getFirstRoundPi() {
		return pi[0].get(new WinnerAndRealized(new BooleanArray(new boolean[] {}), new IntegerArray(new int[] {})));
	}
	
//	public double getSecondRoundBid(int no_goods_won) {
//		// truthful bidding. realized prices don't matter.
//		return v.getValue(no_goods_won+1) - v.getValue(no_goods_won);
//	}
}
