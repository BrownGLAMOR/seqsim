package legacy;
import java.util.ArrayList;
import java.util.List;

// Runs a SeqSSSimulation Simulation to test if FullMDPAgent is working
public class TestFullMDP {
	
	public static void main(String args[])
	{		
		
		// Create a simple joint empirical distribution
		int no_goods = 2;
		double precision = 1.0;
		double max_price = 1.0;
		JointDistributionEmpirical jde = new JointDistributionEmpirical(no_goods, precision, max_price);

		double[] obs1=new double[2], obs2 = new double[2], obs3 = new double[2];
		
		obs1[0] = 0.0;
		obs1[1] = 0.0;
		obs2[0] = 1.0;
		obs2[1] = 1.0;
//		obs3[0] = 2.0;
//		obs3[1] = 2.0;

		jde.populate(obs1);
		jde.populate(obs2);
//		jde.populate(obs3);

		jde.normalize();
		
		// Create valuations		
		Valuation v = null;
		v = new TestValuation(no_goods);		

		// Create agents
		int no_agents = 2;
		List<Agent> agents = new ArrayList<Agent>(no_agents);
		for (int i = 0; i < no_agents; i++){
			agents.add(new FullMDPAgent(i,v,jde));
		}
		
		// Create auctions
		List<SBAuction> auctions = new ArrayList<SBAuction>(no_goods);
		for (int i = 0; i<no_goods; i++) {
			auctions.add(new SBNPAuction(i, 0, 0, 0, agents, 2));
		}

		// Play the auction
		SeqSSSimulation s = new SeqSSSimulation(agents,auctions);
		s.play();
			
	}
}
