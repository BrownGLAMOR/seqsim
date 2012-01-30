package speed;

import java.io.IOException;
import java.util.Set;

public class JointCondFactory extends Thread {
	int no_goods;
	double precision;
	double max_price;
	
	public JointCondFactory(int no_goods, double precision, double max_price) {
		this.no_goods = no_goods;
		this.precision = precision;
		this.max_price = max_price;
	}
	
	// return a uniform joint distribution 
	public JointCondDistributionEmpirical makeUniform() {
		Cache.init();

		JointCondDistributionEmpirical jcde = new JointCondDistributionEmpirical(no_goods, precision, max_price);		

//		Set<BooleanArray> winner = Cache.getWinningHistory(no_goods);
		// enumerate over possible winners and price vectors    
//		for (int[] tmp : Cache.getCartesianProduct(new int[] {0, 1}, no_goods)) {
//			// FIXME: Didn't implement a Cache.getCartesianProduct for boolean array. Do we need one?
//			for (int i = 0; i < no_goods; i++) {
//				if (tmp[i] == 1)
//					winner[i] = true;
//				else
//					winner[i] = false;
//			}
		for (BooleanArray winner : Cache.getWinningHistory(no_goods)) {
			for (IntegerArray realized : Cache.getCartesianProduct(jcde.bins, no_goods)) {
				jcde.populate(new WinnerAndRealized(winner, realized));			
			}
		}

		jcde.normalize();
		
		return jcde;
	}
	
	// return a joint by playing simulations. the distribution is produced by taking the HOB of agent_idx. TODO: to modify
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
	
	// return an array of joints by playing simulations. the distribution is produced by taking the HOB of each agent. TODO: to modify
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
	public JointCondDistributionEmpirical simulAllAgentsOnePP(SeqAuction auction, int no_simulations) throws IOException {	
		SeqAgent[] agents = auction.agents;

		// create JDEs, one per agent	
		JointCondDistributionEmpirical jcde = new JointCondDistributionEmpirical(no_goods, precision, max_price);

		// Array to store winners
		boolean[] w = new boolean[no_goods];
		
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
	public static void main(String args[]) {
		int no_goods = 2;
		double precision = 1;
		double max_price = 3;
		
		JointCondFactory jcf = new JointCondFactory(no_goods, precision, max_price);
		JointCondDistributionEmpirical jcde = new JointCondDistributionEmpirical(no_goods, precision, max_price);
		jcde = jcf.makeUniform();
		
		double[] pmf; 

		// print first round unconditional price
		pmf = jcde.getPMF(new boolean[0], new double[0]);
		System.out.print("pmf(1 | { }, { }) = {");
		for (int i = 0; i < pmf.length; i++)
			System.out.print(pmf[i] + " ");
		System.out.println("}");

		// print some second round conditional prices
		pmf = jcde.getPMF(new boolean[] {false}, new double[] {2});
		System.out.print("pmf(1 | {0}, {2}) = {");
		for (int i = 0; i < pmf.length; i++)
			System.out.print(pmf[i] + " ");
		System.out.println("}");
		
		pmf = jcde.getPMF(new boolean[] {true}, new double[] {3});
		System.out.print("pmf(1 | {1}, {3}) = {");
		for (int i = 0; i < pmf.length; i++)
			System.out.print(pmf[i] + " ");
		System.out.println("}");
	}

}
