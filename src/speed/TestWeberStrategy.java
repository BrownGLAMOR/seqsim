package speed;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

// Test if strategy computed from MDP would deviate if initiated from the Weber-generated equilibrium
public class TestWeberStrategy {
	public static void main(String[] args) throws IOException {
		Cache.init();
		Random rng = new Random();
				
		double max_value = 1.0;
		double jf_precision = 0.01;
		double max_price = max_value;
		
		int no_goods = 2;
		int no_agents = 10;
		int nth_price = 2;

		int no_simulations = 1000000/no_agents;		// run how many games to generate PP. this gets multiplied by no_agents later.		
		int max_iterations = 10000;

//		FileWriter fw_pp = new FileWriter("./weber/weber" + no_agents + "_log.csv");
//		FileWriter fw_bid = new FileWriter("./weber/weber" + no_agents + ".csv");

		FileWriter fw_pp = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/Weber/Weber_vs_fullMDP/weber" + no_agents + "_log.csv");
		FileWriter fw_bid = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/Weber/Weber_vs_fullMDP/weber" + no_agents + ".csv");


		
		JointFactory jf = new JointFactory(no_goods, jf_precision, max_price);

		// Create agents
		WeberAgent[] weber_agents = new WeberAgent[no_agents];
		for (int i = 0; i<no_agents; i++)
			weber_agents[i] = new WeberAgent(new UnitValue(max_value, rng), i, no_agents, no_goods);
		
		// Create auction
		SeqAuction weber_auction = new SeqAuction(weber_agents, nth_price, no_goods);

		// Generate initial condition
		JointDistributionEmpirical pp;
		
		System.out.print("Generating initial PP from Weber agents...");
		pp = jf.simulAllAgentsOnePP(weber_auction, no_simulations);
		
		pp.outputNormalized();
		
		// Some parameters
		fw_pp.write("max_value (to unit value): " + max_value + "\n");
		fw_pp.write("jf_precision: " + jf_precision + "\n");
		fw_pp.write("max_price (to jde): " + max_price + "\n");
		fw_pp.write("no_goods: " + no_goods + "\n");
		fw_pp.write("no_agents: " + no_agents + "\n");
		fw_pp.write("nth_price: " + nth_price + "\n");
		fw_pp.write("no_simulations (to generate pp): " + no_simulations + " * no_agents = " + (no_simulations*no_agents) + "\n");
		fw_pp.write("max_iterations (data points below): " + max_iterations + "\n");
		fw_pp.write("\n");
		fw_pp.write("getValue(1),getValue(2),weber first round bid,mdp first round bid\n");

		
		fw_pp.write("all realized price vectors in pp:\n");
		fw_pp.write("---------------------------------\n");
		pp.outputRaw(fw_pp);

		System.out.println("done");
		
		System.out.println("Generating " + max_iterations + " first-round bids...");
		
		UnitValue value = new UnitValue(max_value, rng);
		
		WeberAgent weber_agent = new WeberAgent(value, 3, no_agents, no_goods);

		FullMDPAgent2 mdp_agent = new FullMDPAgent2(value, 1);
		mdp_agent.setJointDistribution(pp);
		
		for (int iteration_idx = 0; iteration_idx < max_iterations; iteration_idx++) {			
			// Have agents create their bidding strategy using the provided valuation
			// (both agents share the SAME valuation)
			weber_agent.reset(null);
			mdp_agent.reset(null);
			
			fw_bid.write(value.getValue(1) + "," + value.getValue(2) + "," + weber_agent.getBid(0) + "," + mdp_agent.getFirstRoundBid() + "\n");
			
			// Draw new valuation for the next round
			value.reset();
		}
		
		System.out.println("done done");
	}
}
