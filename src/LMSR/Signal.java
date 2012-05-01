package LMSR;

public abstract class Signal {

	static double priorPrice;
	
	// call this to get the valuation
	public abstract boolean getSignal(int agent_id);
		
	// call this to generate a new signal & renew prior
	public abstract void reset();
}
