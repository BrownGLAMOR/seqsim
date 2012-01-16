package speed;

import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Random;

import legacy.DiscreteDistribution;
import legacy.DiscreteDistributionWellman;
import legacy.Histogram;

// Test if strategy computed from MDP would deviate if initiated from a Katzman-generated price distribution
public class TestKatzStrategy {
	public static void main(String[] args) throws IOException {
		Cache.init();
		Random rng = new Random();
		
		double precision = 0.1;
		double max_price = 5.0;
		int no_goods = 2;
		int no_agents = 2;
		int nth_price = 2;
		int no_simulations = 5000;		// run how many games to generate PP
		int max_iterations = 5;
		
		JointFactory jf = new JointFactory(no_goods, precision, max_price);

		// Create a uniform distribution F (for Katzman agents to draw valuation from)
		Histogram h = new Histogram(precision);
		for (int i = 1; i*precision <= max_price; i++)
			h.add(i*precision);
		DiscreteDistribution F = new DiscreteDistributionWellman(h.getDiscreteDistribution(), precision);
		
		// Create agents
//		KatzmanAgent[] katz_agents = new KatzmanAgent[no_agents];
//		for (int i = 0; i<no_agents; i++) {
//			katz_agents[i] = new KatzmanAgent(new DMUValue(no_goods, max_price, rng), i);
//			katz_agents[i].setParameters(F, no_agents-1);
//		}

		KatzmanUniformAgent[] katz_agents = new KatzmanUniformAgent[no_agents];
		for (int i = 0; i<no_agents; i++) {
			katz_agents[i] = new KatzmanUniformAgent(new DMUValue(no_goods, max_price, rng), i);
			katz_agents[i].setParameters(no_agents - 1, max_price, precision);
		}
				
		// Create auction
		SeqAuction katz_auction = new SeqAuction(katz_agents, nth_price, no_goods);

		// Generate initial condition
		JointDistributionEmpirical pp[];
		
		System.out.print("Generating initial PP from katzman agents...");
		pp = jf.simulAllAgents(katz_auction, no_simulations);
		
		System.out.println("sampled price **BINS INDEXES**:");
		for (int i = 0; i<20; i++) {
			int[] sample = pp[0].getSample(rng);

			System.out.print(i + ": {");
			
			for (int d : sample) 
				System.out.print(d + ", ");
			
			System.out.println("}");			
		}

		
//		pp = new JointDistributionEmpirical[no_agents];
//		pp[0] = new JointDistributionEmpirical(no_goods, precision, max_price);
//		pp[0] = jf.makeUniform();
		System.out.println("done");
		
		
//		double increment = 0.1;
//		double[] input_v = new double[3];
//		input_v[0] = 0;
		
		for (int iteration_idx = 0; iteration_idx < max_iterations; iteration_idx++) {
			// Draw a new valuation
			Value v = new DMUValue(no_goods, max_price, rng);
			
			System.out.println("v[1] = "+v.getValue(1)+", v[2] = "+v.getValue(2));
			
			// Katzman Agent computes
//			KatzmanUniformAgent katz_uni_agent = new KatzmanUniformAgent(v, 0);
//			katz_uni_agent.setParameters(no_agents - 1, max_price, precision);
//			System.out.println("first round: \t katzman agent bids " + katz_uni_agent.calculateFirstBid());
//			System.out.println("second round: \t katzman agent bids " + katz_agent.v.getValue(1) + " if lost the first round, and bids " + (katz_agent.v.getValue(2)-katz_agent.v.getValue(1)) + "if won the first round");			

			KatzmanAgent katz_agent = new KatzmanAgent(v, 0);
			katz_agent.setParameters(F, no_agents-1);
			System.out.println("first round: \t old katzman agent bids " + katz_agent.calculateFirstBid());
			
			// Full MDP Agent computes
			FullMDPAgent2 mdp_agent = new FullMDPAgent2(v, 0);
			mdp_agent.setJointDistribution(pp[0]);
			mdp_agent.reset(null);
//			mdp_agent.computeFullMDP();

//			// Simple MDP Agent Computes
//			MDPSeqAgent simple_mdp_agent = new MDPSeqAgent(v, 10);
//			DiscreteDistribution[] pd = new DiscreteDistribution[no_goods];
//
//			for (int i = 0; i < no_goods; i++) {
//				ArrayList<Double> temp = new ArrayList(Arrays.asList(pp[0].getMarginalDist(i)));
//				pd[i] = new DiscreteDistributionWellman(temp, precision);
//			}
//			simple_mdp_agent.setJointDistribution(pd);
//			simple_mdp_agent.computeMDP();
				}
		
	}
}
