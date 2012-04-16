package speed;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

// An agent who bids a power of his getValue(1) in each round; the power is given by user for each round 
// For example, in 3 round auction, user specify order = double[] {0.5, 1.0, 2.0}

public class PolynomialAgentMultiRound extends SeqAgent {

	int agent_idx, no_goods;
	UnitValue valuation;
	double x;
	double[] order;
	
	public PolynomialAgentMultiRound(UnitValue valuation, int agent_idx, double[] order) {
		super(agent_idx, valuation);
		this.agent_idx = agent_idx;
		this.valuation = valuation;
		this.order = order;
		
		this.no_goods = order.length;
	}
	
	@Override
	public void reset(SeqAuction auction) {
		this.auction = auction;
	}
	
	@Override
	public double getBid(int good_id) {
		// Bid "truthfully" in the second round
		
		// if won something already, stop playing
		boolean still_playing = true;
		for (int i = 0; i < good_id; i++){
			if (auction.winner[i] == agent_idx)
				still_playing = false;
		}
		
		if (still_playing == true){
			this.x = valuation.getValue(1);
			return java.lang.Math.pow(x,order[good_id]);
		}else{
			return 0.0;
		}
	}
	
	// Testing
	public static void main(String args[]) throws IOException {

		// general parameters
		double max_value = 1.0;
		int no_agents = 2;
		int no_goods = 3;
		double[] order = {0.5, 1.0, 2.0};
		
		// create agent
		PolynomialAgentMultiRound poly_agent = new PolynomialAgentMultiRound(new UnitValue(max_value, new Random()), no_agents, order);

		// initiate memory to store strategy function
		double cmp_precision = 0.2;
		int no_for_cmp = (int) (1/cmp_precision) + 1;
		double[] v = new double[no_for_cmp]; 		
		for (int i = 0; i < no_for_cmp; i++)
			v[i] = i*cmp_precision;
		
		// Record strategy
		for (int i = 0; i < no_for_cmp; i++) {
			poly_agent.valuation.x = v[i];
			System.out.println(poly_agent.valuation.getValue(1) + " " + poly_agent.getBid(0) + " " + poly_agent.getBid(1) + " "+ poly_agent.getBid(2));
		}
		
		
		System.out.println("done");

	}
}