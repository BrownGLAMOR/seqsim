package speed;

import java.util.Set;

import legacy.CartesianProduct;

// Let's figure out how long it takes to do a full MDP calculation 

public class TimeFullMDP {
	public static void main(String args[]) {

		SeqAgent agent;
		double[] prices;
		double precision = 1.0;
		int no_simulations = 20, i;
		Set<double[]> genPrices;
		
		for (int no_goods = 2; no_goods <= 5; no_goods++){
			for (double max_price = 2; max_price <= 20; max_price++) {
				System.out.print("no_goods = " + no_goods + ", no_possible_prices = " + (int)(max_price+1));
				Value valuation = new SimpleValue(no_goods);						// Create valuation				

				// Create uniform distribution
				JointDistributionEmpirical jde = new JointDistributionEmpirical(no_goods, precision, max_price);			
				prices = new double[(int) max_price + 1];
				for (i = 0; i < prices.length; i++)
					prices[i] = i;
				genPrices = CartesianProduct.generate(prices, no_goods);
				
				for (double[] realized : genPrices)
					jde.populate(realized);
				jde.normalize();

				// play lots of games -- this is the time sensitive part
				long start = System.currentTimeMillis();
				for (i = 0; i<no_simulations; i++) {
					// initialize agent (MDP calculated in the mean time)
					agent = new FullMDPSeqAgent(jde, valuation, 0);
				}
				long finish = System.currentTimeMillis();
				long elapsed = finish - start;
				System.out.println(", speed = " + (int) (elapsed/no_simulations) +" ms/auction");
			}
			System.out.println();
		}
	}
	
	}
