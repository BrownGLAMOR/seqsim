package speed;

// This Agent bids the HOB array fed. Created only for running fake auction purposes (as in AshwinDistance.java) 
public class HOBAgent extends SeqAgent{
	
	double[] HOB;
	public HOBAgent(Value valuation, int agent_idx, double[] HOB) {
		super(agent_idx, valuation);
		this.HOB = HOB;
	}

	@Override
	public void reset(SeqAuction auction) {
		this.auction = auction;
		if (auction.no_goods != HOB.length)
			throw new RuntimeException("no_goods mismatch!");
	}
	
	@Override
	public double getBid(int good_id) {
		return HOB[good_id];
	}		
		
}
