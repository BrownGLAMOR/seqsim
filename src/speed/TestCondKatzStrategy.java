package speed;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

// Test if strategy computed from MDP would deviate if initiated from a Katzman-generated Conditional price distribution
public class TestCondKatzStrategy {
	public static void main(String[] args) throws IOException {
		Cache.init();
		Random rng = new Random();

		// tie breaking preferences
		double epsilon = 0.000001;
		int preference = -1;

		double max_value = 1.0;
		double jf_precision = 0.1;
		double max_price = max_value;
		
		int no_goods = 2;
		int no_agents = 4;
		int nth_price = 2;

		int no_simulations = 100000000/no_agents;		// run how many games to generate PP. this gets multiplied by no_agents later.		
		int max_iterations = 10000;

		boolean take_log = false;
		
		JointCondFactory jcf = new JointCondFactory(no_goods, jf_precision, max_price);

		// Create agents
		KatzmanUniformAgent[] katz_agents = new KatzmanUniformAgent[no_agents];
		for (int i = 0; i<no_agents; i++)
			katz_agents[i] = new KatzmanUniformAgent(new KatzHLValue(no_agents-1, max_value, rng), no_agents, i);
				
		// Create auction
		SeqAuction katz_auction = new SeqAuction(katz_agents, nth_price, no_goods);

		// Generate initial condition
		System.out.print("Generating initial PP from katzman agents...");
		JointCondDistributionEmpirical pp = jcf.simulAllAgentsOnePP(katz_auction, no_simulations,take_log,false,false);
		pp.outputNormalized();
		
		// Output raw realized vectors
		if (take_log == true){
			FileWriter fw = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/Katzman/katz_vs_fullCondMDP/raw" + no_agents + ".csv");
			pp.outputRaw(fw);
			fw.close();
		}
		

		System.out.println("done");		
		System.out.println("Generating " + max_iterations + " first-round bids...");
		
		KatzHLValue value = new KatzHLValue(no_agents-1, max_value, rng);
		
		// initial agents for comparison
		KatzmanUniformAgent katz_agent = new KatzmanUniformAgent(value, no_agents, 0);
//		FullCondMDPAgent mdp_agent = new FullCondMDPAgent(value, 1);
		FullCondMDPAgent3 mdp_agent = new FullCondMDPAgent3(value, 1, preference, epsilon);	// XXX: change things here
		mdp_agent.setCondJointDistribution(pp);
		
//		FileWriter fw_play = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/Katzman/katz_vs_fullCondMDP/" + no_agents + "_" + jf_precision + ".csv");
		FileWriter fw_play = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/paper/fixed_pt/katz_" + no_agents + "_" + jf_precision + ".csv");
		
		fw_play.write("max_value (to katz valuation): " + max_value + "\n");
		fw_play.write("jf_precision: " + jf_precision + "\n");
		fw_play.write("max_price (to jde): " + max_price + "\n");
		fw_play.write("no_goods: " + no_goods + "\n");
		fw_play.write("no_agents: " + no_agents + "\n");
		fw_play.write("nth_price: " + nth_price + "\n");
		fw_play.write("no_simulations (to generate pp): " + no_simulations + " * no_agents = " + (no_simulations*no_agents) + "\n");
		fw_play.write("max_iterations (data points below): " + max_iterations + "\n");
		fw_play.write("\n");
		fw_play.write("getValue(1),getValue(2),katz first round bid,mdp first round bid\n");
		
		for (int iteration_idx = 0; iteration_idx < max_iterations; iteration_idx++) {			
			// Have agents create their bidding strategy using the provided valuation
			// (both agents share the SAME valuation)
			katz_agent.reset(null);
			mdp_agent.reset(null);
			
			fw_play.write(value.getValue(1) + "," + value.getValue(2) + "," + katz_agent.getFirstRoundBid() + "," + mdp_agent.getFirstRoundBid() + "\n");
//			System.out.print(value.getValue(1) + "," + value.getValue(2) + "," + katz_agent.getFirstRoundBid() + "," + mdp_agent.getFirstRoundBid() + "\n");
			
			// Draw new valuation for the next round
			value.reset();
		}
		
		fw_play.close();
		
		System.out.println("done done");
	}
}
