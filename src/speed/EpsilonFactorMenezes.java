package speed;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

// Measures distance between two distributions when the valuation is Menezes. Enumerate discretized valuations input by user. 
public class EpsilonFactorMenezes {

	int no_goods;
	double EU_P, EU_Q, EU_diff;
	double[] uP, uQ, udiff;
	
	public EpsilonFactorMenezes(int no_goods) throws IOException {				
		this.no_goods = no_goods;
	}

	// Calculates the epsilon factor from Q to P: E(S^P|P) - E(S^Q|P) 
	public void jcdeDistance(Random rng, JointCondDistributionEmpirical P, JointCondDistributionEmpirical Q, MenezesValue v, double[] Vs, double[] deltaVs) throws IOException{
		
		// pass the same valuation, but set different PPs
		
		FullCondMDPAgent agentP = new FullCondMDPAgent(v, 0);
		FullCondMDPAgent agentQ = new FullCondMDPAgent(v, 1);

		agentP.setCondJointDistribution(P);
		agentQ.setCondJointDistribution(Q);
		
		this.uP = new double[Vs.length];	// utility log of agentP when PP is P
		this.uQ = new double[Vs.length];	// utility log of agentQ when PP is P
		this.udiff = new double[Vs.length];	// difference b/w these two
		
		double hob4P, hob4Q, Pbid, Qbid;				// hobs for different agents; bids submitted by different agents
		double paymentP, paymentQ;
		int Pwon, Qwon;
		boolean[] winnerP = new boolean[no_goods];		// logs to store winners and realized prices
		boolean[] winnerQ = new boolean[no_goods];
		double[] realizedP = new double[no_goods];
		double[] realizedQ = new double[no_goods];
				
		for (int i = 0; i < Vs.length; i++) {

			// reset values, compute MDP
			v.x = Vs[i];
			v.delta_x = deltaVs[i];
			
			agentP.computeFullMDP();
			agentQ.computeFullMDP();
			
			Pwon = 0;
			Qwon = 0;
			paymentP = 0.0;
			paymentQ = 0.0;

			// Sample from P
			for (int j = 0; j < no_goods; j++) {
				
				hob4P = P.sampleCondPrices(rng, Arrays.copyOfRange(winnerP, 0, j), Arrays.copyOfRange(realizedP, 0, j));
				hob4Q = P.sampleCondPrices(rng, Arrays.copyOfRange(winnerP, 0, j), Arrays.copyOfRange(realizedP, 0, j));

				Pbid = agentP.getBid(j, Arrays.copyOfRange(winnerP, 0, j), Arrays.copyOfRange(realizedP, 0, j));
				Qbid = agentQ.getBid(j, Arrays.copyOfRange(winnerP, 0, j), Arrays.copyOfRange(realizedP, 0, j));
				
				realizedP[j] = hob4P;
				realizedQ[j] = hob4Q;
				
				// "play" the game
				if (Pbid > hob4P) {
					winnerP[j] = true;
					paymentP += hob4P;
					Pwon ++;
				} else {
					winnerP[j] = false;
				}
				
				if (Qbid > hob4Q) {
					winnerQ[j] = true;
					paymentQ += hob4Q;
					Qwon ++;
				} else {
					winnerQ[j] = false;
				}
			
			}
			
			// Compute utility
			uP[i] = agentP.v.getValue(Pwon) - paymentP;
			uQ[i] = agentQ.v.getValue(Qwon) - paymentQ;
			udiff[i] = uP[i] - uQ[i];
		}
		
		// Summary statistics
		EU_P = Statistics.mean(uP);
		EU_Q = Statistics.mean(uQ);
		EU_diff = Statistics.mean(udiff);
	}
		
	// Testing
	public static void main(String args[]) throws IOException {
		
		Random rng = new Random();
		
		int no_goods = 2, nth_price = 2;
		int no_agents = 2, no_Q_simulations = 10000000/no_agents;
		double precision = 0.05, max_price = 1.0;
		
		Boolean decreasing = true;
		int mene_type = 0;
		
		EpsilonFactorMenezes ef = new EpsilonFactorMenezes(no_goods);
		
		// Create 2 distributions
		JointCondDistributionEmpirical P, Q;
		JointCondFactory jcf = new JointCondFactory(no_goods, precision, max_price);

		// P ~ uniform
		P = jcf.makeUniform(false);
		
		// Q ~ from Menezes
		MenezesAgent[] mene_agents = new MenezesAgent[no_agents];
		for (int i = 0; i<no_agents; i++)
			mene_agents[i] = new MenezesAgent(new MenezesValue(max_price, rng, decreasing), no_agents, i, mene_type);
		SeqAuction mene_auction = new SeqAuction(mene_agents, nth_price, no_goods);
		Q = jcf.simulAllAgentsOnePP(mene_auction, no_Q_simulations, false, false,true);

		double[] u = jcf.utility;
		System.out.println("EU(Q|Q) = " + Statistics.mean(u) + ", stdev U(Q|Q) = " + Statistics.stdev(u));
//		System.out.println("EU(Q|Q) = " + Statistics.mean(u) + ", stdev U(Q|Q) = " + Statistics.stdev(u)/java.lang.Math.sqrt((no_Q_simulations*no_agents)));
				
		// valuations to iterate over
		double cmp_precision = 0.001;						// discretization step for valuation examining
		int no_for_cmp = (int) (1/cmp_precision) + 1;
		double[] Vs = new double[no_for_cmp];
		double[] deltaVs = new double[no_for_cmp];
		for (int i = 0; i < no_for_cmp; i++){
			Vs[i] = i*cmp_precision;
			deltaVs[i] = Vs[i] + Vs[i]*Vs[i];
		}

		System.out.println("********");
		
		// compute distances
		ef.jcdeDistance(rng, P, Q, new MenezesValue(max_price, rng, true), Vs, deltaVs);		
		System.out.println("EU(P|P) = " + ef.EU_P + ", stdev U(P|P) = " + Statistics.stdev(ef.uP));
		System.out.println("EU(Q|P) = " + ef.EU_Q + ", stdev U(Q|P) = " + Statistics.stdev(ef.uQ));
		System.out.println("EU(P-Q|P) = " + ef.EU_diff + ", stdev U(P-Q|P) = " + Statistics.stdev(ef.udiff));
		
		ef.jcdeDistance(rng, Q, P, new MenezesValue(max_price, rng, true), Vs, deltaVs);
		System.out.println("EU(Q|Q) = " + ef.EU_P + ", stdev U(Q|Q) = " + Statistics.stdev(ef.uP));
		System.out.println("EU(P|Q) = " + ef.EU_Q + ", stdev U(P|Q) = " + Statistics.stdev(ef.uQ));
		System.out.println("EU(Q-P|Q) = " + ef.EU_diff + ", stdev U(Q-P|Q) = " + Statistics.stdev(ef.udiff));

		
	}
	
}