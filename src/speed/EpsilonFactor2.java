package speed;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

// Play agent[0] against others and measure its utility 
public class EpsilonFactor2 {

	double[] utility;
	
	public EpsilonFactor2() throws IOException {				
	}

	// Calculate the first agent's utility against the others 
	public void StrategyDistance(SeqAuction auction, int no_iters) throws IOException{
		
		SeqAgent[] agents = auction.agents;
		double[] utility = new double[no_iters];
		this.utility = utility;
		
		// Play auctions
		for (int j = 0; j<no_iters; j++) {
	
			// Cause each agent to take on a new valuation by calling reset() on their valuation function
			for (int k = 0; k<agents.length; k++)
				agents[k].v.reset();
		
			// Play the auction. This will call the agent's reset(), which will cause MDP to be recomputed.
			// so long as the agent's reset() function calls its computeMDP().
			auction.play(true, null);		// true=="quiet mode", null=="don't write to disk"
			
			// record utility of agent 0
			utility[j] = auction.profit[0];
			}
	}

	// Refined calculation of strategy distance. 
	// 		INPUTS:
	// auction: all agents endowed with P[T-1]; use no_pts/no_agents points to generate P[T], and no_pts to evaluate utility
	// 		OUTPUTS:
	// a double[2] vector; [0] == difference in means, [1] == standard deviation
	public double[] RefinedStrategyDistance(SeqAuction auction, JointCondDistributionEmpirical oldP, int no_pts) throws IOException{
		
		double[] means = new double[2], stdevs = new double[2];
		
		JointCondFactory jcf = new JointCondFactory(auction.no_goods, oldP.precision, oldP.max_price);
		
		// evaluate u(\sigma^t,\sigma^t), also generate P[T] 
		JointCondDistributionEmpirical newP = jcf.simulAllAgentsOneRealPP(auction, no_pts/auction.no_agents, false, false, true);
		means[0] = Statistics.mean(jcf.utility);
		stdevs[0] = Statistics.stdev(jcf.utility);
		
		// evaluate u(\sigma^{t+1}, \sigma^t)
		auction.agents[0].setCondJointDistribution(newP);
		EpsilonFactor2 ef = new EpsilonFactor2();	// XXX: can call itself here? I believe so. 
		ef.StrategyDistance(auction, no_pts);
		means[1] = Statistics.mean(ef.utility);
		stdevs[1] = Statistics.stdev(ef.utility);
		
		// put together to return
		double[] toreturn = new double[2];
		toreturn[0] = means[1]-means[0];
		toreturn[1] = ((Math.sqrt(stdevs[0]*stdevs[0] + stdevs[1]*stdevs[1]))/Math.sqrt(no_pts));
		
		return toreturn;
	}

	
	// Testing: show shrinkage of epsilon factor after a few iterations in 2 round SP Menezes
	public static void main(String args[]) throws IOException {
		
		Cache.init();
		Random rng = new Random();
		
		// Auction parameters
		boolean decreasing = true;						// decreasing MV in Menezes valuation
		double max_value = 1.0;
		double precision = 0.05;
		double max_price = max_value;
		int no_goods = 2;
		int no_agents = 3;
		int nth_price = 2;
		
		// simulation parameters
		int no_initial_simulations = 10000000/no_agents;	// generating initial PP		
		int T = 10;								// no. of Wellman updates
		int[] NO_PTS = new int[] {1000, 2000, 5000, 10000, 20000, 50000, 100000, 200000, 500000, 1000000};
		int no_per_iteration = 100000;			// no. of games played in each Wellman iteration
		double order = 1.0;

		// agent preferences
		int preference = 0;
		double epsilon = 0.0001;
		boolean discretize_value = false;
		double v_precision = 0.0001;

		// evaluation parameters
		double cmp_precision = 0.01;						// discretization step for valuation examining
		int no_for_cmp = (int) (1/cmp_precision) + 1;
		double[] means, stdevs;
		
		boolean take_log = false;						// record prices for agents
		boolean record_prices = false;					// record prices for seller
		boolean print_strategy = true;					// Output strategy S(t)
		boolean record_utility = false;
		

		
		// record all prices (to compute distances later)
		JointCondDistributionEmpirical[] PP = new JointCondDistributionEmpirical[T+1];		

		// 1)  Initiate PP from Polynomial agents
		System.out.println("Generating initial PP");
		JointCondFactory jcf = new JointCondFactory(no_goods, precision, max_price);
		
		// 1.1)	Create PP[0]
		PolynomialAgent[] poly_agents = new PolynomialAgent[no_agents];
		MenezesMultiroundValue[] poly_values = new MenezesMultiroundValue[no_agents];
		for (int i = 0; i<no_agents; i++){
			poly_values[i] = new MenezesMultiroundValue(max_value, rng, decreasing);
			poly_agents[i] = new PolynomialAgent(poly_values[i], i, order);
		}

		SeqAuction poly_auction = new SeqAuction(poly_agents, nth_price, no_goods);		
		
		PP[0] = jcf.simulAllAgentsOnePP(poly_auction, no_initial_simulations,take_log,record_prices,false);
				
			// Utility comparison storage
			means = new double[NO_PTS.length];
			stdevs = new double[NO_PTS.length];
		
			// initiate agents for later bid comparison
			MenezesMultiroundValue value = new MenezesMultiroundValue(max_value, rng, decreasing);
			MDPAgentSP mdp_agent = new MDPAgentSP(value, 1, preference, discretize_value, v_precision);
			
			// initiate updating agents
			MDPAgentSP[] mdp_agents = new MDPAgentSP[no_agents];
			for (int i = 0; i < no_agents; i++)
				mdp_agents[i] = new MDPAgentSP(new MenezesMultiroundValue(max_value, rng, decreasing), i, preference, discretize_value, v_precision);
			
			SeqAuction updating_auction = new SeqAuction(mdp_agents, nth_price, no_goods);
			
			// initiate tools to compare strategies
			double[] v = new double[no_for_cmp]; 		
			for (int i = 0; i < no_for_cmp; i++)
				v[i] = i*cmp_precision;
			double[][] strategy = new double[T + 1][no_for_cmp];
		
		// 1.2)	Record initial strategy
		for (int i = 0; i < no_for_cmp; i++) {
			poly_values[0].x = v[i];
			strategy[0][i] = poly_agents[0].getBid(0);
		}	
		
		// 2) Wellman updates
		for (int it = 0; it < T; it++) {
			
			System.out.println("Wellman iteration = " + it);

			// Set new gamma and PPs
			for (int i = 0; i < no_agents; i++)
				mdp_agents[i].setCondJointDistribution(PP[it]);
			
			Cache.clearMDPpolicy();
			
			// 2.1) generate new PP
			PP[it+1] = jcf.simulAllAgentsOnePP(updating_auction, no_per_iteration/no_agents, take_log, record_prices, record_utility);

			// 2.2) output first round bids for comparison 
			if (print_strategy == true) {
					
				// initiate agents to compare bids
				mdp_agent.setCondJointDistribution(PP[it]);
				
				// Assign values, instead of sample values
				for (int i = 0; i < no_for_cmp; i++) {
					value.x = v[i];
					mdp_agent.reset(null);		// recompute MDP
					strategy[it+1][i] = mdp_agent.getFirstRoundPi();
				}	
			}
		}

		// output strategies from each iteration
		if (print_strategy == true){

			// Name output file
			FileWriter fw_strat = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/paper/june1st/epsilon/FRbid_" + order + "_" + no_agents + "_" + precision + "_" + T + "_" + no_per_iteration + "pts.csv");

			// write first round bidding functions
			for (int i = 0; i < strategy.length; i++){
				for (int j = 0; j < strategy[i].length - 1; j++){
					fw_strat.write(strategy[i][j] + ",");
				}
				fw_strat.write(strategy[i][strategy[i].length-1] + "\n");
			}
			fw_strat.close();

			
			// Compute epsilons and output
			System.out.println("computing price distances...");
			
			// Name output file
			FileWriter fw_epsilon = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/paper/june1st/epsilon/epsilon_" + order + "_" + no_agents + "_" + precision + "_" + T + "_" + no_per_iteration + "pts.csv");

			// initiate comparison tools
			EpsilonFactor2 ef = new EpsilonFactor2();				
			
			int fix_preference = 0;
			MDPAgentSP[] cmp_agents = new MDPAgentSP[no_agents];
			for (int k = 0; k < no_agents; k++)
				cmp_agents[k] = new MDPAgentSP(new MenezesMultiroundValue(max_value, rng, decreasing), k, fix_preference, discretize_value, v_precision);			
			SeqAuction cmp_auction = new SeqAuction(cmp_agents, nth_price, no_goods);
						
			// compute epsilon factor for final PP
			for (int j = 0; j < NO_PTS.length; j++){
				
				int no_pts = NO_PTS[j];
				Cache.clearMDPpolicy();

				// compute distance
				for (int k = 0; k < no_agents; k++)
					cmp_agents[k].setCondJointDistribution(PP[PP.length-1]);					
				double[] temp = ef.RefinedStrategyDistance(cmp_auction, PP[PP.length-1], no_pts);
												
				fw_epsilon.write(no_pts + "," + temp[0] + "," + temp[1] + "\n"); 
				System.out.print(no_pts + "," + temp[0] + "," + temp[1] + "\n"); 
				
			}
			fw_epsilon.close();			
		System.out.println("done done");
		
		}

	}
		
}
