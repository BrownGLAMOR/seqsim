package speed;

import java.io.IOException;

// Play agent[0] against others and measure its utility 
public class EpsilonFactor2 {

	int no_goods;
	double[] utility;
	
	public EpsilonFactor2(int no_goods) throws IOException {				
		this.no_goods = no_goods;
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
		
//		Random rng = new Random();
//		
//		int no_goods = 2;
//		int no_agents = 5, no_Q_simulations = 1000000/no_agents;
//		double precision = 0.05, max_price = 1.0;
//		
//		EpsilonFactor ef = new EpsilonFactor(no_goods);
//		
//		// Create 2 distributions
//		JointCondDistributionEmpirical P, Q;
//		JointCondFactory jcf = new JointCondFactory(no_goods, precision, max_price);
//
//		// P ~ uniform
//		P = jcf.makeUniform(false);
//		
//		// Q ~ 3 agent Katzman
//		KatzmanUniformAgent[] katz_agents = new KatzmanUniformAgent[no_agents];
//		for (int i = 0; i<no_agents; i++)
//			katz_agents[i] = new KatzmanUniformAgent(new KatzHLValue(no_agents-1, max_price, rng), no_agents, i);
//		
//		SeqAuction katz_auction = new SeqAuction(katz_agents, 2, no_goods);
//		Q = jcf.simulAllAgentsOnePP(katz_auction, no_Q_simulations, false, false,true);
//
//		double[] u = jcf.utility;
//		System.out.println("EU(Q|Q) = " + Statistics.mean(u) + ", stdev U(Q|Q) = " + Statistics.stdev(u));
//		System.out.println("********");
//		
//		int no_iterations = 10000;
//		
//		// compute distances & output
//		ef.jcdeDistance(rng, P, Q, new KatzHLValue(no_agents-1, max_price, rng), no_iterations);		
//		System.out.println("EU(P|P) = " + ef.EU_P + ", stdev U(P|P) = " + ef.stdev_P);
//		System.out.println("EU(Q|P) = " + ef.EU_Q + ", stdev U(Q|P) = " + ef.stdev_Q);
//		System.out.println("EU(P-Q|P) = " + ef.EU_diff + ", stdev U(P-Q|P) = " + ef.stdev_diff);
//
//		// Write v, uP, uQ, u_diff
//		FileWriter fw = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/EpsilonFactor/test1.csv");
////		FileWriter fw = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/EpsilonFactor/test1_" + no_agents + "_" + precision + "_" + no_iterations + ".csv");
//		for (int i = 0; i < no_iterations; i++)
//			fw.write(ef.vs[i] + "," + ef.uP[i] + "," + ef.uQ[i] + "," + ef.udiff[i] + "\n");
//		fw.close();
//
//
//		ef.jcdeDistance(rng, Q, P, new KatzHLValue(no_agents-1, max_price, rng), no_iterations);		
//		System.out.println("EU(Q|Q) = " + ef.EU_P + ", stdev U(Q|Q) = " + ef.stdev_P);
//		System.out.println("EU(P|Q) = " + ef.EU_Q + ", stdev U(P|Q) = " + ef.stdev_Q);
//		System.out.println("EU(Q-P|Q) = " + ef.EU_diff + ", stdev U(Q-P|Q) = " + ef.stdev_diff);
//
//		// Write v, uP, uQ, u_diff
//		FileWriter fw2 = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/EpsilonFactor/test2.csv");
////		FileWriter fw2 = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/EpsilonFactor/test2_" + no_agents + "_" + precision + "_" + no_iterations + ".csv");
//		for (int i = 0; i < no_iterations; i++)
//			fw2.write(ef.vs[i] + "," + ef.uP[i] + "," + ef.uQ[i] + "," + ef.udiff[i] + "\n");
//		fw2.close();
	}
}