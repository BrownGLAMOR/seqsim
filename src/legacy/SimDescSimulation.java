package legacy;
import java.util.HashMap;
import java.util.List;


public class SimDescSimulation {
	// Implements a simultaneous, ascending, auction for n agents and m goods.

	private List<Agent> agents;
	private List<SBAuction> auctions;
	private int rounds = 0;
	
	public SimDescSimulation(List<Agent> agents, List<SBAuction> auctions) {
		this.agents = agents;
		this.auctions = auctions;
	}
	
	public void play() {
		// Tell agents the auctions are open.
		for (int i = 0; i<agents.size(); i++) {
			agents.get(i).openAllAuctions();
		}
		
		boolean activity;
		do {			
			// Ask agents for their for their bids for current round 
			for (int i = 0; i<agents.size(); i++) {
				// i_bids is agent i's bids, as a hash mapping of auction # to bid amount
				HashMap<Integer, Double> i_bids = agents.get(i).getBids();
			
				// i: agent idx, j: auction idx.
				
				for (Integer j : i_bids.keySet())
					auctions.get(j).submitBid(i, i_bids.get(j)); // bids below ask will get rejected.
			}
			
			// Apply allocation & payment rules to each separate auction
			for (SBAuction a : auctions)
				a.solveAuction();
			
			// Check for activity.
			activity = false;
			for (int j = 0; j < auctions.size(); j++) {
				if (auctions.get(j).getNoWinners() == 0 && auctions.get(j).getLastAskPrice() > auctions.get(j).getReservePrice()) {
					activity = true;
					break;
				}
			}
			
			// Report round results.
			System.out.println("---ROUND " + rounds + "---");
			System.out.println("");
			report();
			
			rounds++;
		} while (activity);
		
		// Tell agents the auctions are closed.
		for (int i = 0; i<agents.size(); i++) {
			agents.get(i).closeAllAuctions();
		}
	}
	
	public void report() {
		// Report current auction results
		for (SBAuction a : auctions) {
			a.report();
			System.out.println("");
		}
		
		System.out.println("");
		
		// Report results per-agent
		for (Agent a : agents)
			System.out.println(a.information());
	}
}
