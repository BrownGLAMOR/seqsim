package speed;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

// Do self-confirming updates initiated from Polynomial strategies, with forced convergence. Otherwise the same as SCPoly_smoothed_SP.java.   
public class SCPoly_forced_SP {
	public static void main(String[] args) throws IOException {
		Cache.init();
		Random rng = new Random();
		
		// Parmaters to tune
		boolean initialize_from_eq = false;				// initialize from theoretical equilibrium
		double[] ORDER = new double[] {1.0, 0.5, 2.0};	// otherwise, initiate from polynomial agents
		
		boolean force_convergence = false;
		double w = 0.0;									// PP[t+1] <- w*BR(PP[t]) + (1-w)*PP[t]. High w means more weight to new data
		
		boolean real = false;							// "false" ==> give agents information about opponent second price
		boolean discretize_value = true;
		double v_precision = 0.0001;						// discretization precision if "discretize_value" is true
		
		int[] NO_PER_ITERATION = new int[]{100000};		// no. of pts for each PP update
		int no_refined_pts = 1000000;					// no. of pts for the PP update, for the purpose of refined epsilon evaluation
		int S = 10000, S_final = 100000;					// no. of points for epsilon factor evaluation
		
		// ESS comparison
		boolean compute_ESS = true;						// whether to compare our ending strategy with theoretical strategy
		int S_ESS = 100000;								// play this many times in doing so	
		
		String[] TYPE = {"u"};						// type of updating procedure
		String model = "M";								// valuation scheme
		double eta = 0.2;								// how much to explore off-policy
		double eta2 = 0;								// opponent exploring off-policy
		int preference = 0;								// MDP Agent preference

		// Auction parameters
		boolean decreasing = true;						// decreasing MV in Menezes valuation
		double max_value = 1.0;
		double precision = 0.05;						// precision of bids
		double max_price = max_value;

		int no_goods = 2;
		int[] NO_AGENTS = new int[]{3};
		int nth_price = 2;
		
		int no_initial_simulations = 1000000;	// generating initial PP		
		int max_iteration = 10;								// no. of Wellman updates 

		// evaluation parameters
		double cmp_precision = 0.01;						// discretization step for valuation examining
		int no_for_cmp = (int) (1/cmp_precision) + 1;
		double[] means, stdevs, S_pts;
		
		boolean take_log = false;						// record prices for agents
		boolean record_prices = false;					// record prices for seller
		boolean print_strategy = true;					// Output strategy S(t)
		boolean compute_epsilon = true;					// Compute epsilon factors and output
		boolean record_utility = false;
		
		for(int no_agents: NO_AGENTS){
			System.out.println("no_agents = " + no_agents);
			for (double order: ORDER){
				System.out.println("order = " + order);
				for (String type: TYPE) {
					System.out.println("type = " + type);
					for (int no_per_iteration : NO_PER_ITERATION) {
						
						// record all prices (to compute distances later)
						JointCondDistributionEmpirical[] PP = new JointCondDistributionEmpirical[max_iteration+1];		
				
						// Utility comparison storage
						means = new double[max_iteration+1];
						stdevs = new double[max_iteration+1];
						S_pts = new double[max_iteration+1];		// no. of points used for EUdiff evaluation
					
						// initiate tools for bid comparison
						double[] v = new double[no_for_cmp]; 		
						for (int i = 0; i < no_for_cmp; i++)
							v[i] = i*cmp_precision;
						double[][] strategy = new double[max_iteration + 1][no_for_cmp];
		
		
						// 1)  Initiate PP
						System.out.println("Generating initial PP");
						JointCondFactory jcf = new JointCondFactory(no_goods, precision, max_price);
						
						// 1.1)	Create PP[0]	XXX: change valuation ==> change setup
						MenezesMultiroundValue[] values = new MenezesMultiroundValue[no_agents];
//						UnitValue[] values = new UnitValue[no_agents];
//						KatzHLValue[] values = new KatzHLValue[no_agents];
						for (int i = 0; i<no_agents; i++)
							values[i] = new MenezesMultiroundValue(max_value, rng, decreasing);
//							values[i] = new UnitValue(max_value, new Random());
//							values[i] = new KatzHLValue(0, max_value, rng);
						
//						if (initialize_from_eq == true){
//							WeberAgent[] initial_agents = new WeberAgent[no_agents];
//							for (int i = 0; i < no_agents; i++)	// XXX: change here
//								initial_agents[i] = new WeberAgent(values[i], i, no_agents, no_goods, false);
//								SeqAuction poly_auction = new SeqAuction(initial_agents, nth_price, no_goods);
//								PP[0] = jcf.simulAllAgentsOnePP(poly_auction, no_initial_simulations/no_agents,take_log,record_prices,false);
//								
//								// 1.2)	Record initial strategy
//								for (int i = 0; i < no_for_cmp; i++) {
//									values[0].x = v[i];
//									strategy[0][i] = initial_agents[0].getBid(0);
//								}
//						}
//						else{
							PolynomialAgentMultiRound[] initial_agents = new PolynomialAgentMultiRound[no_agents];
							for (int i = 0; i<no_agents; i++)
								initial_agents[i] = new PolynomialAgentMultiRound(values[i], i, no_goods, order);
							SeqAuction poly_auction = new SeqAuction(initial_agents, nth_price, no_goods);
							PP[0] = jcf.offPolicyEtaSymmetric(poly_auction, eta, eta2, no_per_iteration, real);
//							PP[0] = jcf.simulAllAgentsOnePP(poly_auction, no_initial_simulations,take_log,record_prices,false); 

							
							// 1.2)	Record initial strategy
							for (int i = 0; i < no_for_cmp; i++) {
								values[0].x = v[i];
								strategy[0][i] = initial_agents[0].getBid(0,0);
							}
//						}
								
						
			
						// initiate updating agents
						MDPAgentSP[] updating_agents = new MDPAgentSP[no_agents];
						for (int i = 0; i < no_agents; i++)
							updating_agents[i] = new MDPAgentSP(values[i], i, preference, discretize_value, v_precision);
						
						// initiate comparing agents
						MDPAgentSP[] agents0 = new MDPAgentSP[no_agents];
						MDPAgentSP[] agents1 = new MDPAgentSP[no_agents];
						agents0[0] = new MDPAgentSP(values[0], 0, 0, discretize_value, v_precision);
						agents1[0] = new MDPAgentSP(values[0], 0, 0, false, v_precision);		// he cannot use the Cached strategies
						for (int i = 1; i < no_agents; i++){
							agents0[i] = new MDPAgentSP(values[i], i, preference, discretize_value, v_precision);
							agents1[i] = new MDPAgentSP(values[i], i, preference, discretize_value, v_precision);
						}
						SeqAuction updating_auction = new SeqAuction(updating_agents, nth_price, no_goods);
						SeqAuction auction0 = new SeqAuction(agents0, nth_price, no_goods);
						SeqAuction auction1 = new SeqAuction(agents1, nth_price, no_goods);
			
						// 2) Wellman updates
						int it = 0;		// iteration number
						while (it < max_iteration) {
							
							System.out.print("Wellman iteration = " + it + "...");
							
							// 2.1) generate new PP
							for (int i = 0; i < no_agents; i++)
								updating_agents[i].setCondJointDistribution(PP[it]);
			
							Cache.clearMDPpolicy();
							if (type == "u"){
		//						PP[it+1] = jcf.offPolicySymmetric(updating_auction, no_per_iteration,real);
								PP[it+1] = jcf.offPolicyEtaSymmetric(updating_auction, eta, eta2, no_per_iteration,real);
							}
							else
								PP[it+1] = jcf.simulAllAgentsOnePP(updating_auction, no_per_iteration/no_agents, take_log, record_prices, record_utility);
							
							
							// Add old PP histories if we are doing smoothing
							if (force_convergence == true)
								PP[it+1].addJcde(PP[it], w);
								
		
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
								for (int k = 1; k < no_agents; k++){
									agents0[k].setCondJointDistribution(PP[it]);
									agents1[k].setCondJointDistribution(PP[it]);
								}
								
								if (type == "u")
									agents1[0].setCondJointDistribution(PP[it+1]);
								else{
									JointCondDistributionEmpirical jcde = jcf.offPolicyEtaSymmetric(auction0, eta, eta2, no_per_iteration, real);
									agents1[0].setCondJointDistribution(jcde);
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
		//				JointCondDistributionEmpirical finalPP = jcf.offPolicySymmetric(updating_auction, no_refined_pts, real);
						JointCondDistributionEmpirical finalPP = jcf.offPolicyEtaSymmetric(updating_auction, eta, eta2, no_refined_pts, real);
						
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
			
							FileWriter fw_strat = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/paper/june1st/SP/forced/" + model  + type + no_goods + "_" + w + "_" + no_agents + "_" + order + "_" + discretize_value + v_precision + "_" + max_iteration + "_" + no_per_iteration + "pts.csv");
			
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
							
							FileWriter fw_EUdiff = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/paper/june1st/SP/forced/" + model + type + "epsilon" + w + "_" + no_goods + "_" + no_agents + "_" + order + "_" + discretize_value + v_precision + "_" + max_iteration + "_" + no_per_iteration + "pts.csv");
							
							for (int t = 0; t < means.length; t++){
								fw_EUdiff.write(S_pts[t] + "," + means[t] + "," + stdevs[t] + "\n"); 
								System.out.print(S_pts[t] + "," + means[t] + "," + stdevs[t] + "\n"); 
							}
							fw_EUdiff.close();			
						
						}
						
						// ESS comparison
						if (compute_ESS == true){

							System.out.println("computing ESS...");		
							FileWriter fw_ESS = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/paper/june1st/SP/forced/" + model + type + "ESS" + w + "_" + no_goods + "_" + no_agents + "_" + order + "_" + discretize_value + v_precision + "_" + max_iteration + "_" + no_per_iteration + "pts.csv");
		
							Cache.clearMDPpolicy();
							// Create different mixes. n is number of our agents, 
							for (int n = 0; n <= no_agents; n++){
								System.out.println("n = " + n);
								
								SeqAgent[] agents = new SeqAgent[no_agents]; 
								for (int i = 0; i < n; i++){
									agents[i] = new MDPAgentSP(values[i], i, 0, discretize_value, v_precision);
									agents[i].setCondJointDistribution(finalPP);
								}
			
								for (int i = n; i < no_agents; i++){	// XXX: require changing here
									agents[i] = new MenezesAgent(values[i], no_agents, i, 0, decreasing);
//									agents[i] = new WeberAgent(values[i], i, no_agents, no_goods, false);
//									agents[i] = new KatzmanUniformAgent(values[i], no_agents, i);
								}
							
								SeqAuction auction = new SeqAuction(agents, nth_price, no_goods);
								
								// Compute utility
								ef.ESS(auction, n, S_ESS);								
								fw_ESS.write(no_agents + "," + n);
								System.out.print(no_agents + "," + n);
								for (int k = 0; k < no_agents; k++){
									fw_ESS.write("," + ef.means[k]);
									System.out.print("," + ef.means[k]);
								}
								for (int k = 0; k < no_agents; k++){
									fw_ESS.write("," + ef.stdevs[k]);
									System.out.print("," + ef.stdevs[k]);
								}
								fw_ESS.write("\n");
								System.out.print("\n");
			
							}
							fw_ESS.close();
						}
						System.out.println("done done");
					}
				}						

			}
		}
	}
}


