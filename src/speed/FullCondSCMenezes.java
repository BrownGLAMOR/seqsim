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

		int no_initial_simulations = 1000000/no_agents;	// generating initial PP		
		int no_iterations = 10;								// no. of Wellman updates 
		int no_per_iteration = 100000/no_agents;			// no. of games played in each Wellman iteration
		int no_for_comparison = 1000;						// no. of points for bid comparison
		int no_for_EUdiff = 10000;						// no. of points for EU comparison
				
		boolean take_log = false;						// take log of prices
		boolean print_intermediary = false;				// compare S(t) and S(0), from Katzman
		boolean print_intermediary_itself = false;		// compare S(t) and S(t+1)
		boolean print_end = false;						// compare S(T) and S(0)
		boolean print_diff = true;						// compare EUs and output
		
		// record all prices (to compute distances later)
		JointCondDistributionEmpirical[] PP = new JointCondDistributionEmpirical[no_iterations+1];
		
		// record descriptive statistics of utility
		double[] u;
		double[] u_mean = new double[no_iterations+1];
		double[] u_stdev = new double[no_iterations+1];
		
		// record PP differences
		JointCondFactory jcf = new JointCondFactory(no_goods, precision, max_price);

		// 1)  Initiate PP from Katzman
		
			// Create agents
		MenezesAgent[] mene_agents = new MenezesAgent[no_agents];
		for (int i = 0; i<no_agents; i++)
			mene_agents[i] = new MenezesAgent(new MenezesValue(max_value, rng), no_agents, i);

		// Create auction
		SeqAuction mene_auction = new SeqAuction(mene_agents, nth_price, no_goods);
		System.out.println("Generating initial PP");
		PP[0] = jcf.simulAllAgentsOnePP(mene_auction, no_initial_simulations,take_log);
		
		u = jcf.utility;
		u_mean[0] = Statistics.mean(u);
		u_stdev[0] = Statistics.stdev(u)/java.lang.Math.sqrt((no_initial_simulations*no_agents));
		
//			// Output raw realized vectors
//		if (take_log == true){
////			FileWriter fw = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/Katzman/FullCondUpdates/initial" + no_agents + ".csv");
//			FileWriter fw = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/test.csv");
//			pp_new.outputRaw(fw);
//			fw.close();
//		}
		
		
		// initiate agents to compare bids
		MenezesValue value = new MenezesValue(max_value, rng);
		MenezesAgent mene_agent = new MenezesAgent(value, no_agents, 0);
		FullCondMDPAgent mdp_agent_old = new FullCondMDPAgent(value, 1);
		FullCondMDPAgent mdp_agent_new = new FullCondMDPAgent(value, 1);

		
		// 2) Wellman updates
		for (int it = 0; it < no_iterations; it++) {
			
			System.out.println("Wellman iteration = " + it);

			// 2.1) Compare: how different are we from original strategy?
			if (print_intermediary == true) {
				FileWriter fw_comp1 = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/Menezes/FullCondUpdates/" + no_agents + "_" + it + ".csv");
				
				fw_comp1.write("max_value (to katz valuation): " + max_value + "\n");
				fw_comp1.write("precision: " + precision + "\n");
				fw_comp1.write("max_price (to jde): " + max_price + "\n");
				fw_comp1.write("no_goods: " + no_goods + "\n");
				fw_comp1.write("no_agents: " + no_agents + "\n");
				fw_comp1.write("nth_price: " + nth_price + "\n");
				fw_comp1.write("no_simulations (to generate initial pp): " + no_initial_simulations + " * no_agents = " + (no_initial_simulations*no_agents) + "\n");
				fw_comp1.write("no_per_iteration = " + no_per_iteration + "\n");
				fw_comp1.write("\n");
				fw_comp1.write("getValue(1),getValue(2),katz first round bid,mdp first round bid\n");
	
				mdp_agent_old.setCondJointDistribution(PP[it]);			// set the bid... 
				
				for (int i = 0; i < no_for_comparison; i++) {
					mene_agent.reset(null);
					mdp_agent_old.reset(null);				
					fw_comp1.write(value.getValue(1) + "," + value.getValue(2) + "," + mene_agent.getFirstRoundBid() + "," + mdp_agent_old.getFirstRoundBid() + "\n");
					value.reset();
				}
	
				fw_comp1.close();
			}
			
			// 2.2) Do next iteration to generate a new PP
			FullCondMDPAgent[] mdp_agents = new FullCondMDPAgent[no_agents];
			for (int i = 0; i < no_agents; i++) {
				mdp_agents[i] = new FullCondMDPAgent(new KatzHLValue(no_agents-1, max_value, rng), i);
				mdp_agents[i].setCondJointDistribution(PP[it]);
			}
			SeqAuction updating_auction = new SeqAuction(mdp_agents, nth_price, no_goods);
			
			// generate a new pp
			PP[it+1] = jcf.simulAllAgentsOnePP(updating_auction, no_per_iteration,take_log);
			u = jcf.utility;
			u_mean[it+1] = Statistics.mean(u);
			u_stdev[it+1] = Statistics.stdev(u)/java.lang.Math.sqrt((no_per_iteration*no_agents));

			// 2.3) Compare: how different are we from previous MDP?
			if (print_intermediary_itself == true) {
				
				FileWriter fw_comp2 = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/Menezes/FullCondUpdates/" + no_agents + "_itself_" + it + ".csv");
//				FileWriter fw_comp2 = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/test.csv");
				
				fw_comp2.write("max_value (to katz valuation): " + max_value + "\n");
				fw_comp2.write("precision: " + precision + "\n");
				fw_comp2.write("max_price (to jde): " + max_price + "\n");
				fw_comp2.write("no_goods: " + no_goods + "\n");
				fw_comp2.write("no_agents: " + no_agents + "\n");
				fw_comp2.write("nth_price: " + nth_price + "\n");
				fw_comp2.write("no_simulations (to generate initial pp): " + no_initial_simulations + " * no_agents = " + (no_initial_simulations*no_agents) + "\n");
				fw_comp2.write("no_per_iteration = " + no_per_iteration + "\n");
				fw_comp2.write("\n");
				fw_comp2.write("getValue(1),getValue(2),old mdp first round bid,new mdp first round bid\n");
	
					// initiate agents to compare bids
				mdp_agent_new.setCondJointDistribution(PP[it+1]);			// set the bid... 
				
				for (int i = 0; i < no_for_comparison; i++) {
					mdp_agent_old.reset(null);
					mdp_agent_new.reset(null);
					fw_comp2.write(value.getValue(1) + "," + value.getValue(2) + "," + mdp_agent_old.getFirstRoundBid() + "," + mdp_agent_new.getFirstRoundBid() + "\n");
					value.reset();
				}
	
				fw_comp2.close();
				
			}

		}
				

		if (print_end == true) {
			FileWriter fw_comp1 = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/Menezes/FullCondUpdates/" + no_agents + "_end_" + no_iterations + ".csv");
			
			fw_comp1.write("max_value (to katz valuation): " + max_value + "\n");
			fw_comp1.write("precision: " + precision + "\n");
			fw_comp1.write("max_price (to jde): " + max_price + "\n");
			fw_comp1.write("no_goods: " + no_goods + "\n");
			fw_comp1.write("no_agents: " + no_agents + "\n");
			fw_comp1.write("nth_price: " + nth_price + "\n");
			fw_comp1.write("no_simulations (to generate initial pp): " + no_initial_simulations + " * no_agents = " + (no_initial_simulations*no_agents) + "\n");
			fw_comp1.write("no_per_iteration = " + no_per_iteration + "\n");
			fw_comp1.write("\n");
			fw_comp1.write("getValue(1),getValue(2),katz first round bid,mdp first round bid\n");

			mdp_agent_old.setCondJointDistribution(PP[0]);			// set the bid... 
			
			for (int i = 0; i < no_for_comparison; i++) {
				mene_agent.reset(null);
				mdp_agent_old.reset(null);				
				fw_comp1.write(value.getValue(1) + "," + value.getValue(2) + "," + mene_agent.getFirstRoundBid() + "," + mdp_agent_old.getFirstRoundBid() + "\n");
				value.reset();
			}

			fw_comp1.close();
		}
		

		// output utility log
//		for (int i = 0; i < no_iterations+1; i++)
//			System.out.println("mean(u) = " + u_mean[i] + ", stdev(u) = " + u_stdev[i]);
		
		// Compute distances and output
		if (print_diff == true) {
		
//		double[] EU_diff = new double[no_iterations];		// to store PP distances
//		double[] EU_high = new double[no_iterations];
//		double[] EU_low = new double[no_iterations];
//		double[] EU_stdev = new double[no_iterations];		// to store stdev of PPs
		EpsilonFactor ef = new EpsilonFactor(no_goods);
		EpsilonFactor ef_2 = new EpsilonFactor(no_goods);	// 2 lags
		
		FileWriter fw_EUdiff = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/Menezes/FullCondUpdates/EUdiffer" + no_agents + "_" + no_iterations + ".csv");
		fw_EUdiff.write("EU diff1, EU stdev1, EU diff2, EU stdev2, EU high, EU low \n");
		int lag = 2;
		for (int it = 0; it < no_iterations-lag+1; it++){
			ef.jcdeDistance(rng, PP[it+1], PP[it], new KatzHLValue(no_agents-1, max_price, rng), no_for_EUdiff);
			ef_2.jcdeDistance(rng, PP[it+2], PP[it], new KatzHLValue(no_agents-1, max_price, rng), no_for_EUdiff);

			fw_EUdiff.write(ef.EU_diff + "," + ef.stdev_diff + "," + ef_2.EU_diff + "," + ef_2.stdev_diff + "," + ef.EU_P + "," + ef.EU_Q + "," + ef_2.EU_P + "," + ef_2.EU_Q + "\n");
			System.out.print(ef.EU_diff + "," + ef.stdev_diff + "," + ef_2.EU_diff + "," + ef_2.stdev_diff + "," + ef.EU_P + "," + ef.EU_Q + "," + ef_2.EU_P + "," + ef_2.EU_Q + "\n");
		}
		
		fw_EUdiff.close();
		
		System.out.println("done done");
		
		}
	}
}

