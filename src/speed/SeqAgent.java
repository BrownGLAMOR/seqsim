package speed;

public abstract class SeqAgent {
	// use this to reset the agent for another auction. Note that your agent is guaranteed to get
	// one reset() at the start of every auction. The agent should save a reference to "auction"
	// so that it can query auction information (such as past results).
	public abstract void reset(SeqAuction auction);
	
	// use this during the auction to ask the agent for his/her bid. Note that your agent will get
	// exactly one getBid() for each good in the auction. If the agent wishes to use information
	// from previous rounds, it should inspect the public member variables of the SeqAuction instance
	// it was given in the last reset();
	public abstract double getBid(int good_id);
}
