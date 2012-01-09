package legacy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import speed.JointDistribution;
import speed.JointDistributionEmpirical;

//Implements agent for Sequential SPSB auction based on MDP. Details of MDP is included in J's write up. 

public class FullMDPAgent extends Agent {

	JointDistributionEmpirical jde;

	public FullMDPAgent(int agent_idx, Valuation valuation, JointDistributionEmpirical jde) {
			super(agent_idx, valuation);
			this.jde = jde;
			
			// Do MDP calculation when initiating agent
			computeFullMDP(jde);


			if (agent_idx==0){
				System.out.println("\nAgent " + agent_idx + " Valuation:");
					valuation.print();
			}
					/*

				// Print out the mapping \pi: state --> optimal bid
				System.out.println("\nAgent " + agent_idx + ": my /pi and V mapping: ");
				for (P_X_t pxt : pi.keySet()) {
					System.out.println("pi("+pxt.toString()+") = " + pi.get(pxt));
				}

				for (P_X_t pxt : V.keySet()) {
					System.out.println("V("+pxt.toString()+") = " + V.get(pxt));
				}				
				*/

	}

	// Declare some variables. (X,t) is a state in MDP. Meaning: the set of goods obtained at step/auction t is X 		
	ArrayList<Double> b, Q, Reward, f;
	int no_slots = valuation.getNoValuations(), t, max_idx;
	double temp, temp2, bid, optimal_bid, max_value;
	P_X_t pxt,PXT;
	Set<Integer> X, X_more;
	double[] realized, realized_plus;
	Set<double[]> genPrices;
	DiscreteDistribution condDist;	// conditional price distribution
	HashMap<P_X_t,Double> V, pi;

	
	// Ask the agent to computes optimal bidding policy \pi((X,t)) using MDP. The two steps correspond to the two steps in write-up	
	public void computeFullMDP(JointDistribution pd){	

		V = new HashMap<P_X_t,Double>();			// Value function V((X,t))
		pi = new HashMap<P_X_t,Double>();			// optimal bidding function \pi((X,t))
		
		// 1) ******************************** Initialize V values for t = no_slots; corresponding to after all rounds are closed. 
		t = no_slots;
		
		// Start from the whole set, and assign V values to all its power sets
		Set<Integer> remaining_set = new HashSet<Integer>();
		for (int i = 0; i< no_slots; i++){
			remaining_set.add(i);
		}
		Set<Set<Integer>> genSet = PowerSet.generate(remaining_set);			// enumerate over all X
		int price_length = (int) (jde.max_price/jde.precision+1);
		
		double[] prices = new double[price_length];	// enumerate over all realized prices
		for (int i = 0; i < price_length; i++) {
			prices[i] = i*jde.precision;
		}
		
		genPrices = CartesianProduct.generate(prices, t);

		// Assign values to states
		for (Set<Integer> X : genSet) {
			for (double[] realized : genPrices) {
				pxt = new P_X_t(realized,X,t);
				V.put(pxt,valuation.getValue(X)); 
			}
		}

		
		// 2) ******************************** Recursively assign values for t = no_slots-1,...,1
		
		// > Loop over auction t
		for (t = no_slots-1; t>-1; t--){ 
			 
			// list of possible bids (to maximize over)
			b = new ArrayList<Double>();
			b.add(0.0);
			for (int i = 0; i < prices.length; i++){
				b.add(jde.precision* ((double) (i+(i+1))/2 - 0.1) );		// bid = (p_{i}+p_{i+1})/2 - 0.1*precision
			}

			// ----- loops start here
			genPrices = CartesianProduct.generate(prices, t);	// possible realized historical prices			
			remaining_set.remove(t);
    		genSet = PowerSet.generate(remaining_set);			// possible subset of goods			

    		// > Loop over possible realized historical prices
			for (double[] realized : genPrices){				
				
				// get conditional Distribution
				f = new ArrayList<Double>();
				for (int i = 0; i < prices.length; i++) {
					f.add(jde.getProb(prices[i], realized));
				}
				condDist = new DiscreteDistributionStatic(f, jde.precision);
				
				// Precompute Reward = R(b,(realized,X,t)) for each potential bid in b
				Reward = new ArrayList<Double>();
	    		temp = 0;
	    		Reward.add(0.0);				// 0 reward when bidding 0
	    		for (int j = 0; j < b.size() - 1; j++){
	    			temp += -(j*condDist.precision)*condDist.f.get(j);	// add -condDist*f(p)
	    			Reward.add(temp);
	    		}
	    		
	    		// > Loop over subsets of goods X = {0,...,t-1}
	    		for (Set<Integer> X : genSet) {
	    			pxt = new P_X_t(realized,X,t);

	    			X_more = new HashSet<Integer>();
		    		X_more.addAll(X);
		    		X_more.add(t);		    		
		    		
		    		// copy realized prices (to append later)
		    		realized_plus = new double[realized.length+1];
    				for (int k = 0; k < realized.length; k++)
    					realized_plus[k] = realized[k];
		    		    				
	    			// Compute Q(b,(realized,X,t)) for each bid b
	    			Q = new ArrayList<Double>(); 
		    		for (int i = 0; i < b.size(); i++) {		    			
		    			temp2 = Reward.get(i);

		    			// if agent wins round t
		    			for (int j = 0; j < i; j++){
		    				realized_plus[realized.length] = j*condDist.precision;
		    				PXT = new P_X_t(realized_plus,X_more,t+1);
		    				temp2 += condDist.f.get(j)*V.get(PXT);
		    			}
		    			// if agent doesn't win round t
		    			for (int j = i; j < condDist.f.size(); j++) {
		    				realized_plus[realized.length] = j*condDist.precision;
		    				PXT = new P_X_t(realized_plus,X,t+1);
		    				temp2 += condDist.f.get(j)*V.get(PXT);
		    			}
		    			Q.add(temp2);
		    		}

		    		// print Q function
		    		if (agent_idx == 0) {
			    		System.out.print("Q(b,"+pxt.toString()+")=");
			    		for (int i = 0; i < Q.size(); i++)
			    			System.out.print(Q.get(i)+",");
			    		System.out.println();
		    		}
		    		
		    		// Find \pi_((realized,X,t)) = argmax_b Q(b,(realized,X,t))
		    		max_value = Q.get(1);		// Value of largest Q((X,t),b) TODO: currently avoid bidding 0 by neglecting bid = 0.0; to fix later
			    	max_idx = 1;				// Index of largest Q((X,t),b)
			    	for (int i = 2; i < Q.size(); i++) {
			    		if (Q.get(i) > max_value) {	// Compare
			    			max_value = Q.get(i);
			    			max_idx = i;
			    		}
			    	}
		    		// Now we found the optimal bid for state (X,t). Assign values to \pi((X,t)) and V((X,t))
			    	V.put(pxt,Q.get(max_idx));
		    		pi.put(pxt,b.get(max_idx));
	    		}
    		}
		    	
		}
	}

	// This getBids need to be called once in each SPSB auction
	@Override
	public HashMap<Integer, Double> getBids() {

		// Figure out which auction is currently open
		Iterator<Integer> it_auction = openAuctions.iterator();
		int current_auction = it_auction.next();
		
		// Figure out what we have won in the past
		Set<Integer> goods_won = new HashSet<Integer>();
		for (int i = 0; i < current_auction; i++){
			if (results.get(i).getIsWinner() == true){
				goods_won.add(i);
			}
		}
	
		// figure out realized HOBs
		realized = new double[current_auction];
		for (int i = 0; i < current_auction;i ++) {
			realized[i] = results.get(i).getAuction().getHOB(agent_idx);	// TODO: ad hoc way to make prices integers.
			realized[i] = (int) realized[i];
		}
		
		P_X_t state = new P_X_t(realized,goods_won,current_auction);			// Current state (realized,X,t)
		HashMap<Integer, Double> bids = new HashMap<Integer, Double>();
		bids.put(current_auction, pi.get(state));
		
		System.out.println("agent "+agent_idx+": state = "+state.toString()+", current auction = "+current_auction+", and bid = "+pi.get(state));
/*		
		if (valuation.getValue(0) > 0 && current_auction == 0 && bids.get(0) == 0) {
			System.out.println("agent_idx: " + agent_idx + ", current_auction: " + current_auction + ", bid=" + bids.get(current_auction));
			System.exit(1);
		}
*/
		return bids;
	}

}
