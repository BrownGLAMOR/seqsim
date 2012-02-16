package speed;

import legacy.DiscreteDistribution;
// Make conditional bids as user wants (only work in 2 rounds)

public class SimpleAgent extends SeqAgent {

	int agent_idx;
	Value valuation;
	
	public SimpleAgent(Value valuation, int agent_idx) {
		super(agent_idx, valuation);
		this.agent_idx = agent_idx;
		this.valuation = valuation;
	}

	// Calculate the first Bid
	
	@Override
	public void reset(SeqAuction auction) {
		this.auction = auction;
	}

	@Override
	// Bids prescribed by user
	public double getBid(int good_id) {
		if (good_id == 1) {
			if (auction.winner[0] == agent_idx)
				return 1;
			else
				return 2;
		}
		else
			return 3;
	}
}

