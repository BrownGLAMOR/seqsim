package speed;

import java.util.HashMap;

public class FullMDPNumGoodsSeqAgent extends SeqAgent {

	// auction variables
	SeqAuction auction;
	JointDistributionEmpirical jde;
	int agent_idx;
	int no_goods_won;
	int no_goods;
	
	// computational variables
	int price_length;
	
	HashMap<IntegerArray, Double>[][] pi; // [no_goods_won][t].get(realized prices) ==> pi
	HashMap<IntegerArray, Double>[][] V; // [no_goods_won][t].get(realized prices) ==> V

	double[] Q;
	double[] Reward;
	double[] b;
	
	IntegerArray[] tmp_r;

	public FullMDPNumGoodsSeqAgent(Value valuation, int agent_idx) {
		super(agent_idx, valuation);
				
		// initiate (to be used when calling "getBid()"; to be cleared when "reset()")
		no_goods_won = 0;
	}
	
	@SuppressWarnings("unchecked")
	private void allocate() {
		// list of possible bids (to maximize over)
		b = new double[price_length];

		for (int i = 0; i < b.length; i++)
			b[i] = jde.precision*((i+(i+1))/2.0 - 0.1);		// bid = (p_{i}+p_{i+1})/2 - 0.1*precision	

		Q = new double[price_length];
		
		Reward = new double[price_length];
		
		V = new HashMap[jde.no_goods+1][jde.no_goods+1]; // Value function V((P,X,t))
		
		pi = new HashMap[jde.no_goods+1][jde.no_goods+1]; // optimal bidding function \pi((P,X,t))
		
		for (int i = 0; i<=jde.no_goods; i++) {
			for (int j = 0; j<=jde.no_goods; j++) {
				V[i][j] = new HashMap<IntegerArray, Double>();
				pi[i][j] = new HashMap<IntegerArray, Double>();
			}
		}
		
		// Create a temporary area for getBids() to retrieve results
		// with having to perform a "new" operation (speed improvement)
		tmp_r = new IntegerArray[no_goods+1];
		
		for (int i = 0; i<no_goods+1; i++)
			tmp_r[i] = new IntegerArray(i);
	}
	
	// Ask the agent to computes optimal bidding policy \pi((X,t)) using MDP. The two steps correspond to the two steps in write-up	
	public void computeFullMDP() {	
		// Reset MDP state vars
		for (int i = 0; i<=jde.no_goods; i++) {
			for (int j = 0; j<=jde.no_goods; j++) {
				V[i][j].clear();
				pi[i][j].clear();
			}
		}
		
		// 1) ******************************** Initialize V values for t = no_slots; corresponding to after all rounds are closed. 
		int t = jde.no_goods;
		
		// Start from the whole set, and assign V values to all its power sets		
		
		// Assign values to states
		// x == no_goods_won (the "X" in our state)
		for (int x = 0; x<=jde.no_goods; x++) {
			for (IntegerArray realized : Cache.getCartesianProduct(jde.bins, t)) {
				//System.out.println("V[x=" + x + "][t=" + t + "][prices=" + realized + "] => " + v.getValue(x));
				V[x][t].put(realized, v.getValue(x)); 
			}
		}
		
		// 2) ******************************** Recursively assign values for t = no_slots-1,...,1
		
		// > Loop over auction t
		for (t = jde.no_goods-1; t>-1; t--) {
			// ----- loops start here

			//System.out.println("START t = " + t + " (genPrices=" + genPrices.size() + ")");

    		// > Loop over possible realized historical prices
			for (IntegerArray realized : Cache.getCartesianProduct(jde.bins, t)) {
				/*System.out.print("p{");
				for (int d : realized.d)
					System.out.print(d + ",");
				System.out.println("}");
				*/
				
				// get conditional Distribution
				double[] condDist = jde.getPMF(realized);
				IntegerArray realized_plus = tmp_r[realized.d.length + 1];
				
				// Precompute Reward = R(b,(realized,X,t)) for each potential bid in b
				double temp = 0;
	    		for (int j = 0; j < b.length; j++){
	    			temp += -(j*jde.precision) * condDist[j];	// add -condDist*f(p)
	    			Reward[j] = temp;
	    		}

	    		// copy realized prices (to append later)
	    		for (int k = 0; k < realized.d.length; k++)
					realized_plus.d[k] = realized.d[k];

	    		// > Loop over no_of_goods 0 ... t 
	    		for (int x = 0; x<=t; x++) {
		    		    				
	    			// Compute Q(b,(realized,X,t)) for each bid b

		    		// At same time, Find \pi_((realized,X,t)) = argmax_b Q(b,(realized,X,t))
		    		double max_value = Double.MIN_VALUE;		// Value of largest Q((X,t),b)
			    	int max_idx = -1;							// Index of largest Q((X,t),b)
			    	
	    			for (int i = 0; i < b.length; i++) {
		    			double temp2 = Reward[i];

		    			// if agent wins round t
		    			for (int j = 0; j <= i; j++) {
		    				realized_plus.d[realized.d.length] = j;
		    				temp2 += condDist[j] * V[x+1][t+1].get(realized_plus);
		    			}
		    			
		    			// if agent doesn't win round t
		    			for (int j = i+1; j < condDist.length; j++) {
		    				realized_plus.d[realized.d.length] = j;
		    				temp2 += condDist[j] * V[x][t+1].get(realized_plus);
		    			}
		    					    			
			    		if (temp2 > max_value) {	// Compare
			    			max_value = temp2;
			    			max_idx = i;
			    		}
			    		
		    			Q[i] = temp2;
		    		}
	    			
//// print Q function (to comment out) 
/*			    		System.out.print("Q(b,(" + x + "," + t +"))=");
			    		for (int i = 0; i < Q.length; i++)
			    			System.out.print(Q[i]+",");
			    		System.out.println();
	*/	    		
			    	
		    		// Now we found the optimal bid for state (X,t). Assign values to \pi((X,t)) and V((X,t))
			    	V[x][t].put(realized, Q[max_idx]);
		    		pi[x][t].put(realized, b[max_idx]);
		    		
		    		//System.out.println("assign pi[" + x + "][" + t + "].put(" + realized + ") ==> " + b[max_idx]);
	    		}
    		}		    	
		}
		
		//System.out.println("---");
	}

	
	@Override
	public void reset(SeqAuction auction) {
		this.auction = auction;
		no_goods_won = 0;
		
		computeFullMDP();
	}

	@Override
	public void setJointDistribution(JointDistributionEmpirical jde) {
		this.jde = jde;
		
		// If this JDE does not have the same parameters as our last jde, then (re)allocate memory
		if (pi == null || jde.no_bins != price_length || jde.no_goods != no_goods) {
			price_length = jde.no_bins;
			no_goods = jde.no_goods;
			
			allocate();
		}
	}
	
	@Override
	public double getBid(int good_id) {	
		// Do MDP calculation the first time
		if (good_id > 0) {
			if (auction.winner[good_id-1] == agent_idx)
				no_goods_won++;
		}

		// figure out realized HOBs
		IntegerArray realized = tmp_r[good_id];
		for (int i = 0; i < realized.d.length; i++)
			realized.d[i] = JointDistributionEmpirical.bin(auction.hob[agent_idx][i], jde.precision);
		
		//System.out.println("good id = "+good_id+",current state = ");

		//System.out.println("pi: " + pi[no_goods_won][good_id]);
		//System.out.println("pi.get: " + pi[no_goods_won][good_id].get(realized));
				
		return pi[no_goods_won][good_id].get(realized);
	}
	
}
