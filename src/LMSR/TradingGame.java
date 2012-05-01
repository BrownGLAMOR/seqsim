package LMSR;

import java.io.IOException;

public class TradingGame {
	int no_agents;				// number of agents playing
	int no_rounds;				// number of rounds (total rounds = no_rounds*no_agents)

	TradingAgent[] agents;
	SimpleSignal signal;
	boolean quiet;

	// public information
	double[] price;				// [round] ==> evolution of security price
	int[] history;				// [round] ==> {-1,0,1} <==> {down,not report,up}
	int us;						// number of u's in history
	int ms;						// number of m's in history
	int ds;						// number of d's in history
	int[] mover;				// [round] ==> agent_id of who'se moving at each round

	// private information, released at the end
	boolean world;				// true, binary state of world: {true, false} 
	double[] reward;			// [round] ==> reward to agent at each round, after disclosing world state
	double[] profit;			// [agent_id] ==> total reward to agent
	
	// construct the game with a set of agents.
	public TradingGame(TradingAgent[] agents, int no_rounds, SimpleSignal signal) {
		this.agents = agents;
		this.no_agents = agents.length;
		this.no_rounds = no_rounds;
				
		// do all of our memory allocations upfront.
		mover = new int[no_agents*no_rounds];		
		price = new double[no_agents*no_rounds+1];		// prior is price[0]
		reward = new double[no_agents*no_rounds];
		profit = new double[no_agents];

		// assign movers of each round
		for (int r = 0; r < no_rounds; r++){
			for (int k = 0; k < agents.length; k++)
				mover[r*agents.length+k] = agents[k].agent_idx;
		}
	}
	
	// play the game. it is safe to re-play the auction as many times as one wishes.
	// the results are available in the public memory variables: price, reward, etc.
	// if quiet == true, then no diagnostic output is sent to the screen
	public void play(boolean quiet) throws IOException {
		
		// Reset memory
		us = 0;
		ms = 0;
		ds = 0;
		for (int i = 0; i < history.length; i++)
			history[i] = 0;
		for (int i = 0; i < price.length; i++)
			price[i] = 0.0;
		for (int i = 0; i < reward.length; i++)
			reward[i] = 0.0;
		for (int i = 0; i < profit.length; i++)
			profit[i] = 0.0;
		
		// Regenerate the signal
		signal.reset();
		
		// tell all the agents that we are playing a new game.
		for (TradingAgent a : agents)
			a.reset(this);
		
		// Set the prior
		price[0] = Signal.priorPrice;
		
		// Ask players to submit actions, record it in history
		for (int i = 0; i < mover.length; i++){
			history[i] = agents[mover[i]].move(i);
			if (history[i] == 1)
				us ++;
			else if (history[i] == -1)
				ds ++;
			else
				ms ++;
				
			price[i+1] = computePrice(us,ds);
		}
		
		// Figure out rewards: LMSR
		if (world == true){
			for (int i = 0; i < price.length-1; i++){
				reward[i] = java.lang.Math.log(price[i+1]/price[i]);
				profit[mover[i]] += reward[i];
			}
		}
		else{
			for (int i = 0; i < price.length-1; i++){
				reward[i] = java.lang.Math.log(1-price[i+1]/1-price[i]);
				profit[mover[i]] += reward[i];
			}
		}
		
		// Print game information if not quiet
		if (!quiet){
			// Print world state and signals
			System.out.println("World state = " + signal.world);
			System.out.print("Player signals: ");
			for (int i = 0; i < agents.length; i++){
				System.out.print(signal.s[i] + " ");
			}

			// Print "mover"
			System.out.print("agent moving sequence: ");
			for (int i = 0; i < mover.length; i++)
				System.out.print(mover[i] + " ");
			
			// Print "history"
			System.out.print("\nSubmitted actions: ");
			for (int i = 0; i < mover.length; i++){
				if (history[i] == 1)
					System.out.print("U ");
				else if (history[i] == -1)
					System.out.print("D ");
				else
					System.out.print("M ");
			}
			
			// Print "prices"
			System.out.print("\nPrices = ");
			for (int i = 0; i < price.length; i++)
				System.out.print(price[i] + " ");
			
			// Print partial and total rewards
			System.out.print("\nRewards = ");
			for (int i = 0; i < reward.length; i ++)
				System.out.print(reward[i] + " ");
			System.out.print("\nTotal rewards to agents: ");
			for (int i = 0; i < profit.length; i++)
				System.out.print(profit[i] + " ");
		}
		
	}
	
	// Automated posterior price calculation. Things made simple because of the binomial structure
	double computePrice(int hs, int ds){
		int diff = hs - ds;
		double PH, PL;
		if (diff >= 0){
			// P(H)P(sig|H) \propto p0*rho^diff
			// P(L)P(sig|H) \propto (1-p0)*(1-rho)^diff
			PH = signal.p0*java.lang.Math.pow(signal.rho, diff);
			PL = (1-signal.p0)*java.lang.Math.pow(1-signal.rho, diff);
		}else{
			PH = signal.p0*java.lang.Math.pow(1-signal.rho, -diff);
			PL = (1-signal.p0)*java.lang.Math.pow(signal.rho, -diff);
		}
		return PH/(PH+PL);			
	}

	// Test
	public static void main(String[] args) throws IOException {
		
	}
	
}
