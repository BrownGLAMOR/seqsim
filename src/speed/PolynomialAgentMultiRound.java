package speed;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

// An agent who bids a power of marginal v in each round 

public class PolynomialAgentMultiRound extends SeqAgent {

	int agent_idx, no_goods, goods_won;
	Value valuation;
	double x;
	double order;
	
	public PolynomialAgentMultiRound(Value valuation, int agent_idx, int no_goods, double order) {
		super(agent_idx, valuation);
		this.agent_idx = agent_idx;
		this.valuation = valuation;
		this.order = order;		
		this.no_goods = no_goods;
	}
	
	@Override
	public void reset(SeqAuction auction) {
		this.auction = auction;
	}
	
	// Return bids
	public double getBid(int good_id, int goods_won){
		// Bid truthfully in the last round
		if (good_id == no_goods-1)
			return valuation.getValue(goods_won+1)-valuation.getValue(goods_won);
		// Bid a power of marginal value in earlier rounds
		else
			return Math.pow(valuation.getValue(goods_won+1)-valuation.getValue(goods_won),order);
	}
	
	@Override
	public double getBid(int good_id) {
		
		// Figure out number of things won
		goods_won = 0;
		for (int i = 0; i < good_id; i++){
			if (auction.winner[i] == agent_idx)
				goods_won++;
		}
		
		return getBid(good_id, goods_won);
	}
	
	// Testing
	public static void main(String args[]) throws IOException {

		// general parameters
		double max_value = 1.0;
		int no_goods = 3;
		double order = 1.0;
		
		boolean decreasing = true;
		Random rng = new Random();
		
		// create agent
		MenezesMultiroundValue v = new MenezesMultiroundValue(max_value, rng, decreasing);
		PolynomialAgentMultiRound agent = new PolynomialAgentMultiRound(v, 0, no_goods, order);

		// check if bidding is right
		v.reset();
		System.out.println("decreasing scheme: MV(0,1,2,3^rd goods) = [" + v.getValue(0) + "," + (v.getValue(1)-v.getValue(0)) + "," + (v.getValue(2)-v.getValue(1)) + "," + (v.getValue(3)-v.getValue(2)) + "]");
		System.out.println("marginal bids: getBid(0,0) = " + agent.getBid(0,0) + ", getBid(1,0) = " + agent.getBid(1,0) + ", getBid(1,1) = " + agent.getBid(1,1));
		System.out.println("getBid(2,0) = " + agent.getBid(2,0) + ", getBid(2,1) = " + agent.getBid(2,1) + ", getBid(2,2) = " + agent.getBid(2,2));
		
	}
}