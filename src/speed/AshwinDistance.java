package speed;

import java.util.Arrays;
import java.util.Random;

public class AshwinDistance {

	// Measures distance between two distributions in the way inspired by talking with Ashwin over phone in Ratty
	
	public double AshwinDistance(double[][] realized_old, double[][] realized_new, JointDistributionEmpirical jde_old, JointDistributionEmpirical jde_new) {		
//	public double AshwinDistance(double[][] realized_old, double[][] realized_new, boolean sample, int no_samples)		
		// realized_old/new: [no. of realized samples][no_goods]
		
		assert (realized_old[0].length != realized_new[0].length);
		
		int no_samples = 10000;
		int nth_price = 2;
		double max_price = 10.0;

		int n_old = realized_old.length;
		int n_new = realized_new.length;
		int no_goods = realized_old[0].length;
		int idx, no_goods_won0, no_goods_won1;
		double[] sample_hob = new double[no_goods];
		double bid0, bid1, total_cost0, total_cost1, ave_surplus0, ave_surplus1, surplus0, surplus1;
		
		// TODO: may not need to have any randomness at all (maybe just enumerate it)
		Random rng = new Random();

		FullMDPNumGoodsSeqAgent[] agent = new FullMDPNumGoodsSeqAgent[2];
		agent[0] = new FullMDPNumGoodsSeqAgent(new DMUValue(no_goods, max_price, rng), 0);
		agent[1] = new FullMDPNumGoodsSeqAgent(new DMUValue(no_goods, max_price, rng), 0);

		agent[0].setJointDistribution(jde_old);		// agent[0] assumes old distribution
		agent[1].setJointDistribution(jde_new);		// agent[1] assumes new distribution
		
		
		SeqAuction auction0 = new SeqAuction(agent, (double) nth_price, no_goods);
		SeqAuction auction1 = new SeqAuction(agent, (double) nth_price, no_goods);
		surplus0 = 0;
		surplus1 = 0;
		
		for (int i = 0; i < no_samples; i++) {

			// draw new valuations
			agent[0].v.reset();
			agent[1].v.reset();

			idx = rng.nextInt(n_new);
			sample_hob = realized_new[idx];

			// manually run auction0 and auction1
			agent[0].reset(auction0);
			agent[1].reset(auction1);
			no_goods_won0 = 0;
			no_goods_won1 = 0;
			total_cost0 = 0;
			total_cost1 = 0;

			for (int good_id = 0; good_id < no_goods; good_id++) {
				bid0 = agent[0].getBid(good_id);
				if (bid0 > sample_hob[good_id]){
					auction0.winner[good_id] = 0;
					auction0.hob[0][good_id] = sample_hob[good_id];
					no_goods_won0 ++;
					total_cost0 += sample_hob[good_id];
				}
				else {
					auction0.winner[good_id] = 1;
					auction0.hob[0][good_id] = sample_hob[good_id];
				}
				
				bid1 = agent[1].getBid(good_id);
				if (bid1 > sample_hob[good_id]){
					auction1.winner[good_id] = 1;
					auction1.hob[0][good_id] = sample_hob[good_id];
					no_goods_won1 ++;
					total_cost1 += sample_hob[good_id];
				}
				else {
					auction0.winner[good_id] = 0;
					auction0.hob[0][good_id] = sample_hob[good_id];
				}
			}
		surplus0 += agent[0].v.getValue(no_goods_won0) - total_cost0;
		surplus1 += agent[1].v.getValue(no_goods_won1) - total_cost1;
		}
		ave_surplus0 = surplus0/no_samples;
		ave_surplus1 = surplus1/no_samples;
		
		return (ave_surplus1 - ave_surplus0);
	}
}
