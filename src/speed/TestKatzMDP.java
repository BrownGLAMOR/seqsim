package speed;

import java.io.IOException;
import java.util.Random;

import legacy.DiscreteDistribution;
import legacy.DiscreteDistributionWellman;
import legacy.Histogram;

public class TestKatzMDP {
	public static void main(String[] args) throws IOException {
		Cache.init();
		Random rng = new Random();
		
		double precision = 0.5;
		double max_price = 10.0;
		int no_goods = 2;
		int no_agents = 2;
		int nth_price = 2;
		int no_simulations = 5000;
		int max_iterations = 10;
		
		JointFactory jf = new JointFactory(no_goods, precision, max_price);

		// Create a uniform distribution F
		Histogram h = new Histogram(precision);
		for (int i = 1; i*precision <= max_price; i++)
			h.add(i*precision);
		DiscreteDistribution F = new DiscreteDistributionWellman(h.getDiscreteDistribution(), precision);
		
		// Create agents
		KatzmanAgent[] katz_agents = new KatzmanAgent[no_agents];
		
		for (int i = 0; i<no_agents; i++) {
			katz_agents[i] = new KatzmanAgent(new DMUValue(no_goods, max_price, rng), i);
			katz_agents[i].setParameters(F, no_agents);
		}
		
		// Create auction
		SeqAuction katz_auction = new SeqAuction(katz_agents, nth_price, no_goods);
		
		// Generate initial condition
		JointDistributionEmpirical pp_new[];
		JointDistributionEmpirical pp_old[] = new JointDistributionEmpirical[no_agents];
		
		System.out.print("Generating initial PP from katzman agents...");
		pp_new = jf.simulAllAgents(katz_auction, no_simulations);
		System.out.println("done");
		
		// Do price-prediction
		FullMDPNumGoodsSeqAgent[] agents = new FullMDPNumGoodsSeqAgent[no_agents];
		for (int i = 0; i<no_agents; i++) {
			agents[i] = new FullMDPNumGoodsSeqAgent(new DMUValue(no_goods, max_price, rng), i);
			agents[i].setJointDistribution(pp_new[i]);
		}
		
		SeqAuction auction = new SeqAuction(agents, nth_price, no_goods);
		
		// 2) Do price prediction (iterations) -- update one agent at a time
		for (int iteration_idx = 0; iteration_idx < max_iterations; iteration_idx++) {
			for (int agent_idx = 0; agent_idx < no_agents; agent_idx++) {
				// Create a new price prediction for agent_idx.
				pp_old[agent_idx] = pp_new[agent_idx];
				
				// Setup a new distribution for the new round of simulations
				long start = System.currentTimeMillis();
				pp_new[agent_idx] = jf.simulOneAgent(auction, agent_idx, no_simulations);
				long elapsed = System.currentTimeMillis() - start;
				
				// Print KS statistic for this iteration
				System.out.print("[" + elapsed + " ms] iteration " + iteration_idx + ", agent " + agent_idx + "'s marginal KS's: \t");
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
