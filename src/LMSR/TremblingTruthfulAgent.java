package LMSR;

import java.io.IOException;
import java.util.Random;

// An agent that intends to play truthfully, but trembles to the two wrong actions with prob epsilon (each)
public class TremblingTruthfulAgent extends TruthfulAgent{

	double epsilon;
	Random rng;
	
	public TremblingTruthfulAgent(int agent_idx, double epsilon, Random rng) {
		super(agent_idx);
		this.epsilon = epsilon;
		this.rng = rng;
	}
	
	@Override
	public int move(int round) {
		
		double r = rng.nextDouble();	// tremble in both rounds

		if (reported == false){
			reported = true;
			
			if (theta==true){
				// tremble
				if (r < 1 - 2*epsilon)
					return 1;
				else if (r < 1 - epsilon)
					return 0;
				else
					return -1;
			}
			else{
				// tremble
				if (r < 1 - 2*epsilon)
					return -1;
				else if (r < 1 - epsilon)
					return 0;
				else
					return 1;
			}
		}
		
		else{
			if (r < 1 - 2*epsilon)
				return 0;
			else if (r < 1 - epsilon)
				return -1;
			else
				return 1;
		}
	}

	public static void main(String[] args) throws IOException {
		
		// parameters
		int no_rounds = 2;
		int no_agents = 2;
		int no_plays = 10;
		double p0 = 0.5;
		double rho = 0.9;
		double epsilon = 0.1; 
		Random rng = new Random();

		// Set up agents, signal, and game
		TradingAgent[] agents = new TradingAgent[no_agents];
		for(int i = 0; i < no_agents; i++)
			agents[i] = new TremblingTruthfulAgent(i, epsilon, rng);
		SimpleSignal signal = new SimpleSignal(p0, rho, no_agents, rng);		
		TradingGame G = new TradingGame(agents, no_rounds, signal);
		
		// play a few times
		for (int i = 0; i < no_plays; i++)
			G.play(false);
		
	}

}
