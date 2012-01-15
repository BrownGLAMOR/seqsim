package speed;

import java.util.Random;
import java.util.Set;

// Test if FullMDPAgent2 works as expected. For now, affirmative.  
public class TestFullMDPAgent {
	public static void main(String args[]) {
		// Init cache. Must be done once at program start.
		Cache.init();

		double[] prices;
		double precision = 1.0;
		int i, no_goods = 2, max_price = 3, no_price_samples = 100;
		Set<double[]> genPrices;
		
		Random rng = new Random();
		
				Value v = new SimpleValue(no_goods);						// Create and print valuation
//				System.out.println("marginal values = [" + v.getValue(1) + ", " + (v.getValue(2)-v.getValue(1)) + "]");
				
//				// Create uniform distribution
//				JointDistributionEmpirical jde = new JointDistributionEmpirical(no_goods, precision, max_price);			
//				prices = new double[(int) (max_price/precision) + 1];
//				for (i = 0; i < prices.length; i++)
//					prices[i] = i*precision;
//				genPrices = CartesianProduct.generate(prices, no_goods);
//				
//				for (double[] realized : genPrices)
//					jde.populate(realized);
//				jde.normalize();

				// Create random distribution
				JointDistributionEmpirical jde = new JointDistributionEmpirical(no_goods, precision, max_price);			
				prices = new double[(int) (max_price/precision) + 1];
				for (i = 0; i < prices.length; i++)
					prices[i] = i*precision;

				double[] realized = new double[no_goods];
				for (i = 0; i < no_price_samples; i++) {
					for (int j = 0; j < no_goods; j++)
						realized[j] = prices[rng.nextInt(prices.length)];
					jde.populate(realized);
				}
				jde.normalize();

				// Test if MDP calculate is right (things printed within computeFullMDP)
				FullMDPAgent2 agent = new FullMDPAgent2(v, 0);
				agent.setJointDistribution(jde);
				agent.computeFullMDP();

	}
	
}