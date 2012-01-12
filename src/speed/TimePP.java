package speed;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class TimePP {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// Init the Cache!
		Cache.init();
		
		Random rng = new Random();
		
		double precision = 1;
		int no_simulations = 10000;
//		int no_iterations = 10;
		int nth_price = 2;
		long start, finish, elapsed;
		
		for (int no_goods = 2; no_goods < 5; no_goods++) {
			for (int no_agents = 2; no_agents < 10; no_agents++) {
				for (int max_price = 5; max_price < 20; max_price++) { 

					System.out.print("no_goods = " + no_goods + ", no_agents = " + no_agents + ", max_price = " + max_price+", no_simulations = " + no_simulations);

					JointDistributionEmpirical pp_new[] = new JointDistributionEmpirical[no_agents];
					JointDistributionEmpirical pp_old[] = new JointDistributionEmpirical[no_agents];
				
					// Start with a uniform price distribution. TODO: put this into a separate file? May need to call repeatedly in the future 
					double[] prices = new double[max_price+1];
					for (int j = 0; j <= max_price; j++)
						prices[j] = (double) j;					
					
					for (int i = 0; i<no_agents; i++) {
						pp_old[i] = new JointDistributionEmpirical(no_goods, precision, max_price);
						for (double[] realized : Cache.getCartesianProduct(prices, no_goods))
							pp_old[i].populate(realized);
						pp_old[i].normalize();
					}

		
					// **** Do price prediction
					FullMDPNumGoodsSeqAgent[] mdp_agents = new FullMDPNumGoodsSeqAgent[no_agents];
					for (int i = 0; i<no_agents; i++)
						mdp_agents[i] = new FullMDPNumGoodsSeqAgent(new DMUValue(no_goods, max_price, rng), i);

//// Checking that rng takes a different value every time
//					System.out.println("rng = "+(double) rng);
					
					SeqAuction mdp_auction = new SeqAuction(mdp_agents, nth_price, no_goods);
					
//						System.out.println("Iteration " + i + ": Playing " + no_simulations + " MDP simulations...");

					for (int j = 0; j<no_agents; j++) {
						mdp_agents[j].setJointDistribution(pp_old[j]);
						pp_new[j] = new JointDistributionEmpirical(no_goods, precision, max_price);
					}
		
					start = System.currentTimeMillis();
					
					for (int j = 0; j<no_simulations; j++) {				
						// Cause each agent to take on a new valuation
						for (int k = 0; k<no_agents; k++)
							mdp_agents[k].v.reset();
						
						// Play the auction. This will call the agent's reset(), which will cause MDP to be recomputed.
						mdp_auction.play(true, null);		// true=="quiet mode", null=="don't write to disk"
						//auction.play(true, fw);
						
						// Add the result to the agent's JDE
						for (int k = 0; k<no_agents; k++)
							pp_new[k].populate(mdp_auction.hob[k]);
					}

					for (int k = 0; k<no_agents; k++)
						pp_new[k].normalize();
					
					finish = System.currentTimeMillis();
					elapsed = finish - start;
//					System.out.println(": " + (int) (no_simulations / (elapsed/1000.0)) + " auctions/second.");
					System.out.println("\t took: " + (int) elapsed + " ms.");
				}

			}
		}
	}
}
