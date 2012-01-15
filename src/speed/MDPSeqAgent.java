package speed;

import legacy.DiscreteDistribution;


public class MDPSeqAgent extends SeqAgent {

	// auction variables
	SeqAuction auction;
	DiscreteDistribution[] pd;
	int agent_idx;
	int no_goods_won;
	
	// computational variables
	int price_length;
	double[][] V, pi; // [no_goods_won][t] ==> V, pi

	public MDPSeqAgent(Value valuation, int agent_idx) {
		super(agent_idx, valuation);
		this.agent_idx = agent_idx;
		no_goods_won = 0;
	}
	
	// Ask the agent to computes optimal bidding policy \pi((X,t)) using MDP. The two steps correspond to the two steps in write-up	
	public void computeMDP(){	
		double[] Q = new double[price_length];
		double[] Reward = new double[price_length];
		
		V = new double[pd.length+1][pd.length+1]; // Value function V((X,t))
		pi = new double[pd.length+1][pd.length+1]; // optimal bidding function \pi((X,t))
		
		// 1) ******************************** Initialize V values for t = no_slots; corresponding to after all rounds are closed. 
		int t = pd.length;
		
		// Start from the whole set, and assign V values to all its power sets		
		final double[] prices = new double[price_length];	// enumerate over all realized prices
		for (int i = 0; i < price_length; i++)
			prices[i] = i*pd[0].precision;

		// Assign values to states
		// x == no_goods_won (the "X" in our state)
		for (int x = 0; x<=pd.length; x++)
				V[x][t] = v.getValue(x); 

		// list of possible bids (to maximize over)
		double[] b = new double[price_length];	// enumerate over all realized prices

		
		// 2) ******************************** Recursively assign values for t = no_slots-1,...,1

		for (int i = 0; i < prices.length; i++)
			b[i]=pd[0].precision*((double) (i+(i+1))/2-0.1);		// bid = (p_{i}+p_{i+1})/2 - 0.1*precision	
		
		// > Loop over auction t
		for (t = pd.length-1; t>=0; t--) {

			// Precompute Reward = R(b,(X,t)) for each potential bid in b
				double temp = 0;
	    		for (int j = 0; j < b.length; j++){
	    			temp += -(j*pd[0].precision)*pd[t].f.get(j);	// TODO: we need to make sure that f is fully populated up to max_price
	    			Reward[j]=temp;
	    		}
	    		
	    		// > Loop over no_of_goods 0 ... t 
	    		for (int x = 0; x<=t; x++) {

	    			// Compute Q(b,(X,t)) for each bid b
	    			for (int i = 0; i < b.length; i++) {
		    			Q[i] = Reward[i];
	    				Q[i] += pd[t].getCDF(b[i], 0.0) * V[x+1][t+1];
	    				Q[i] += (1-pd[t].getCDF(b[i], 0.0)) * V[x][t+1];
		    		}
	    			
//// print Q
//	    			if (agent_idx == 0) {
//		    			System.out.print("Q["+x+"]["+t+"] = ");
//		    			for (int i = 0; i < Q.length; i++)
//		    				System.out.print(Q[i]+" ");
//		    			System.out.println();
//	    			}
	    			
	    			// Find \pi_((X,t)) = argmax_b Q(b,(X,t))
		    		double max_value = Q[0];		// Value of largest Q((X,t),b)
			    	int max_idx = 0;			// Index of largest Q((X,t),b)
			    	for (int i = 1; i < Q.length; i++) {
			    		if (Q[i] > max_value) {	// Compare
			    			max_value = Q[i];
			    			max_idx = i;
			    		}
			    	}
			    	
		    		// Now we found the optimal bid for state (X,t). Assign values to \pi((X,t)) and V((X,t))
			    	V[x][t] = Q[max_idx];
		    		pi[x][t] = b[max_idx];
		    		
//	Print pi	    		
		    		System.out.println("agent "+agent_idx+": pi[x="+x+"][t="+t+"]="+pi[x][t]);
	    		}
    		}		    	
		}
	
	
	@Override
	public void reset(SeqAuction auction) {
		this.auction = auction;
		no_goods_won = 0;
		computeMDP();
	}

	public void setJointDistribution(DiscreteDistribution[] pd) {
		this.pd = pd;
		price_length = (int) (pd[0].f.size());	// (used in computeMDP)
	}
	
	@Override
	public double getBid(int good_id) {	

		if (good_id > 0) {
			if (auction.winner[good_id-1] == agent_idx)
				no_goods_won++;
		}

//		System.out.println("Agent "+agent_idx+", good id = "+good_id+",current state = ["+no_goods_won+"]["+good_id+"]");
				
		return pi[no_goods_won][good_id];
	}
	
}
