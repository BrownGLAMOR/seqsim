package speed;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class ConvergePP {

	// Run PP to see if any convergence happens 
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// Init the Cache!
		Cache.init();
		
		Random rng = new Random();

		double precision = 1;
		int max_iterations = 100; 
		int no_simulations = 10000;		// samples per PP-update iteration
		int nth_price = 2;
		int no_goods = 2, no_agents = 2, max_price = 10;
		long start, finish, elapsed;

//		System.out.print("no_goods = " + no_goods + ", no_agents = " + no_agents + ", max_price = " + max_price+", no_simulations = " + no_simulations);

		JointDistributionEmpirical pp_new[] = new JointDistributionEmpirical[no_agents];
		JointDistributionEmpirical pp_old[] = new JointDistributionEmpirical[no_agents];
	
		// Start with a uniform price distribution. TODO: put this into a separate file? May need to call repeatedly in the future 
		final double[] prices = new double[max_price+1];
		for (int j = 0; j <= max_price; j++)
			prices[j] = (double) j;

		for (int k = 0; k<no_agents; k++) {
			pp_new[k] = new JointDistributionEmpirical(no_goods, precision, max_price);
			for (double[] realized : Cache.getCartesianProduct(prices, no_goods))
				pp_new[k].populate(realized);
			pp_new[k].normalize();
		}

		// initialize agents and auction
		FullMDPNumGoodsSeqAgent[] agents = new FullMDPNumGoodsSeqAgent[no_agents];
		for (int i = 0; i<no_agents; i++)
			agents[i] = new FullMDPNumGoodsSeqAgent(new DMUValue(no_goods, max_price, rng), i);
		SeqAuction mdp_auction = new SeqAuction(agents, nth_price, no_goods);

		for (int iteration = 0; iteration < max_iterations; iteration++) {
			
			pp_old = pp_new;
			pp_new = new JointDistributionEmpirical[no_agents];
			for (int k = 0; k<no_agents; k++) {
				agents[k].setJointDistribution(pp_old[k]);
				pp_new[k] = new JointDistributionEmpirical(no_goods, precision, max_price);
			}

			start = System.currentTimeMillis();

			for (int j = 0; j<no_simulations; j++) {				
				// Cause each agent to take on a new valuation
				for (int k = 0; k<no_agents; k++)
					agents[k].v.reset();
				
				// Play the auction. This will call the agent's reset(), which will cause MDP to be recomputed.
				mdp_auction.play(true, null);		// true=="quiet mode", null=="don't write to disk"
				//auction.play(true, fw);
				
				// Add the result to the agent's JDE
				for (int k = 0; k<no_agents; k++)
					pp_new[k].populate(mdp_auction.hob[k]);
			}

			finish = System.currentTimeMillis();
			elapsed = finish - start;
//			System.out.println("Iteration " + iteration + " took: \t" + (int) elapsed + " ms.");

			for (int k = 0; k<no_agents; k++)
				pp_new[k].normalize();			
		}
	}
}
