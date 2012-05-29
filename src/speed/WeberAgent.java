package speed;

// bids according to (Weber '81)
public class WeberAgent extends SeqAgent {

	int agent_idx, no_agents, no_goods_won, no_goods, i;
	Value valuation;
	double x;
	boolean first_price;
	
	public WeberAgent(Value valuation, int agent_idx, int no_agents, int no_goods, boolean first_price) {
		super(agent_idx, valuation);
		this.agent_idx = agent_idx;
		this.valuation = valuation;
		this.no_agents = no_agents;
		this.no_goods = no_goods;
		this.first_price = first_price;
	}

	@Override
	public void reset(SeqAuction auction) {
		this.auction = auction;
		no_goods_won = 0;
	}

	@Override
	public double getBid(int good_id) {
		// Tally number of goods won (if haven't won anything yet)
		if (no_goods_won == 0 && good_id > 0) {
			if (auction.winner[good_id-1] == agent_idx)
				no_goods_won++;
		}

		// bid according to Weber (82)
		if (no_goods_won > 0)
			return 0.0;
		else {
			x = valuation.getValue(1);
			if (first_price == true)
				return x*(no_agents - no_goods)/(no_agents-good_id);
			else
				return x*(no_agents - no_goods)/(no_agents-(good_id+1));
//			System.out.println("x = " + x + ", round = " + (good_id+1) +", bid = " + x*(no_agents - no_goods)/(no_agents-(good_id+1)));
		}
}

}

