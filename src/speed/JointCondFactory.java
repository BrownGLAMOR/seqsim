package speed;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

public class JointCondFactory extends Thread {
	int no_goods;
	double precision;
	double max_price;
	boolean take_log;
	
	public JointCondFactory(int no_goods, double precision, double max_price) {
		this.no_goods = no_goods;
		this.precision = precision;
		this.max_price = max_price;
	}
	
	// return a uniform joint distribution 
	public JointCondDistributionEmpirical makeUniform(boolean take_log) {
		Cache.init();

		JointCondDistributionEmpirical jcde = new JointCondDistributionEmpirical(no_goods, precision, max_price, take_log);		

		for (BooleanArray winner : Cache.getWinningHistory(no_goods)) {
			for (IntegerArray realized : Cache.getCartesianProduct(jcde.bins, no_goods)) {
				jcde.populate(new WinnerAndRealized(winner, realized));
			}
		}

		jcde.normalize();
		
		return jcde;
	}
	
	// return a joint by playing simulations. the distribution is produced by taking the HOB of agent_idx. TODO: haven't worked on yet
	public JointDistributionEmpirical simulOneAgent(SeqAuction auction, int agent_idx, int no_simulations) throws IOException {	
		SeqAgent[] agents = auction.agents;

		JointDistributionEmpirical jde = new JointDistributionEmpirical(no_goods, precision, max_price);
		
		for (int j = 0; j<no_simulations; j++) {
			// Cause each agent to take on a new valuation by calling reset() on their valuation function
			for (int k = 0; k<agents.length; k++)
				agents[k].v.reset();
		
			// Play the auction. This will call the agent's reset(), which will cause MDP to be recomputed.
			// (so long as the agent's reset() function calls its computeMDP()).
			auction.play(true, null);		// true=="quiet mode", null=="don't write to disk"
			
			// Add results to PP distribution
			jde.populate(auction.hob[agent_idx]);
		}
		
		jde.normalize();
		
		return jde;
	}
	
	// return an array of joints by playing simulations. the distribution is produced by taking the HOB of each agent. TODO: haven't worked on yet
	public JointDistributionEmpirical[] simulAllAgents(SeqAuction auction, int no_simulations) throws IOException {	
		SeqAgent[] agents = auction.agents;

		// create JDEs, one per agent
		JointDistributionEmpirical[] jde = new JointDistributionEmpirical[agents.length];
		
		for (int j = 0; j<agents.length; j++)
			jde[j] = new JointDistributionEmpirical(no_goods, precision, max_price);

		// Play auctions
		for (int j = 0; j<no_simulations; j++) {
			// Cause each agent to take on a new valuation by calling reset() on their valuation function
			for (int k = 0; k<agents.length; k++)
				agents[k].v.reset();
		
			// Play the auction. This will call the agent's reset(), which will cause MDP to be recomputed.
			// so long as the agent's reset() function calls its computeMDP().
			auction.play(true, null);		// true=="quiet mode", null=="don't write to disk"
			
			// Add results to PP distribution
			for (int k = 0; k<agents.length; k++)
				jde[k].populate(auction.hob[k]);
		}
		
		for (int j = 0; j<agents.length; j++)
			jde[j].normalize();
		
		return jde;
	}

	// return an array of joints by playing simulations. the distribution is produced by taking the HOB of each agent.
//	public JointCondDistributionEmpirical simulAllAgentsOnePP(SeqAuction auction, int no_simulations, boolean take_log) throws IOException {	
//		SeqAgent[] agents = auction.agents;
//
//		// create JDEs, one per agent	
//		JointCondDistributionEmpirical jcde = new JointCondDistributionEmpirical(no_goods, precision, max_price, take_log);
//
//		// Play auctions
//		for (int j = 0; j<no_simulations; j++) {
//			// Cause each agent to take on a new valuation by calling reset() on their valuation function
//			for (int k = 0; k<agents.length; k++)
//				agents[k].v.reset();
//		
//			// Play the auction. This will call the agent's reset(), which will cause MDP to be recomputed.
//			// so long as the agent's reset() function calls its computeMDP().
//			auction.play(true, null);		// true=="quiet mode", null=="don't write to disk"
//			
//			// Add results from ALL agents to PP distribution
//			for (int k = 0; k<agents.length; k++) {
//				boolean[] w = new boolean[no_goods];
//				for (int l = 0; l < no_goods; l++) {
//					if (auction.winner[l] == k)
//						w[l] = true;
//					else
//						w[l] = false;
//				}
//				jcde.populate(w,auction.hob[k]);
//			}
//		}
//		
//		jcde.normalize();
//		
//		return jcde;
//	}

	// return an array of joints by playing simulations. the distribution is produced by taking the HOB of each agent.
	public JointCondDistributionEmpirical simulAllAgentsOnePP(SeqAuction auction, int no_simulations, boolean take_log) throws IOException {	
		SeqAgent[] agents = auction.agents;

		// create JDEs, one per agent	
		JointCondDistributionEmpirical jcde = new JointCondDistributionEmpirical(no_goods, precision, max_price, take_log);

		// Play auctions
		for (int j = 0; j<no_simulations; j++) {
			// Cause each agent to take on a new valuation by calling reset() on their valuation function
			for (int k = 0; k<agents.length; k++)
				agents[k].v.reset();
		
			// Play the auction. This will call the agent's reset(), which will cause MDP to be recomputed.
			// so long as the agent's reset() function calls its computeMDP().
			auction.play(true, null);		// true=="quiet mode", null=="don't write to disk"
			
			// Add results from ALL agents to PP distribution
			for (int k = 0; k<agents.length; k++) {
				boolean[] w = new boolean[no_goods];
				for (int l = 0; l < no_goods; l++) {
					if (auction.winner[l] == k)
						w[l] = true;
					else
						w[l] = false;
				}
				jcde.populate(w,auction.hob[k]);
			}
		}
		
		jcde.normalize();
		
		return jcde;
	}

	
	// Testing
	public static void main(String args[]) throws IOException {
		int no_goods = 2;
		double precision = 1;
		double max_price = 3;
		int no_agents = 2;
		int nth_price = 2;

		// Test make uniform
//		JointCondFactory jcf = new JointCondFactory(no_goods, precision, max_price);
//		JointCondDistributionEmpirical jcde = new JointCondDistributionEmpirical(no_goods, precision, max_price);
//		jcde = jcf.makeUniform();
//		
//		double[] pmf; 
//
//		// print first round unconditional price
//		pmf = jcde.getPMF(new boolean[0], new double[0]);
//		System.out.print("pmf(1 | { }, { }) = {");
//		for (int i = 0; i < pmf.length; i++)
//			System.out.print(pmf[i] + " ");
//		System.out.println("}");
//
//		// print some second round conditional prices
//		pmf = jcde.getPMF(new boolean[] {false}, new double[] {2});
//		System.out.print("pmf(1 | {0}, {2}) = {");
//		for (int i = 0; i < pmf.length; i++)
//			System.out.print(pmf[i] + " ");
//		System.out.println("}");
//		
//		pmf = jcde.getPMF(new boolean[] {true}, new double[] {3});
//		System.out.print("pmf(1 | {1}, {3}) = {");
//		for (int i = 0; i < pmf.length; i++)
//			System.out.print(pmf[i] + " ");
//		System.out.println("}");

	
	
	
		// Test simulAllAgentsOnePP
		JointCondFactory jcf = new JointCondFactory(no_goods, precision, max_price);
		
		// Create agents
		SimpleAgent[] agents = new SimpleAgent[no_agents];
		for (int i = 0; i<no_agents; i++)
			agents[i] = new SimpleAgent(new SimpleValue(no_goods), i);
				
		// Create auction
		SeqAuction auction = new SeqAuction(agents, nth_price, no_goods);

		int no_simulations = 5;
		
		// Simulate
		System.out.print("Start simulating... ");
		JointCondDistributionEmpirical pp = jcf.simulAllAgentsOnePP(auction, no_simulations,true);

		// write result
		FileWriter fw = new FileWriter("/Users/jl52/Desktop/Amy_paper/workspace/test.txt");
		pp.outputRaw(fw);
		fw.close();
		
		pp.outputNormalized();
				
		double[] pmf; 

		// print first round unconditional price
		pmf = pp.getPMF(new boolean[0], new double[0]);
		System.out.print("pmf(1 | { }, { }) = {");
		for (int i = 0; i < pmf.length; i++)
			System.out.print(pmf[i] + " ");
		System.out.println("}");

		// print some second round conditional prices
		pmf = pp.getPMF(new boolean[] {false}, new double[] {3});
		System.out.print("pmf(1 | {0}, {3}) = {");
		for (int i = 0; i < pmf.length; i++)
			System.out.print(pmf[i] + " ");
		System.out.println("}");
		
		pmf = pp.getPMF(new boolean[] {true}, new double[] {3});
		System.out.print("pmf(1 | {1}, {3}) = {");
		for (int i = 0; i < pmf.length; i++)
			System.out.print(pmf[i] + " ");
		System.out.println("}");
	
	}

}
