package speed;

import java.io.IOException;

import legacy.DiscreteDistribution;

public class RandomSeqAgent extends SeqAgent {
	double max_bid, precision;
	
	// Agent that uniformly randomly bids over admissible bids
	public RandomSeqAgent(int agent_idx, double max_bid, double precision, Value v) {
		super(agent_idx, v);
		this.max_bid = max_bid;
		this.precision = precision;
	}

	@Override
	public void reset(SeqAuction auction) {
		// this method must be overridden to reset the client. The first step
		// must be to call super.reset(auction). sub-class tasks may follow.
		super.reset(auction);
		
		// Random agent has no sub-tasks required.
	}

	@Override
	public double getBid(int good_id) {
		return DiscreteDistribution.bin(Math.random()*max_bid, precision)*precision;
	}

	// Testing
	public static void main(String args[]) throws IOException {
		double max_bid = 1.0, precision = 0.02;
		int no_goods = 2;
		SeqAgent agent = new RandomSeqAgent(0, max_bid, precision, new SimpleValue(no_goods));
		
		for (int i = 0; i < 100; i++)
			System.out.println(agent.getBid(0));
		
	}	
}
