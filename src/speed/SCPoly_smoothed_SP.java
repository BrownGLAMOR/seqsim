package speed;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

// Do self-confirming smoothed updates initiated from Polynomial strategies, using Menezes decreasing valuation. Try other schemes later.  
public class SCPoly_smoothed_SP {
	public static void main(String[] args) throws IOException {
		Cache.init();
		Random rng = new Random();
		
		// Parmaters to tune
		boolean real = false;
		boolean discretize_value = true;
		double v_precision = 0.001;
		int[] NO_PER_ITERATION = new int[]{100000};		// no. of pts for PP update
		int no_refined_pts = 1000000;						// no. of pts for the PP update, for the purpose of refined epsilon evaluation
		double order = 1.0;		
		double epsilon = 0.00001;

		int S = 100, S_final = 1000;								// no. of points for EU comparison		
		
		String[] TYPE = {"g","u"};					// type of updating procedure XXX
		String model = "M";							// valuation scheme 
		
		int preference;
	//		if (type == "s")
	//			preference = 3;
	//		else
	//			preference = 0;
		preference = 0;

		// Auction parameters
		boolean decreasing = true;						// decreasing MV in Menezes valuation
		double max_value = 1.0;
		double precision = 0.05;
		double max_price = max_value;
		int no_goods = 3;
		int no_agents = 3;
		int nth_price = 2;
		
		// simulation parameters
		int no_initial_simulations = 10000000/no_agents;	// generating initial PP		
		int max_iteration = 10;								// no. of Wellman updates 
		
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
		
		boolean take_log = false;						// record prices for agents
		boolean record_prices = false;					// record prices for seller
		boolean print_strategy = true;					// Output strategy S(t)
		boolean compute_epsilon = true;					// Compute epsilon factors and output
		boolean record_utility = false;
		
		for (String type: TYPE) {
			
			System.out.println("type = " + type);
			
			for (int npi = 0; npi < NO_PER_ITERATION.length; npi++) {
				
				int no_per_iteration = NO_PER_ITERATION[npi];
				
				// record all prices (to compute distances later)
				JointCondDistributionEmpirical[] PP = new JointCondDistributionEmpirical[max_iteration+1];		
		
				// 1)  Initiate PP from Polynomial agents
				System.out.println("Generating initial PP");
				JointCondFactory jcf = new JointCondFactory(no_goods, precision, max_price);
				
				// 1.1)	Create PP[0]	XXX: change valuation ==> change setup
				MenezesMultiroundValue[] values = new MenezesMultiroundValue[no_agents];
//				UnitValue[] values = new UnitValue[no_agents];
//				KatzHLValue[] values = new KatzHLValue[no_agents];
				PolynomialAgentMultiRound[] poly_agents = new PolynomialAgentMultiRound[no_agents];
				for (int i = 0; i<no_agents; i++){
					values[i] = new MenezesMultiroundValue(max_value, rng, decreasing);
//					values[i] = new UnitValue(max_value, rng);
//					values[i] = new KatzHLValue(0, max_value, rng);
					poly_agents[i] = new PolynomialAgentMultiRound(values[i], i, no_goods, order);
				}
				SeqAuction poly_auction = new SeqAuction(poly_agents, nth_price, no_goods);
				PP[0] = jcf.simulAllAgentsOnePP(poly_auction, no_initial_simulations,take_log,record_prices,false);
						
					// Utility comparison storage
					means = new double[max_iteration+1];
					stdevs = new double[max_iteration+1];
					S_pts = new double[max_iteration+1];		// no. of points used for EUdiff evaluation
				
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
	
				// initiate updating agents
				MDPAgentSP[] updating_agents = new MDPAgentSP[no_agents];
				for (int i = 0; i < no_agents; i++){
					updating_agents[i] = new MDPAgentSP(values[i], i, preference, discretize_value, v_precision);
					updating_agents[i].inputEpsilon(epsilon);
				}
				
				// initiate comparing agents
				MDPAgentSP[] agents0 = new MDPAgentSP[no_agents];
				MDPAgentSP[] agents1 = new MDPAgentSP[no_agents];
				agents0[0] = new MDPAgentSP(values[0], 0, 0, discretize_value, v_precision);
				agents1[0] = new MDPAgentSP(values[0], 0, 0, false, v_precision);		// he cannot use the Cached strategies
					agents0[0].inputEpsilon(epsilon);
					agents1[0].inputEpsilon(epsilon);
				for (int i = 1; i < no_agents; i++){
					agents0[i] = new MDPAgentSP(values[i], i, preference, discretize_value, v_precision);
					agents1[i] = new MDPAgentSP(values[i], i, preference, discretize_value, v_precision);
						agents0[i].inputEpsilon(epsilon);
						agents1[i].inputEpsilon(epsilon);
				}
				SeqAuction updating_auction = new SeqAuction(updating_agents, nth_price, no_goods);
				SeqAuction auction0 = new SeqAuction(agents0, nth_price, no_goods);
				SeqAuction auction1 = new SeqAuction(agents1, nth_price, no_goods);
	
				// 2) Wellman updates
				int it = 0;		// iteration number
				while (it < max_iteration) {
					
					System.out.print("Wellman iteration = " + it + "...");
					
					// 2.1) generate new PP
					for (int i = 0; i < no_agents; i++){
						updating_agents[i].setCondJointDistribution(PP[it]);
	//					agents0[i].inputGamma(GAMMA[it]);
					}
	
					Cache.clearMDPpolicy();
					if (type == "u")
						PP[it+1] = jcf.offPolicySymmetric(updating_auction, no_per_iteration,real);
					else
						PP[it+1] = jcf.simulAllAgentsOnePP(updating_auction, no_per_iteration/no_agents, take_log, record_prices, record_utility);
					
					// 2.2) output first round bids for comparison 
					if (print_strategy == true) {
						for (int i = 0; i < no_for_cmp; i++) {
							values[0].x = v[i];
							updating_agents[0].reset(null);		// recompute MDP
							strategy[it+1][i] = updating_agents[0].getFirstRoundPi();
						}	
					}
					
					// 2.3) Compute epsilon w/ PP[it+1] to evaluate convergence
					if (compute_epsilon == true) {
						
						System.out.println("... computing epsilon");
						EpsilonFactor2 ef = new EpsilonFactor2();				
						
						// compute distance with PP[it+1]
						agents0[0].setCondJointDistribution(PP[it]);
						agents1[0].setCondJointDistribution(PP[it+1]);
						for (int k = 1; k < no_agents; k++){
							agents0[k].setCondJointDistribution(PP[it]);
							agents1[k].setCondJointDistribution(PP[it]);
						}
						
						// convergence diagnosis
						ef.StrategyDistance2(auction0, auction1, S);
						means[it] = Statistics.mean(ef.utility_diff);
						stdevs[it] = Statistics.stdev(ef.utility_diff);
						S_pts[it] = S;
						System.out.println("tstat = " +  means[it]/(stdevs[it]/Math.sqrt(S)));
												
						
					}
					it ++;
				}
		
				// Compute epsilon factor for final step
				System.out.println("computing refined epsilon for final step...");
				// do a refined uniform update
				for (int i = 0; i < no_agents; i++)
					updating_auction.agents[i].setCondJointDistribution(PP[PP.length-1]);
				Cache.clearMDPpolicy();
				JointCondDistributionEmpirical finalPP = jcf.offPolicySymmetric(updating_auction, no_refined_pts, real);
				
				auction0.agents[0].setCondJointDistribution(PP[PP.length-1]);
				auction1.agents[0].setCondJointDistribution(finalPP);
				for (int i = 1; i < no_agents; i++){
					auction0.agents[i].setCondJointDistribution(PP[PP.length-1]);
					auction1.agents[i].setCondJointDistribution(PP[PP.length-1]);
				}
				EpsilonFactor2 ef = new EpsilonFactor2();				
				ef.StrategyDistance2(auction0, auction1, S_final);
				means[max_iteration] = Statistics.mean(ef.utility_diff);
				stdevs[max_iteration] = Statistics.stdev(ef.utility_diff);
				S_pts[max_iteration] = S_final;
				System.out.println("final tstat = " +  means[max_iteration]/(stdevs[max_iteration]/Math.sqrt(S_final)));

				
				// write strategies to file
				if (print_strategy == true){
	
					FileWriter fw_strat = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/paper/june1st/SP/eric/" + model  + type + no_goods + "_" + no_agents + "_" + order + "_" + precision + "_" + discretize_value + v_precision + "_" + max_iteration + "_" + no_per_iteration + "pts.csv");
	
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
					
					FileWriter fw_EUdiff = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/paper/june1st/SP/eric/" + model + type + "epsilon" + no_goods + "_" + no_agents + "_" + order + "_" + precision + "_" + discretize_value + v_precision + "_" + max_iteration + "_" + no_per_iteration + "pts.csv");
					
					for (int t = 0; t < means.length; t++){
						fw_EUdiff.write(S_pts[t] + "," + means[t] + "," + stdevs[t] + "\n"); 
						System.out.print(S_pts[t] + "," + means[t] + "," + stdevs[t] + "\n"); 
					}
					fw_EUdiff.close();			
				System.out.println("done done");
				
				}
			}
		}
	}
}


