package speed;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

// Do self-confirming smoothed updates initiated from Polynomial strategies, using Menezes decreasing valuation. 
// First try 2 rounds and then 3 rounds. 

public class SCPoly_smoothed_FP {
	public static void main(String[] args) throws IOException {
		Cache.init();
		Random rng = new Random();
		
		// Auction parameters
		boolean decreasing = true;						// decreasing MV in Menezes valuation	TODO: change
		double max_value = 1.0;
		double precision = 0.05;
		double max_price = max_value;
		int no_goods = 2;
		int no_agents = 5;
		int nth_price = 1;
		
		// simulation parameters
		int no_initial_simulations = 100000/no_agents;	// generating initial PP		
		int T = 20;								// no. of Wellman updates 
		int no_per_iteration = 100000/no_agents;			// no. of games played in each Wellman iteration
		
		// Cooling scheme
		double[] ORDER = new double[] {3.0};
		double gamma_0 = 900.0, gamma_end = 1000.0;						// target \gamma values
		double[] gamma = new double[] {gamma_0, gamma_0};						// initial
		double alpha = (gamma_end-gamma_0)/Math.log(T), beta = Math.pow(T, gamma_0/(gamma_end-gamma_0));	// corresponding parameters
		double[][] GAMMA = new double[T][no_goods];
		for (int t = 0; t < T; t++)
			GAMMA[t] = new double[]{alpha*Math.log(beta*(t+1)), alpha*Math.log(beta*(t+1))};

		// agent preferences
		int preference = 0;					// For simple MDP Agents' tie breaking
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
//		if (compute_epsilon == true)
//			record_utility = true;
		
		for (int o = 0; o < ORDER.length; o++) {
			
			double order = ORDER[o];

			// record all prices (to compute distances later)
			JointCondDistributionEmpirical[] PP = new JointCondDistributionEmpirical[T+1];		
	
			// 1)  Initiate PP from Polynomial agents
			System.out.println("Generating initial PP");
			JointCondFactory jcf = new JointCondFactory(no_goods, precision, max_price);
			
			// 1.1)	Create PP[0]
			PolynomialAgent[] poly_agents = new PolynomialAgent[no_agents];
			MenezesValue[] poly_values = new MenezesValue[no_agents];
			for (int i = 0; i<no_agents; i++){
				poly_values[i] = new MenezesValue(max_value, rng, decreasing);
				poly_agents[i] = new PolynomialAgent(poly_values[i], no_agents, order);
			}
			SeqAuction poly_auction = new SeqAuction(poly_agents, nth_price, no_goods);
			
			PP[0] = jcf.simulAllAgentsOnePP(poly_auction, no_initial_simulations,take_log,record_prices,false);
					
				// Utility comparison storage
				means = new double[2][T];
				stdevs = new double[2][T];
			
				// initiate agents for later bid comparison
				MenezesValue value = new MenezesValue(max_value, rng, decreasing);
				MDPAgentFP_smoothed mdp_agent = new MDPAgentFP_smoothed(value, 1, gamma, discretize_value, v_precision);
//				FullCondMDPAgent4FP mdp_agent = new FullCondMDPAgent4FP(value, 1, preference, epsilon, discretize_value, v_precision);
				
				// initiate updating tools
				MDPAgentFP_smoothed[] mdp_agents = new MDPAgentFP_smoothed[no_agents];	//XXX: change
				for (int i = 0; i < no_agents; i++)
					mdp_agents[i] = new MDPAgentFP_smoothed(new MenezesValue(max_value, rng, decreasing), 1, gamma, discretize_value, v_precision);

//				FullCondMDPAgent4FP[] mdp_agents = new FullCondMDPAgent4FP[no_agents];
//				for (int i = 0; i < no_agents; i++)
//					mdp_agents[i] = new FullCondMDPAgent4FP(new MenezesValue(max_value, rng, decreasing), 1, preference, epsilon, discretize_value, v_precision);
				
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
					mdp_agents[i].setGamma(GAMMA[it]);	// XXX: change
					mdp_agents[i].setCondJointDistribution(PP[it]);
				}
				
				// 2.1) generate new PP	XXX: changed here!
//				PP[it+1] = jcf.offPolicySymmetricReal(updating_auction, no_per_iteration);
				PP[it+1] = jcf.simulAllAgentsOneRealPP(updating_auction, no_per_iteration, take_log, record_prices, record_utility);
				if (record_utility == true){
					means[0][it] = Statistics.mean(jcf.utility);
					stdevs[0][it] = Statistics.stdev(jcf.utility);
				}
	
				// 2.2) output first round bids for comparison 
				if (print_strategy == true) {
						
					// initiate agents to compare bids
					mdp_agent.setCondJointDistribution(PP[it]);
					
					// Assign values, instead of sample values
					for (int i = 0; i < no_for_cmp; i++) {
						value.x = v[i];
						if (decreasing == true)
							value.delta_x = v[i] + v[i]*v[i];
						else
							value.delta_x = v[i] + java.lang.Math.sqrt(v[i]);
						mdp_agent.reset(null);		// recompute MDP
//						strategy[it+1][i] = mdp_agent.getFirstRoundPi();
						strategy[it+1][i] = mdp_agent.getFirstRoundBid();
					}	
				}
			}
	
			// output strategies from each iteration
			if (print_strategy == true){
				FileWriter fw_strat = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/paper/june1st/smoothed/poly" + order + "_" + no_agents + "_" + precision + "_" + T + ".csv");
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
				FileWriter fw_EUdiff = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/paper/june1st/smoothed/poly_epsilon" + order + "_" + no_agents + "_" + precision + "_" + T + ".csv");
			
				// initiate comparison tools
				EpsilonFactor2 ef = new EpsilonFactor2();

				// Bid optimal values for comparison
				SeqAgent[] cmp_agents = new SeqAgent[no_agents];
				for (int k = 0; k < no_agents; k++)
					cmp_agents[k] = new FullCondMDPAgent4FP(new MenezesValue(max_value, rng, decreasing), 1, preference, epsilon, discretize_value, v_precision);

//				FullCondMDPAgent4FP[] cmp_agents = new FullCondMDPAgent4FP[no_agents];
//				for (int k = 0; k < no_agents; k++)
//					cmp_agents[k] = new FullCondMDPAgent4FP(new MenezesValue(max_value, rng, decreasing), 1, preference, epsilon, discretize_value, v_precision);
				
				SeqAuction cmp_auction = new SeqAuction(cmp_agents, nth_price, no_goods);
				
				// compute distance with future BR PPs, not past ones
				for (int it = 0; it < PP.length - 1; it++){

					// \sigma^t against \sigma^t
					for (int k = 0; k < no_agents; k++)
						cmp_agents[k].setCondJointDistribution(PP[it]);
					ef.StrategyDistance(cmp_auction, no_for_EUdiff);
					means[0][it] = Statistics.mean(ef.utility);
					stdevs[0][it] = Statistics.stdev(ef.utility);

					// \sigma^{t+1} against \sigma^t
					cmp_agents[0].setCondJointDistribution(PP[it+1]);					
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

