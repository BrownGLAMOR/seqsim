package speed;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

// Test if strategy computed from MDP would deviate if initiated from a Katzman-generated Conditional price distribution
public class FullCondSC {
	public static void main(String[] args) throws IOException {
		Cache.init();
		Random rng = new Random();
		
		// Parameters
		double max_value = 1.0;
		double precision = 0.05;
		double max_price = max_value;
		
		int no_goods = 2;
		int no_agents = 6;
		int nth_price = 2;

		int no_initial_simulations = 10000/no_agents;	// generating initial PP		
		int no_iterations = 4;								// no. of Wellman updates 
		int no_per_iteration = 10000/no_agents;			// no. of games played in each Wellman iteration
		int no_for_comparison = 1000;						// no. of points for bid comparison
		
		boolean take_log = false;
		boolean print_intermediary = true;
		boolean print_end = false;
		
		// record descriptive statistics of utility
		double[] u;
		double[] u_mean = new double[no_iterations+1];
		double[] u_stdev = new double[no_iterations+1];
		
		JointCondDistributionEmpirical pp_new, pp_old, pp0;
		JointCondFactory jcf = new JointCondFactory(no_goods, precision, max_price);

		// 1)  Initiate PP from Katzman
		
			// Create agents
//		KatzmanUniformImpreciseAgent[] katz_agents = new KatzmanUniformImpreciseAgent[no_agents];
//		for (int i = 0; i<no_agents; i++)
//			katz_agents[i] = new KatzmanUniformImpreciseAgent(new KatzHLValue(no_agents-1, max_value, rng), i, precision);
		KatzmanUniformAgent[] katz_agents = new KatzmanUniformAgent[no_agents];
		for (int i = 0; i<no_agents; i++)
			katz_agents[i] = new KatzmanUniformAgent(new KatzHLValue(no_agents-1, max_value, rng), i);

		// Create auction
		SeqAuction katz_auction = new SeqAuction(katz_agents, nth_price, no_goods);
		System.out.println("Generating initial PP");
		pp0 = jcf.simulAllAgentsOnePP(katz_auction, no_initial_simulations,take_log);
		pp_new = pp0;
		u = jcf.utility;
		u_mean[0] = Statistics.mean(u);
		u_stdev[0] = Statistics.stdev(u)/java.lang.Math.sqrt((no_initial_simulations*no_agents));
		
			// Output raw realized vectors
		if (take_log == true){
//			FileWriter fw = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/Katzman/FullCondUpdates/initial" + no_agents + ".csv");
			FileWriter fw = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/test.csv");
			pp_new.outputRaw(fw);
			fw.close();
		}
		
		
		// initiate agents to compare bids
		KatzHLValue value = new KatzHLValue(no_agents-1, max_value, rng);		
		KatzmanUniformAgent katz_agent = new KatzmanUniformAgent(value, 0);
//		KatzmanUniformImpreciseAgent katz_agent = new KatzmanUniformImpreciseAgent(value, 0, precision);
		FullCondMDPAgent mdp_agent_old = new FullCondMDPAgent(value, 1);
		FullCondMDPAgent mdp_agent_new = new FullCondMDPAgent(value, 1);

		
		// 2) Wellman updates
		for (int it = 0; it < no_iterations; it++) {
			
			pp_old = pp_new;
			
			System.out.println("Wellman iteration = " + it);

			// 2.1) Compare: how different are we from original Katzman?
			if (print_intermediary == true) {
//				FileWriter fw_comp1 = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/Katzman/FullCondUpdates/discretized" + no_agents + "_" + it + ".csv");
				FileWriter fw_comp1 = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/test.csv");
				
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
	
				mdp_agent_old.setCondJointDistribution(pp_old);			// set the bid... 
				
				for (int i = 0; i < no_for_comparison; i++) {
					katz_agent.reset(null);
					mdp_agent_old.reset(null);				
					fw_comp1.write(value.getValue(1) + "," + value.getValue(2) + "," + katz_agent.getFirstRoundBid() + "," + mdp_agent_old.getFirstRoundBid() + "\n");
					value.reset();
				}
	
				fw_comp1.close();
			}
			
			// 2.2) Do next iteration to generate a new PP
			FullCondMDPAgent[] mdp_agents = new FullCondMDPAgent[no_agents];
			for (int i = 0; i < no_agents; i++) {
				mdp_agents[i] = new FullCondMDPAgent(new KatzHLValue(no_agents-1, max_value, rng), i);
				mdp_agents[i].setCondJointDistribution(pp_old);
			}
			SeqAuction updating_auction = new SeqAuction(mdp_agents, nth_price, no_goods);
			
			// generate a new pp
			pp_new = jcf.simulAllAgentsOnePP(updating_auction, no_per_iteration,take_log);
			u = jcf.utility;
			u_mean[it+1] = Statistics.mean(u);
			u_stdev[it+1] = Statistics.stdev(u)/java.lang.Math.sqrt((no_per_iteration*no_agents));

			// 2.3) Compare: how different are we from previous MDP?
			if (print_intermediary == true) {
				
//				FileWriter fw_comp2 = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/Katzman/FullCondUpdates/discretized" + no_agents + "_itself_" + it + ".csv");
				FileWriter fw_comp2 = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/test.csv");
				
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
				mdp_agent_new.setCondJointDistribution(pp_new);			// set the bid... 
				
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
			FileWriter fw_comp1 = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/Katzman/FullCondUpdates/" + no_agents + "er_end_" + no_iterations + ".csv");
			
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

			mdp_agent_old.setCondJointDistribution(pp_new);			// set the bid... 
			
			for (int i = 0; i < no_for_comparison; i++) {
				katz_agent.reset(null);
				mdp_agent_old.reset(null);				
				fw_comp1.write(value.getValue(1) + "," + value.getValue(2) + "," + katz_agent.getFirstRoundBid() + "," + mdp_agent_old.getFirstRoundBid() + "\n");
				value.reset();
			}

			fw_comp1.close();
		}
		
		
		
		System.out.println("done done");
		
		// output utility log
		for (int i = 0; i < no_iterations+1; i++)
			System.out.println("mean(u) = " + u_mean[i] + ", stdev(u) = " + u_stdev[i]);
	}
}

