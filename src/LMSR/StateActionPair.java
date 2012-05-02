package LMSR;


import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import speed.IntegerArray;

public class StateActionPair {
	public IntegerArray state;
	public IntegerArray stateaction;
	Integer action;
	
	public StateActionPair(int N) {
		// N = k*n + i, if it is k^th move for agent i, no_agents = n. k and i start with 0.    
		state = new IntegerArray(N);
		stateaction = new IntegerArray(N+1);			// State and action combined
	}

	public StateActionPair(IntegerArray state, int action) {
		this.state = state;
		this.action = action;
	}
	
	@Override
	public boolean equals(Object that) {
		// same object
		if (this == that)
			return true;

		// not same class
		if (! (that instanceof StateActionPair))
			return false;
		
		StateActionPair aThat = (StateActionPair) that;
		
		// underlying data points to same array?
		if (this.state == aThat.state && this.action == aThat.action)
			return true;
		
		// else, do pairwise compare
		return (Arrays.equals(this.state.d, aThat.state.d) && (this.action == aThat.action));
	}
	
	// prints the content within
	public String print(){
		String str = "{";
		if (state.d.length == 0)
			str += "}";
		else {		
			for (int i = 0; i < state.d.length - 1; i++)
				str += state.d[i] + ","; 
		}
		str += state.d[state.d.length-1] + "}{";
		str += action + "}";
		
		return str;
	}
	
	@Override
	public int hashCode() {
		return Arrays.hashCode(state.d)*action.hashCode();
	}
	
	// Test
	public static void main(String[] args) throws IOException {

		// Initialize a state (second mover in an ABCABC game, second chance)
		int i = 1;
		int k = 1;
		int no_agents = 3;
		int N = no_agents*k + i;
		
		// Randomly populate the state
		RandomAgent agent = new RandomAgent(0, new Random());
		IntegerArray state = new IntegerArray(N);
		for (int j = 0; j < N; j++){
			state.d[j] = agent.move(0);
		}
		
		// Input a state action pair, and print it
		int action = -1;
		StateActionPair SA = new StateActionPair(state,action);
		System.out.println("SA = " + SA.print());
		
	}

}
