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
		
		// to store joint distributions
		JointDistributionEmpirical pp_new[] = new JointDistributionEmpirical[no_agents];
		JointDistributionEmpirical pp_old[] = new JointDistributionEmpirical[no_agents];
		
		// to store marginal distributions
		Histogram h_new[][] = new Histogram[no_agents][no_goods];
		Histogram h_old[][] = new Histogram[no_agents][no_goods];
		for (int i = 0; i < no_agents; i++) {
			for (int j =0; j < no_goods; j++)
				h_old[i][j] = new Histogram(precision);
		}
		
		DiscreteDistribution[][] pd_new = new DiscreteDistribution[no_agents][no_goods];
		DiscreteDistribution[][] pd_old = new DiscreteDistribution[no_agents][no_goods];

		// 1) Initiate with a uniform price distribution. TODO: put this into a separate file? May need to call repeatedly in the future 
		double[] prices = new double[max_price+1];
		for (int j = 0; j <= max_price; j++)
			prices[j] = (double) j;
		
		for (int i = 0; i<no_agents; i++) {
			pp_old[i] = new JointDistributionEmpirical(no_goods, precision, max_price);
			// enumerate over possible price vectors
			for (double[] realized : Cache.getCartesianProduct(prices, no_goods)) {
				pp_old[i].populate(realized);
				for (int j = 0; j < no_goods; j++) {
					h_old[i][j].add(realized[j]);
				}
			}
			// aggregate
			pp_old[i].normalize();
			for (int j = 0; j < no_goods; j++)
				pd_old[i][j] = new DiscreteDistributionWellman(h_old[i][j].getDiscreteDistribution(), precision);
		}

		// Set up
		FullMDPNumGoodsSeqAgent[] agents = new FullMDPNumGoodsSeqAgent[no_agents];
		for (int i = 0; i<no_agents; i++) {
			Value v = new DMUValue(no_goods, (double) max_price, rng);
			agents[i] = new FullMDPNumGoodsSeqAgent(v, i);
			agents[i].setJointDistribution(pp_old[i]);		// assign last update's price distribution
		}
		SeqAuction auction = new SeqAuction(agents, nth_price, no_goods);	

		// 2) Do price prediction (iterations)
		for (int iteration = 0; iteration < max_iteration; iteration++) {

			pp_new = new JointDistributionEmpirical[no_agents];
			h_new = new Histogram[no_agents][no_goods];
			pd_new = new DiscreteDistribution[no_agents][no_goods];

			for (int i = 0; i < no_agents; i++) {	
				pp_new[i] = new JointDistributionEmpirical(no_goods, precision, max_price);
				for (int j =0; j < no_goods; j++) {
					h_new[i][j] = new Histogram(precision);
				}
			}
				
			// > Simulations
			for (int j = 0; j<no_simulations; j++) {				
			// Cause each agent to take on a new valuation
				for (int k = 0; k<no_agents; k++)
					agents[k].v.reset();
			
				// Play the auction. This will call the agent's reset(), which will cause MDP to be recomputed.
				auction.play(true, null);		// true=="quiet mode", null=="don't write to disk"
						
				// Add results
				for (int i = 0; i < no_agents; i++){
						pp_new[i].populate(auction.hob[i]);
						for (int l = 0; l < no_goods; l++)
							h_new[i][l].add(auction.hob[i][l]);
				}
			}
					
			// normalize, and print KS statistic 
			System.out.print("iteration " + iteration + ", agents' marginal KS's: \t");
			for (int i = 0; i < no_agents; i++){
				pp_new[i].normalize();
				System.out.print("[");
				for (int l = 0; l < no_goods; l++) {
					pd_new[i][l] = new DiscreteDistributionWellman(h_new[i][l].getDiscreteDistribution(), precision);
					System.out.print(pd_new[i][l].getKSStatistic(pd_old[i][l])+" ");
				}
				System.out.print("]\t");
			}
			System.out.println();
			
				
			// assign new distribution
			pp_old = pp_new;
			pd_old = pd_new;
			for (int i = 0; i < no_agents; i++)
				agents[i].setJointDistribution(pp_new[i]);				
		}
	}
}

