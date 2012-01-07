package speed;


import default;
import JointDistributionEmpirical;
import Valuation;

public class FullMDPSeqAgent extends SeqAgent {
	SeqAuction auction;

	JointDistributionEmpirical jde;

	public FullMDPSeqAgent(JointDistributionEmpirical jde) {
		this.jde = jde;
		
		// Do MDP calculation when initiating agent
		computeFullMDP(jde);

	
	@Override
	public void reset(SeqAuction auction) {
		this.auction = auction;
	}

	@Override
	public double getBid(int good_id) {
		return Math.random();
	}
	
}
