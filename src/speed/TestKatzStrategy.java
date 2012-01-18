package speed;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Random;

import legacy.DiscreteDistribution;
import legacy.DiscreteDistributionWellman;
import legacy.Histogram;

// Test if strategy computed from MDP would deviate if initiated from a Katzman-generated price distribution
public class TestKatzStrategy {
	public static void main(String[] args) throws IOException {
		Cache.init();
		Random rng = new Random();
				
		double katz_precision = 0.01;
		double max_value = 10.0;

		double jf_precision = 0.1;
		double max_price = max_value;
		
		int no_goods = 2;
		int no_agents = 3;
		int nth_price = 2;
		int no_simulations = 1333333;		// run how many games to generate PP. this gets multiplied by no_agents.
		
		int max_iterations = 100000;

		FileWriter fw = new FileWriter("katz" + no_agents + ".csv");

		JointFactory jf = new JointFactory(no_goods, jf_precision, max_price);

		KatzmanUniformAgent[] katz_agents = new KatzmanUniformAgent[no_agents];
		for (int i = 0; i<no_agents; i++)
			katz_agents[i] = new KatzmanUniformAgent(new KatzHLValue(no_agents - 1, max_value, katz_precision, rng), i);
				
		// Create auction
		SeqAuction katz_auction = new SeqAuction(katz_agents, nth_price, no_goods);

		// Generate initial condition
		JointDistributionEmpirical pp;
		
		System.out.print("Generating initial PP from katzman agents...");
		pp = jf.simulAllAgentsOnePP(katz_auction, no_simulations);
		
		pp.output();

		System.out.println("done");
		
		KatzHLValue value = new KatzHLValue(no_agents - 1, max_value, katz_precision, rng);
		
		KatzmanUniformAgent katz_agent = new KatzmanUniformAgent(value, 0);

		FullMDPAgent2 mdp_agent = new FullMDPAgent2(value, 1);
		mdp_agent.setJointDistribution(pp);
		
		fw.write("katz_precision: " + katz_precision + "\n");
		fw.write("max_value (to katz valuation): " + max_value + "\n");
		fw.write("jf_precision: " + jf_precision + "\n");
		fw.write("max_price (to jde): " + max_price + "\n");
		fw.write("no_goods: " + no_goods + "\n");
		fw.write("no_agents: " + no_agents + "\n");
		fw.write("nth_price: " + nth_price + "\n");
		fw.write("no_simulations (to generate pp): " + no_simulations + " * no_agents = " + (no_simulations*no_agents) + "\n");
		fw.write("max_iterations (data points below): " + max_iterations + "\n");
		fw.write("\n");
		fw.write("getValue(1),getValue(2),katz first round bid,mdp first round bid\n");
		
		for (int iteration_idx = 0; iteration_idx < max_iterations; iteration_idx++) {			
			// Have agents create their bidding strategy using the provided valuation
			// (both agents share the SAME valuation)
			katz_agent.reset(null);
			mdp_agent.reset(null);
			
			fw.write(value.getValue(1) + "," + value.getValue(2) + "," + katz_agent.getFirstRoundBid() + "," + mdp_agent.getFirstRoundBid() + "\n");
			
			// Draw new valuation for the next round
			value.reset();
		}		
	}
}
