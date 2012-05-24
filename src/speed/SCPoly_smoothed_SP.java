package speed;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

// Do self-confirming smoothed updates initiated from Polynomial strategies, using Menezes decreasing valuation. Try other schemes later.  
public class SCPoly_smoothed_SP {
	public static void main(String[] args) throws IOException {
		Cache.init();
		Random rng = new Random();
		
		// Parmaters to tune
		boolean real = true;
		boolean discretize_value = false;
		double v_precision = 0.001;
		int[] NO_PER_ITERATION = new int[]{100000};		// no. of pts for PP update
		
//		int S0 = 1000, S_refined = 100000;				// S0: initial pts for epsilon calculation; S_refined = 
		int S_increment = 5000, S_max = 10000;								// no. of points for EU comparison		
		
		String type = "g";								// type of updating procedure XXX
		int preference;
		if (type == "s")
			preference = 3;
		else
			preference = 0;

		// Auction parameters
		boolean decreasing = true;						// decreasing MV in Menezes valuation
		double max_value = 1.0;
		double precision = 0.05;
		double max_price = max_value;
		int no_goods = 2;
		int no_agents = 2;
		int nth_price = 2;
		
		// simulation parameters
		double order = 1.0;
		int no_initial_simulations = 1000000/no_agents;	// generating initial PP		
		int max_iteration = 5;								// no. of Wellman updates 
		
//			// Cooling scheme
//			double gamma_0 = 0.9, gamma_end = 1.0;						// target \gamma values
//			double alpha = (gamma_end-gamma_0)/Math.log(max_iteration), beta = Math.pow(max_iteration, gamma_0/(gamma_end-gamma_0));	// corresponding parameters
//			double[][] GAMMA = new double[max_iteration][no_goods];
//			for (int t = 0; t < max_iteration; t++){
//				for (int i = 0; i < no_goods; i++)
//					GAMMA[t][i] = alpha*Math.log(beta*(t+1));
//			}
		

		// evaluation parameters
		double cmp_precision = 0.01;						// discretization step for valuation examining
		int no_for_cmp = (int) (1/cmp_precision) + 1;
		double[] means, stdevs, S_pts;
		double mean = 0, stdev = 0;
		
		boolean take_log = false;						// record prices for agents
		boolean record_prices = false;					// record prices for seller
		boolean print_strategy = true;					// Output strategy S(t)
		boolean compute_epsilon = true;					// Compute epsilon factors and output
		boolean record_utility = false;
		
		for (int npi = 0; npi < NO_PER_ITERATION.length; npi++) {
			
			int no_per_iteration = NO_PER_ITERATION[npi];
			
			// record all prices (to compute distances later)
			JointCondDistributionEmpirical[] PP = new JointCondDistributionEmpirical[max_iteration+1];		
	
			// 1)  Initiate PP from Polynomial agents
			System.out.println("Generating initial PP");
			JointCondFactory jcf = new JointCondFactory(no_goods, precision, max_price);
			
			// 1.1)	Create PP[0]
			PolynomialAgentMultiRound[] poly_agents = new PolynomialAgentMultiRound[no_agents];
			MenezesMultiroundValue[] values = new MenezesMultiroundValue[no_agents];
//			MenezesValue[] values = new MenezesValue[no_agents];
			for (int i = 0; i<no_agents; i++){
				values[i] = new MenezesMultiroundValue(max_value, rng, decreasing);
//				values[i] = new MenezesValue(max_value, rng, decreasing);
				poly_agents[i] = new PolynomialAgentMultiRound(values[i], i, no_goods, order);
			}
			SeqAuction poly_auction = new SeqAuction(poly_agents, nth_price, no_goods);
			PP[0] = jcf.simulAllAgentsOnePP(poly_auction, no_initial_simulations,take_log,record_prices,false);
					
				// Utility comparison storage
				means = new double[max_iteration];
				stdevs = new double[max_iteration];
				S_pts = new double[max_iteration];		// no. of points used for EUdiff evaluation
				double[] Udiff = new double[S_max];
			
				// initiate tools for bid comparison
//				mdp_agent.inputGamma(GAMMA[0]);
				double[] v = new double[no_for_cmp]; 		
				for (int i = 0; i < no_for_cmp; i++)
					v[i] = i*cmp_precision;
				double[][] strategy = new double[max_iteration + 1][no_for_cmp];

			
			// 1.2)	Record initial strategy
			for (int i = 0; i < no_for_cmp; i++) {
				values[0].x = v[i];
				strategy[0][i] = poly_agents[0].getBid(0,0);
			}	

			// initiate updating & comparison agents
			MDPAgentSP[] agents0 = new MDPAgentSP[no_agents];
			MDPAgentSP[] agents1 = new MDPAgentSP[no_agents];
			
			agents0[0] = new MDPAgentSP(values[0], 0, preference, discretize_value, v_precision);
			agents1[0] = new MDPAgentSP(values[0], 0, preference, false, v_precision);		// he cannot use the Cached strategies
			for (int i = 1; i < no_agents; i++){
				agents0[i] = new MDPAgentSP(values[i], i, preference, discretize_value, v_precision);
				agents1[i] = new MDPAgentSP(values[i], i, preference, discretize_value, v_precision);
			}
			SeqAuction auction0 = new SeqAuction(agents0, nth_price, no_goods);
			SeqAuction auction1 = new SeqAuction(agents1, nth_price, no_goods);

			// 2) Wellman updates
			int it = 0;		// iteration number
			boolean converged = false;
			while (it < max_iteration && converged == false) {
				
				System.out.print("Wellman iteration = " + it + "...");
				
				// 2.1) generate new PP
				for (int i = 0; i < no_agents; i++){
					agents0[i].setCondJointDistribution(PP[it]);
//					agents0[i].inputGamma(GAMMA[it]);
				}

				Cache.clearMDPpolicy();
				if (type == "u")
					PP[it+1] = jcf.offPolicySymmetric(auction0, no_per_iteration,real);
				else
					PP[it+1] = jcf.simulAllAgentsOnePP(auction0, no_per_iteration/no_agents, take_log, record_prices, record_utility);
//					PP[it+1] = jcf.simulAllAgentsOneRealPP(auction0, no_per_iteration/no_agents, take_log, record_prices, record_utility);
				
				// 2.2) output first round bids for comparison 
				if (print_strategy == true) {
					for (int i = 0; i < no_for_cmp; i++) {
						values[0].x = v[i];
						agents0[0].reset(null);		// recompute MDP
						strategy[it+1][i] = agents0[0].getFirstRoundPi();
					}	
				}
				
				// 2.3) Compute epsilon w/ PP[it+1] to evaluate convergence
				if (compute_epsilon == true) {
					
					System.out.println("... computing epsilon");
					EpsilonFactor2 ef = new EpsilonFactor2();				
					
					// compute distance with PP[it+1]
					agents1[0].setCondJointDistribution(PP[it+1]);
					for (int k = 1; k < no_agents; k++)
						agents1[k].setCondJointDistribution(PP[it]);
					
					int S = 0;
					double tstat = 0.0;
					while (S < S_max && Math.abs(tstat) < 2){
						ef.StrategyDistance2(auction0, auction1, S_increment);
						
						// evaluate convergence
						for (int i = 0; i < S_increment; i++)
							Udiff[S+i] = ef.utility_diff[i];
						mean = Statistics.mean(Arrays.copyOfRange(Udiff, 0, S+S_increment));
						stdev = Statistics.stdev(Arrays.copyOfRange(Udiff, 0, S+S_increment));
						tstat = mean/(stdev/Math.sqrt(S+S_increment));
						
						System.out.println("tstat = " + tstat);
						S = S + S_increment;						
					}
					
					// ending parameters
					means[it] = mean;
					stdevs[it] = stdev;
					S_pts[it] = S;
					
					// convergence diagnosis
					if (tstat < 2 && tstat > 0)
						converged = true;
					
				}
				it ++;
			}
	
			// write strategies to file
			if (print_strategy == true){

				FileWriter fw_strat = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/paper/june1st/SP/epsilonpursuit/" + type + no_goods + "_" + order + "_" + no_agents + "_" + precision + "_" + discretize_value + v_precision + "_" + max_iteration + "_" + no_per_iteration + "pts.csv");

				// write first round bidding functions
				for (int i = 0; i < it; i++){
					for (int j = 0; j < strategy[i].length - 1; j++){
						fw_strat.write(strategy[i][j] + ",");
					}
					fw_strat.write(strategy[i][strategy[i].length-1] + "\n");
				}
				fw_strat.close();
			}
			
			// Write epsilons to file
			if (compute_epsilon == true) {
				
				FileWriter fw_EUdiff = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/paper/june1st/SP/epsilonpursuit/" + type + no_goods + "epsilon_" + order + "_" + no_agents + "_" + precision + "_" + discretize_value + v_precision + "_" + max_iteration + "_" + no_per_iteration + "pts.csv");
				
				for (int t = 0; t < it; t++){
					fw_EUdiff.write(S_pts[t] + "," + means[t] + "," + stdevs[t] + "\n"); 
					System.out.print(S_pts[t] + "," + means[t] + "," + stdevs[t] + "\n"); 
				}
				fw_EUdiff.close();			
			System.out.println("done done");
			
			}
		}
	}
}


