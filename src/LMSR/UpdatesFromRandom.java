package LMSR;

import java.io.IOException;
import java.util.Random;

// Initiate BR dynamics from random agents and see if convergence happens
public class UpdatesFromRandom {

	public static void main(String[] args) throws IOException {

	Cache.init();

	// Game parameters
	int no_rounds = 2;
	int no_agents = 2;
	Random rng = new Random();
	double p0 = 0.5, rho = 0.9;

	// Simulation parameters
	int no_init_pts = 100000;
	int no_iterations = 10, update_pts = 100000;
	boolean take_log = false, record_utility = false;

	// Set up MDP agents
	MDPAgent[] agents = new MDPAgent[no_agents];
	for (int k = 0; k < no_agents; k++)
		agents[k] = new MDPAgent(k);

	
	// 1) Initiate MDP from random agents
	MDPFactory f = new MDPFactory();

	TradingAgent[] agents0 = new TradingAgent[no_agents];
	for(int i = 0; i < no_agents; i++)
		agents0[i] = new RandomAgent(i, rng);
	SimpleSignal signal = new SimpleSignal(p0, rho, no_agents, rng);		
	TradingGame G = new TradingGame(agents0, no_rounds, signal);
	
	MDP mdp = f.onPolicyUpdate(G, no_init_pts, take_log, record_utility);
	

	// 2) BR updates
	
	for (int k = 0; k < no_agents; k++){
		agents[k].reset(G);
		agents[k].inputMDP(mdp);
		agents[k].SolveBellman(true);		// "true" = quite mode

		// Print Bellman solutions
		System.out.println("\nagent " + k + ":");
		agents[k].printpi();
	
	}


	
	G = new TradingGame(agents0, no_rounds, signal);
 
	

	}
}
