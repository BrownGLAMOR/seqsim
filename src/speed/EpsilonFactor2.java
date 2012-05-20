package speed;

import java.io.IOException;

// Play agent[0] against others and measure its utility 
public class EpsilonFactor2 {

	double[] utility;
	
	public EpsilonFactor2() throws IOException {				
	}

	// Calculate the first agent's utility against the others 
	public void StrategyDistance(SeqAuction auction, int no_iters) throws IOException{
		
		SeqAgent[] agents = auction.agents;
		double[] utility = new double[no_iters];
		this.utility = utility;
		
		// Play auctions
		for (int j = 0; j<no_iters; j++) {
	
			// Cause each agent to take on a new valuation by calling reset() on their valuation function
			for (int k = 0; k<agents.length; k++)
				agents[k].v.reset();
		
			// Play the auction. This will call the agent's reset(), which will cause MDP to be recomputed.
			// so long as the agent's reset() function calls its computeMDP().
			auction.play(true, null);		// true=="quiet mode", null=="don't write to disk"
			
			// record utility of agent 0
			utility[j] = auction.profit[0];
			}
	}
	
	// Testing
	public static void main(String args[]) throws IOException {
		
	}
}