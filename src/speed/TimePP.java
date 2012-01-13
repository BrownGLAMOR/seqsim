package speed;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

import legacy.DiscreteDistribution;
import legacy.DiscreteDistributionWellman;
import legacy.Histogram;

public class TimePP {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// Init the Cache!
		Cache.init();
		
		Random rng = new Random();
		
		// update parameters
		double precision = 1.0;
		int no_simulations = 10000;
		int max_iteration = 100;
		int nth_price = 2;
		int no_goods = 2, no_agents = 2, max_price = 10;
		

		// to store joint distributions
		JointDistributionEmpirical pp_new[] = new JointDistributionEmpirical[no_agents];
		JointDistributionEmpirical pp_old[] = new JointDistributionEmpirical[no_agents];
		
		// to store marginal distributions
		Histogram h_new[][] = new Histogram[no_agents][no_goods];
		Histogram h_old[][] = new Histogram[no_agents][no_goods];
		for (int i = 0; i < no_agents; i++) {
			for (int j =0; j < no_goods; j++) { 
				h_new[i][j] = new Histogram(precision);
				h_old[i][j] = new Histogram(precision);
			}
		}
		DiscreteDistribution[][] pd_new = new DiscreteDistribution[no_agents][no_goods];
		DiscreteDistribution[][] pd_old = new DiscreteDistribution[no_agents][no_goods];
		
		// 1) Initiate with a uniform price distribution. TODO: put this into a separate file? May need to call repeatedly in the future 
		double[] prices = new double[max_price+1];
		for (int j = 0; j <= max_price; j++)
			prices[j] = (double) j;					
		
		for (int i = 0; i<no_agents; i++) {
			pp_new[i] = new JointDistributionEmpirical(no_goods, precision, max_price);
			// enumerate over possible price vectors
			for (double[] realized : Cache.getCartesianProduct(prices, no_goods)) {
				pp_new[i].populate(realized);
				for (int j = 0; j < no_goods; j++) {
					h_new[i][j].add(realized[j]);
				}
			}
			// aggregate
			pp_new[i].normalize();
			for (int j = 0; j < no_goods; j++)
				pd_new[i][j] = new DiscreteDistributionWellman(h_new[i][j].getDiscreteDistribution(), precision);
		}


		// 2) Do price prediction (iterations)
		for (int iteration = 0; iteration < max_iteration; iteration++) {

			// copy old distributions, and initiate new distributions
			pp_old = pp_new;
			pd_old = pd_new;
			
			pp_new = new JointDistributionEmpirical[no_agents];
			h_new = new Histogram[no_agents][no_goods];
			pd_new = new DiscreteDistribution[no_agents][no_goods];
			for (int i = 0; i < no_agents; i++) {
				for (int j =0; j < no_goods; j++) 
					h_new[i][j] = new Histogram(precision);
			}

			// Set up
			FullMDPNumGoodsSeqAgent[] agents = new FullMDPNumGoodsSeqAgent[no_agents];
			for (int i = 0; i<no_agents; i++) {
				Value v = new DMUValue(no_goods, (double) max_price, rng);
				agents[i] = new FullMDPNumGoodsSeqAgent(v, i);
			}

			for (int i = 0; i <no_agents; i++) {
				agents[i].setJointDistribution(pp_old[i]);
				pp_new[i] = new JointDistributionEmpirical(no_goods, precision, max_price);
			}
			
			SeqAuction auction = new SeqAuction(agents, nth_price, no_goods);	
	
	//					start = System.currentTimeMillis();
			
			for (int j = 0; j<no_simulations; j++) {				
				// Cause each agent to take on a new valuation
				for (int k = 0; k<no_agents; k++)
					agents[k].v.reset();
				
				// Play the auction. This will call the agent's reset(), which will cause MDP to be recomputed.
				auction.play(true, null);		// true=="quiet mode", null=="don't write to disk"
				//auction.play(true, fw);
				
				// Add the result
				for (int k = 0; k<no_agents; k++) {
					pp_new[k].populate(auction.hob[k]);
					for (int l = 0; l < no_goods; l++) {
						h_new[k][l].add(auction.hob[k][l]);
//						System.out.println("agent "+k+", good "+l+", add hob "+auction.hob[k][l]);
					}
				}
			}
	
			// normalize
			for (int k = 0; k<no_agents; k++) {
				pp_new[k].normalize();
				for (int l = 0; l < no_goods; l++)
					pd_new[k][l] = new DiscreteDistributionWellman(h_new[k][l].getDiscreteDistribution(), precision);
			}
			
			// print KS statistic
			System.out.print("iteration " + iteration + ", marginal KS's: \t");
			for (int k = 0; k < no_agents; k++){
				System.out.print("[");
				for (int l = 0; l < no_goods; l++)
					System.out.print(pd_new[k][l].getKSStatistic(pd_old[k][l])+" ");
				System.out.print("]\t");
			}
			System.out.println();
		}
	}
}

