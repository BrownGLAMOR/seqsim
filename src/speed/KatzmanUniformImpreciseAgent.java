package speed;

import java.util.Random;

// The same as KatzmanUniformAgent.java, only it bids according to 
public class KatzmanUniformImpreciseAgent extends SeqAgent {
	Random rng;

	int agent_idx;
	int no_goods_won;
	double precision;
	
	KatzHLValue v;
	
	public KatzmanUniformImpreciseAgent(KatzHLValue v, int agent_idx, double precision) {
		super(agent_idx, v);
		this.agent_idx = agent_idx;
		this.v = v;
		this.precision = precision;
	}

	// Calculate the first Bid
	public double calculateFirstBid() {		
		return JointCondDistribution.bin((1.0- (1.0/(2.0*v.N)) )*v.H,precision)*precision;		// Theoretical formula for calculating bids 
	}
	
	@Override
	public void reset(SeqAuction auction) {
		this.auction = auction;
		no_goods_won = 0;
	}
	
	@Override
	public double getBid(int good_id) {
		// Bid "truthfully" in the second round
		if (good_id == 1) {
			if (auction.winner[0] == agent_idx)
				no_goods_won++;
			
			return (v.getValue(no_goods_won+1) - v.getValue(no_goods_won));
		}
		else
			return calculateFirstBid();	// bid a specific amount in the first round (according to Katzman paper)
	}
	
	// helpers. these may cheat.
	public double getFirstRoundBid() {
		// no goods won. good_id == 0. tmp_r[0] is a special global for good 0. 
		return calculateFirstBid();
	}
	
	public double getSecondRoundBid(int no_goods_won) {
		// truthful bidding. realized prices don't matter.
		return v.getValue(no_goods_won+1) - v.getValue(no_goods_won);
	}
}

