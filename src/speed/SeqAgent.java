package speed;

// All agents are derived from the SeqAgent class.  This class requires two
// parameters: the agent ID, and its valuation function.
public abstract class SeqAgent {
	int agent_idx;
	Value v;
	SeqAuction auction;

	public SeqAgent(int agent_idx, Value v) {
		this.agent_idx = agent_idx;
		this.v = v;
	}

	// use this to reset the agent for another auction. Note that your agent is guaranteed to get
	// one reset() at the start of every auction. The agent should save a reference to "auction"
	// so that it can query auction information (such as past results).

	// note that this method should be overridden as necessary in your agent sub-class. if you
	// override it, however, make sure to call super.reset(auction) as the first task in your reset
	public void reset(SeqAuction auction) {
		this.auction = auction;
	}

	// use this during the auction to ask the agent for his/her bid. Note that your agent will get
	// exactly one getBid() for each good in the auction. If the agent wishes to use information
	// from previous rounds, it should inspect the public member variables of the SeqAuction instance
	// it was given in the last reset();
	public abstract double getBid(int good_id);

	public void setJointDistribution(JointDistributionEmpirical jde_old) {
	}

	public void setCondJointDistribution(JointCondDistributionEmpirical jcde) {
	}
}

