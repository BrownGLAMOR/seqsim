package speed;

import java.io.IOException;
import java.util.Random;

import legacy.DiscreteDistribution;
import legacy.DiscreteDistributionWellman;
import legacy.Histogram;

// Test to see if MenezesValue.java and MenezesAgent.java works as expected. 
public class TestMenezesAgent {
	public static void main(String args[]) throws IOException {		
		
		double precision = 0.1;
		double max_value = 10.0;
		int no_goods = 2;
		int no_agents = 3;
		Random rng = new Random();

		// Test MenezesValue.java ... affirmative
		MenezesValue v = new MenezesValue(max_value, precision, rng);
		System.out.println("MenezesValue, x = " + v.getValue(1) + ", delta(x) = " + v.getValue(2) + ", delta(x) - x = " + (v.getValue(2) - v.getValue(1)));
		
		// create distribution F		
		Histogram h = new Histogram(precision);
		for (int i = 1; i*precision <= max_value; i++)
			h.add(i*precision);
		DiscreteDistribution F = new DiscreteDistributionWellman(h.getDiscreteDistribution(), precision);
		
//		System.out.print("F = ");
//		for (int i = 0; i <= max_value/precision; i++)
//			System.out.print(F.getCDF(i*precision, 0.0) + " ");
		
		// Create Agent & assign valuation
		MenezesAgent[] agent = new MenezesAgent[1];
		agent[0] = new MenezesAgent(v, 1);
		agent[0].setParameters(F, max_value, no_agents);
		
		// Create auction
		SeqAuction auction = new SeqAuction(agent, 2, no_goods);
		agent[0].reset(auction);
		
		for (int good_id = 0; good_id < 2; good_id++)
			System.out.println("round " + good_id + ", Menezes bids " + agent[0].getBid(good_id));		
		auction.winner[0] = 1;
		System.out.println("if wins first round, Menezes bids " + agent[0].getBid(1) + " in round 1.");

	}
}
