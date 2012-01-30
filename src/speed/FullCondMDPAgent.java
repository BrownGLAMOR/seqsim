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
	int x;
	double optimal_bid, temp, winning_prob;
	IntegerArray realized, realized_plus;
	BooleanArray winner, winner_plus;
	
	HashMap<WinnerAndRealized, Double>[][] pi; // [no_goods_won][t].get([winner] [realized]) ==> pi
	HashMap<WinnerAndRealized, Double>[][] V; // [no_goods_won][t].get([winner] [realized]) ==> V

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
		no_goods_won = 0;
	}
	
	// What does this do? 
	@SuppressWarnings("unchecked")
	private void allocate() {
		// list of possible bids (to maximize over)
		b = new double[price_length];

		for (int i = 0; i < b.length; i++)
			b[i] = jcde.precision*((i+(i+1))/2.0 - 0.1);		// bid = (p_{i}+p_{i+1})/2 - 0.1*precision	

		Q = new double[price_length];		
		Reward = new double[price_length];		
		V = new HashMap[jcde.no_goods+1][jcde.no_goods+1]; // Value function V((P,X,t))		
		pi = new HashMap[jcde.no_goods+1][jcde.no_goods+1]; // optimal bidding function \pi((P,X,t))
		
		for (int i = 0; i<=jcde.no_goods; i++) {
			for (int j = 0; j<=jcde.no_goods; j++) {
				V[i][j] = new HashMap<WinnerAndRealized, Double>();
				pi[i][j] = new HashMap<WinnerAndRealized, Double>();
			}
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
			for (int j = 0; j<=jcde.no_goods; j++) {
				V[i][j].clear();
				pi[i][j].clear();
			}
		}
		
		// 1) ******************************** Initialize V values for t = no_slots; corresponding to after all rounds are closed. 
		int t = jcde.no_goods;

		wr = tmp_wr[t];
		// State: x == no_goods_won
			for (BooleanArray winner : Cache.getWinningHistory(t)) {
				x = winner.getSum();
				for (IntegerArray realized : Cache.getCartesianProduct(jcde.bins, t)) {
					// XXX: We can do this, right? 
					wr.w = winner;
					wr.r = realized;
					V[x][t].put(wr, v.getValue(x)); 
				}
		}

		// 2) ******************************** The last round of auction: truthful bidding is optimal
		
		t = jcde.no_goods-1;
		
		wr = tmp_wr[t];
		
		for (BooleanArray winner : Cache.getWinningHistory(t)) {
			x = winner.getSum();
			for (IntegerArray realized : Cache.getCartesianProduct(jcde.bins, t)) {
				// XXX: same as previous
				wr.w = winner;
				wr.r = realized;
	    		optimal_bid = v.getValue(x+1) - v.getValue(x);
				pi[x][t].put(wr, optimal_bid);

////				
//				if (x == 0) {
//					System.out.println("optimal bid = "+optimal_bid);
//				}
				// get conditional Distribution
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

	    		temp += winning_prob*v.getValue(x+1) + (1-winning_prob)*v.getValue(x);	    		
	    		V[x][t].put(wr, temp);
	    		
//	    		System.out.println("V["+x+"]["+t+"].get("+realized.d[0]*jde.precision+") = " + V[x][t].get(realized));
			}
		}
		// 3) ******************************** Recursively assign values for t = no_slots-1,...,1
		
		// > Loop over auction t
		for (t = jcde.no_goods-2; t>-1; t--) {

			wr = tmp_wr[t];
			wr_plus = tmp_wr[t+1];

    		// > Loop over possible realized historical prices
			for (IntegerArray realized : Cache.getCartesianProduct(jcde.bins, t)) {
				
	    		wr.r = realized;
	    		wr_plus.r = realized_plus;

	    		// > Loop over past histories 
	    		for (BooleanArray winner : Cache.getWinningHistory(t)) {
	    			
					// copy winner array (to append later)
					winner_plus = tmp_w[winner.d.length + 1];
		    		for (int k = 0; k < winner.d.length; k++)
						winner_plus.d[k] = winner.d[k];
	    			
		    		wr.w = winner;
	    			wr_plus.w = winner_plus;
	    			
	    			x = winner.getSum();
	    			
	    			// get conditional Distribution
					double[] condDist = jcde.getPMF(wr);				
					// Compute Reward
					double temp = 0;								// Sum
		    		for (int j = 0; j < b.length; j++){
		    			temp += -(j*jcde.precision) * condDist[j];	// add -condDist*f(p)
		    			Reward[j] = temp;
		    		}

	    			
	    			// Compute Q(b,(realized,X,t)) for each bid b

		    		// At same time, Find \pi_((realized,X,t)) = argmax_b Q(b,(realized,X,t))
		    		double max_value = Double.MIN_VALUE;		// Value of largest Q((X,t),b)
			    	int max_idx = -1;							// Index of largest Q((X,t),b)
			    	
	    			for (int i = 0; i < b.length; i++) {
		    			double temp2 = Reward[i];

		    			// if agent wins round t
		    			for (int j = 0; j <= i; j++) {
		    				realized_plus.d[realized.d.length] = j;
		    				winner_plus.d[winner.d.length] = true;
		    				temp2 += condDist[j] * V[x+1][t+1].get(wr_plus);
		    			}
		    			
		    			// if agent doesn't win round t
		    			for (int j = i+1; j < condDist.length; j++) {
		    				realized_plus.d[realized.d.length] = j;
		    				winner_plus.d[winner.d.length] = false;
		    				temp2 += condDist[j] * V[x][t+1].get(wr_plus);
		    			}
		    					    			
			    		if (temp2 > max_value || i == 0) {	// Compare
			    			max_value = temp2;
			    			max_idx = i;
			    		}
			    		
		    			Q[i] = temp2;
		    		}
	    			
		    		// Now we found the optimal bid for state (X,t). Assign values to \pi((X,t)) and V((X,t))
			    	V[x][t].put(wr, Q[max_idx]);
		    		pi[x][t].put(wr, b[max_idx]);
		    		
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
		no_goods_won = 0;
		winner = tmp_w[good_id]; 
		if (good_id > 0) {
			for (int i = 0; i < good_id; i++){
				if (auction.winner[good_id-1] == agent_idx) {
					winner.d[i] = true;
					no_goods_won++;
				}
				else
					winner.d[i] = false;
			}
		}
		
		// figure out realized prices
		realized = tmp_r[good_id];
		for (int i = 0; i < realized.d.length; i++)
			realized.d[i] = JointDistributionEmpirical.bin(auction.hob[agent_idx][i], jcde.precision);
		
		// Wrap them up
		WinnerAndRealized wr = tmp_wr[good_id];
		wr.w = winner;
		wr.r = realized;
		
		return pi[no_goods_won][good_id].get(wr);
	}
	
	// helpers. these may cheat.
	public double getFirstRoundBid() {
		// no goods won. good_id == 0. tmp_r[0] is a special global for good 0. 
		return pi[0][0].get(tmp_r[0]);
	}
	
	public double getSecondRoundBid(int no_goods_won) {
		// truthful bidding. realized prices don't matter.
		return v.getValue(no_goods_won+1) - v.getValue(no_goods_won);
	}
}
