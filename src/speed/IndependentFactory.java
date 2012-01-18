package speed;

import java.io.IOException;

import legacy.DiscreteDistribution;
import legacy.DiscreteDistributionWellman;
import legacy.Histogram;

// Generates an array of DiscreteDistribution as output of simulations
public class IndependentFactory extends Thread {
	int no_goods;
	double precision;
	double max_price;
	
	public IndependentFactory(int no_goods, double precision, double max_price) {
		this.no_goods = no_goods;
		this.precision = precision;
		this.max_price = max_price;
	}
	
	// return a uniform joint distribution TODO: to modify this
	public JointDistributionEmpirical makeUniform() {
		JointDistributionEmpirical jde = new JointDistributionEmpirical(no_goods, precision, max_price);
		
		// enumerate over possible price vectors
		for (IntegerArray realized : Cache.getCartesianProduct(jde.bins, no_goods))
			jde.populate(realized);

		jde.normalize();
		
		return jde;
	}
	
	// return a joint by playing simulations. the distribution is produced by taking the HOB of agent_idx.
	public DiscreteDistribution[] simulOneAgent(SeqAuction auction, int agent_idx, int no_simulations) throws IOException {	
		SeqAgent[] agents = auction.agents;

		// Initiate storage box
		Histogram hist[] = new Histogram[auction.no_goods];
		DiscreteDistribution[] pd = new DiscreteDistribution[auction.no_goods];
		for (int i = 0; i < auction.no_goods; i++)
			hist[0] = new Histogram(precision);
		
		for (int j = 0; j<no_simulations; j++) {
			// Cause each agent to take on a new valuation by calling reset() on their valuation function
			for (int k = 0; k<agents.length; k++)
				agents[k].v.reset();
		
			// Play the auction. This will call the agent's reset(), which will cause MDP to be recomputed.
			// (so long as the agent's reset() function calls its computeMDP()).
			auction.play(true, null);		// true=="quiet mode", null=="don't write to disk"
			
			// Add results to PP distribution
			for (int i = 0; i < auction.no_goods; i++)
				hist[i].add(auction.hob[agent_idx][i]);
		}

		for (int i = 0; i < auction.no_goods; i++)
			pd[i] = new DiscreteDistributionWellman(hist[i].getDiscreteDistribution(), precision);
		
		return pd;
	}
	
	// return an array of joints by playing simulations. the distribution is produced by taking the HOB of each agent. TODO: modify this. 
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

}
