package speed;

public class RandomSeqAgent extends SeqAgent {
	SeqAuction auction;
	
	@Override
	public void reset(SeqAuction auction) {
		this.auction = auction;
	}

	@Override
	public double getBid(int good_id) {
		return Math.random();
	}

}
