package LMSR;

import java.util.Random;

// Simple signal:
// 	>	Boolean world state, true w/ prob p0. 
// 	>	Conditionally iid boolean signals coinciding with world state with probability rho 
public class SimpleSignal extends Signal{
	boolean[] s;
	boolean world, wrong_world;
	double p0, rho;
	int no_agents;
	Random rng;
	
	public SimpleSignal(double p0, double rho, int no_agents, Random rng) {
		this.p0=p0;
		this.rho=rho;
		this.no_agents=no_agents;
		this.rng = rng;
	}

	// Game administrator uses this to know the world state
	public boolean getWorld(){
		return world;
	}
	
	@Override
	// Players use this to get signal
	public boolean getSignal(int agent_id) {
		return s[agent_id];
	}

	@Override
	// Draw the world state and signals
	public void reset() {
		// Draw the world state
		if (rng.nextDouble() <= p0){
			world = true;
			wrong_world = false;
		}
		else{
			world = false;
			wrong_world = true;
		}
		
		// Draw signals
		s = new boolean[no_agents];
		for (int i = 0; i < no_agents; i++){
			if (rng.nextDouble() <= rho)
				s[i] = world;
			else
				s[i] = wrong_world;
		}
	}

}