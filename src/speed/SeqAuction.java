package speed;

import java.util.Random;

public class SeqAuction {
	double nth_price;			// 1st or 2nd price
	int no_goods;				// number of goods to be auctioned
	int no_agents;				// number of agents playing
	
	int game_no = 0;			// for debugging
	
	// auction information; this is available to agents.
	double[] fp;				// [good_id] ==> first price in the auction
	double[] sp;				// [good_id] ==> second price in the auction
	// note that an agent with id "agent_id" can determine its HOB for good "good_id" using
	// the expression: (winner[good_id] == agent_id ? sp[good_id] : fp[good_id])
	// that is, if you are the winner the highest other bid is the second price of the auction. 
	// otherwise, the highest other bid is the first price of the auction.
	double[][] bids;			// [good_id][agent_id] ==> bid amount by agent_id on good_id
	double[] price;				// [good_id] ==> clearing_price (amount paid by winner)
	int[] winner;				// [good_id] ==> agent_id (winner of auction good_id)
	
	int[] no_goods_won;			// [agent_id] ==> total cumulative number of goods this agent has won
	double[] payment;			// [agent_id] ==> total cumulative payment for this agent
	double[] valuation;			// [agent_id] ==> current valuation for the agent
	double[] profit;			// [agent_id] ==> current profit for the agent
	
	// list of other agents
	private SeqAgent[] agents;
	
	// temporary list of winners. this is a memory variable so that
	// we need only instantiate once (performance optimization).
	private int[] tmp;
	
	// Random number generator
	private Random rng;
	
	// construct the agent with a set of agents.
	public SeqAuction(SeqAgent[] agents, double nth_price, int no_goods) {
		this.agents = agents;
		this.no_agents = agents.length;
		
		this.nth_price = nth_price;
		assert(nth_price == 1 || nth_price == 2);
		
		this.no_goods = no_goods;
		this.bids = new double[no_goods][agents.length];
		
		// do all of our memory allocations upfront.
		fp = new double[no_goods];
		sp = new double[no_goods];
		bids = new double[no_goods][no_agents];
		price = new double[no_goods];
		winner = new int[no_goods];
		
		no_goods_won = new int[no_agents];
		payment = new double[no_agents];
		valuation = new double[no_agents];
		profit = new double[no_agents];
		
		tmp = new int[no_agents];
		
		rng = new Random();
	}
	
	// play the auction. it is safe to re-play the auction as many times as one wishes.
	// the results are available in the public memory variables: bids, prices, hob, winner, etc.
	public void play(boolean quiet) {
		// reset our internal variables
		
		// commented out those which really DON'T need to be cleared so long as the
		// agent knows not to read array entries which havn't been populated yet.
		// For example, if the agent it being asked to bid in auction n, it should
		// only read information for auctions 0, ..., n-1
		/*
		 for (int i = 0; i<no_goods; i++) {
			fp[i] = 0.0;
			sp[i] = 0.0;
			price[i] = 0.0;
			winner[i] = 0;
			
			for (int j = 0; j<no_agents; j++) {
				bids[i][j] = 0.0;	
			}
		}
		*/

		for (int j = 0; j<no_agents; j++) {
			no_goods_won[j] = 0;
			payment[j] = 0.0;
			//valuation[j] = 0.0;
			//profit[j] = 0.0;
			//tmp[j] = 0;
		}
		
		// tell all the agents that we are playing a new game.
		for (SeqAgent a : agents)
			a.reset(this);
				
		// play the game, one good at a time.
		for (int i = 0; i<no_goods; i++) {
			int cnt = 0;
			double _fp = 0;
			double _sp = 0;
			
			for (int j = 0; j<agents.length; j++) {
				// ask each agent for its bid
				double bid = agents[j].getBid(i);
				assert(bid > 0);
				
				bids[i][j] = bid;
				
				// see if we have a new winner or new 2nd price?
				if (bid > _fp) {
					_sp = _fp;
					_fp = bid;
					
					tmp[0] = i;
					cnt = 1; // new highest bidder, force cnt to 1
				} else if (bid == _fp) {
					// additional winner. note that if agent 0 bids 0, this clause will activate. this is OK.
					// note that this causes sp to equal fp due to the tie.
					_sp = _fp;
					tmp[cnt++] = i;
				} else if (bid > _sp) {
					_sp = bid;
				}
			}
			
			// choose a random winner from the set of winners
			int winning_agent = tmp[rng.nextInt(cnt)];
			winner[i] = winning_agent;

			fp[i] = _fp;
			sp[i] = _sp;
			price[i] = nth_price == 1 ? _fp : _sp;

			// record that winner has a payment obligation & an additional item won
			// note that these are CUMULATIVE in the sense that they increase as the auction progresses.
			// an agent may therefore query these during the course of the auction.
			payment[winning_agent] += price[i];
			no_goods_won[winning_agent] += 1; 
			valuation[winning_agent] = agents[i].v.getValue(no_goods_won[winning_agent]);
			profit[winning_agent] = valuation[winning_agent] - payment[winning_agent];
			
			// note that we do not explicitly post results back to the agent.
			// if the agent desires to use auction information in its bidding
			// decision for the next round, it should inspect the member
			// variables of this auction (prices, bids, etc) during its getBid().			
		}
				
		// output debugging info
		if (!quiet) {
			System.out.println("GAME " + game_no);

			for (int i = 0; i<no_goods; i++) {					
				System.out.println("\tGood " + i + " [fp=" + fp[i] + ", sp=" + sp[i] + ", winner=" + winner[i] + ", price=" + price[i] + "]");

				/*System.out.print("\t\tBids=[");
				
				for (int j = 0; j<no_agents; j++)
					System.out.print(bids[i][j] + (j != no_agents-1 ? ", " : ""));
				
				System.out.println("");
				*/
				for (int j = 0; j<no_agents; j++)
					System.out.println("\t\tAgent " + j + " [bid=" + bids[i][j] + ", no_goods=" + no_goods_won[j] + ", payment=" + payment[j] + ", value=" + valuation[j] + ", profit=" + profit[j] + "]");
			}
			
			System.out.println("");
		}

		game_no++;
	}
}
