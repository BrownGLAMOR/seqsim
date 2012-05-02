package LMSR;

import java.io.IOException;
import java.util.Random;

public class RandomAgent extends TradingAgent{

	boolean signal;
	Random rng;
	// An agent that moves randomly. Used for benchmark and also for PR factory. 
	public RandomAgent(int agent_idx, Random rng) {
		super(agent_idx);
		this.rng = rng;
	}

	@Override
	public int move(int round) {
		// Move randomly
		double r = rng.nextDouble();
		if (r <= 1.0/3.0)
			return -1;
		else if (r <= 2.0/3.0)
			return 0;
		else
			return 1;
	}

	@Override
	public void reset(TradingGame G) {
	}
	
	public static void main(String[] args) throws IOException {
		Random rng = new Random();
		RandomAgent agent = new RandomAgent(0,rng);
		agent.move(1);
	}
}
