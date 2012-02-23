package speed;

import java.util.HashMap;

public class FullCondMDPAgent extends SeqAgent {

	// auction variables
	SeqAuction auction;
	JointCondDistributionEmpirical jcde;
	
	int agent_idx;
	int no_goods_won;
	int no_goods;
	
	// computational variables
	int price_length;
	double optimal_bid, temp, winning_prob;
	IntegerArray realized, realized_plus;
	BooleanArray winner, winner_plus;
	
	HashMap<WinnerAndRealized, Double>[] pi; // [t].get([winner] [realized]) ==> pi
	HashMap<WinnerAndRealized, Double>[] V; // [t].get([winner] [realized]) ==> V

	double[] Q;
	double[] Reward;
	double[] b;
	
	IntegerArray[] tmp_r;
	BooleanArray[] tmp_w;
	WinnerAndRealized[] tmp_wr;
	
	WinnerAndRealized wr, wr_plus;

	public FullCondMDPAgent(Value valuation, int agent_idx) {
		super(agent_idx, valuation);
				
		// initiate (to be used when calling "getBid()"; to be cleared when "reset()")
//		no_goods_won = 0;
	}
	
	// What does this do? 
	@SuppressWarnings("unchecked")
	private void allocate() {
		// list of possible bids (to maximize over)
		b = new double[price_length];

		for (int i = 0; i < b.length; i++)
			b[i] = jcde.precision*((i+(i+1))/2.0 - 0.49999);		// bid = (p_{i}+p_{i+1})/2 - 0.49999*precision	
//			b[i] = jcde.precision*((i+(i+1))/2.0 - 0.500001);		// bid = (p_{i}+p_{i+1})/2 - 0.49999*precision
//		b[0] = 0.000001;

		Q = new double[price_length];		
		Reward = new double[price_length];		
		V = new HashMap[jcde.no_goods+1]; // Value function V		
		pi = new HashMap[jcde.no_goods+1]; // optimal bidding function \pi
		
		for (int i = 0; i<=jcde.no_goods; i++) {
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
		for (int i = 0; i<=jcde.no_goods; i++) {
				V[i].clear();
				pi[i].clear();
		}
		
		// 1) ******************************** Initialize V values for t = no_slots; corresponding to after all rounds are closed. 
		int t = jcde.no_goods;

		for (BooleanArray winner : Cache.getWinningHistory(t)) {
				for (IntegerArray realized : Cache.getCartesianProduct(jcde.bins, t)) {
					V[t].put(new WinnerAndRealized(winner, realized), v.getValue(winner.getSum())); 
				}
		}

		// 2) ******************************** The last round of auction: truthful bidding is optimal
		
		t = jcde.no_goods-1;
		
		for (BooleanArray winner : Cache.getWinningHistory(t)) {

			for (IntegerArray realized : Cache.getCartesianProduct(jcde.bins, t)) {
				
				wr = new WinnerAndRealized(winner, realized);
	    		
				optimal_bid = v.getValue(winner.getSum()+1) - v.getValue(winner.getSum());
	    		pi[t].put(wr, optimal_bid);
				double[] condDist = jcde.getPMF(wr);
				
//// Print conditional Prices
//				System.out.print("condDist(realized = " + realized.d[0]*jde.precision + ") = ");
//				for (int i = 0; i < condDist.length; i++)
//					System.out.print(condDist[i] + " ");
//				System.out.println("]");
				
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
		// 3) ******************************** Recursively assign values for t = no_slots-1,...,1
		
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
		    			Reward[j] = temp;
		    		}

	    			
	    			// Compute Q(b,state) for each bid b
		    		// At same time, Find \pi_(state) = argmax_b Q(b,state)
		    		double max_value = Double.MIN_VALUE;		// Value of largest Q((X,t),b)
			    	int max_idx = -1;							// Index of largest Q((X,t),b)
			    	
	    			for (int i = 0; i < b.length; i++) {
		    			double temp2 = Reward[i];

		    			// if agent wins round t
		    			for (int j = 0; j <= i; j++) {
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
		    					    			
			    		if (temp2 > max_value || i == 0) {	// Compare
			    			max_value = temp2;
			    			max_idx = i;
			    		}
			    		
		    			Q[i] = temp2;
		    		}
	    			
	    			
		    		// Now we found the optimal bid for state (X,t). Assign values to \pi((X,t)) and V((X,t))
		    		V[t].put(wr, Q[max_idx]);
		    		pi[t].put(wr, b[max_idx]);
		    		
		    		// XXX: Do I need two unique instances of "wr" for V and pi as well? 
		    		
// Print condDist		    		
					//System.out.println("realized.length = " + realized.d.length);
					// Print conditional Prices
					/*System.out.print("condDist(no realized) = [");
					for (int i = 0; i < condDist.length; i++)
						System.out.print(condDist[i] + " ");
					System.out.println("]");
					*/

// print Q function 
//			    		System.out.print("Q(b,(" + x + "," + t +"))=");
//			    		for (int i = 0; i < Q.length; i++)
//			    			System.out.print(Q[i]+",");
//			    		System.out.println();
//			    	
//					System.out.println("first round: \t MDP agent bids " + pi[0][0].get(realized));
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

	
	@Override
	public void reset(SeqAuction auction) {
		this.auction = auction;		
		computeFullMDP();
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
	
	public double getSecondRoundBid(int no_goods_won) {
		// truthful bidding. realized prices don't matter.
		return v.getValue(no_goods_won+1) - v.getValue(no_goods_won);
	}
}
