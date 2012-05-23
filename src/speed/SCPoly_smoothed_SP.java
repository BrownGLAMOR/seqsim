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
		int no_goods = 3;
		int no_agents = 2;
		int nth_price = 2;
		
		// simulation parameters
		int no_initial_simulations = 10000000/no_agents;	// generating initial PP		
		int T = 10;								// no. of Wellman updates 
//		int no_per_iteration = 10000;		// no. of games played in each Wellman iteration
		int[] NO_PER_ITERATION = new int[]{100000};
		
		// Cooling scheme
		double[] ORDER = new double[] {0.5,1.0,2.0};
		double gamma_0 = 0.9, gamma_end = 1.0;						// target \gamma values
		double alpha = (gamma_end-gamma_0)/Math.log(T), beta = Math.pow(T, gamma_0/(gamma_end-gamma_0));	// corresponding parameters
		double[][] GAMMA = new double[T][no_goods];
		for (int t = 0; t < T; t++){
			for (int i = 0; i < no_goods; i++)
				GAMMA[t][i] = alpha*Math.log(beta*(t+1));
		}
		// agent preferences
		String type = "g";								// type of updating procedure XXX
		int preference;
		if (type == "s")
			preference = 3;
		else
			preference = 0;
		
		boolean discretize_value = true;
		double v_precision = 0.00001;

		// evaluation parameters
		double cmp_precision = 0.01;						// discretization step for valuation examining
		int no_for_cmp = (int) (1/cmp_precision) + 1;
		int no_for_EUdiff = 10000;							// no. of points for EU comparison XXX: restricted
		double[][] means, stdevs;
		
		boolean take_log = false;						// record prices for agents
		boolean record_prices = false;					// record prices for seller
		boolean print_strategy = true;					// Output strategy S(t)
		boolean compute_epsilon = true;					// Compute epsilon factors and output
		boolean record_utility = false;
		
		for (int npi = 0; npi < NO_PER_ITERATION.length; npi++) {
			
			int no_per_iteration = NO_PER_ITERATION[npi];
			
		for (int o = 0; o < ORDER.length; o++) {
			
			double order = ORDER[o];

			// record all prices (to compute distances later)
			JointCondDistributionEmpirical[] PP = new JointCondDistributionEmpirical[T+1];		
	
			// 1)  Initiate PP from Polynomial agents
			System.out.println("Generating initial PP");
			JointCondFactory jcf = new JointCondFactory(no_goods, precision, max_price);
			
			// 1.1)	Create PP[0]
//			PolynomialAgent[] poly_agents = new PolynomialAgent[no_agents];
//			MenezesMultiroundValue[] poly_values = new MenezesMultiroundValue[no_agents];
//			for (int i = 0; i<no_agents; i++){
//				poly_values[i] = new MenezesMultiroundValue(max_value, rng, decreasing);
//				poly_agents[i] = new PolynomialAgent(poly_values[i], i, order);
//			}

			PolynomialAgentMultiRound[] poly_agents = new PolynomialAgentMultiRound[no_agents];
			MenezesMultiroundValue[] poly_values = new MenezesMultiroundValue[no_agents];
			for (int i = 0; i<no_agents; i++){
				poly_values[i] = new MenezesMultiroundValue(max_value, rng, decreasing);
				poly_agents[i] = new PolynomialAgentMultiRound(poly_values[i], i, no_goods, order);
			}

			SeqAuction poly_auction = new SeqAuction(poly_agents, nth_price, no_goods);
			
			PP[0] = jcf.simulAllAgentsOnePP(poly_auction, no_initial_simulations,take_log,record_prices,false);
					
				// Utility comparison storage
				means = new double[2][T];
				stdevs = new double[2][T];
			
				// initiate agents for later bid comparison
				MenezesMultiroundValue value = new MenezesMultiroundValue(max_value, rng, decreasing);
				MDPAgentSP mdp_agent = new MDPAgentSP(value, 1, preference, discretize_value, v_precision);
				mdp_agent.inputGamma(GAMMA[0]);
				
				// initiate updating agents
				MDPAgentSP[] mdp_agents = new MDPAgentSP[no_agents];
				for (int i = 0; i < no_agents; i++)
					mdp_agents[i] = new MDPAgentSP(new MenezesMultiroundValue(max_value, rng, decreasing), i, preference, discretize_value, v_precision);
				
				SeqAuction updating_auction = new SeqAuction(mdp_agents, nth_price, no_goods);
				
				// initiate tools to compare strategies
				double[] v = new double[no_for_cmp]; 		
				for (int i = 0; i < no_for_cmp; i++)
					v[i] = i*cmp_precision;
				double[][] strategy = new double[T + 1][no_for_cmp];
			
			// 1.2)	Record initial strategy
			for (int i = 0; i < no_for_cmp; i++) {
				poly_values[0].x = v[i];
//				strategy[0][i] = poly_agents[0].getBid(0);
				strategy[0][i] = poly_agents[0].getBid(0,0);
			}	
			
			// 2) Wellman updates
			for (int it = 0; it < T; it++) {
				
				System.out.println("Wellman iteration = " + it);
	
				// Set new gamma and PPs
				for (int i = 0; i < no_agents; i++){
					mdp_agents[i].setCondJointDistribution(PP[it]);
//					mdp_agents[i].inputGamma(GAMMA[it]);
				}
				
				Cache.clearMDPpolicy();
				
				// 2.1) generate new PP
				if (type == "u")
					PP[it+1] = jcf.offPolicySymmetricReal(updating_auction, no_per_iteration);
				else
					PP[it+1] = jcf.simulAllAgentsOnePP(updating_auction, no_per_iteration/no_agents, take_log, record_prices, record_utility);
//					PP[it+1] = jcf.simulAllAgentsOneRealPP(updating_auction, no_per_iteration/no_agents, take_log, record_prices, record_utility);
	
				// 2.2) output first round bids for comparison 
				if (print_strategy == true) {
						
					// initiate agents to compare bids
					mdp_agent.setCondJointDistribution(PP[it]);
					
					// Assign values, instead of sample values
					for (int i = 0; i < no_for_cmp; i++) {
						value.x = v[i];
						mdp_agent.reset(null);		// recompute MDP
						strategy[it+1][i] = mdp_agent.getFirstRoundPi();
//						System.out.println("value.x = " + value.x + ", first round bid = " + mdp_agent.getFirstRoundPi());
//						strategy[it+1][i] = mdp_agent.getFirstRoundBid();
					}	
				}
			}
	
			// output strategies from each iteration
			if (print_strategy == true){

				// Name output file
				FileWriter fw_strat = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/paper/june1st/SP/" + type + no_goods + "_" + order + "_" + no_agents + "_" + precision + "_" + discretize_value + v_precision + "_" + T + "_" + no_per_iteration + "pts.csv");
				if (preference == 3)
					fw_strat = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/paper/june1st/SP/" + type + no_goods + "_" + order + "_" + no_agents + "_" + precision + "_" + discretize_value + v_precision + "_" + T + "_gamma" + (int) gamma_end + "_" + no_per_iteration + "pts.csv");

				// write first round bidding functions
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
				
				// Name output file
				FileWriter fw_EUdiff = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/paper/june1st/SP/" + type + no_goods + "epsilon_" + order + "_" + no_agents + "_" + precision + "_" + discretize_value + v_precision + "_" + T + "_" + no_for_EUdiff + "pts.csv");
				if (preference == 3)
					fw_EUdiff = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/paper/june1st/SP/" + type + no_goods + "epsilon_" + order + "_" + no_agents + "_" + precision + "_" + discretize_value + v_precision + "_" + T + "_gamma" + (int) gamma_end + "_" + no_for_EUdiff + "pts.csv");			
				// initiate comparison tools
				EpsilonFactor2 ef = new EpsilonFactor2();				
				
				int fix_preference = 0;
				MDPAgentSP[] cmp_agents = new MDPAgentSP[no_agents];
				for (int k = 0; k < no_agents; k++)
					cmp_agents[k] = new MDPAgentSP(new MenezesMultiroundValue(max_value, rng, decreasing), k, fix_preference, discretize_value, v_precision);
				
				SeqAuction cmp_auction = new SeqAuction(cmp_agents, nth_price, no_goods);
				
				
				// compute distance with future BR PPs, not past ones
				for (int it = 0; it < PP.length - 1; it++){

//					// \sigma^{t} against \sigma^t
//					Cache.clearMDPpolicy();
//					for (int k = 0; k < no_agents; k++)
//						cmp_agents[k].setCondJointDistribution(PP[it]);					
//					ef.StrategyDistance(cmp_auction, no_for_EUdiff);
//					means[0][it] = Statistics.mean(ef.utility);
//					stdevs[0][it] = Statistics.stdev(ef.utility);
//					
//					// \sigma^{t+1} against \sigma^t
//					Cache.clearMDPpolicy();
//					cmp_agents[0].setCondJointDistribution(PP[it+1]);
//					ef.StrategyDistance(cmp_auction, no_for_EUdiff);
//					means[1][it] = Statistics.mean(ef.utility);
//					stdevs[1][it] = Statistics.stdev(ef.utility);
					
//					fw_EUdiff.write((means[1][it]-means[0][it]) + "," + ((Math.sqrt(stdevs[0][it]*stdevs[0][it] + stdevs[1][it]*stdevs[1][it]))/Math.sqrt(no_for_EUdiff)) + "\n");	// Assume additive stdev... 
//					System.out.print((means[1][it]-means[0][it]) + "," + ((Math.sqrt(stdevs[0][it]*stdevs[0][it] + stdevs[1][it]*stdevs[1][it]))/Math.sqrt(no_for_EUdiff)) + "\n"); 

					double[] temp = ef.RefinedStrategyDistance(cmp_auction, PP[it], no_for_EUdiff);
					
					fw_EUdiff.write((temp[0]) + "," + temp[1] + "\n");	// Assume additive stdev... 
					System.out.print((temp[0]) + "," + temp[1] + "\n"); 
	
				}
				fw_EUdiff.close();			
			System.out.println("done done");
			
			}
		}
	}
	}
}

