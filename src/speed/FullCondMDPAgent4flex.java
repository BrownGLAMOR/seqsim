package speed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

// A FullCondMDPAgent that can handle discretized types, save MDP computed for each type into Cache, and handle first price.  
public class FullCondMDPAgent4flex extends SeqAgent {

	Random rng = new Random();
	
	// auction variables
	SeqAuction auction;
	boolean first_ardprice;
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
	HashMap<WinnerAndRealized, Double>[] V; // [t].get([winner] [realized]) ==> V

	double[] Q;
	double[] Reward;
	double[] b;
	
	IntegerArray[] tmp_r;
	BooleanArray[] tmp_w;
	WinnerAndRealized[] tmp_wr;
	
	WinnerAndRealized wr, wr_plus;

	// preference variables (some used when breaking ties)
	int preference;
	double epsilon;
	boolean discretize_value;
	
	// The same as FullCondMDPAgent.java, except has different tie breaking rules when comparing bids giving similar utility. 
	// preference = {-1,0,1,2}, each representing: favor lower bound, choose highest, favoring upper bound, favor mixing.  
	// The agent will choose bid from the bids generating the highest utility, or within epsilon of the highest utility
	// 
	// discretize_value --> whether to discretize types or not
	// and if yes, v_precision --> precision
	public FullCondMDPAgent4flex(Value valuation, int agent_idx, boolean first_price, int preference, double epsilon, boolean discretize_value, double v_precision) {
		super(agent_idx, valuation);
		this.epsilon = epsilon;
		this.preference = preference;
		this.v_precision = v_precision;
		this.discretize_value = discretize_value;
		this.first_price = first_price;
	}
	
	// What does this do? 
	// Allocate memory for MDP calculation
	@SuppressWarnings("unchecked")
	private void allocate() {
		// list of possible bids (to maximize over)
		b = new double[price_length];

		for (int i = 0; i < b.length; i++)
			b[i] = jcde.precision*((i+(i+1))/2.0 - 0.5);		// bid = (p_{i}+p_{i+1})/2 - 0.49999*precision
		
		Q = new double[price_length];		
		Reward = new double[price_length];		
		V = new HashMap[jcde.no_goods]; // Value function V		
		pi = new HashMap[jcde.no_goods]; // optimal bidding function \pi
		
		for (int i = 0; i<jcde.no_goods; i++) {
				V[i] = new HashMap<WinnerAndRealized, Double>();
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
		}
		
		// 1) ******************************** The last round of auction: truthful bidding is no longer optimal
		
		t = jcde.no_goods-1;
		
		// > Loop over possible price histories
		for (IntegerArray realized : Cache.getCartesianProduct(jcde.bins, t)) {
			
			winner_plus = new BooleanArray(t+1);	// Needs to redeclare since size changing as t changes
			realized_plus = new IntegerArray(t+1);
			
			// copy realized prices (to append later)
			for (int k = 0; k < realized.d.length; k++)
				realized_plus.d[k] = realized.d[k];
			
    		// > Loop over possible winning histories 
    		for (BooleanArray winner : Cache.getWinningHistory(t)) {	// TODO: get losing history for weber
    			
    			wr = new WinnerAndRealized(winner, realized);	

    			// copy winner array (to append later)
	    		for (int k = 0; k < winner.d.length; k++)
					winner_plus.d[k] = winner.d[k];

    			// get conditional HOB distribution
				double[] condDist = jcde.getPMF(wr);				

				// Compute cost incurred by bidding
				double temp = 0;								// Sum
	    		for (int j = 0; j < b.length; j++){				// TODO: take into account tie breaking
//	    			temp += -(j*jcde.precision) * condDist[j];	// add -condDist*f(p)
	    			temp += - condDist[j];	// add -f(p)
	    			Reward[j] = temp;
	    		}
	    		
	    		for (int j = 0; j < b.length; j++)
	    			Reward[j] = b[j]*Reward[j];
    			
    			// Compute Q(b,state) for each bid b
	    		double max_value = Double.MIN_VALUE;		// Value of largest Q((X,t),b)
		    	int max_idx = -1;							// Index of largest Q((X,t),b)			    	
    			for (int i = 0; i < b.length; i++) {
	    			double temp2 = Reward[i];

	    			// if agent wins round t
    				realized_plus.d[realized.d.length] = i;
    				winner_plus.d[winner.d.length] = true;
	    			for (int j = 0; j <= i; j++) {			    			// XXX: j <= i or j < i? Do we assume winning if bids are the same? 
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

		    		Q[i] = temp2;
	    		}
    			
    			// just choose the best one
    			if (preference == 0){
    					V[t].put(wr, Q[max_idx]);
			    		pi[t].put(wr, b[max_idx]);
			    	}
    			else{
    				// find all the choosable bids, and select according to preference
    				indices.clear();
    				for (int i = 0; i < Q.length; i++){
	    				if (Q[i] > max_value - epsilon){
	    					indices.add(i);		    					
	    				}
	    			}
	    			if (preference == -1){	// prefer lowerbound
	    				V[t].put(wr,Q[indices.get(0)]);
	    				pi[t].put(wr, b[indices.get(0)]);
	    			}
	    			else if (preference == 1){ // prefer upperbound
		    				V[t].put(wr,Q[indices.get(indices.size()-1)]);
		    				pi[t].put(wr, b[indices.get(indices.size()-1)]);
	    			}
	    			else{
		    				idx = indices.get(rng.nextInt(indices.size()));	// pick one randomly
		    				V[t].put(wr,Q[idx]);
		    				pi[t].put(wr, b[idx]);
	    			}
    				
	    		}
    				
    		}
		}		    	
	

//		
//		for (BooleanArray winner : Cache.getWinningHistory(t)) {
//			for (IntegerArray realized : Cache.getCartesianProduct(jcde.bins, t)) {				
//				wr = new WinnerAndRealized(winner, realized); 	    		
//				
//				value_if_win = v.getValue(winner.getSum()+1);
//				value_if_lose = v.getValue(winner.getSum());
//				
//				if (discretize_value == true){
//					value_if_win = v_precision*legacy.DiscreteDistribution.bin(value_if_win,v_precision);
//					value_if_lose = v_precision*legacy.DiscreteDistribution.bin(value_if_lose,v_precision);
////					System.out.print
//				}
//				
//				optimal_bid = value_if_win - value_if_lose;	    		
//				pi[t].put(wr, optimal_bid);
//				double[] condDist = jcde.getPMF(wr);
//				
////// Print conditional Prices
////				System.out.print("condDist(realized = " + realized.d[0]*jde.precision + ") = ");
////				for (int i = 0; i < condDist.length; i++)
////					System.out.print(condDist[i] + " ");
////				System.out.println("]");
//				
//				// Compute Reward and F(p)
//				temp = 0;
//	    		winning_prob = 0;
//	    		for (int j = 0; j*jcde.precision < optimal_bid && j < condDist.length; j++) {	// TODO: take into account tie breaking, ie, optimal bid = j*jcde.precision?
//	    			temp += -(j*jcde.precision) * condDist[j];	// add -condDist*f(p): incur lose due to bidding
//	    			winning_prob += condDist[j];
//	    		}
//
//    			temp += winning_prob*value_if_win + (1-winning_prob)*value_if_lose;	    		
//	    		V[t].put(wr, temp);
//			}
//		}

		// 3) ******************************** Recursively assign values for t = no_slots-1,...,1
		
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

	    			
	    			// Compute Q(b,state) for each bid b
		    		double max_value = Double.MIN_VALUE;		// Value of largest Q((X,t),b)
			    	int max_idx = -1;							// Index of largest Q((X,t),b)			    	
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

			    		Q[i] = temp2;
		    		}
	    			
	    			// just choose the best one
	    			if (preference == 0){
	    					V[t].put(wr, Q[max_idx]);
				    		pi[t].put(wr, b[max_idx]);
				    	}
	    			else{
	    				// find all the choosable bids, and select according to preference
	    				indices.clear();
	    				for (int i = 0; i < Q.length; i++){
		    				if (Q[i] > max_value - epsilon){
		    					indices.add(i);		    					
		    				}
		    			}
		    			if (preference == -1){	// prefer lowerbound
		    				V[t].put(wr,Q[indices.get(0)]);
		    				pi[t].put(wr, b[indices.get(0)]);
		    			}
		    			else if (preference == 1){ // prefer upperbound
			    				V[t].put(wr,Q[indices.get(indices.size()-1)]);
			    				pi[t].put(wr, b[indices.get(indices.size()-1)]);
		    			}
		    			else{
			    				idx = indices.get(rng.nextInt(indices.size()));	// pick one randomly
			    				V[t].put(wr,Q[idx]);
			    				pi[t].put(wr, b[idx]);
		    			}
	    				
		    		}
	    				
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
				System.out.println("v_id = " + v_id + ", calculate MDP... ");
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
		return pi[good_id].get(new WinnerAndRealized(winner, realized));
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
		realized = tmp_r[good_id];
		for (int i = 0; i < realized.d.length; i++)
			realized.d[i] = JointDistributionEmpirical.bin(auction.hob[agent_idx][i], jcde.precision);
		
		// Get optimal bid
		return pi[good_id].get(new WinnerAndRealized(winner, realized));
	}
	
	// helpers. these may cheat.
	public double getFirstRoundBid() {
		// no goods won. good_id == 0. tmp_r[0] is a special global for good 0. 
		return pi[0].get(new WinnerAndRealized(new BooleanArray(new boolean[] {}) {},new IntegerArray(new int[] {}) ));
	}
	
	public double getIntermediateRoundBid(WinnerAndRealized wr) {
		return pi[0].get(wr);	// XXX: copy of? 
	}
	
	public double getLastRoundBid(int no_goods_won) {
		// truthful bidding. realized prices don't matter.
		return v.getValue(no_goods_won+1) - v.getValue(no_goods_won);
	}
	
	// Test certain aspects
	public static void main(String args[]) {
		// What does bin really do? I see... rounded to nearest integer. Not bad. 
		double precision = 0.02;
		for (int i = 0; i < 10; i++){
			double num = 0.005*i;
			System.out.println(num + " binned to " + precision*legacy.DiscreteDistribution.bin(num, precision));
		}
	}
}
