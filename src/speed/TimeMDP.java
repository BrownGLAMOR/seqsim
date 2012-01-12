package speed;

import java.io.IOException;
import legacy.DiscreteDistribution;
import legacy.DiscreteDistributionWellman;
import legacy.Histogram;

// Let's make sure that MDPNmuGoodsSeqAgent is working well, and figure out how fast it works 

public class TimeMDP {
	public static void main(String args[]) throws IOException {

		double precision = 1.0;
		int no_simulations = 10000, i, no_goods = 2, max_price;

// Test to see that MDPNumGoodsAgent.java works as expected. Affirmative.
		/* 
		// Initialize agents
		MDPSeqAgent[] agents = new MDPSeqAgent[no_agents];
		
		agents[0] = new MDPSeqAgent(new SimpleValue(no_goods), 0);
		agents[1] = new MDPSeqAgent(new SimpleValue(no_goods), 1);

//		for (i = 0; i<no_agents; i++)
//			agents[i] = new MDPSeqAgent(new SimpleValue(no_goods), i);
				
		// Initialize auction
		SeqAuction auction = new SeqAuction(agents, 2, no_goods);
		
		// Create price distribution
		DiscreteDistribution[] pd = new DiscreteDistribution[no_goods];			
		
		Histogram h1 = new Histogram(1);
		Histogram h2 = new Histogram(1);
		h1.add(1);
		h1.add(2);
		h2.add(1);
		h2.add(2);

		pd[0] = new DiscreteDistributionWellman(h1.getDiscreteDistribution(), precision);
		pd[1] = new DiscreteDistributionWellman(h2.getDiscreteDistribution(), precision);
		
		// Calculate MDP
		for (i = 0; i<no_agents; i++)
			agents[i].setJointDistribution(pd);
			
		// play auction
		auction.play(false, null);
		*/

		for (no_goods = 2; no_goods <= 5; no_goods++){
			for (max_price = 2; max_price <= 10; max_price++) {

				System.out.print("no_goods = " + no_goods + ", no_possible_prices = " + (int)(max_price+1));
				Value valuation = new SimpleValue(no_goods);						// Create valuation				

				// Create price distribution
				DiscreteDistribution[] pd = new DiscreteDistribution[no_goods];			
				Histogram h = new Histogram(1);
				h.add(max_price);
				for (i = 0; i < no_goods; i++)
					pd[i] = new DiscreteDistributionWellman(h.getDiscreteDistribution(), precision);

				MDPSeqAgent[] agents = new MDPSeqAgent[1];
				agents[0] = new MDPSeqAgent(valuation, 0);
//				MDPSeqAgent agent = new MDPSeqAgent(valuation, 0);
				SeqAuction auction = new SeqAuction(agents, 2, no_goods);
				
				// Calculate MDP -- this is the time sensitive part
				long start = System.currentTimeMillis();
				for (i = 0; i<no_simulations; i++) {
					agents[0].setJointDistribution(pd);
					agents[0].reset(auction);
				}
				long finish = System.currentTimeMillis();
				long elapsed = finish - start;
				System.out.println(", MDPSeqAgent speed = " + (elapsed/(double)no_simulations) +" ms/MDP calculation");
			}
			System.out.println();
		}
		
	}	
}
