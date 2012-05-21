package speed;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

// OLD FILE: Do self-confirming updates initiated from Polynomial strategies, using Menezes decreasing valuation
public class FullCondSCPoly {
	public static void main(String[] args) throws IOException {
		Cache.init();
		Random rng = new Random();
		
		// general Parameters
		double max_value = 1.0;
		double precision = 0.05;
		double max_price = max_value;
		int no_goods = 2;
		int no_agents = 2;
		int nth_price = 2;

		// agent preferences		
		double[] ORDER = new double[] {1};
		int preference = 0;								// MDP agent preference
		double epsilon = 0.00005;						// tie-breaking threshold
		boolean decreasing = true;						// decreasing MV in Menezes valuation
		boolean discretize_value = false;
		double v_precision = 0.0001;

		// simulation parameters
		int no_initial_simulations = 1000000/no_agents;	// generating initial PP		
		int no_iterations = 5;								// no. of Wellman updates 
		int no_per_iteration = 100000/no_agents;			// no. of games played in each Wellman iteration		
		double cmp_precision = 0.01;						// discretization step for valuation examining
		int no_for_cmp = (int) (1/cmp_precision) + 1;
		int no_for_EUdiff = 10000;							// no. of points for EU comparison
		int price_lags = 3;									// compute epsilon factor for PPs different up to "price_lags" iterations apart

		
		boolean take_log = false;						// record prices for agents
		boolean record_prices = false;					// record prices for seller		
		boolean print_strategy = true;				// Output strategy S(t)
		boolean compute_epsilon = false;					// Compute epsilon factors and output

		
		for (int o = 0; o < ORDER.length; o++) {
			
			double order = ORDER[o];

			// record all prices (to compute distances later)
			JointCondDistributionEmpirical[] PP = new JointCondDistributionEmpirical[no_iterations+1];		
	
			// 1)  Initiate PP from Polynomial agents
			System.out.println("Generating initial PP");
			JointCondFactory jcf = new JointCondFactory(no_goods, precision, max_price);
			
			// 1.1)	Create PP[0]
			MenezesValue[] poly_values = new MenezesValue[no_agents];
			PolynomialAgent[] poly_agents = new PolynomialAgent[no_agents];
			for (int i = 0; i<no_agents; i++){
				poly_values[i] = new MenezesValue(max_value, rng, decreasing);
				poly_agents[i] = new PolynomialAgent(poly_values[i], no_agents, order);
			}
			SeqAuction poly_auction = new SeqAuction(poly_agents, nth_price, no_goods);
			
			PP[0] = jcf.simulAllAgentsOnePP(poly_auction, no_initial_simulations,take_log,record_prices,false);
					
				// initiate agents for later bid comparison
				MenezesValue value = new MenezesValue(max_value, rng, decreasing);
//				FullCondMDPAgent3 mdp_agent = new FullCondMDPAgent3(value, 1, preference, epsilon);
				FullCondMDPAgent4 mdp_agent = new FullCondMDPAgent4(value, 1, preference, epsilon, discretize_value, v_precision);

		
				// initiate tools to compare strategies
				double[] v = new double[no_for_cmp]; 		
				for (int i = 0; i < no_for_cmp; i++)
					v[i] = i*cmp_precision;
				double[][] strategy = new double[no_iterations + 1][no_for_cmp];
			
			// 1.2)	Record initial strategy
			for (int i = 0; i < no_for_cmp; i++) {
				poly_values[0].x = v[i];
				strategy[0][i] = poly_agents[0].getBid(0);
			}
	
			
			// 2) Wellman updates
			for (int it = 0; it < no_iterations; it++) {
				
				System.out.println("Wellman iteration = " + it);
	
				// 2.1) Do next iteration to generate a new PP
//				FullCondMDPAgent3[] mdp_agents = new FullCondMDPAgent3[no_agents];
//				for (int i = 0; i < no_agents; i++) {
//					mdp_agents[i] = new FullCondMDPAgent3(new MenezesValue(max_value, rng, decreasing), i, preference, epsilon);
//					mdp_agents[i].setCondJointDistribution(PP[it]);
//				}
				FullCondMDPAgent4[] mdp_agents = new FullCondMDPAgent4[no_agents];
				for (int i = 0; i < no_agents; i++) {
					mdp_agents[i] = new FullCondMDPAgent4(new MenezesValue(max_value, rng, decreasing), 1, preference, epsilon, discretize_value, v_precision);
					mdp_agents[i].setCondJointDistribution(PP[it]);
				}
				SeqAuction updating_auction = new SeqAuction(mdp_agents, nth_price, no_goods);
				
				// generate a new pp
				PP[it+1] = jcf.simulAllAgentsOnePP(updating_auction, no_per_iteration,take_log,record_prices,false);
				
	
				// 2.2) Compare: how different are we from previous step? output bids. 
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
						strategy[it+1][i] = mdp_agent.getFirstRoundBid();
					}	
				}
			}
	
			// output each step's strategy
			if (print_strategy == true){
				FileWriter fw_strat = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/paper/june1st/SP/old_agent4_" + order + "_" + no_agents + "_" + precision + "_" + no_iterations + ".csv");
//				FileWriter fw_strat = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/paper/updates/poly" + order + "_" + no_agents + "_" + precision + "_" + no_iterations + ".csv");
				for (int i = 0; i < strategy.length; i++){
					for (int j = 0; j < strategy[i].length - 1; j++){
						fw_strat.write(strategy[i][j] + ",");
					}
					fw_strat.write(strategy[i][strategy[i].length-1] + "\n");
				}
				fw_strat.close();
			}
			
			// Compute PP distances and output
			if (compute_epsilon == true) {
				System.out.println("computing price distances...");
	
				FileWriter fw_EUdiff = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/paper/updates/poly" + order + "_EUdiff_" + no_agents + "_" + precision + "_" + no_iterations + ".csv");

				EpsilonFactor ef = new EpsilonFactor(no_goods);
				// compute distance with future BR PPs, not past ones
				for (int it = 0; it < PP.length - price_lags; it++){
					for (int j = 0; j < price_lags - 1; j++){
						ef.jcdeDistance(rng, PP[it+j+1], PP[it], value, no_for_EUdiff);
						fw_EUdiff.write(ef.EU_diff + "," + Statistics.stdev(ef.udiff) + ",");
					}
					int j = price_lags - 1;
					ef.jcdeDistance(rng, PP[it+j+1], PP[it], value, no_for_EUdiff);
					fw_EUdiff.write(ef.EU_diff + "," + Statistics.stdev(ef.udiff) + "\n");				
				}
			
			fw_EUdiff.close();
			System.out.println("done done");
			
			}
		}
	}
}

