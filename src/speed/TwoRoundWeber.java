package speed;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

// Let's see if the MDPCondMDPAgent4 works in two round Weber set up, with discretized/ not discretized utility. If not, then debug. 
public class TwoRoundWeber {
	public static void main(String[] args) throws IOException {
		Cache.init();
		Random rng = new Random();
		
		// general Parameters
		boolean first_price = false;
		double max_value = 1.0;
		double p_precision = 0.02;	// price precision
		double max_price = max_value;
		int no_goods = 2;
		int no_agents = 3;
		int nth_price = 2;

		// agent preferences		
		boolean discretize_value = true;				// whether to discretized value
		double v_precision = 0.0001;				// valuation precision
//		double v_precision = p_precision;				// valuation precision
		int preference = 0;							// MDP agent preference: 2 = favor mixing, +/-1 = favor upper/lower bound
		double epsilon = 0.00005;						// tie-breaking threshold

		// simulation parameters
		int no_initial_simulations = 100000000/no_agents;	// generating initial PP		
		int no_iterations = 1;								// no. of Wellman updates 
		int no_per_iteration = 10/no_agents;			// no. of self play in each Wellman update		
		
		// Evaluation parameters
		double cmp_precision = 0.01;						// discretization precision when evaluating strategy 
//		double cmp_precision = v_precision;						// discretization precision when evaluating strategy 
		int no_for_cmp = (int) (1/cmp_precision) + 1;
		int no_for_EUdiff = 10000;							// no. of points for EU comparison
		int price_lags = 3;									// compute epsilon factor for PPs different up to "price_lags" iterations apart

		// what to record
		boolean take_log = false;						// record prices for agents
		boolean record_prices = false;					// record prices for seller		
		boolean print_strategy = true;					// Output strategy S(t)
		boolean compute_epsilon = false;					// Compute epsilon factors and output
		
		// 0) Some intializations...
		// record all prices
		JointCondDistributionEmpirical[] PP = new JointCondDistributionEmpirical[no_iterations+1];		
		
		// initiate agents for later bid comparison
		UnitValue value = new UnitValue(max_value, rng);
		FullCondMDPAgent4 mdp_agent = new FullCondMDPAgent4(value, 1, preference, epsilon, discretize_value, v_precision);
	
		// initiate tools to compare strategies
		double[] v = new double[no_for_cmp]; 		
		for (int i = 0; i < no_for_cmp; i++)
			v[i] = i*cmp_precision;
		double[][] strategy = new double[no_iterations + 1][no_for_cmp];
		

		// 1)  Initiate PP[0] from Weber agents
		System.out.println("Generating initial PP");

		// create agents and action for initialization
		WeberAgent[] weber_agents = new WeberAgent[no_agents];
		for (int i = 0; i<no_agents; i++)
			weber_agents[i] = new WeberAgent(new UnitValue(max_value, rng), i, no_agents, no_goods, first_price);

		JointCondFactory jcf = new JointCondFactory(no_goods, p_precision, max_price);		
		SeqAuction poly_auction = new SeqAuction(weber_agents, nth_price, no_goods);
		PP[0] = jcf.simulAllAgentsOnePP(poly_auction, no_initial_simulations,take_log,false,false);
				
		PP[0].outputNormalized();
		
//		FileWriter fw_pp0 = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/paper/threerounds/weber_pp0_" + no_agents + "_" + v_precision + "_" + p_precision + "_" + no_iterations + ".csv");
//		PP[0].outputRaw(fw_pp0);
//		fw_pp0.close();
			
		// 2) Wellman updates
		for (int it = 0; it < no_iterations; it++) {
			
			System.out.println("Wellman iteration = " + it);
			Cache.clearMDPpolicy();
			
			// 2.1) Do next iteration to generate a new PP
			FullCondMDPAgent4[] mdp_agents = new FullCondMDPAgent4[no_agents];
			for (int i = 0; i < no_agents; i++) {
				mdp_agents[i] = new FullCondMDPAgent4(new UnitValue(max_value, rng), i, preference, epsilon, discretize_value, v_precision);
				mdp_agents[i].setCondJointDistribution(PP[it]);
			}
			SeqAuction updating_auction = new SeqAuction(mdp_agents, nth_price, no_goods);
			
			// generate a new pp
			PP[it+1] = jcf.simulAllAgentsOnePP(updating_auction, no_per_iteration,take_log,record_prices,false);


			// 2.2) Compare: how different are we from previous step? output bids. 
			if (print_strategy == true) {
					
				// initiate agents to compare bids
				mdp_agent.setCondJointDistribution(PP[it]);
				
				Cache.clearMDPpolicy();		// TODO: to remove
				// Assign values, instead of sample values
				for (int i = 0; i < no_for_cmp; i++) {
					value.x = v[i];
					mdp_agent.reset(null);		// recompute MDP
					mdp_agent.printpi();
					mdp_agent.printV();
//					if (value.x != 0) {
//						System.out.println("value.x = " + value.x);
//						mdp_agent.printpi();
//						mdp_agent.printV();
//					}

					strategy[it+1][i] = mdp_agent.getFirstRoundBid();
				}	
			}
		}

		// output each step's strategy
		if (print_strategy == true){
			FileWriter fw_strat = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/paper/tworoundweber/best" + no_agents + "_" + v_precision + "_" + p_precision + "_" + no_iterations + ".csv");
			for (int i = 1; i < strategy.length; i++){		// XXX: start from i = 0 if initial is defined
				for (int j = 0; j < strategy[i].length - 1; j++){
					fw_strat.write(strategy[i][j] + ",");
				}
				fw_strat.write(strategy[i][strategy[i].length-1] + "\n");
			}
			fw_strat.close();
		}
		
		// Compute PP distances and output TODO: not touched yet. This is instrumental in comparing strategies in three rounds. 
		if (compute_epsilon == true) {
			System.out.println("computing price distances...");

			FileWriter fw_EUdiff = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/paper/updates/poly" + "_EUdiff_" + no_agents + "_" + p_precision + "_" + no_iterations + ".csv");
		
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

