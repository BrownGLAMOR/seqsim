package speed;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

// Do self-confirming smoothed updates initiated from Polynomial strategies, using Menezes decreasing valuation. 
// First try 2 rounds and then 3 rounds. 

public class SCPoly_smoothed_SP {
	public static void main(String[] args) throws IOException {
		Cache.init();
		Random rng = new Random();
		
		// Auction parameters
		boolean decreasing = true;						// decreasing MV in Menezes valuation
		double max_value = 1.0;
		double precision = 0.05;
		double max_price = max_value;
		int no_goods = 3;								// XXX: 3 rounds
		int no_agents = 2;
		int nth_price = 2;
		
		// simulation parameters
		int no_initial_simulations = 1000000/no_agents;	// generating initial PP		
		int T = 10;								// no. of Wellman updates 
		int no_per_iteration = 100000/no_agents;			// no. of games played in each Wellman iteration
		
		// Cooling scheme
		double[] ORDER = new double[] {1.0};
		double gamma_0 = 0.9, gamma_end = 1.0;						// target \gamma values
		double alpha = (gamma_end-gamma_0)/Math.log(T), beta = Math.pow(T, gamma_0/(gamma_end-gamma_0));	// corresponding parameters
		double[] GAMMA = new double[T];
		for (int t = 0; t < T; t++){
			GAMMA[t] = alpha*Math.log(beta*(t+1));
		}
		// agent preferences
		int preference = 3;								// let's try Boltzman smoothing
		String type = "s";									// in file name XXX
		double epsilon = 0.0001;
		boolean discretize_value = false;
		double v_precision = 0.001;

		// evaluation parameters
		double cmp_precision = 0.01;						// discretization step for valuation examining
		int no_for_cmp = (int) (1/cmp_precision) + 1;
		int no_for_EUdiff = no_per_iteration*no_agents;							// no. of points for EU comparison
		double[][] means, stdevs;
		
		boolean take_log = false;						// record prices for agents
		boolean record_prices = false;					// record prices for seller
		boolean print_strategy = true;					// Output strategy S(t)
		boolean compute_epsilon = true;					// Compute epsilon factors and output
		boolean record_utility = false;
		
		for (int o = 0; o < ORDER.length; o++) {
			
			double order = ORDER[o];

			// record all prices (to compute distances later)
			JointCondDistributionEmpirical[] PP = new JointCondDistributionEmpirical[T+1];		
	
			// 1)  Initiate PP from Polynomial agents
			System.out.println("Generating initial PP");
			JointCondFactory jcf = new JointCondFactory(no_goods, precision, max_price);
			
			// 1.1)	Create PP[0]
			PolynomialAgent[] poly_agents = new PolynomialAgent[no_agents];
			MenezesMultiroundValue[] poly_values = new MenezesMultiroundValue[no_agents];
			for (int i = 0; i<no_agents; i++){
				poly_values[i] = new MenezesMultiroundValue(max_value, rng, decreasing);
				poly_agents[i] = new PolynomialAgent(poly_values[i], no_agents, order);
			}
			SeqAuction poly_auction = new SeqAuction(poly_agents, nth_price, no_goods);
			
			PP[0] = jcf.simulAllAgentsOnePP(poly_auction, no_initial_simulations,take_log,record_prices,false);
					
				// Utility comparison storage
				means = new double[2][T];
				stdevs = new double[2][T];
			
				// initiate agents for later bid comparison
				MenezesMultiroundValue value = new MenezesMultiroundValue(max_value, rng, decreasing);
				MDPAgentSP mdp_agent = new MDPAgentSP(value, 1, preference);
				mdp_agent.inputGamma(new double[] {GAMMA[0], 0});
				
				// initiate updating agents
				MDPAgentSP[] mdp_agents = new MDPAgentSP[no_agents];
				for (int i = 0; i < no_agents; i++)
					mdp_agents[i] = new MDPAgentSP(new MenezesMultiroundValue(max_value, rng, decreasing), i, preference);
				
				SeqAuction updating_auction = new SeqAuction(mdp_agents, nth_price, no_goods);
				
				// initiate tools to compare strategies
				double[] v = new double[no_for_cmp]; 		
				for (int i = 0; i < no_for_cmp; i++)
					v[i] = i*cmp_precision;
				double[][] strategy = new double[T + 1][no_for_cmp];
			
			// 1.2)	Record initial strategy
			for (int i = 0; i < no_for_cmp; i++) {
				poly_values[0].x = v[i];
				strategy[0][i] = poly_agents[0].getBid(0);
			}	
			
			// 2) Wellman updates
			for (int it = 0; it < T; it++) {
				
				System.out.println("Wellman iteration = " + it);
	
				// Set new gamma and PPs
				for (int i = 0; i < no_agents; i++){
					mdp_agents[i].setCondJointDistribution(PP[it]);
					mdp_agents[i].inputGamma(new double[] {GAMMA[it], 0});
				}
				
				// 2.1) generate new PP	XXX: changed here!
				PP[it+1] = jcf.offPolicySymmetricReal(updating_auction, no_per_iteration);
//				PP[it+1] = jcf.simulAllAgentsOneRealPP(updating_auction, no_per_iteration, take_log, record_prices, record_utility);
	
				// 2.2) output first round bids for comparison 
				if (print_strategy == true) {
						
					// initiate agents to compare bids
					mdp_agent.setCondJointDistribution(PP[it]);
					
					// Assign values, instead of sample values
					for (int i = 0; i < no_for_cmp; i++) {
						value.x = v[i];
						mdp_agent.reset(null);		// recompute MDP
						strategy[it+1][i] = mdp_agent.getFirstRoundPi();
//						strategy[it+1][i] = mdp_agent.getFirstRoundBid();
					}	
				}
			}
	
			// output strategies from each iteration
			if (print_strategy == true){		// XXX: change name
//				FileWriter fw_strat = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/paper/june1st/SP/s_" + order + "_" + no_agents + "_" + precision + "_" + T + "_gamma" + gamma_end + ".csv");
				FileWriter fw_strat = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/paper/june1st/SP/u_" + order + "_" + no_agents + "_" + precision + "_" + T + ".csv");
				for (int i = 0; i < strategy.length; i++){
					for (int j = 0; j < strategy[i].length - 1; j++){
						fw_strat.write(strategy[i][j] + ",");
					}
					fw_strat.write(strategy[i][strategy[i].length-1] + "\n");
				}
				fw_strat.close();
			}
			
			// Compute epsilons and output
			if (compute_epsilon == true) {
				System.out.println("computing price distances...");
				FileWriter fw_EUdiff = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/paper/june1st/SP/" + type + "epsilon_" + order + "_" + no_agents + "_" + precision + "_" + T + ".csv");
			
				// initiate comparison tools
				EpsilonFactor2 ef = new EpsilonFactor2();				
				MDPAgentSP[] cmp_agents = new MDPAgentSP[no_agents];
				for (int k = 0; k < no_agents; k++)
					cmp_agents[k] = new MDPAgentSP(new MenezesMultiroundValue(max_value, rng, decreasing), k, preference);
				
				SeqAuction cmp_auction = new SeqAuction(cmp_agents, nth_price, no_goods);
				
				// compute distance with future BR PPs, not past ones
				for (int it = 0; it < PP.length - 1; it++){

					// \sigma^{t} against \sigma^t
					
					// \sigma^{t+1} against \sigma^t
					cmp_agents[0].setCondJointDistribution(PP[it+1]);
					for (int k = 1; k < no_agents; k++)
						cmp_agents[k].setCondJointDistribution(PP[it]);
					
					ef.StrategyDistance(cmp_auction, no_for_EUdiff);
					means[1][it] = Statistics.mean(ef.utility);
					stdevs[1][it] = Statistics.stdev(ef.utility);
					
					fw_EUdiff.write((means[1][it]-means[0][it]) + "," + ((Math.sqrt(stdevs[0][it]*stdevs[0][it] + stdevs[1][it]*stdevs[1][it]))/Math.sqrt(no_for_EUdiff)) + "\n");	// Assume additive stdev... 
					System.out.print((means[1][it]-means[0][it]) + "," + ((Math.sqrt(stdevs[0][it]*stdevs[0][it] + stdevs[1][it]*stdevs[1][it]))/Math.sqrt(no_for_EUdiff)) + "\n"); 
	
				}
				fw_EUdiff.close();			
			System.out.println("done done");
			
			}
		}
	}
}
