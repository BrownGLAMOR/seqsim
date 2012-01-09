package speed;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class TestSeqAuction {
	public static void main(String args[]) throws IOException {
		Random rng = new Random();
		
		int no_agents = 2;
		int no_goods = 4;
		double max_price = 10;
		double precision = 1;
		int no_simulations = 1000000;
		int nth_price = 2;
		
		// setup -- this is not time sensitive
		SeqAgent[] agents = new RandomSeqAgent[no_agents];
		for (int i = 0; i<no_agents; i++)
			agents[i] = new RandomSeqAgent(i, max_price, new DMUValue(no_goods, max_price, rng));
		
		SeqAuction auction = new SeqAuction(agents, nth_price, no_goods);
		
		FileWriter fw = new FileWriter("test.csv");
		JointDistributionEmpirical jde[] = new JointDistributionEmpirical[no_agents];
		
		for (int i = 0; i<no_agents; i++)
			jde[i] = new JointDistributionEmpirical(no_goods, precision, max_price);
		
		System.out.println("Playing " + no_simulations + " simulations...");
		
		// play lots of games -- this is the time sensitive part
		long start, finish, elapsed;
		
		start = System.currentTimeMillis();
		for (int i = 0; i<no_simulations; i++) {
			// play the auction
			auction.play(true, null);		// true=="quiet mode", null=="don't write to disk"
			//auction.play(true, fw);
			
			// do something with the results?
			for (int j = 0; j<no_agents; j++)
				jde[j].populate(auction.hob[j]);
		}
		
		finish = System.currentTimeMillis();
		elapsed = finish - start;

		System.out.println("Elapsed: " + elapsed + "ms.");
		System.out.println("Rate: " + (no_simulations / (elapsed/1000.0)) + " auctions/second.");
		System.out.println("done.");
		
		System.out.println("");
		
		System.out.println("Normalizing JDEs...");
		for (int j = 0; j<no_agents; j++)
			jde[j].normalize();
		System.out.println("done");
		
		// Debug
		jde[0].output();
	}
}
