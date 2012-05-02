package LMSR;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import speed.IntegerArray;
import speed.WinnerAndRealized;
import legacy.CartesianProduct;

public class Cache {
	// Configuration
	static final int max_times = 15;	// 3^15 = 14 million

//	// Map from StateActionPair to index
//	static Integer getWRidx(StateActionPair past) {
//		return WR2idx.get(past);
//	}
//
//	// Map from index to StateActionPair
//	static StateActionPair getWRfromidx(Integer i){
//		return idx2WR.get(i);
//	}
	

	// XXX: new stuff here
	static Set<IntegerArray>[] possible_states;

	// Cache: generate possible states for round times: Cartesian product over {-1,0,1}
	static Set<IntegerArray> genStates(int times) {
		Set<IntegerArray> states = possible_states[times];
		if (states.size() == 0) {
			IntegerArray ActionSpace = new IntegerArray(new int[] {-1,0,1});
			states = CartesianProduct.generate(ActionSpace, times);
			possible_states[times] = states;
		}
		return states;
	}
	
	// Cache: generate all possible states
	static Set<IntegerArray>[] GenAllStates(int total_times) {
		for (int i = 0; i < total_times; i++)
			genStates(i);
		return Arrays.copyOfRange(possible_states, 0, total_times);
	}

	// Cached: possible extensions of next round states from a state-action pair
	static HashMap<IntegerArray,ArrayList<IntegerArray>>[] extension_states;

	// Not Cached: extension states of SA, a state action pair
	static ArrayList<IntegerArray> ExtensionStates(IntegerArray SA, int no_agents){
		ArrayList<IntegerArray> ret = extension_states[SA.d.length].get(SA);
		if (ret == null) {
			ret = new ArrayList<IntegerArray>();
			IntegerArray ActionSpace = new IntegerArray(new int[] {-1,0,1});
			Set<IntegerArray> toappend = new HashSet<IntegerArray>();
			toappend = CartesianProduct.generate(ActionSpace, no_agents-1);
			
			int[] holder = new int[SA.d.length+no_agents-1];		// holder
			for (int i = 0; i < SA.d.length; i++)
				holder[i] = SA.d[i];
			
			for (IntegerArray s: toappend) {
				for (int i = 0; i < s.d.length; i++)
					holder[SA.d.length + i] = s.d[i];
				ret.add(new IntegerArray(Arrays.copyOf(holder, holder.length)));
			}
			extension_states[SA.d.length].put(SA, ret);
		}
		return ret;
	}

	@SuppressWarnings("unchecked")
	public static void init() {
		
		// All MDP policies
//		MDPpolicies = new HashMap<Integer,HashMap<StateActionPair, Double>[]>();
		
		// Mapping between StateActionPair and indices
//		allWR = new HashSet<StateActionPair>();
//		WR2idx = new HashMap<StateActionPair,Integer>();
//		idx2WR = new HashMap<Integer,StateActionPair>();
		

		// All states: Cartesian product over {-1,0,1}
		possible_states = new Set[max_times];
		for (int i = 0; i<max_times; i++)
			possible_states[i] = new HashSet<IntegerArray>();

		// All extension states
		extension_states = new HashMap[max_times];		
		for (int i = 0; i<max_times; i++)
				extension_states[i] = new HashMap<IntegerArray, ArrayList<IntegerArray>>();
		
	}
	
	public static void main(String args[]) {
		long start = System.currentTimeMillis();
		Cache.init();
		
		// Test genAllStates utilities
		int no_agents = 5;
		int no_rounds = 2;
		int total_times = no_agents*no_rounds;
		
		Set<IntegerArray>[] all = Cache.GenAllStates(total_times);
		for (int i = 0; i < all.length; i++){
			Iterator<IntegerArray> it = all[i].iterator();
			if (i > 0)
				System.out.println("all[" + i + "].size() = " + all[i].size() + ". First element = " + it.next().print());
			else
				System.out.println("all[" + i + "].size() = " + all[i].size());
		}

		// Test ExtensionStates() utilities
		System.out.println("\n");
		IntegerArray SA = new IntegerArray(new int[] {-1, 0});
		for (no_agents = 2; no_agents < 5; no_agents ++) {
			ArrayList<IntegerArray> furtherstates = new ArrayList<IntegerArray>();
			Cache.init();			// Have to clean no_agents
			furtherstates = ExtensionStates(SA, no_agents);
			System.out.println("no_agents = " + no_agents + ", furtherstates.size() = " + furtherstates.size() + ", first element = " + furtherstates.get(0).print());
		}
		
		long finish = System.currentTimeMillis();
		System.out.println("Computed in " + (finish-start) + " milliseconds.");
	}
}
