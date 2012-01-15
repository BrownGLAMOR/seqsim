package speed;

import legacy.DiscreteDistribution;
// bids according to the Equilibrium Agent documented in Katzman's paper

public class KatzmanAgent extends SeqAgent {

	boolean ready = false;

	DiscreteDistribution F;
	int agent_idx;
	int no_goods_won;
	int N;
	int i;
	Value valuation;
	double H, L;
	
	public KatzmanAgent(Value valuation, int agent_idx) {
		super(agent_idx, valuation);
		this.agent_idx = agent_idx;
		this.valuation = valuation;
	}

	// Calculate the first Bid
	public double calculateFirstBid() {
		if (!ready)
			throw new RuntimeException("must input Distribution F first");

		H = valuation.getValue(1);
		L = valuation.getValue(2) - H;
		
		if (H <= L)
			System.out.println("ERROR: H=" + H + ", L=" + L);
		
		int H_bin = DiscreteDistribution.bin(H, F.precision);
		
		double denominator = java.lang.Math.pow(F.getCDF(H, 0.0), 2*N-1);
		double numerator = 0;
		// We assume that lower bound of F is 0
		for (i = 0; i <= H_bin; i++) {
			numerator += java.lang.Math.pow(F.getCDF(F.precision*i, 0.0), 2*N-1)*F.precision;
//			System.out.println("adding: CDF = "+F.getCDF(F.precision*i, 0.0)+", exponent = "+(2*N-1)+", so adding "+java.lang.Math.pow(F.getCDF(F.precision*i, 0.0), 2*N-1)*F.precision);
		}
	
		return (H - numerator/denominator);
	}
	
	@Override
	public void reset(SeqAuction auction) {
		this.auction = auction;
		no_goods_won = 0;
	}

	
	// Input F, the common prior distribution from which valuations (H, L) are initially drawn from
	public void setParameters(DiscreteDistribution F, int N) {
		this.F = F;
		this.N = N; 
		ready = true;
	}
	
	@Override
	public double getBid(int good_id) {
		// Bid "truthfully" in the second round
		if (good_id == 1) {
			if (auction.winner[0] == agent_idx)
				no_goods_won++;
			
			return (valuation.getValue(no_goods_won+1) - valuation.getValue(no_goods_won));
		}
		else
			return calculateFirstBid();	// bid a specific amount in the first round (according to Katzman paper)
	}
}

