package speed;

import legacy.DiscreteDistribution;
// bids according to the Equilibrium Agent documented in Menezes's paper

public class MenezesAgent extends SeqAgent {

//	boolean ready = false;

	DiscreteDistribution F;
	int agent_idx;
	int no_goods_won;
	int n, no_agents;
	int i;
	Value valuation;
	double x, delta_x, max_value;
	
	public MenezesAgent(Value valuation, int no_agents, int agent_idx) {
		super(agent_idx, valuation);
		this.agent_idx = agent_idx;
		this.n = no_agents;
		this.valuation = valuation;
		this.x = valuation.getValue(1);
		this.delta_x = valuation.getValue(2);
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
		
		// theoretical calculation
		double numerator = java.lang.Math.pow(delta_x - x, n-1) + (n-2) * java.lang.Math.pow(x, n-1);
		double denominator = (n-1) * java.lang.Math.pow(x, n-2);

		return (numerator/denominator);
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
		else if (n == 2)
			return delta_x - x;
		else
			return calculateFirstBid();	// bid a specific amount in the first round (according to Menezes paper)
	}
}


