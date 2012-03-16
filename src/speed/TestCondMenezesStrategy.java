package speed;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

// Test if strategy computed from MDP would deviate if initiated from Weber-induced Conditional price distribution
public class TestCondMenezesStrategy {
	public static void main(String[] args) throws IOException {
		Cache.init();
		Random rng = new Random();
		
		double max_value = 1.0;
		double jf_precision = 0.02;
		double max_price = max_value;
		
		int no_goods = 2;
		int no_agents = 2;
		int nth_price = 2;

		boolean output_pp = false;
		boolean decreasing = true;
		int no_simulations = 10000000/no_agents;		// run how many games to generate PP. this gets multiplied by no_agents later.
		int max_iterations = 100000;
		
		JointCondFactory jcf = new JointCondFactory(no_goods, jf_precision, max_price);

		// Create agents
		MenezesAgent[] menezes_agents = new MenezesAgent[no_agents];
		for (int i = 0; i<no_agents; i++) {
			menezes_agents[i] = new MenezesAgent(new MenezesValue(max_value, rng, decreasing), no_agents, i);
		}
				
		// Create auction
		SeqAuction menezes_auction = new SeqAuction(menezes_agents, nth_price, no_goods);

		// Generate initial condition
		System.out.print("Generating initial PP from Menezes agents...");
		JointCondDistributionEmpirical pp = jcf.simulAllAgentsOnePP(menezes_auction, no_simulations, output_pp, false);
		pp.outputNormalized();
		
		// Output raw realized vectors
		if (output_pp == true) {
			FileWriter fw = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/Menezes/Menezes_vs_fullCondMDP/raw" + no_agents + ".csv");
//			FileWriter fw = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/Menezes/Menezes_vs_fullCondMDP/increasing_raw" + no_agents + ".csv");
			pp.outputRaw(fw);
			fw.close();
		}
		
		FileWriter fw_play = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/Menezes/Menezes_vs_fullCondMDP/play" + no_agents + ".csv");
//		FileWriter fw_play = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/Menezes/Menezes_vs_fullCondMDP/precision_" + jf_precision + "play" + no_agents + ".csv");
//		FileWriter fw_play = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/Menezes/Menezes_vs_fullCondMDP/increasing_play" + no_agents + ".csv");

		System.out.println("done");
		System.out.println("Generating " + max_iterations + " first-round bids...");
		
		MenezesValue value = new MenezesValue(max_value, rng, decreasing);

		// initial agents for comparison
		MenezesAgent menezes_agent = new MenezesAgent(value, no_agents, 3);
		
		FullCondMDPAgent mdp_agent = new FullCondMDPAgent(value, 1);
		mdp_agent.setCondJointDistribution(pp);
		
		fw_play.write("max_value (to menezes valuation): " + max_value + "\n");
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
			menezes_agent.reset(null);
			mdp_agent.reset(null);
			
			fw_play.write(value.getValue(1) + "," + value.getValue(2) + "," + menezes_agent.getBid(0) + "," + mdp_agent.getFirstRoundBid() + "\n");
			
			// Draw new valuation for the next round
			value.reset();
		}
		
		fw_play.close();
		
		System.out.println("done done");
	}
}
