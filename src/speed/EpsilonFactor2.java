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

	// Refined calculation of strategy distance. 
	// 		INPUTS:
	// auction: all agents endowed with P[T-1]; use no_pts/no_agents points to generate P[T], and no_pts to evaluate utility
	// 		OUTPUTS:
	// a double[2] vector; [0] == difference in means, [1] == standard deviation
	public double[] RefinedStrategyDistance(SeqAuction auction, JointCondDistributionEmpirical oldP, int no_pts) throws IOException{
		
		double[] means = new double[2], stdevs = new double[2];
		
		JointCondFactory jcf = new JointCondFactory(auction.no_goods, oldP.precision, oldP.max_price);
		
		// evaluate u(\sigma^t,\sigma^t), also generate P[T] 
		JointCondDistributionEmpirical newP = jcf.simulAllAgentsOneRealPP(auction, no_pts/auction.no_agents, false, false, true);
		auction.agents[0].setCondJointDistribution(newP);
		means[0] = Statistics.mean(jcf.utility);
		stdevs[0] = Statistics.stdev(jcf.utility);
		
		// evaluate u(\sigma^{t+1}, \sigma^t)
		EpsilonFactor2 ef = new EpsilonFactor2();	// XXX: can call itself here? I believe so. 
		ef.StrategyDistance(auction, no_pts);
		means[1] = Statistics.mean(ef.utility);
		stdevs[1] = Statistics.stdev(ef.utility);
		
		// put together to return
		double[] toreturn = new double[2];
		toreturn[0] = means[1]-means[0];
		toreturn[1] = ((Math.sqrt(stdevs[0]*stdevs[0] + stdevs[1]*stdevs[1]))/Math.sqrt(no_pts));
		
		return toreturn;
	}

	
	// Testing
	public static void main(String args[]) throws IOException {
		
	}
}