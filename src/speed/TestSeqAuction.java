package speed;

public class TestSeqAuction {
	public static void main(String args[]) {
		int no_agents = 8;
		int nth_price = 2;
		int no_goods = 5;
		int no_simulations = 1000000;
		
		// setup -- this is not time sensitive
		SeqAgent[] agents = new RandomSeqAgent[no_agents];

		for (int i = 0; i<no_agents; i++)
			agents[i] = new RandomSeqAgent();
		
		SeqAuction auction = new SeqAuction(agents, nth_price, no_goods);
		
		System.out.print("Playing simulations...");
		
		// play lots of games -- this is the time sensitive part
		long start = System.currentTimeMillis();
		for (int i = 0; i<no_simulations; i++) {
			// play the auction
			auction.play(true);		// true=="quiet mode"
			
			// do something with the results?
		}
		long finish = System.currentTimeMillis();
		
		System.out.println("done.");
		
		long elapsed = finish - start;
		
		System.out.println("Elapsed: " + elapsed + "ms.");
		System.out.println("Rate: " + (no_simulations / (elapsed/1000.0)) + " auctions/second.");
	}
}
