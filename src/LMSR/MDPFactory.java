package LMSR;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import speed.IntegerArray;
import speed.Statistics;

//Simulator where agents are played against each other to induce an MDP for everyone
public class MDPFactory extends Thread {

	int no_rounds, no_simulations;
	TradingGame G;
	boolean take_log, record_utility;

	double[][] utility;	// utilities of each player
	int[][] log;		// game histories

	
	public MDPFactory() {}
	
	//  Main function: generates and records MDP for each player by repeate playing games
	public MDP simulateMDP(TradingGame G, int no_simulations, boolean take_log, boolean record_utility) throws IOException {	
		
		// memory allocation
		if (take_log == true){
			int[][] log = new int[no_simulations][G.no_agents*G.no_rounds];			// record realized prices from seller's point of view
			this.log = log;
		}
		if (record_utility == true) {
			double[][] utility = new double[G.no_agents][no_simulations];
			this.utility = utility;
		}

		// Play game
		MDP mdp = new MDP(G.no_agents, G.no_rounds);
		for (int j = 0; j<no_simulations; j++) {
		
			// Play the game. This will call the agent's reset(), which will cause them to renew signal
			G.play(true);		// true=="quiet mode"
			
			// record if instructed
			if (take_log == true)
					log[j] = G.history.clone();
			if (record_utility == true){
				for (int k = 0; k < G.no_agents; k++)
					utility[k][j] = G.profit[k];
			}

			// Add data points to MDP
			mdp.populate(new IntegerArray(G.history), G.reward, G.signal);

			}
		mdp.normalize();
		return mdp;
	}

	// Testing: Initiate from truthful agents, and report MDP generated
	public static void main(String args[]) throws IOException {

		Cache.init();
		
		// parameters
		int no_rounds = 2;
		int no_agents = 2;
		double p0 = 0.5;
		double rho = 0.9;		// signal strength
		double epsilon = 0.1;	// trembling
		Random rng = new Random();
		
		int no_simulations = 100000;
		boolean take_log = true, record_utility = true;

		// Set up agents, signal, and game
		TradingAgent[] agents = new TradingAgent[no_agents];
		for(int i = 0; i < no_agents; i++)
			agents[i] = new TremblingTruthfulAgent(i,epsilon,rng);
		SimpleSignal signal = new SimpleSignal(p0, rho, no_agents, rng);		
		TradingGame G = new TradingGame(agents, no_rounds, signal);
		
		// Generate MDP
		MDPFactory f = new MDPFactory();
		MDP mdp = f.simulateMDP(G, no_simulations, take_log, record_utility);

		// Print
		ArrayList<IntegerArray> SAs = new ArrayList<IntegerArray>();
		SAs.add(new IntegerArray(new int[] {-1}));
		SAs.add(new IntegerArray(new int[] {0}));
		SAs.add(new IntegerArray(new int[] {1}));
		SAs.add(new IntegerArray(new int[] {1,1}));
		
		IntegerArray SA;
		Iterator<IntegerArray> it = SAs.iterator();
		while (it.hasNext()){
			SA = it.next();

			// Get stuff
			ArrayList<IntegerArray> NSs = mdp.getNextStates(SA);
			
			// print SA and next round states
			System.out.println("\nSA = " + SA.print() + ", NSs = ");
			for (int i = 0; i < NSs.size()-1; i++)
				System.out.print(NSs.get(i).print() + "  ");
			System.out.println(NSs.get(NSs.size()-1).print());
			
			// print transitions probs and rewards (if true)
			double[] p = mdp.getP(SA,true);
			double r = mdp.getR(SA,true);
			System.out.print("signal = true: r = " + r + ", p = [");
			for (int i = 0; i < p.length-1; i++)
				System.out.print(p[i] + ",");
			System.out.println(p[p.length-1] + "]");

			// print transitions probs and rewards (if false)
			p = mdp.getP(SA,false);
			r = mdp.getR(SA,false);
			System.out.print("signal = false: r = " + r + ", p = [");
			for (int i = 0; i < p.length-1; i++)
				System.out.print(p[i] + ",");
			System.out.println(p[p.length-1] + "]\n\n");
			
			// Print utility
			System.out.println("agent 0: mean(utility) = " + Statistics.mean(f.utility[0]) + ", std(utility) = " + Statistics.stdev(f.utility[0]));
			System.out.println("agent 1: mean(utility) = " + Statistics.mean(f.utility[1]) + ", std(utility) = " + Statistics.stdev(f.utility[1]));

			// Print some game logs: TODO
			
		}
	}
}
