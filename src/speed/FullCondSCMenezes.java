package speed;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

// Test if strategy computed from MDP would deviate if initiated from a Menezes-generated Conditional price distribution
public class FullCondSCMenezes {
	public static void main(String[] args) throws IOException {
		Cache.init();
		Random rng = new Random();
		
		// Parameters
		double max_value = 1.0;
		double precision = 0.05;
		double max_price = max_value;
		
		int no_goods = 2;
		int no_agents = 3;
		int nth_price = 2;

		int no_initial_simulations = 10000000/no_agents;	// generating initial PP		
		int no_iterations = 20;								// no. of Wellman updates 
		int no_per_iteration = 50000/no_agents;			// no. of games played in each Wellman iteration		
		double cmp_precision = 0.001;						// discretization step for valuation examining
		int no_for_cmp = (int) (1/cmp_precision) + 1;
		int no_for_EUdiff = 10000;							// no. of points for EU comparison
		int price_lags = 3;									// compute epsilon factor for PPs different up to "price_lags" iterations apart

		// agent preferences		
		int preference = -1;							// MDP agent preference
		double epsilon = 0.00005;						// tie-breaking threshold
		boolean decreasing = true;						// decreasing MV in Menezes valuation
		int mene_type = 0;								// Menezes agent
		
		boolean take_log = false;						// record prices for agents
		boolean record_prices = false;					// record prices for seller		
		boolean print_strategy = true;				// Output strategy S(t)
		boolean compute_epsilon = true;					// Compute epsilon factors and output
		
		// record all prices (to compute distances later)
		JointCondDistributionEmpirical[] PP = new JointCondDistributionEmpirical[no_iterations+1];		

		// 1)  Initiate PP from Menezes
		System.out.println("Generating initial PP");
		JointCondFactory jcf = new JointCondFactory(no_goods, precision, max_price);
		
		// Create agents and auction
		MenezesAgent[] mene_agents = new MenezesAgent[no_agents];
		for (int i = 0; i<no_agents; i++)
			mene_agents[i] = new MenezesAgent(new MenezesValue(max_value, rng, decreasing), no_agents, i, mene_type);
		SeqAuction mene_auction = new SeqAuction(mene_agents, nth_price, no_goods);
		
		PP[0] = jcf.simulAllAgentsOnePP(mene_auction, no_initial_simulations,take_log,record_prices,false);
				
		// initiate agents for later bid comparison
		MenezesValue value = new MenezesValue(max_value, rng, decreasing);
		FullCondMDPAgent3 mdp_agent = new FullCondMDPAgent3(value, 1, preference, epsilon);

		// initiate tools to compare strategies
		double[] v = new double[no_for_cmp]; 		
		for (int i = 0; i < no_for_cmp; i++)
			v[i] = i*cmp_precision;
		double[][] strategy = new double[no_iterations][no_for_cmp];
		
		
		// 2) Wellman updates
		for (int it = 0; it < no_iterations; it++) {
			
			System.out.println("Wellman iteration = " + it);

			// 2.1) Do next iteration to generate a new PP
			FullCondMDPAgent3[] mdp_agents = new FullCondMDPAgent3[no_agents];
			for (int i = 0; i < no_agents; i++) {
				mdp_agents[i] = new FullCondMDPAgent3(new MenezesValue(max_value, rng, decreasing), i, preference, epsilon);
				mdp_agents[i].setCondJointDistribution(PP[it]);
			}
			SeqAuction updating_auction = new SeqAuction(mdp_agents, nth_price, no_goods);
			
			// generate a new pp
			PP[it+1] = jcf.simulAllAgentsOnePP(updating_auction, no_per_iteration,take_log,record_prices,false);
			
			// Record realized prices from seller's point of view
			if (record_prices == true){
				double[][] price_log = jcf.price_log;
				FileWriter fw_prices = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/Menezes/FullCondUpdates/" + no_agents + "_pricessold_" + it + ".csv");
				for (int i = 0; i < price_log.length; i++){
					for (int j = 0; j < price_log[i].length - 1; j ++)
						fw_prices.write(price_log[i][j] + ",");
					fw_prices.write(price_log[i][price_log[i].length-1] + "\n");
				}
				fw_prices.close();
			}

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
					strategy[it][i] = mdp_agent.getFirstRoundBid();
				}	
			}
		}

		// output each step's strategy
		if (print_strategy == true){
			FileWriter fw_strat = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/paper/updates/mene_" + no_agents + "_" + precision + "_" + no_iterations + ".csv");
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

			FileWriter fw_EUdiff = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/paper/updates/mene_EUdiff_" + no_agents + "_" + precision + " " + no_iterations + ".csv");
		
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

