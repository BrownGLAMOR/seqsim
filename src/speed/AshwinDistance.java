package speed;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

// Measures distance between two distributions in the way inspired by talking with Ashwin over phone in Ratty
public class AshwinDistance {

	double distance;
	double[][] realized_old, realized_new;
	JointDistributionEmpirical jde_old, jde_new;
	// 																		realized_old/new: [no. of realized samples][no_goods]
	public AshwinDistance(double distance, double[][] realized_old, double[][] realized_new, JointDistributionEmpirical jde_old, JointDistributionEmpirical jde_new) throws IOException {		
		this.distance = distance;
		this.realized_old = realized_old;
		this.realized_new = realized_new;
		this.jde_old = jde_old;
		this.jde_new = jde_new;
		
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

		surplus0 = 0;
		surplus1 = 0;
		
		for (int i = 0; i < no_samples; i++) {

			// draw new HOB
			idx = rng.nextInt(n_new);
			sample_hob = realized_new[idx];

			// Initialize agents
			SeqAgent[] agents_old = new SeqAgent[2];
			SeqAgent[] agents_new = new SeqAgent[2];
			
			agents_old[0] = new FullMDPNumGoodsSeqAgent(new DMUValue(no_goods, max_price, rng), 0);
			agents_new[0] = new FullMDPNumGoodsSeqAgent(new DMUValue(no_goods, max_price, rng), 0);
			
			
			
			agents_old[0].setJointDistribution(jde_old);		// agent[0] assumes old distribution
			agents_new[0].setJointDistribution(jde_new);		// agent[1] assumes new distribution

			agents_old[1] = new HOBAgent(null, 1, sample_hob);
			agents_new[1] = new HOBAgent(null, 1, sample_hob);

			// draw new valuations
//			agents_old[0].v.reset();
//			agents_new[0].v.reset();

			// open auctions
			SeqAuction auction0 = new SeqAuction(agents_old, (double) nth_price, no_goods);
			SeqAuction auction1 = new SeqAuction(agents_new, (double) nth_price, no_goods);
			
			auction0.play(true, null);
			auction1.play(true, null);
			
			surplus0 += auction0.profit[0];
			surplus1 += auction1.profit[0];
//			// manually run auction0 and auction1
//			agent[0].setJointDistribution(jde_old);
//			agent[1].setJointDistribution(jde_new);
//			agent[0].reset(auction0);
//			agent[1].reset(auction1);
//			no_goods_won0 = 0;
//			no_goods_won1 = 0;
//			total_cost0 = 0;
//			total_cost1 = 0;
//
//			for (int good_id = 0; good_id < no_goods; good_id++) {
//				bid0 = agent[0].getBid(good_id);
//				if (bid0 > sample_hob[good_id]){
//					auction0.winner[good_id] = 0;
//					auction0.hob[0][good_id] = sample_hob[good_id];
//					no_goods_won0 ++;
//					total_cost0 += sample_hob[good_id];
//				}
//				else {
//					auction0.winner[good_id] = 1;
//					auction0.hob[0][good_id] = sample_hob[good_id];
//				}
//				
//				bid1 = agent[1].getBid(good_id);
//				if (bid1 > sample_hob[good_id]){
//					auction1.winner[good_id] = 1;
//					auction1.hob[0][good_id] = sample_hob[good_id];
//					no_goods_won1 ++;
//					total_cost1 += sample_hob[good_id];
//				}
//				else {
//					auction0.winner[good_id] = 0;
//					auction0.hob[0][good_id] = sample_hob[good_id];
//				}
//			}
//		surplus0 += agent[0].v.getValue(no_goods_won0) - total_cost0;
//		surplus1 += agent[1].v.getValue(no_goods_won1) - total_cost1;

		
		}
		ave_surplus0 = surplus0/no_samples;
		ave_surplus1 = surplus1/no_samples;
		
		distance =  ave_surplus1 - ave_surplus0;
	}

}