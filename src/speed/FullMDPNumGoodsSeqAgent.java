package speed;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

public class FullMDPNumGoodsSeqAgent extends SeqAgent {

	// auction variables
	SeqAuction auction;
	JointDistributionEmpirical jde;
	int agent_idx;
	int no_goods_won;
	
	// computational variables
	int price_length;
	HashMap<DoubleArray, Double>[][] V, pi; // [no_goods_won][t].get(realized prices) ==> V, pi

	public FullMDPNumGoodsSeqAgent(Value valuation, int agent_idx) {
		super(agent_idx, valuation);
				
		// initiate (to be used when calling "getBid()"; to be cleared when "reset()")
		no_goods_won = 0;
	}
	
	// Ask the agent to computes optimal bidding policy \pi((X,t)) using MDP. The two steps correspond to the two steps in write-up	
	public void computeFullMDP(){	
		double[] Q = new double[price_length];
		double[] Reward = new double[price_length];
		
		HashMap<DoubleArray, Double>[][] V = new HashMap[jde.no_goods+1][jde.no_goods+1]; // Value function V((P,X,t))
		pi = new HashMap[jde.no_goods+1][jde.no_goods+1]; // optimal bidding function \pi((P,X,t))
		
		for (int i = 0; i<=jde.no_goods; i++) {
			for (int j = 0; j<=jde.no_goods; j++) {
				V[i][j] = new HashMap<DoubleArray, Double>();
				pi[i][j] = new HashMap<DoubleArray, Double>();
			}
		}		
		
		// 1) ******************************** Initialize V values for t = no_slots; corresponding to after all rounds are closed. 
		int t = jde.no_goods;
		
		// Start from the whole set, and assign V values to all its power sets		
		
		// Assign values to states
		// x == no_goods_won (the "X" in our state)
		for (int x = 0; x<=jde.no_goods; x++) {
			for (double[] realized : Cache.getCartesianProduct(jde.prices, t))
			{
				//System.out.println("V[x=" + x + "][t=" + t + "][prices=" + realized + "] => " + valuation.getValue(x));
				V[x][t].put(new DoubleArray(realized), v.getValue(x)); 
			}
		}

		// list of possible bids (to maximize over)
		double[] b = new double[price_length];	// enumerate over all realized prices

		
		// 2) ******************************** Recursively assign values for t = no_slots-1,...,1
		
		// > Loop over auction t
		for (t = jde.no_goods-1; t>-1; t--) {
			// ----- loops start here
			for (int i = 0; i < jde.prices.length; i++)
				b[i]=jde.precision*((double) (i+(i+1))/2-0.1);		// bid = (p_{i}+p_{i+1})/2 - 0.1*precision	

			//System.out.println("START t = " + t + " (genPrices=" + genPrices.size() + ")");

    		// > Loop over possible realized historical prices
			for (double[] realized : Cache.getCartesianProduct(jde.prices, t)) {
				/*System.out.print("p{");
				for (double d : realized)
					System.out.print(d + ",");
				System.out.println("}");
				*/
				
				// get conditional Distribution
				double[] condDist = jde.getPMF(realized);
				double[] realized_plus = new double[realized.length+1];
				
				// Precompute Reward = R(b,(realized,X,t)) for each potential bid in b
				double temp = 0;
	    		for (int j = 0; j < b.length; j++){
	    			temp += -(j*jde.precision)*condDist[j];	// add -condDist*f(p)
	    			Reward[j]=temp;
	    		}
	    		
	    		// > Loop over no_of_goods 0 ... t 
	    		for (int x = 0; x<=t; x++) {
		    		// copy realized prices (to append later)
		    		for (int k = 0; k < realized.length; k++)
    					realized_plus[k] = realized[k];
		    		    				
	    			// Compute Q(b,(realized,X,t)) for each bid b
	    			for (int i = 0; i < b.length; i++) {
		    			double temp2 = Reward[i];

		    			// if agent wins round t
		    			for (int j = 0; j <= i; j++){
		    				realized_plus[realized.length] = j*jde.precision;
		    				temp2 += condDist[j] * V[x+1][t+1].get(new DoubleArray(Arrays.copyOf(realized_plus, realized_plus.length)));
		    			}
		    			
		    			// if agent doesn't win round t
		    			for (int j = i+1; j < condDist.length; j++) {
		    				realized_plus[realized.length] = j*jde.precision;
		    				temp2 += condDist[j] * V[x][t+1].get(new DoubleArray(Arrays.copyOf(realized_plus, realized_plus.length)));
		    			}
		    			
		    			Q[i]=temp2;
		    		}

//// print Q function (to comment out) 
/*			    		System.out.print("Q(b,(" + x + "," + t +"))=");
			    		for (int i = 0; i < Q.length; i++)
			    			System.out.print(Q[i]+",");
			    		System.out.println();
	*/	    		
		    		// Find \pi_((realized,X,t)) = argmax_b Q(b,(realized,X,t))
		    		double max_value = Q[0];		// Value of largest Q((X,t),b)
			    	int max_idx = 0;			// Index of largest Q((X,t),b)
			    	for (int i = 1; i < Q.length; i++) {
			    		if (Q[i] > max_value) {	// Compare
			    			max_value = Q[i];
			    			max_idx = i;
			    		}
			    	}
			    	
		    		// Now we found the optimal bid for state (X,t). Assign values to \pi((X,t)) and V((X,t))
			    	DoubleArray r = new DoubleArray(realized);
			    	V[x][t].put(r, Q[max_idx]);
		    		pi[x][t].put(r, b[max_idx]);
		    		
		    		//System.out.println("assign pi[" + x + "][" + t + "].put(" + r + ") ==> " + b[max_idx]);
	    		}
    		}		    	
		}
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
		price_length = (int) (jde.max_price/jde.precision+1);	// (used in computeFullMDP)
	}
	
	@Override
	public double getBid(int good_id) {	
		// Do MDP calculation the first time
		if (good_id > 0) {
			if (auction.winner[good_id-1] == agent_idx)
				no_goods_won++;
		}

		// figure out realized HOBs
		double[] realized = new double[good_id];
		for (int i = 0; i < good_id;i ++)
			realized[i] = (int)(auction.hob[agent_idx][i]); // TODO: currently, forced prices to be integers in an ad hoc way. To be fixed.
		
		//System.out.println("good id = "+good_id+",current state = ");

		//System.out.println("pi: " + pi[no_goods_won][good_id]);
		//System.out.println("pi.get: " + pi[no_goods_won][good_id].get(realized));
				
		return pi[no_goods_won][good_id].get(new DoubleArray(realized));
	}
	
}
