package speed;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

// Do self-confirming updates for three rounds, under Weber set up, initiated with Weber agents
public class ThreeRoundWeberPolyStart {
	public static void main(String[] args) throws IOException {
		Cache.init();
		Random rng = new Random();
		
		// general Parameters
		int no_goods = 3;
		int no_agents = 4;
		int nth_price = 2;
		double max_value = 1.0;
		double max_price = max_value;

		// simulation & evaluation parameters
		double p_precision = 0.02;	// price precision
		double v_precision = 0.0001;	// valuation precision
		int no_initial_simulations = 1000000000/no_agents;	// generating initial PP		
		double cmp_precision = 0.01;						// discretization precision when evaluating strategy 
		int no_for_cmp = (int) (1/cmp_precision) + 1;

		// agent preferences		
		boolean discretize_value = true;		// whether to discretized value
		int preference = 0;							// MDP agent preference: 2 = favor mixing, -1 = favor lower bound
		double epsilon = 0.00005;						// tie-breaking threshold
		
		// what to record
		boolean take_log = false;						// record prices for agents
		boolean print_strategy = true;					// Output strategy S(t)
		
		// 1) Initialize PP0
		System.out.println("Generating PP0");

		// create agents and action for initialization
		WeberAgent[] weber_agents = new WeberAgent[no_agents];
		for (int i = 0; i<no_agents; i++)
			weber_agents[i] = new WeberAgent(new UnitValue(max_value, rng), i, no_agents, no_goods);

		JointCondFactory jcf = new JointCondFactory(no_goods, p_precision, max_price);		
		SeqAuction auction = new SeqAuction(weber_agents, nth_price, no_goods);
		JointCondDistributionEmpirical PP0 = jcf.simulAllAgentsOnePP(auction, no_initial_simulations,take_log,false,false);

		PP0.outputNormalized();
		
//		FileWriter fw_pp0 = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/paper/threerounds/weber_pp0_" + no_agents + "_" + v_precision + "_" + p_precision + "_" + no_iterations + ".csv");
//		PP[0].outputRaw(fw_pp0);
//		fw_pp0.close();
			
		// 2) Compare
		if (print_strategy == true) {

			// Initiate agent
			UnitValue value = new UnitValue(max_value, rng);
			FullCondMDPAgent4 mdp_agent = new FullCondMDPAgent4(value, 1, preference, epsilon, discretize_value, v_precision);
			mdp_agent.setCondJointDistribution(PP0);
			
			// valuations to compare bids at
			double[] v = new double[no_for_cmp]; 		
			for (int i = 0; i < no_for_cmp; i++)
				v[i] = i*cmp_precision;

			// file to write to
			FileWriter fw_strat = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/paper/threerounds/weber_weberstart_" + no_agents + "_" + v_precision + "_" + p_precision + ".csv");
			
			Cache.clearMDPpolicy();
			for (int i = 0; i < no_for_cmp; i++) {
				value.x = v[i];
				mdp_agent.reset(null);		// recompute MDP
				
				fw_strat.write(v[i] + "," + mdp_agent.getFirstRoundBid() + "\n");					
//				if (value.x != 0) {
//					System.out.println("value.x = " + value.x);
//					mdp_agent.printpi();
//					mdp_agent.printV();
//				}
				}	
			fw_strat.close();
		}

	}	
}

