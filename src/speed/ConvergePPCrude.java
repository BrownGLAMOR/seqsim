package speed;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

import legacy.DiscreteDistribution;
import legacy.DiscreteDistributionWellman;
import legacy.Histogram;

// Runs PP-updates, with all agents updating together
public class ConvergePPCrude {
	public static void main(String[] args) throws IOException {
		Cache.init();		
		Random rng = new Random();
		
		// parameters
		double precision = 1.0;
		int max_iteration = 50;		// no. of PP-updates
		int no_simulations = 10000;		// no. of simulations per PP-update
		int nth_price = 2;
		int no_goods = 2, no_agents = 2, max_price = 10;
		
		JointFactory jf = new JointFactory(no_goods, precision, max_price);
		
		// to store joint distributions
		JointDistributionEmpirical pp_new[] = new JointDistributionEmpirical[no_agents];
		JointDistributionEmpirical pp_old[];
		
		// 1) Initiate with a uniform price distribution. TODO: put this into a separate file? May need to call repeatedly in the future 
		System.out.println("Generating initial uniform distribution...");
		
		JointDistributionEmpirical uniform = jf.makeUniform();

		// Assign to each agent
		for (int i = 0; i<no_agents; i++)
			pp_new[i] = uniform;
		
		System.out.println("done");
		
		// Set up
		FullMDPNumGoodsSeqAgent[] agents = new FullMDPNumGoodsSeqAgent[no_agents];
		for (int i = 0; i<no_agents; i++) {
			Value v = new DMUValue(no_goods, (double) max_price, rng);
			agents[i] = new FullMDPNumGoodsSeqAgent(v, i);
		}
		
		SeqAuction auction = new SeqAuction(agents, nth_price, no_goods);	

		// 2) Do price prediction (iterations)
		for (int iteration = 0; iteration < max_iteration; iteration++) {
			pp_old = pp_new;
			
			for (int i = 0; i < no_agents; i++)
				agents[i].setJointDistribution(pp_old[i]);
			
			pp_new = jf.simulAllAgents(auction, no_simulations);

			// Print KS statistic 
			System.out.print("iteration " + iteration + ", agents' marginal KS's: \t");

			for (int i = 0; i < no_agents; i++){				
				System.out.print("[");
				
				for (int l = 0; l < no_goods; l++) {
					double[] dold = pp_old[i].getMarginalDist(l);
					double[] dnew = pp_new[i].getMarginalDist(l);
									
					System.out.print(JointDistribution.getKSStatistic(dold, dnew) + " ");				
				}
				
				System.out.print("]\t");
			}
			
			System.out.println();				
		}
	}
}

