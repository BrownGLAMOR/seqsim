import java.util.ArrayList;

// Testing that the "getHOB" function works well in SBAuction.java
public class TestgetHOB {

	public static void main(String args[]) {
		
		int no_agents = 3, no_auctions = 2, i, j;
		ArrayList<SBAuction> auctions = new ArrayList<SBAuction>();
		ArrayList<Agent> agents = new ArrayList<Agent>();
		Valuation v = new SchedulingValuation(no_auctions); 

		// create list of agents
		for (i = 0; i < no_agents; i++)
			agents.add(new RandomAgent(i,v));
		
		// create list of auctions
		for (i = 0; i < no_auctions; i++)
			auctions.add(new SBNPAuction(i, 0.0, 0, 0, agents, 2));
	
		// submit bids
		for (i = 0; i < no_auctions; i++) {
			for (j = 0; j < no_agents; j++) {
				auctions.get(i).submitBid(j, (i+1)*j);
				System.out.println("agent "+j+" submits "+(i+1)*j+" in round "+i+".");
			}
		}

		// query HOBs
		for (i = 0; i < no_auctions; i++) {
			for (j = 0; j < no_agents; j++) {
				System.out.println("round " + i + ",agent " + j + ": HOB = " + auctions.get(i).getHOB(j));
			}
		}
		
	}	
}
