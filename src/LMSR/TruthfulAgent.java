package LMSR;

public class TruthfulAgent extends TradingAgent{

	boolean reported, bluffed;
	boolean theta;
	// Agent that reports theta truthfully their first chance
	public TruthfulAgent(int agent_idx) {
		super(agent_idx);
	}

	@Override
	public int move(int round) {
		// report truthfully for the first time
		if (reported == false){
			reported = true;
			if (theta==true)
				return 1;
			else
				return -1;
		}
		else{
			return 0;
		}
	}

	@Override
	public void reset(TradingGame G) {
		// learn theta
		this.theta = G.signal.getSignal(agent_idx);

		// Haven't reported theta yet
		this.reported = false;
		this.bluffed = false;
	}
}
