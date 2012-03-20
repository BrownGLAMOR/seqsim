package speed;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

// Test if strategy computed from MDP would deviate if initiated from a Menezes-generated Conditional price distribution
public class FullCondSCRandomStart {
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
		int no_iterations = 20;								// no. of Wellman updates 
		int no_per_iteration = 50000/no_agents;			// no. of games played in each Wellman iteration
		int no_for_comparison = 1000;						// no. of points for bid comparison
		int no_for_EUdiff = 1000;						// no. of points for EU comparison
				
		boolean decreasing = true;						// decreasing MV in Menezes valuation
		
		boolean take_log = false;						// record prices for agents
		boolean record_prices = false;					// record prices for seller
		
		boolean print_intermediary = true;				// compare bids b/w S(t) and S(0)
//		boolean print_intermediary_itself = true;		// compare bids b/w S(t) and S(t+1)
		boolean print_diff = false;						// compare EUs and output
		int price_lags = 3;								// how far away do we compare price distances? 
		
		int no_cases = 3;		// we run 4 starting points: uniform, Katzman, Menezes, Weber (only for n >= 3)

		// record all prices
		JointCondDistributionEmpirical[][] PP = new JointCondDistributionEmpirical[no_iterations+1][no_cases];		
		
		// 1)  Initiate PPs

		JointCondFactory jcf = new JointCondFactory(no_goods, precision, max_price);
		
		// Create agents and auctions
		KatzmanUniformAgent[] katz_agents = new KatzmanUniformAgent[no_agents];
		for (int i = 0; i<no_agents; i++)
			katz_agents[i] = new KatzmanUniformAgent(new KatzHLValue(no_agents-1, max_value, rng), no_agents, i);
		SeqAuction katz_auction = new SeqAuction(katz_agents, nth_price, no_goods);
		
		MenezesAgent[] mene_agents = new MenezesAgent[no_agents];
		for (int i = 0; i<no_agents; i++)
			mene_agents[i] = new MenezesAgent(new MenezesValue(max_value, rng, decreasing), no_agents, i);
		SeqAuction mene_auction = new SeqAuction(mene_agents, nth_price, no_goods);

//		WeberAgent[] weber_agents = new WeberAgent[no_agents];
//		for (int i = 0; i<no_agents; i++)
//			weber_agents[i] = new WeberAgent(new UnitValue(max_value, rng), i, no_agents, no_goods);
//		SeqAuction weber_auction = new SeqAuction(weber_agents, nth_price, no_goods);

		// Create PP
		System.out.println("Generating initial PP");
		PP[0][0] = jcf.makeUniform(take_log);
		PP[0][1] = jcf.simulAllAgentsOnePP(katz_auction, no_initial_simulations,take_log,record_prices,false);
		PP[0][2] = jcf.simulAllAgentsOnePP(mene_auction, no_initial_simulations,take_log,record_prices,false);
//		PP[0][3] = jcf.simulAllAgentsOnePP(weber_auction, no_initial_simulations,take_log,record_prices);
				
		// Output raw realized vectors
		if (take_log == true){
			for (int i = 0; i < no_cases; i++){
				FileWriter fw_p0 = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/randomstart/p0/" + no_agents + "_" + i + ".csv");
				PP[0][i].outputRaw(fw_p0);
				fw_p0.close();
			}
		}
		
		// initiate agents to compare bids
		Value value = new MenezesValue(max_value, rng, decreasing);
		MenezesAgent mene_agent = new MenezesAgent((MenezesValue) value, no_agents, 11);
		KatzmanUniformAgent katz_agent = new KatzmanUniformAgent(value, no_agents, 12);
		WeberAgent weber_agent = new WeberAgent(value, 13, no_agents, no_goods);
				
		// agents to update for each stream
		FullCondMDPAgent[] mdp_reporting_agents = new FullCondMDPAgent[no_cases];
		for (int i = 0; i < no_cases; i++)
			mdp_reporting_agents[i] = new FullCondMDPAgent(value, i);
		
		
		// 2) Wellman updates
		for (int it = 0; it < no_iterations; it++) {
			
			System.out.println("Wellman iteration = " + it);

			// 2.1) Compare: how are we bidding, comparing to Katzman and Menezes? (Weber does not bid in 2 rounds)
			if (print_intermediary == true) {
				FileWriter fw_comp1 = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/randomstart/compare/" + no_agents + "_" + it + ".csv");

				fw_comp1.write("getValue(1),getValue(2),katz first round bid,mene first round bid, weber first round bid, mdp s0, mdp s1, mdp s2, mdp s3\n");
	
				for (int i = 0; i < no_cases; i++)
					mdp_reporting_agents[i].setCondJointDistribution(PP[it][i]);
				
				for (int i = 0; i < no_for_comparison; i++) {

					value.reset();
					katz_agent.reset(null);
					mene_agent.reset(null);
					weber_agent.reset(null);
					
					for (int j = 0; j < no_cases; j++)
						mdp_reporting_agents[j].reset(null);
					fw_comp1.write(value.getValue(1) + "," + value.getValue(2) + "," + katz_agent.getFirstRoundBid() + "," + mene_agent.getFirstRoundBid() + "," + weber_agent.getBid(0)  + "," + mdp_reporting_agents[0].getFirstRoundBid() + "," + mdp_reporting_agents[1].getFirstRoundBid()+ "," + mdp_reporting_agents[2].getFirstRoundBid() + "\n");
//					fw_comp1.write(value.getValue(1) + "," + value.getValue(2) + "," + katz_agent.getFirstRoundBid() + "," + mene_agent.getFirstRoundBid() + "," + weber_agent.getBid(0)  + "," + mdp_reporting_agents[0].getFirstRoundBid() + "," + mdp_reporting_agents[1].getFirstRoundBid()+ "," + mdp_reporting_agents[2].getFirstRoundBid()+ "," + mdp_reporting_agents[3].getFirstRoundBid() + "\n");
				}	
				fw_comp1.close();
			}
			
			// 2.2) Do next iteration to generate a new PP
			FullCondMDPAgent[][] mdp_updating_agents = new FullCondMDPAgent[4][no_agents];
			for (int i = 0; i < no_cases; i++) {
				for (int j = 0; j < no_agents; j++) {
					mdp_updating_agents[i][j] = new FullCondMDPAgent(new MenezesValue(max_value, rng, decreasing), i);
					mdp_updating_agents[i][j].setCondJointDistribution(PP[it][i]);
				}
			}
			SeqAuction[] updating_auctions = new SeqAuction[no_cases];
			for (int i = 0; i < no_cases; i++)
				updating_auctions[i] = new SeqAuction(mdp_updating_agents[i], nth_price, no_goods);
			
			// generate new PPs
			for (int i = 0; i < no_cases; i++)
				PP[it+1][i] = jcf.simulAllAgentsOnePP(updating_auctions[i], no_per_iteration,take_log,record_prices,false);
			
//			// Record realized prices from seller's point of view
//			if (record_prices == true){
//				double[][] price_log = jcf.price_log;
//				FileWriter fw_prices = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/Menezes/FullCondUpdates/" + no_agents + "_pricessold_" + it + ".csv");
//				for (int i = 0; i < price_log.length; i++){
//					for (int j = 0; j < price_log[i].length - 1; j ++)
//						fw_prices.write(price_log[i][j] + ",");
//					fw_prices.write(price_log[i][price_log[i].length-1] + "\n");
//				}
//				fw_prices.close();
//			}

		}
		
		System.out.println("Done updating.");
		
		// Compute distances and output
		if (print_diff == true) {
		
		System.out.println("Computing price distances... ");
			
			EpsilonFactor ef = new EpsilonFactor(no_goods);
		
			for (int lag = 1; lag <= price_lags; lag++){
				FileWriter fw_diff = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/randomstart/EUdiff/" + no_agents + "_" + lag + ".csv");
				// always write in EU_diff[i], EU_stdev[i] fashion
				for (int it = 0; it < no_iterations - price_lags + 1; it++) {
					// calculate distances and output for all cases; each line correspond to an iteration
					for (int i = 0; i < no_cases - 1; i++){
						ef.jcdeDistance(rng, PP[it][i], PP[it+lag][i], new MenezesValue(max_value, rng, decreasing), no_for_EUdiff);
						fw_diff.write(ef.EU_diff + "," + ef.stdev_diff + ",");
					}
					ef.jcdeDistance(rng, PP[it][no_cases-1], PP[it+lag][no_cases-1], new MenezesValue(max_value, rng, decreasing), no_for_EUdiff);
					fw_diff.write(ef.EU_diff + "," + ef.stdev_diff + "\n");
				}
				fw_diff.close();
			}
		
//		int start = 4;	// second comparison
//		FileWriter fw_EUdiff2 = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/Menezes/FullCondUpdates/EUdiff_fromstart" + no_agents + "_" + no_iterations + ".csv");
//		fw_EUdiff2.write("EU diff w/ pp[0], stdev, EU diff w/ pp[" + start + "], stdev\n");
//		for (int it = 1; it < no_iterations; it++){
//			ef.jcdeDistance(rng, PP[0], PP[it], new MenezesValue(max_value, rng, decreasing), no_for_EUdiff);
//			ef_2.jcdeDistance(rng, PP[start], PP[it], new MenezesValue(max_value, rng, decreasing), no_for_EUdiff);
//
//			fw_EUdiff2.write(ef.EU_diff + "," + ef.stdev_diff + "," + ef_2.EU_diff + "," + ef_2.stdev_diff + "\n");
//			System.out.print(ef.EU_diff + "," + ef.stdev_diff + "," + ef_2.EU_diff + "," + ef_2.stdev_diff + "\n");
//		}
//		
//		fw_EUdiff2.close();
		
		}
		System.out.println("done done");

	}
}

