package speed;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

// Measures distance between two distributions, in second price auctions
public class EpsilonFactor {

	int no_goods;
	double EU_P, EU_Q, EU_diff;
	double stdev_P, stdev_Q, stdev_diff;
	double[] uP, uQ, udiff, vs;
	
	public EpsilonFactor(int no_goods) throws IOException {				
		this.no_goods = no_goods;
	}

	// Calculates the epsilon factor from Q to P: E(S^P|P) - E(S^Q|P) 
	public void jcdeDistance(Random rng, JointCondDistributionEmpirical P, JointCondDistributionEmpirical Q, Value v, int no_iterations) throws IOException{
		
		// pass the same valuation, but set different PPs
		FullCondMDPAgent agentP = new FullCondMDPAgent(v, 0);
		FullCondMDPAgent agentQ = new FullCondMDPAgent(v, 1);

		agentP.setCondJointDistribution(P);
		agentQ.setCondJointDistribution(Q);
		
		this.uP = new double[no_iterations];	// utility log of agentP when PP is P
		this.uQ = new double[no_iterations];	// utility log of agentQ when PP is P
		this.udiff = new double[no_iterations];	// difference b/w these two
		this.vs = new double[no_iterations];
		
		double hob4P, hob4Q, Pbid, Qbid;				// hobs for different agents; bids submitted by different agents
		double paymentP, paymentQ;
		int Pwon, Qwon;
		boolean[] winnerP = new boolean[no_goods];		// logs to store winners and realized prices
		boolean[] winnerQ = new boolean[no_goods];
		double[] realizedP = new double[no_goods];
		double[] realizedQ = new double[no_goods];
				
		for (int i = 0; i < no_iterations; i++) {

			// reset values, compute MDP
			v.reset();
			vs[i] = v.getValue(1);
			
//			agentP.v.reset();
//			agentQ.v.reset();

			agentP.computeFullMDP();
			agentQ.computeFullMDP();
			
			Pwon = 0;
			Qwon = 0;
			paymentP = 0.0;
			paymentQ = 0.0;

			// Sample from P and "play" an auction game
			for (int j = 0; j < no_goods; j++) {
				
				hob4P = P.sampleCondPrices(rng, Arrays.copyOfRange(winnerP, 0, j), Arrays.copyOfRange(realizedP, 0, j));
				hob4Q = P.sampleCondPrices(rng, Arrays.copyOfRange(winnerP, 0, j), Arrays.copyOfRange(realizedP, 0, j));

				Pbid = agentP.getBid(j, Arrays.copyOfRange(winnerP, 0, j), Arrays.copyOfRange(realizedP, 0, j));
				Qbid = agentQ.getBid(j, Arrays.copyOfRange(winnerP, 0, j), Arrays.copyOfRange(realizedP, 0, j));
				
				realizedP[j] = hob4P;
				realizedQ[j] = hob4Q;
				
				// "play" the game
				if (Pbid >= hob4P) {
					winnerP[j] = true;
					paymentP += hob4P;
					Pwon ++;
				} else {
					winnerP[j] = false;
				}
				
				if (Qbid >= hob4Q) {
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
		stdev_P = Statistics.stdev(uP);				// crude stdev, not CLT treated
		stdev_Q = Statistics.stdev(uQ);
		stdev_diff = Statistics.stdev(udiff);
	}
		
	// Testing
	public static void main(String args[]) throws IOException {
		
		Random rng = new Random();
		
		int no_goods = 2;
		int no_agents = 5, no_Q_simulations = 1000000/no_agents;
		double precision = 0.05, max_price = 1.0;
		
		EpsilonFactor ef = new EpsilonFactor(no_goods);
		
		// Create 2 distributions
		JointCondDistributionEmpirical P, Q;
		JointCondFactory jcf = new JointCondFactory(no_goods, precision, max_price);

		// P ~ uniform
		P = jcf.makeUniform(false);
		
		// Q ~ 3 agent Katzman
		KatzmanUniformAgent[] katz_agents = new KatzmanUniformAgent[no_agents];
		for (int i = 0; i<no_agents; i++)
			katz_agents[i] = new KatzmanUniformAgent(new KatzHLValue(no_agents-1, max_price, rng), no_agents, i);
		
		SeqAuction katz_auction = new SeqAuction(katz_agents, 2, no_goods);
		Q = jcf.simulAllAgentsOnePP(katz_auction, no_Q_simulations, false, false,true);

		double[] u = jcf.utility;
		System.out.println("EU(Q|Q) = " + Statistics.mean(u) + ", stdev U(Q|Q) = " + Statistics.stdev(u));
		System.out.println("********");
		
		int no_iterations = 10000;
		
		// compute distances & output
		ef.jcdeDistance(rng, P, Q, new KatzHLValue(no_agents-1, max_price, rng), no_iterations);		
		System.out.println("EU(P|P) = " + ef.EU_P + ", stdev U(P|P) = " + ef.stdev_P);
		System.out.println("EU(Q|P) = " + ef.EU_Q + ", stdev U(Q|P) = " + ef.stdev_Q);
		System.out.println("EU(P-Q|P) = " + ef.EU_diff + ", stdev U(P-Q|P) = " + ef.stdev_diff);

		// Write v, uP, uQ, u_diff
		FileWriter fw = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/EpsilonFactor/test1.csv");
//		FileWriter fw = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/EpsilonFactor/test1_" + no_agents + "_" + precision + "_" + no_iterations + ".csv");
		for (int i = 0; i < no_iterations; i++)
			fw.write(ef.vs[i] + "," + ef.uP[i] + "," + ef.uQ[i] + "," + ef.udiff[i] + "\n");
		fw.close();


		ef.jcdeDistance(rng, Q, P, new KatzHLValue(no_agents-1, max_price, rng), no_iterations);		
		System.out.println("EU(Q|Q) = " + ef.EU_P + ", stdev U(Q|Q) = " + ef.stdev_P);
		System.out.println("EU(P|Q) = " + ef.EU_Q + ", stdev U(P|Q) = " + ef.stdev_Q);
		System.out.println("EU(Q-P|Q) = " + ef.EU_diff + ", stdev U(Q-P|Q) = " + ef.stdev_diff);

		// Write v, uP, uQ, u_diff
		FileWriter fw2 = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/EpsilonFactor/test2.csv");
//		FileWriter fw2 = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/EpsilonFactor/test2_" + no_agents + "_" + precision + "_" + no_iterations + ".csv");
		for (int i = 0; i < no_iterations; i++)
			fw2.write(ef.vs[i] + "," + ef.uP[i] + "," + ef.uQ[i] + "," + ef.udiff[i] + "\n");
		fw2.close();
	}
	
}