package speed;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

// Self confirming updates under Weber set up. Multiround, first price. 
public class FullCondSCWeberMRFP {
	public static void main(String[] args) throws IOException {
		Cache.init();
		Random rng = new Random();
		
		// general Parameters
		boolean first_price = true;
		int no_goods = 2;
		int no_agents = 3;
		int nth_price = 1;
		double max_value = 1.0;
		double max_price = max_value;

		// calculation & evaluation parameters
		double p_precision = 0.02;	// price precision
		double v_precision = 0.0001;	// valuation precision
		double cmp_precision = 0.05;						// discretization precision when evaluating strategy 
		int no_for_cmp = (int) (1/cmp_precision) + 1;
		
		// simulation parameters
		int no_initial_simulations = 1000000/no_agents;	// generating initial PP		
		int no_iterations = 10;								// no. of Wellman updates 
		int no_per_iteration = 1000000/no_agents;			// no. of games played in each Wellman iteration
		int no_for_EUdiff = 1000;							// no. of points for EU comparison
				
		// agent preferences		
		boolean discretize_value = true;		// whether to discretized value
		int preference = 0;							// MDP agent preference: 2 = favor mixing, -1 = favor lower bound
		double epsilon = 0.00005;						// tie-breaking threshold
		
		// what to record
		boolean take_log = false;						// record prices for agents
		boolean print_strategy = true;					// whether to print out strategy functions after each Wellman update
		boolean cmp_with_Weber = true;					// whether to compute distance to Weber equilibrium after each Wellman update
		
		// Some spaces
		JointCondDistributionEmpirical[] PP = new JointCondDistributionEmpirical[no_iterations+1];
		
		// 1) Initialize PP0 -- from Wellman
		System.out.println("Generating PP[0]");

		// create agents and action for initialization
		WeberAgent[] weber_agents = new WeberAgent[no_agents];
		for (int i = 0; i<no_agents; i++)
			weber_agents[i] = new WeberAgent(new UnitValue(max_value, rng), i, no_agents, no_goods, first_price);

		JointCondFactory jcf = new JointCondFactory(no_goods, p_precision, max_price);		
		SeqAuction auction = new SeqAuction(weber_agents, nth_price, no_goods);
		PP[0] = jcf.simulAllAgentsOneRealPP(auction, no_initial_simulations,take_log,false,cmp_with_Weber);
		PP[0].outputNormalized();		

		// 0.1) Initialize utility comparison device
//		if (cmp_with_Weber==true){
			// record mean and stdev of utility when playing against Weber
			double[] Umean = new double[no_iterations+1];
			double[] Ustdev = new double[no_iterations+1];
			Umean[0] = Statistics.mean(jcf.utility);
			Ustdev[0] = Statistics.stdev(jcf.utility);
			
			// create comparison device
			EpsilonFactor2 ef = new EpsilonFactor2(no_goods);
			SeqAgent[] cmp_agents = new SeqAgent[no_agents];
			cmp_agents[0] = new FullCondMDPAgent4FP(new UnitValue(max_value, rng), 0, preference, epsilon, discretize_value, v_precision);
			for (int k = 1; k < no_agents; k++)
				cmp_agents[k] = new WeberAgent(new UnitValue(max_value, rng), k, no_agents, no_goods, first_price);
			SeqAuction cmp_auction = new SeqAuction(cmp_agents,nth_price, no_goods);			
//		}
		
//		FileWriter fw_pp0 = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/paper/threerounds/weber_pp0_" + no_agents + "_" + v_precision + "_" + p_precision + "_" + no_iterations + ".csv");
//		PP[0].outputRaw(fw_pp0);
//		fw_pp0.close();

		// 0.2) Strategy outputting devices
//		if (print_strategy == true){
		UnitValue value = new UnitValue(max_value, rng);
		FullCondMDPAgent4FP mdp_agent = new FullCondMDPAgent4FP(value, 1, preference, epsilon, false, v_precision);	// XXX: never discretize here
			// valuations to compare bids at
			double[] v = new double[no_for_cmp]; 		
			for (int i = 0; i < no_for_cmp; i++)
				v[i] = i*cmp_precision;
			double[][] strategy = new double[no_iterations][no_for_cmp];
			
			
//		}
		
		// 0.3) Agent for updating (always needed)
		FullCondMDPAgent4FP[] updating_agents = new FullCondMDPAgent4FP[no_agents];
		for (int i = 0; i < no_agents; i++)
			updating_agents[i] = new FullCondMDPAgent4FP(new UnitValue(max_value, rng), i, preference, epsilon, discretize_value, v_precision);
		SeqAuction updating_auction = new SeqAuction(updating_agents, nth_price, no_goods);
	

		// 2) Wellman updates
		for (int it = 0; it < no_iterations; it++) {
			
			// 2.1) Output local strategy? 
			if (print_strategy == true){
				mdp_agent.setCondJointDistribution(PP[it]);
				System.out.println("round " + it + "bidding strategy: = [");
				for (int i = 0; i < v.length; i++) {
					value.x = v[i];
					mdp_agent.reset(null);		// recompute MDP
					strategy[it][i] = mdp_agent.getFirstRoundBid();
					System.out.print(strategy[it][i] + ",");
				}
			}
			System.out.println("]");
			
			// 2.2) Output utility difference? 
			if (cmp_with_Weber == true){
				cmp_agents[0].setCondJointDistribution(PP[it]);				
				
				ef.StrategyDistance(cmp_auction, no_for_EUdiff);
				Umean[it+1] = Statistics.mean(ef.utility);
				Ustdev[it+1] = Statistics.stdev(ef.utility);
				System.out.println("it = " + it + ", Umean = " + Umean[it+1] + ", Ustdev = " + Ustdev[it+1]);
			}
			
			// 2.3) Update: PP[it] --> PP[it+1]
			for (int i = 0; i < no_agents; i++)
				updating_agents[i].setCondJointDistribution(PP[it]);
			Cache.clearMDPpolicy();
			PP[it+1] = jcf.simulAllAgentsOneRealPP(updating_auction, no_per_iteration,take_log,false,false);
		}

		
		
		
//		// file to write to
//		FileWriter fw_strat = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/paper/firstprice/weber_" + no_goods + "_" + no_agents + "_" + v_precision + "_" + p_precision + "_discretized.csv");
//		
//		Cache.clearMDPpolicy();
//		for (int i = 0; i < no_for_cmp; i++) {
//			value.x = v[i];
//			mdp_agent.reset(null);		// recompute MDP
//			
//			fw_strat.write(v[i] + "," + mdp_agent.getFirstRoundBid() + "\n");					
////				if (value.x != 0) {
////					System.out.println("value.x = " + value.x);
////					mdp_agent.printpi();
////					mdp_agent.printV();
////				}
//			}	
//		fw_strat.close();
	}
}

