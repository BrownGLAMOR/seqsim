package speed;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

// An agent who bids a power of his valuation in the first round; the order is given by user. 
// For example, a bidder with order 3 will bid x^3 in the first round, where x = valuation.getValue(1)

public class PolynomialAgent extends SeqAgent {

	int agent_idx;
	Value valuation;
	double x, order;
	
	public PolynomialAgent(Value valuation, int agent_idx, double order) {
		super(agent_idx, valuation);
		this.agent_idx = agent_idx;
		this.valuation = valuation;
//		this.x = valuation.getValue(1);
		this.order = order;
	}
	
	@Override
	public void reset(SeqAuction auction) {
		this.auction = auction;
	}
	
	@Override
	public double getBid(int good_id) {
		// Bid "truthfully" in the second round
		if (good_id == 1) {
			if (auction.winner[0] == agent_idx)
				return valuation.getValue(2) - valuation.getValue(1);
			else
				return valuation.getValue(1);
		}
		else
			return java.lang.Math.pow(valuation.getValue(1), order);
	}
	
	// Testing
	public static void main(String args[]) throws IOException {

		// general parameters
		double max_value = 1.0;
		int no_agents = 2;
		double order = 0.5;
		boolean decreasing = true;
		
		// create agent
		MenezesValue value = new MenezesValue(max_value, new Random(), decreasing);
		PolynomialAgent poly_agent = new PolynomialAgent(value, no_agents, order);

		// initiate memory to store strategy function
		double cmp_precision = 0.01;
		int no_for_cmp = (int) (1/cmp_precision) + 1;
		double[] v = new double[no_for_cmp]; 		
		for (int i = 0; i < no_for_cmp; i++)
			v[i] = i*cmp_precision;
		double[] strategy = new double[no_for_cmp];
		
		// Record strategy
		for (int i = 0; i < no_for_cmp; i++) {
			value.x = v[i];
			System.out.println(poly_agent.valuation.getValue(1) + " " + poly_agent.getBid(0) + " " + java.lang.Math.pow(poly_agent.valuation.getValue(1), order));
			strategy[i] = poly_agent.getBid(0);
		}
		
		// Print
		FileWriter fw = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/paper/updates/polytest_" + order + ".csv");
		for (int i = 0; i < strategy.length - 1; i++)
				fw.write(strategy[i] + ",");
			fw.write(strategy[strategy.length-1] + "\n");
		fw.close();
		
		System.out.println("done");

	}
}