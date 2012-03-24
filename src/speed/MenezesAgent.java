package speed;

import java.util.Random;

import legacy.DiscreteDistribution;
// bids according to the Equilibrium Agent documented in Menezes's paper

public class MenezesAgent extends SeqAgent {

//	boolean ready = false;

	DiscreteDistribution F;
	int type;				// Type = 0 means bid as equilibrium strategy specifies; type = {-1,1,2} means bid lower bound, upper bound, or mixed
	int agent_idx;
	int no_goods_won;
	int n, no_agents;
	int i;
	MenezesValue valuation;
	double x, delta_x, max_value, numerator, denominator, highestbid, lowestbid;
	
	public MenezesAgent(MenezesValue valuation, int no_agents, int agent_idx, int type) {
		super(agent_idx, valuation);
		this.agent_idx = agent_idx;
		this.n = no_agents;
		this.valuation = valuation;
		this.x = valuation.getValue(1);
		this.delta_x = valuation.getValue(2);
		this.type = type;
	}
		// Calculate the first Bid
	public double calculateFirstBid() {
//		if (!ready)
//			throw new RuntimeException("must input Distribution F first");
	
//		 Numerical calculation
//		int bin_halfway = DiscreteDistribution.bin(delta_x - x, F.precision);
//		int bin_x = DiscreteDistribution.bin(x, F.precision);
//
//		// calculation
//		double constant = (n-2) / java.lang.Math.pow(F.getCDF(x, 0.0), n-2);
//		System.out.println("constant = " + constant);
//		double sum = 0;
//		for (i = 0; i <= bin_halfway; i++)
//			sum += (delta_x - x) * java.lang.Math.pow(F.getCDF(F.precision*i, 0.0), n-3) * (1/max_value) * F.precision;
//		if (bin_x > bin_halfway) {
//			for (i = bin_halfway + 1; i < bin_x; i++)
//				sum += (F.precision*i) * java.lang.Math.pow(F.getCDF(F.precision*i, 0.0), n-3) * (1/max_value) * F.precision;
//		}
//		
//		return constant*sum;

		// bid differently according to agent type 
		if (n == 2) {
			if (type == 0){
				return delta_x - x;
			}
			else if (type == -1){					// bid lower bound (pretend to be type \delta(x)
				return (delta_x - x)*(delta_x - x);
			}
			else if (type == 1)						// bid upper bound (pretend to be type \delta^{-1}(x)
				return java.lang.Math.sqrt(delta_x - x);
			else{									// bid randomly between lower and upper bound
				Random rng = new Random();
				double r = rng.nextDouble();
				return (1-r)*(delta_x - x)*(delta_x - x) + r*java.lang.Math.sqrt(delta_x - x);
			}
		}
		else if (valuation.decreasing == true) {
			// upper bound of bids
			numerator = java.lang.Math.pow(delta_x - x, n-1) + (n-2) * java.lang.Math.pow(x, n-1);
			denominator = (n-1) * java.lang.Math.pow(x, n-2);
			highestbid = numerator/denominator;
			
			// lower bound of bids
			numerator = java.lang.Math.pow(delta_x - x, 2*n-2) + (n-2) * java.lang.Math.pow(x, 2*n-2);	// TODO: this only works for delta(x) = x^2, not for general cases
			denominator = (n-1) * java.lang.Math.pow(x, 2*n-4);
			lowestbid = numerator/denominator;
			
			if (type == 0 || type == 1)
				return highestbid;
			else if (type == -1)
				return lowestbid;
			else{
				Random rng = new Random();
				double r = rng.nextDouble();
				return (1-r)*lowestbid + r*highestbid;
			}
		}
		else {
			return (delta_x - x);			// XXX: not changed yet - not considering increasing MV cases
		}
	}
	
	@Override
	public void reset(SeqAuction auction) {
		this.auction = auction;
		no_goods_won = 0;
		this.x = valuation.getValue(1);
		this.delta_x = valuation.getValue(2);
	}

	// Input F, the common prior valuation distribution from which x is drawn from
//	public void setParameters(DiscreteDistribution F, double max_value, int no_agents) {
//		this.F = F;
//		this.max_value = max_value;
//		this.n = no_agents; 		// Note that this n is the (N+1) in KatzmanAgent.java
//		ready = true;
//	}
	
	public double getFirstRoundBid() {
		return calculateFirstBid();
	}
	
	@Override
	public double getBid(int good_id) {
		// Bid "truthfully" in the second round
		if (good_id == 1) {
			if (auction.winner[0] == agent_idx)
				return delta_x - x;
			else
				return x;
		}
		else
			return calculateFirstBid();	// bid a specific amount in the first round (according to Menezes paper)
	}
}


