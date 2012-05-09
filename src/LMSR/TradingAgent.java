package LMSR;


public abstract class TradingAgent {
	TradingGame G;
	int agent_idx;
	
	// constructor requires that the agent's index in the auction be passed,
	// as well as a valuation function
	public TradingAgent(int agent_idx) {
		this.agent_idx = agent_idx;
	}
	
	// use this to reset the agent for another auction. Note that your agent is guaranteed to get
	// one reset() at the start of every auction. The agent should save a reference to "auction"
	// so that it can query auction information (such as past results).
	
	// note that this method should be overridden as neccessary in your agent sub-class. if you
	// override it, however, make sure to call super.reset(auction) as the first task in your reset
	public abstract void reset(TradingGame G);
	
	// the game asks agent to move the price at a specific round. Agent should figure out game history
	// through the public variables
	public abstract int move(int round);

	
}
