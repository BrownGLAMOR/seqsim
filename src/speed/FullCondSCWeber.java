package speed;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

// Test if strategy computed from MDP would deviate if initiated from a Weber-generated Conditional price distribution
public class FullCondSCWeber {
	public static void main(String[] args) throws IOException {
		Cache.init();
		Random rng = new Random();
		
		// preferences
		boolean first_price = false;
		int preference = 2;			// prefer mixing
		double epsilon = 0.00005;
		boolean discretize_value = false;
		double v_precision = 0.0001;

		// Parameters
		double order = 1.0;
		double max_value = 1.0;
		double precision = 0.05;
		double max_price = max_value;
		
		int no_goods = 2;
		int no_agents = 3;
		int nth_price = 2;

		int no_initial_simulations = 10000000/no_agents;	// generating initial PP		
		int no_iterations = 10;								// no. of Wellman updates 
		int no_per_iteration = 100000/no_agents;			// no. of games played in each Wellman iteration
		int no_for_comparison = 1000;						// no. of points for bid comparison
		int no_for_EUdiff = 10000;						// no. of points for EU comparison
				
		boolean take_log = false;						// take log of prices
		boolean print_intermediary = true;				// compare bids b/w S(t) and S(0)
		boolean print_diff = false;						// compare EUs and output
		
		// record all prices (to compute distances later)
		JointCondDistributionEmpirical[] PP = new JointCondDistributionEmpirical[no_iterations+1];
		

		// 1)  Initiate PP[0] from polynomial agents		
		JointCondFactory jcf = new JointCondFactory(no_goods, precision, max_price);
		// Create agents
		PolynomialAgent[] poly_agents = new PolynomialAgent[no_agents];
		for (int i = 0; i<no_agents; i++)
			poly_agents[i] = new PolynomialAgent(new UnitValue(max_value, rng), no_agents, order);
		SeqAuction auction0 = new SeqAuction(poly_agents, nth_price, no_goods);
		System.out.println("Generating initial PP");
		PP[0] = jcf.simulAllAgentsOnePP(auction0, no_initial_simulations,take_log,false,false);
		
		// initiate agents to compare bids
		UnitValue value = new UnitValue(max_value, rng);
		WeberAgent weber_agent = new WeberAgent(value, 0, no_agents, no_goods, first_price);
//		FullCondMDPAgent mdp_agent = new FullCondMDPAgent(value, 1);
		FullCondMDPAgent4 mdp_agent = new FullCondMDPAgent4(value, 1, preference, epsilon, discretize_value, v_precision);
		
		// 2) Wellman updates
		for (int it = 0; it < no_iterations; it++) {
			
			System.out.println("Wellman iteration = " + it);

			// 2.1) Compare: how different are we from original strategy?
			if (print_intermediary == true) {
				FileWriter fw_comp1 = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/Weber/FullCondUpdates/" + no_agents + "_" + it + ".csv");
	
				mdp_agent.setCondJointDistribution(PP[it]);			// set the bid... 
				
				for (int i = 0; i < no_for_comparison; i++) {
					value.reset();
					weber_agent.reset(null);
					mdp_agent.reset(null);				
					fw_comp1.write(value.getValue(1) + "," + value.getValue(2) + "," + weber_agent.getBid(0) + "," + mdp_agent.getFirstRoundBid() + "\n");
				}
	
				fw_comp1.close();
			}
			
			// 2.2) Do next iteration to generate a new PP
			FullCondMDPAgent4[] mdp_agents = new FullCondMDPAgent4[no_agents];
//			FullCondMDPAgent[] mdp_agents = new FullCondMDPAgent[no_agents];
			for (int i = 0; i < no_agents; i++) {
				mdp_agents[i] = new FullCondMDPAgent4(new UnitValue(max_value,rng), i, preference, epsilon, discretize_value, v_precision);
//				mdp_agents[i] = new FullCondMDPAgent(new UnitValue(max_value, rng), i);
				mdp_agents[i].setCondJointDistribution(PP[it]);
			}
			SeqAuction updating_auction = new SeqAuction(mdp_agents, nth_price, no_goods);
			
			// generate a new pp
			PP[it+1] = jcf.simulAllAgentsOnePP(updating_auction, no_per_iteration,take_log,false,false);
		}
				

		
		// Compute distances and output
		if (print_diff == true) {
		
		EpsilonFactor ef = new EpsilonFactor(no_goods);
		EpsilonFactor ef_2 = new EpsilonFactor(no_goods);	// 2 lags
		
		FileWriter fw_EUdiff = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/Weber/FullCondUpdates/EUdiff" + no_agents + "_" + no_iterations + ".csv");
		fw_EUdiff.write("EU diff1, EU stdev1, EU diff2, EU stdev2, EU high, EU low \n");
		int lag = 2;
		for (int it = 0; it < no_iterations-lag+1; it++){
			ef.jcdeDistance(rng, PP[it+1], PP[it], new UnitValue(max_value, rng), no_for_EUdiff);
			ef_2.jcdeDistance(rng, PP[it+2], PP[it], new UnitValue(max_value, rng), no_for_EUdiff);

			fw_EUdiff.write(ef.EU_diff + "," + ef.stdev_diff + "," + ef_2.EU_diff + "," + ef_2.stdev_diff + "," + ef.EU_P + "," + ef.EU_Q + "," + ef_2.EU_P + "," + ef_2.EU_Q + "\n");
			System.out.print(ef.EU_diff + "," + ef.stdev_diff + "," + ef_2.EU_diff + "," + ef_2.stdev_diff + "," + ef.EU_P + "," + ef.EU_Q + "," + ef_2.EU_P + "," + ef_2.EU_Q + "\n");
		}
		
		fw_EUdiff.close();
		
		
		int start = 5;	// second comparison
		FileWriter fw_EUdiff2 = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/Weber/FullCondUpdates/EUdiff_fromstart" + no_agents + "_" + no_iterations + ".csv");
		fw_EUdiff2.write("EU diff w/ pp[0], stdev, EU diff w/ pp[" + start + "], stdev\n");
		for (int it = 1; it < no_iterations; it++){
			ef.jcdeDistance(rng, PP[0], PP[it], new UnitValue(max_value, rng), no_for_EUdiff);
			ef_2.jcdeDistance(rng, PP[start], PP[it], new UnitValue(max_value, rng), no_for_EUdiff);

			fw_EUdiff2.write(ef.EU_diff + "," + ef.stdev_diff + "," + ef_2.EU_diff + "," + ef_2.stdev_diff + "\n");
			System.out.print(ef.EU_diff + "," + ef.stdev_diff + "," + ef_2.EU_diff + "," + ef_2.stdev_diff + "\n");
		}
		
		fw_EUdiff2.close();
		
		System.out.println("done done");
		
		}
	}
}

