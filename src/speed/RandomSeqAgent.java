package speed;

public class RandomSeqAgent extends SeqAgent {
	public RandomSeqAgent(int agent_idx, Value v) {
		super(agent_idx, v);
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
		return Math.random();
	}

}
