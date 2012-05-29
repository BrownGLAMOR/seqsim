package speed;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

// Play agent[0] against others and measure its utility 
public class EpsilonFactor2 {

	double[] utility;
	double[] utility_diff;
	double[] means, stdevs;
	
	public EpsilonFactor2() throws IOException {				
	}

	// Computes symmetrized distance between two updates. TODO: not tested yet 
	// 		input requirement: PPs[] input order:  [oldP, newP, oldQ, newQ]
	// 		output: double[2], with temp[0] mean & temp[1] stdev 
	public double[] SymmetricDistance(SeqAuction auction0, SeqAuction auction1, SeqAgent[] agents0, SeqAgent[] agents1, JointCondDistributionEmpirical[] PPs, int no_samples) throws IOException {
		Cache.clearMDPpolicy();	// can't use any Cached policies. TODO: make several different Caches! 
		
		if (PPs.length != 4)
			throw new RuntimeException("must input four PPs: [oldP, newP, oldQ, newQ]");
		
		double[] means = new double[2], stdevs = new double[2];
		EpsilonFactor2 ef = new EpsilonFactor2();
		
		// estimate E[u(newP,oldP) - u(oldQ, oldP)]
		agents1[0].setCondJointDistribution(PPs[2]);
		agents0[0].setCondJointDistribution(PPs[3]);
		for (int k = 1; k < agents0.length; k++){
			agents0[k].setCondJointDistribution(PPs[1]);
			agents1[k].setCondJointDistribution(PPs[1]);
		}		
		ef.StrategyDistance2(auction0, auction1, no_samples);
		means[0] = Statistics.mean(ef.utility_diff);
		stdevs[0] = Statistics.stdev(ef.utility_diff);

		// estimate E[u(newQ,oldQ) - u(oldP, oldQ)]
		agents1[0].setCondJointDistribution(PPs[4]);
		agents0[0].setCondJointDistribution(PPs[1]);
		for (int k = 1; k < agents0.length; k++){
			agents0[k].setCondJointDistribution(PPs[3]);
			agents1[k].setCondJointDistribution(PPs[3]);
		}		
		ef.StrategyDistance2(auction0, auction1, no_samples);
		means[1] = Statistics.mean(ef.utility_diff);
		stdevs[1] = Statistics.stdev(ef.utility_diff);
		
		double[] temp = new double[2];
		temp[0] = means[0]+means[1];
		temp[1] = Math.sqrt(Math.pow(stdevs[0], 2) + Math.pow(stdevs[1], 2));
		
		return temp;
	}
	
	// approximates expected utility difference through sampling with correlated valuation: 
	// 			in both auctions, agents have the same valuation
	//				only auction1.agent[0] has PP[it+1], the rest all have PP[it]
	public void StrategyDistance2(SeqAuction auction0, SeqAuction auction1, int no_iters) throws IOException{
		
		double[] utility_diff = new double[no_iters];
		this.utility_diff = utility_diff;
		
		// Play auctions
		for (int j = 0; j<no_iters; j++) {
	
			// Cause each agent to take on a new valuation by calling reset() on their valuation function
			for (int k = 0; k < auction0.agents.length; k++)
				auction0.agents[k].v.reset();
		
			// Play the auction. This will call the agent's reset(), which will cause MDP to be recomputed.
			// so long as the agent's reset() function calls its computeMDP().
			auction0.play(true, null);		// true=="quiet mode", null=="don't write to disk"
			auction1.play(true, null);		// true=="quiet mode", null=="don't write to disk"
			
			// record utility of agent 0
			utility_diff[j] = auction1.profit[0] - auction0.profit[0];
		}
	}

	// Calculates refined epsilon factor: generate new PP with N samples, and then evaluate using S data points
	// 		"different_agent" is a reference to auction1.agents[0]
	public double[] RefinedStrategyDistance2(SeqAuction auction0, SeqAuction auction1, double precision, double max_price, SeqAgent different_agent, int N, int S) throws IOException{
		
		
		// TODO: modify parameters 
		double eta = 0.2;
		double eta2 = 0.0;
		
		// generate new PP
		JointCondFactory jcf = new JointCondFactory(auction0.no_goods, precision, max_price);
		JointCondDistributionEmpirical newP = jcf.offPolicyEtaSymmetric(auction0, eta, eta2, N, false);
//		JointCondDistributionEmpirical newP = jcf.simulAllAgentsOnePP(auction0, N/auction0.no_agents, false, false, false);
//		JointCondDistributionEmpirical newP = jcf.offPolicySymmetric(auction0, N, false);
		different_agent.setCondJointDistribution(newP);
		
		EpsilonFactor2 ef = new EpsilonFactor2(); 
		ef.StrategyDistance2(auction0, auction1, S);
		
		// put together to return
		double[] toreturn = new double[] {Statistics.mean(ef.utility_diff), Statistics.stdev(ef.utility_diff)};
		
		return toreturn;
	}
	
	// compare utility of n our agents against (no_agents-n) original agents, evaluated with S simulations.
	// Means and Stdevs of utilities stored in public variables "means" and "stdevs"
	public void ESS(SeqAuction auction, int n, int S) throws IOException{
		
		double[][] utility_log = new double[auction.no_agents][S];
		
		// Play auctions
		for (int j = 0; j<S; j++) {
	
			// Cause each agent to take on a new valuation by calling reset() on their valuation function
			for (int k = 0; k < auction.agents.length; k++){
				auction.agents[k].v.reset();
				auction.agents[k].reset(auction);
			}
		
			// Play the auction. This will call the agent's reset(), which will cause MDP to be recomputed.
			// so long as the agent's reset() function calls its computeMDP().
			auction.play(true, null);		// true=="quiet mode", null=="don't write to disk"

			// record utility
			for (int k = 0; k < auction.agents.length; k++)
				utility_log[k][j] = auction.profit[k];
			
			// calculate and output
			double[] means = new double[auction.no_agents], stdevs = new double[auction.no_agents];
			this.means = means;
			this.stdevs = stdevs;
			
				// our agent
			for (int i = 0; i < n; i++){
				means[i] = Statistics.mean(utility_log[i]);
				stdevs[i] = Statistics.stdev(utility_log[i]);
			}
//			means[0] /= n;
//			stdevs[0] /= n;
			
				// original agent
			for (int i = n; i < auction.no_agents; i++){
				means[i] += Statistics.mean(utility_log[i]);
				stdevs[i] += Statistics.stdev(utility_log[i]);
			}
//			means[1] /= (auction.no_agents - n);
//			stdevs[1] /= (auction.no_agents - n);			
			
		}
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
	// auction: all agents endowed with P[T-1]; use N/no_agents points to generate P[T], and N to evaluate utility
	// 		OUTPUTS:
	// a double[2] vector; [0] == difference in means, [1] == standard deviation
	public double[] RefinedStrategyDistance(SeqAuction auction, JointCondDistributionEmpirical oldP, int N) throws IOException{
		
		double[] means = new double[2], stdevs = new double[2];
		
		JointCondFactory jcf = new JointCondFactory(auction.no_goods, oldP.precision, oldP.max_price);
		
		// evaluate u(\sigma^t,\sigma^t), also generate P[T] 
		Cache.clearMDPpolicy();
		JointCondDistributionEmpirical newP = jcf.simulAllAgentsOnePP(auction, N/auction.no_agents, false, false, true);
		means[0] = Statistics.mean(jcf.utility);
		stdevs[0] = Statistics.stdev(jcf.utility);
		
		// evaluate u(\sigma^{t+1}, \sigma^t)
		auction.agents[0].setCondJointDistribution(newP);
		EpsilonFactor2 ef = new EpsilonFactor2(); 
		ef.StrategyDistance(auction, N);
		means[1] = Statistics.mean(ef.utility);
		stdevs[1] = Statistics.stdev(ef.utility);
		
		// put together to return
		double[] toreturn = new double[2];
		toreturn[0] = means[1]-means[0];
		toreturn[1] = ((Math.sqrt(stdevs[0]*stdevs[0] + stdevs[1]*stdevs[1]))/Math.sqrt(N));
		
		return toreturn;
	}

	
	// Testing: show shrinkage of epsilon factor after a few iterations in 2 round SP Menezes
	public static void main(String args[]) throws IOException {
		
		Cache.init();
		Random rng = new Random();
		
		// MAIN: to tune XXX
		// Vary N, and fix/vary S
//		int[] Ns = new int[] {100, 500, 1000, 5000, 10000, 50000, 100000, 500000, 1000000, 5000000, 10000000};
		int[] Ns = new int[] {100, 500, 1000, 5000, 10000, 50000, 100000, 500000, 1000000};
		String Stype = "vary";
		int S_fixed = 100000;
		boolean discretize_value = true;
		double v_precision = 0.00001;

		
		// Auction parameters
		boolean decreasing = true;						// decreasing MV in Menezes valuation
		double max_value = 1.0;
		double precision = 0.02;
		double max_price = max_value;
		int no_goods = 2;
		int no_agents = 2;
		int nth_price = 2;

		
			EpsilonFactor2 ef = new EpsilonFactor2();				
			
			// Different agents, shared values
			int fix_preference = 0;
			SeqAgent[] agents0 = new SeqAgent[no_agents];
			SeqAgent[] agents1 = new SeqAgent[no_agents];			
			MenezesMultiroundValue[] values = new MenezesMultiroundValue[no_agents];	// 
			for (int k = 0; k < no_agents; k++)
				values[k] = new MenezesMultiroundValue(max_value, rng, decreasing);
			
			agents0[0] = new MenezesAgent(values[0], no_agents, 0, 0, decreasing);
			agents1[0] = new MDPAgentSP(values[0], 0, fix_preference, discretize_value, v_precision);		// this guy cannot discretize values
			for (int k = 1; k < no_agents; k++){
				agents0[k] = new MenezesAgent(values[k], no_agents, k, 0, decreasing);
				agents1[k] = new MenezesAgent(values[k], no_agents, k, 0, decreasing);
			}
			
			SeqAuction auction0 = new SeqAuction(agents0, nth_price, no_goods);
			SeqAuction auction1 = new SeqAuction(agents1, nth_price, no_goods);
			
			// Name output file
			FileWriter fw_epsilon = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/paper/june1st/epsilon/epsilonest_" + Stype + "_" + no_agents + "_" + precision + "_" + "pts.csv");
			
//			// Utility comparison storage
//			double[] means = new double[Ns.length];
//			double[] stdevs = new double[Ns.length];

			// epsilon(N,S)
			for (int j = 0; j < Ns.length; j++){
				
				int N = Ns[j];
				int S;
				if (Stype ==  "fixed")
					S = S_fixed;
				else
					S = 10*N;

				Cache.clearMDPpolicy();
				double[] temp = ef.RefinedStrategyDistance2(auction0, auction1, precision, max_price, agents1[0], N, S);
												
				fw_epsilon.write(S + "," + N + "," + temp[0] + "," + temp[1] + "\n"); 
				System.out.print(S + "," + N + "," + temp[0] + "," + temp[1]/Math.sqrt(S) + "\n"); 
				
			}
			fw_epsilon.close();
		System.out.println("done done");
		
		}


}
		

