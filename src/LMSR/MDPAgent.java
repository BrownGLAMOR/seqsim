package LMSR;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

import speed.IntegerArray;

// An Agent that plays optimal strategy wrt an MDP
public class MDPAgent extends TradingAgent{

	boolean theta;
	int theta_id;
	boolean ready = false;		// whether has MDP inputted yet
	boolean solved = false;		// whether optimal solution has been solved on the newly inputted MDP
	boolean allocated = false;
	
	int[] ActionSpace;
	double[] Q_tmp;					// Holder of Q values
	IntegerArray[] SA_tmp;
	MDP mdp;
	
	// Bellman storage
	HashMap<IntegerArray, Double>[][] V;	// [individual_round][T/F]: state ==> value  
	HashMap<IntegerArray, Integer>[][] pi;	// [individual_round][T/F]: past ==> action

	
	public MDPAgent(int agent_idx) {
		super(agent_idx);
	}

	// Allocate memory for pi and V, and Action space
	@SuppressWarnings("unchecked")
	void allocate(){
		
		// pi and V
		V = new HashMap[G.no_rounds][2];
		pi = new HashMap[G.no_rounds][2];
		for (int k = 0; k < G.no_rounds; k++){
			for (int theta_id = 0; theta_id <= 1; theta_id++){
				V[k][theta_id] = new HashMap<IntegerArray, Double>();
				pi[k][theta_id] = new HashMap<IntegerArray, Integer>();
			}
		}
				
		// Hardcoded for now
		this.ActionSpace = new int[] {-1,0,1};
		
		// Holder of state after playing action
		SA_tmp = new IntegerArray[G.no_rounds];
		for (int k = 0; k < G.no_rounds; k++)
			SA_tmp[k] = new IntegerArray(k*G.no_agents + agent_idx + 1);
		
		// Holder of Q values
		Q_tmp = new double[ActionSpace.length];

		this.allocated = true;
	}
	
	// input MDP
	public void inputMDP(MDP mdp){
		this.mdp = mdp;
		this.ready = true;
		this.solved = false;
	}
	
	// Solve optimal strategy via Bellman equations
	public void SolveBellman(boolean quiet){
		
		// Allocate memory
		if (allocated == false)
			allocate();
		
		// 1) terminal round: k --> individual round index; t --> global round index
		int k = G.no_rounds-1;
		int t = k*G.no_agents + agent_idx;
		
		for (theta_id = 0; theta_id <= 1; theta_id++){
			for (IntegerArray S: Cache.genStates(t)){

			// Copy, to append
			for (int j = 0; j < S.d.length; j++)
				SA_tmp[k].d[j] = S.d[j];
			
				int max_id = -1;
				double max_value = Double.MIN_VALUE;

				// Get and compare Q values
				for (int i = 0; i < ActionSpace.length; i++){
					SA_tmp[k].d[SA_tmp[k].d.length-1] = ActionSpace[i];	// Append
					Q_tmp[i] = mdp.getR(SA_tmp[k], theta_id);			// XXX: need "get copy of"?

					// record highest
					if (Q_tmp[i] > max_value || i == 0){
						max_id = i;
						max_value = Q_tmp[i];
					}
				}
//				 Output Q values
				System.out.println("state = (" + theta_id + "," + S.print() + "), Q = [" + Q_tmp[0] + "," + Q_tmp[1] + "," + Q_tmp[2] + "], pi = " + (max_id-1));

				// Store pi and V
				pi[k][theta_id].put(S, max_id - 1);
				V[k][theta_id].put(S, max_value);
				
			}
		}
		
		// 2) non terminal rounds
		for (k = G.no_rounds - 2; k >= 0; k--){
			
			t = k*G.no_agents + agent_idx;	// k is individual round, t is global round

			for (theta_id = 0; theta_id <= 1; theta_id++){
				for (IntegerArray S: Cache.genStates(t)){
	
				// Copy, to append
				for (int j = 0; j < S.d.length; j++)
					SA_tmp[k].d[j] = S.d[j];
				
					int max_id = -1;
					double max_value = Double.MIN_VALUE;
	
					// Get and compare Q values
					for (int i = 0; i < ActionSpace.length; i++){
						SA_tmp[k].d[SA_tmp[k].d.length-1] = ActionSpace[i];	// Append
						Q_tmp[i] = mdp.getR(SA_tmp[k], theta_id);			// XXX: need "get copy of"?
	
						// record highest
						if (Q_tmp[i] > max_value || i == 0){
							max_id = i;
							max_value = Q_tmp[i];
						}
					}
	
//					 Output Q values
					System.out.println("state = (" + theta_id + "," + S.print() + "), Q = [" + Q_tmp[0] + "," + Q_tmp[1] + "," + Q_tmp[2] + "], pi = " + (max_id-1));

					// Store pi and V
					pi[k][theta_id].put(S, max_id-1);
					V[k][theta_id].put(S, max_value);
					
				}
			}
		}
		
		this.solved = true;
	}
	
	// Print pi
	public void printpi(){
		if (solved == false)
			throw new IllegalArgumentException("Bellman equations not solved yet");

		for (theta_id = 0; theta_id <= 1; theta_id++){
			System.out.println();
			for (int k = 0; k < G.no_rounds; k++){
				int t = k*G.no_agents + agent_idx;	// k is individual round, t is global round
				for (IntegerArray S: Cache.genStates(t)){
					System.out.println("pi(" + theta_id + "," + S.print() + ") = " + pi[k][theta_id].get(S));
				}
			}
		}
	}
	
	@Override
	public void reset(TradingGame G) {
		// learn signal
		this.G = G;
		this.theta = G.signal.getSignal(agent_idx);
		if (theta == true)
			this.theta_id = 1;
		else
			this.theta_id = 0;
	}

	
	@Override
	public int move(int t) {
		if (solved == false)
			SolveBellman(true);

		// figure out what state we are in
		int k = (t - agent_idx)/G.no_agents;	
		IntegerArray S = new IntegerArray(Arrays.copyOfRange(G.history, 0, t-1));

		// Make a move
		return pi[k][theta_id].get(S);
	}
	
	// Test: play against Trembling Truthful Agent
	public static void main(String[] args) throws IOException {
		
		Cache.init();
		
		// Game parameters
		int no_rounds = 2;
		int no_agents = 2;
		double p0 = 0.5;
		double rho = 0.99;
		double epsilon = 0.01;
		Random rng = new Random();

		// Simulation parameters
		int no_simulations = 100000;
		boolean take_log = false, record_utility = true;

		// Get MDP from Trembling Truthful Agents
		MDPFactory f = new MDPFactory();

		TradingAgent[] agents = new TradingAgent[no_agents];
		for(int i = 0; i < no_agents; i++)
			agents[i] = new TremblingTruthfulAgent(i, epsilon, rng);
		SimpleSignal signal = new SimpleSignal(p0, rho, no_agents, rng);		
		TradingGame G = new TradingGame(agents, no_rounds, signal);
		
		MDP mdp = f.simulateMDP(G, no_simulations, take_log, record_utility);
		
		// Set up MDP agents
		MDPAgent[] mdp_agents = new MDPAgent[no_agents];
		for (int k = 0; k < no_agents; k++){
			mdp_agents[k] = new MDPAgent(k);
			mdp_agents[k].reset(G);
			mdp_agents[k].inputMDP(mdp);
			mdp_agents[k].SolveBellman(true);		// "true" = quite mode
			
			// Print Bellman solutions
			System.out.println("\nagent " + k + ":");
			mdp_agents[k].printpi();
			
		}
		
			
	}


}
