package speed;

// Test if FullCondMDPAgent2 works as expected. For now,   
public class TestFullCondMDPAgent {
	public static void main(String args[]) {

		// Init cache. Must be done once at program start.
		Cache.init();

		double precision = 1.0, max_price = 1.0;
		int no_goods = 2;
		
		// Create uniform distribution
		JointCondFactory jcf = new JointCondFactory(no_goods, precision, max_price);
		JointCondDistributionEmpirical jcde = jcf.makeUniform(false); 
		
		// Test if MDP calculate is right (things printed within computeFullMDP)
		Value v = new SimpleValue(no_goods);

		FullCondMDPAgent agent = new FullCondMDPAgent(1, v);
		agent.setCondJointDistribution(jcde);
		agent.computeFullMDP();
		
		// Let's see if computations are correct
		agent.printpi();
		agent.printV();

	}
	
}