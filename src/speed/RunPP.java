package speed;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class RunPP {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// Init the Cache!
		Cache.init();
		
		Random rng = new Random();
		
		int no_agents = 2;
		int no_goods = 2;
		double max_price = 10;
		double precision = 1;
		int no_simulations = 10000;
		int no_iterations = 10;
		int nth_price = 2;
		
		// **** setup -- this is not time sensitive
		SeqAgent[] ran_agents = new SeqAgent[no_agents];
		for (int i = 0; i<no_agents; i++)
			ran_agents[i] = new RandomSeqAgent(i, max_price, new DMUValue(no_goods, max_price, rng));

		SeqAuction ran_auction = new SeqAuction(ran_agents, nth_price, no_goods);
		
		JointDistributionEmpirical pp_new[] = new JointDistributionEmpirical[no_agents];
		JointDistributionEmpirical pp_old[] = new JointDistributionEmpirical[no_agents];
		
		for (int i = 0; i<no_agents; i++)
			pp_new[i] = new JointDistributionEmpirical(no_goods, precision, max_price);
		
		System.out.println("Initial: Playing " + no_simulations + " random simulations...");
		
		// **** Generate initial price prediction using random agent
		long start, finish, elapsed;
		
		start = System.currentTimeMillis();
		for (int i = 0; i<no_simulations; i++) {
			// Cause each agent to take on a new valuation
			for (int j = 0; j<no_agents; j++)
				ran_agents[j].v.reset();
			
			// Play the auction.
			ran_auction.play(true, null);		// true=="quiet mode", null=="don't write to disk"
			//auction.play(true, fw);
			
			// Add the result to the agent's JDE
			for (int j = 0; j<no_agents; j++)
				pp_new[j].populate(ran_auction.hob[j]);
		}
		
		finish = System.currentTimeMillis();
		elapsed = finish - start;

		System.out.println("Elapsed: " + elapsed + "ms.");
		System.out.println("Rate: " + (no_simulations / (elapsed/1000.0)) + " auctions/second.");
		System.out.println("done.");
		
		System.out.println("");
		
		for (int j = 0; j<no_agents; j++)
			pp_new[j].normalize();

		// **** Do price prediction
		SeqAgent[] mdp_agents = new SeqAgent[no_agents];
		FullMDPNumGoodsSeqAgent[] mdp_agents2 = new FullMDPNumGoodsSeqAgent[no_agents];
		
		for (int i = 0; i<no_agents; i++) {
			mdp_agents2[i] = new FullMDPNumGoodsSeqAgent(new DMUValue(no_goods, max_price, rng), i);
			mdp_agents[i] = mdp_agents2[i];
		}
		
		SeqAuction mdp_auction = new SeqAuction(mdp_agents, nth_price, no_goods);
		
		for (int i = 0; i<no_iterations; i++) {
			System.out.println("Iteration " + i + ": Playing " + no_simulations + " MDP simulations...");

			pp_old = pp_new;
			
			pp_new = new JointDistributionEmpirical[no_agents];

			for (int j = 0; j<no_agents; j++) {
				mdp_agents2[j].setJointDistribution(pp_old[j]);
				pp_new[j] = new JointDistributionEmpirical(no_goods, precision, max_price);
			}

			start = System.currentTimeMillis();
			for (int j = 0; j<no_simulations; j++) {				
				// Cause each agent to take on a new valuation
				for (int k = 0; k<no_agents; k++)
					mdp_agents2[k].v.reset();
				
				// Play the auction. This will call the agent's reset(), which will cause MDP to be recomputed.
				mdp_auction.play(true, null);		// true=="quiet mode", null=="don't write to disk"
				//auction.play(true, fw);
				
				// Add the result to the agent's JDE
				for (int k = 0; k<no_agents; k++)
					pp_new[k].populate(mdp_auction.hob[k]);
			}
			
			finish = System.currentTimeMillis();
			elapsed = finish - start;

			System.out.println("Elapsed: " + elapsed + "ms.");
			System.out.println("Rate: " + (no_simulations / (elapsed/1000.0)) + " auctions/second.");
			System.out.println("done.");
			
			System.out.println("");
			
			for (int k = 0; k<no_agents; k++)
				pp_new[k].normalize();
		}
	}
}
