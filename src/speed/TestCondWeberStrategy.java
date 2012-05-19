package speed;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

// Let's try testing Weber on real updates and see if things change
public class TestCondWeberStrategy {
	public static void main(String[] args) throws IOException {
		Cache.init();
		Random rng = new Random();

		// tie breaking preferences
		double epsilon = 0.000001;
		int preference = 0;

		double max_value = 1.0;
		double precision = 0.02;
		
		int no_goods = 2;
		int no_agents = 3;
		int nth_price = 2;

		int no_simulations = 10000000/no_agents;		// run how many games to generate PP. this gets multiplied by no_agents later.
		int max_iterations = 1000;
		
		FileWriter fw_play = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/paper/june1st/uniform/realtest_weber.csv");
		
		JointCondFactory jcf = new JointCondFactory(no_goods, precision, max_value);

		// Create agents
		WeberAgent[] weber_agents = new WeberAgent[no_agents];
		for (int i = 0; i<no_agents; i++)
			weber_agents[i] = new WeberAgent(new UnitValue(max_value, rng), i, no_agents, no_goods, false);
				
		// Create auction
		SeqAuction weber_auction = new SeqAuction(weber_agents, nth_price, no_goods);

		// Generate initial condition
		System.out.print("Generating initial PP from Weber agents...");
//		JointCondDistributionEmpirical pp = jcf.simulAllAgentsOnePP(weber_auction, no_simulations, false, false, false);
		JointCondDistributionEmpirical pp = jcf.simulAllAgentsOneRealPP(weber_auction, no_simulations, false, false, false);
		pp.outputNormalized();
		
		System.out.println("done");
		System.out.println("Generating " + max_iterations + " first-round bids...");
		
		// initial agents for comparison
		UnitValue value = new UnitValue(max_value, rng);
		WeberAgent weber_agent = new WeberAgent(value, 3, no_agents, no_goods, false);
		FullCondMDPAgent3 mdp_agent = new FullCondMDPAgent3(value, 1, preference, epsilon);
//		FullCondMDPAgent4 mdp_agent = new FullCondMDPAgent4(value, 1, preference, epsilon, false, 0.01);
		mdp_agent.setCondJointDistribution(pp);
		
		for (int iteration_idx = 0; iteration_idx < max_iterations; iteration_idx++) {			
			// Have agents create their bidding strategy using the provided valuation
			// (both agents share the SAME valuation)
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
