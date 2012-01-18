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
		
		V = new double[pd.length+1][pd.length+1]; // Value function V((X,t))
		pi = new double[pd.length+1][pd.length+1]; // optimal bidding function \pi((X,t))
		
		// 1) ******************************** Last stage 
		int t = pd.length;
		
		// Assign values to states
		for (int x = 0; x <= t; x++) {
				V[x][t] = v.getValue(x); 
//// print			
//			System.out.println("V["+x+"]["+t+"] = "+V[x][t]);
		}

		// 2) ******************************** Recursively go back

		// > Loop over auction t
		for (t = pd.length-1; t>=0; t--) {
			for (int x = 0; x <= t; x++) {
				pi[x][t] = V[x+1][t+1] - V[x][t+1];	// optimal bidding (Greenwald Boyan '02)
			
			// Value calculation
			double temp = 0;
			for (int j = 0; j*pd[t].precision <= pi[x][t] && j < pd[t].f.size(); j++)		// calculate reward
				temp += -(j*pd[t].precision) * pd[t].f.get(j);	// add -condDist*f(p)
	
			double winning_prob = pd[t].getCDF(pi[x][t], 0.0);
			temp += winning_prob*v.getValue(x+1) + (1-winning_prob)*v.getValue(x);	    		
			V[x][t] = temp;
//// print			
//			System.out.println("pi["+x+"]["+t+"] = "+pi[x][t]+", V["+x+"]["+t+"] = "+V[x][t]);
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
