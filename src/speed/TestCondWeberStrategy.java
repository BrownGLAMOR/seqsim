package speed;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

// Test if strategy computed from MDP would deviate if initiated from Weber-induced Conditional price distribution
public class TestCondWeberStrategy {
	public static void main(String[] args) throws IOException {
		Cache.init();
		Random rng = new Random();
				
		double max_value = 1.0;
		double jf_precision = 0.01;
		double max_price = max_value;
		
		int no_goods = 2;
		int no_agents = 3;
		int nth_price = 2;

		int no_simulations = 1000000/no_agents;		// run how many games to generate PP. this gets multiplied by no_agents later.
		int max_iterations = 1000;

		FileWriter fw = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/Weber/Weber_vs_fullCondMDP/rawer" + no_agents + ".csv");
		
		JointCondFactory jcf = new JointCondFactory(no_goods, jf_precision, max_price);

		// Create agents
		WeberAgent[] weber_agents = new WeberAgent[no_agents];
		for (int i = 0; i<no_agents; i++)
			weber_agents[i] = new WeberAgent(new UnitValue(max_value, rng), i, no_agents, no_goods);
//		KatzmanUniformAgent[] katz_agents = new KatzmanUniformAgent[no_agents];
//		for (int i = 0; i<no_agents; i++)
//			katz_agents[i] = new KatzmanUniformAgent(new KatzHLValue(no_agents-1, max_value, rng), i);
				
		// Create auction
		SeqAuction katz_auction = new SeqAuction(weber_agents, nth_price, no_goods);

		// Generate initial condition
		System.out.print("Generating initial PP from Weber agents...");
		JointCondDistributionEmpirical pp = jcf.simulAllAgentsOnePP(katz_auction, no_simulations);
		
		pp.outputNormalized();
		
		// Output raw realized vectors
		pp.outputRaw(fw);
		fw.close();
		
		FileWriter fw_play = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/Weber/Weber_vs_fullCondMDP/player" + no_agents + ".csv");

		System.out.println("done");
		System.out.println("Generating " + max_iterations + " first-round bids...");
		
//		KatzHLValue value = new KatzHLValue(no_agents-1, max_value, rng);
		UnitValue value = new UnitValue(max_value, rng);
		

		// initial agents for comparison
//		KatzmanUniformAgent katz_agent = new KatzmanUniformAgent(value, 0);
		WeberAgent weber_agent = new WeberAgent(value, 3, no_agents, no_goods);
		FullCondMDPAgent mdp_agent = new FullCondMDPAgent(value, 1);
		mdp_agent.setCondJointDistribution(pp);
		
		fw_play.write("max_value (to unit valuation): " + max_value + "\n");
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
//			katz_agent.reset(null);
			weber_agent.reset(null);
			mdp_agent.reset(null);
			
			fw_play.write(value.getValue(1) + "," + value.getValue(2) + "," + weber_agent.getBid(0) + "," + mdp_agent.getFirstRoundBid() + "\n");
			
			// Draw new valuation for the next round
			value.reset();
		}
		
		fw_play.close();
		
		System.out.println("done done");
	}
}