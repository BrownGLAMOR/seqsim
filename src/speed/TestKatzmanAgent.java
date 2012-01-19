package speed;

import java.io.IOException;

import legacy.DiscreteDistribution;
import legacy.DiscreteDistributionWellman;
import legacy.Histogram;

// Test to see if KatzmanAgent works as expected. Affirmative. 
public class TestKatzmanAgent {
	public static void main(String args[]) throws IOException {		
		
		// create distribution F
		double precision = 0.5;
		double v_max = 10.0;
		int no_goods = 2;
		int N = 2;
		
		Histogram h = new Histogram(precision);
		for (int i = 1; i*precision <= v_max; i++)
			h.add(i*precision);
		DiscreteDistribution F = new DiscreteDistributionWellman(h.getDiscreteDistribution(), precision);
		
		// Create Agent & assign valuation
		KatzmanAgent[] agent = new KatzmanAgent[1];
		agent[0] = new KatzmanAgent(new SimpleValue(2), 0);
		agent[0].setParameters(F, N);
		
		// Create auction
		SeqAuction auction = new SeqAuction(agent, 2, no_goods);
		
		auction.play(false, null);
		
//		for (int good_id = 0; good_id < 2; good_id++)
//			System.out.println("round " + good_id + ", Katzman bids " + agent.getBid(good_id));

	}
}
