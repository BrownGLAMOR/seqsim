package speed;

import java.io.IOException;
import java.util.Random;

// Test to see if UnitValue.java and WeberAgent.java works as expected. Affirmative.  
public class TestWeberAgent {
	public static void main(String args[]) throws IOException {		
		
		double max_value = 10.0;
		int no_goods = 2;
		int no_agents = 5;
		Random rng = new Random();

		// Test MenezesValue.java ... affirmative
		UnitValue v = new UnitValue(max_value, rng);
		System.out.println("UnitValue, v(0) = " + v.getValue(0) + ", v(1) = "+ v.getValue(1) + ", v(2) = " + v.getValue(2));
		

		// Create Agent & assign valuation
		WeberAgent[] agent = new WeberAgent[no_agents];
		agent[0] = new WeberAgent(v, 1, no_agents, no_goods);
		
		// Create auction
		SeqAuction auction = new SeqAuction(agent, 2, no_goods);
		agent[0].reset(auction);

		// 
		for (int good_id = 0; good_id < no_goods; good_id++)
			System.out.println("round " + good_id + ", Weber bids " + agent[0].getBid(good_id));		
		auction.winner[0] = 1;
		System.out.println("if wins first round, Weber bids " + agent[0].getBid(1) + " in round 1.");

	}
}
