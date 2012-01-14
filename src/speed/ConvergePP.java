package speed;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

// Runs PP-updates, with one agent updating at a time
public class ConvergePP {
	public static void main(String[] args) throws IOException {
		Cache.init();		
		Random rng = new Random();
		
		// parameters
		double precision = 1.0;
		int max_iteration = 50;			// no. of PP-updates
		int no_simulations = 10000;		// no. of simulations per PP-update
		int nth_price = 2;
		int no_goods = 2, no_agents = 2, max_price = 10;
		
		JointFactory jf = new JointFactory(no_goods, precision, max_price);
		
		// to store joint distributions
		JointDistributionEmpirical pp_new[] = new JointDistributionEmpirical[no_agents];
		JointDistributionEmpirical pp_old[] = new JointDistributionEmpirical[no_agents];
		
		// 1) Initiate with a uniform price distribution. TODO: put this into a separate file? May need to call repeatedly in the future 
		System.out.println("Generating initial uniform distribution...");
		
		JointDistributionEmpirical uniform = jf.makeUniform();

		// Assign to each agent
		for (int i = 0; i<no_agents; i++)
			pp_new[i] = uniform;
		
		System.out.println("done.");
		
		// Set up agents & auction
		FullMDPNumGoodsSeqAgent[] agents = new FullMDPNumGoodsSeqAgent[no_agents];
		for (int i = 0; i<no_agents; i++) {
			agents[i] = new FullMDPNumGoodsSeqAgent(new DMUValue(no_goods, (double) max_price, rng), i);
			agents[i].setJointDistribution(pp_new[i]);
		}
		
		SeqAuction auction = new SeqAuction(agents, nth_price, no_goods);	

		// 2) Do price prediction (iterations)
		for (int iteration_idx = 0; iteration_idx < max_iteration; iteration_idx++) {
			for (int agent_idx = 0; agent_idx < no_agents; agent_idx++) {
				// Create a new price prediction for agent_idx.
				
				pp_old[agent_idx] = pp_new[agent_idx];
			
				// Setup a new distribution for the new round of simulations
				pp_new[agent_idx] = jf.simulOneAgent(auction, agent_idx, no_simulations);

				// Print KS statistic for this iteration
				System.out.print("iteration " + iteration_idx + ", agent " + agent_idx + "'s marginal KS's: \t");
				System.out.print("[");
				
				for (int l = 0; l < no_goods; l++) {
					double[] dold = pp_old[agent_idx].getMarginalDist(l);
					double[] dnew = pp_new[agent_idx].getMarginalDist(l);
					
					System.out.print(JointDistribution.getKSStatistic(dold, dnew) + " ");
				}
				
				System.out.println("], max price = " + pp_new[agent_idx].getWitnessedMaxPrice());
			}
		}
	}
}

