package speed;

import java.util.Arrays;
import java.util.Random;

import legacy.DiscreteDistribution;
// The same as KatzmanAgent.java, only it directly draws valuation from Uniform[0, max_price] (to avoid discretization issues)
public class KatzmanUniformAgent extends SeqAgent {

	boolean ready = false;

	int agent_idx;
	int no_goods_won;
	int N;
	int i;
	Value valuation;
	double H, L, max_price, precision;
	
	public KatzmanUniformAgent(Value valuation, int agent_idx) {
		super(agent_idx, valuation);
		this.agent_idx = agent_idx;
		this.valuation = valuation;
	}

	// Calculate the first Bid
	public double calculateFirstBid() {
		if (!ready)
			throw new RuntimeException("must input Distribution F first");

		if (H < L)
			System.out.println("ERROR: H=" + H + ", L=" + L);
		
		double denominator = java.lang.Math.pow(H/max_price, 2*N-1);
		double numerator = 0;		
		// We assume that lower bound of F is 0
		for (i = 1; i*precision <= H; i++)
			numerator += java.lang.Math.pow(i*precision/max_price, 2*N-1)*precision;

		double bid = (H - numerator/denominator);
		// bid has to be non-negative
		if (bid < 0) {
			if (bid < -precision)
				System.out.println("Warning: agent "+agent_idx+", bids "+bid+", too negative");	// print warning
			bid = 0;
		}
		return bid;
	}
	
	@Override
	public void reset(SeqAuction auction) {
		this.auction = auction;
		no_goods_won = 0;
	}

	// Draw valuations (H, L) from Uniform(0, max_price)
	public void setParameters(int N, double max_price, double precision) {
		ready = true;
		this.N = N;
		this.max_price = max_price;
		this.precision = precision;
		
		Random rng = new Random();

		double[] u = new double[2];		
		for (int i = 0; i < 2; i++)
			u[i] = rng.nextDouble() * max_price;
		
		// sort the array in ascending order. 
		Arrays.sort(u);
		L = u[0];
		H = u[1];
		
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

