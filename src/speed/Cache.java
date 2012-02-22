package speed;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import legacy.CartesianProduct;
import legacy.PowerSet;

public class Cache {
	// Configuration
	static final int max_goods = 11;

	
	// CACHE: store all possible WinnerAndRealized, and map them back and forth into indices
	static Set<WinnerAndRealized> allWR;
	static HashMap<WinnerAndRealized,Integer> WR2idx;
	static HashMap<Integer,WinnerAndRealized> idx2WR;
	
	static Set<WinnerAndRealized> GenarateAllWR(int no_goods, double max_price, double precision) {
		Set<WinnerAndRealized> ret = allWR;
		if (ret.size() == 0) {
			// Generate all bins
			int no_bins = JointCondDistributionEmpirical.bin(max_price, precision) + 1;
			IntegerArray bins = new IntegerArray(no_bins);
			for (int j = 0; j < no_bins; j++)
				bins.d[j] = j;
			
			// Iterate over all possible Winner and Realized
			int i = 0;
			for(BooleanArray winner : getWinningHistory(no_goods)){
				for (IntegerArray realized : Cache.getCartesianProduct(bins, no_goods)) {
					WinnerAndRealized wr = new WinnerAndRealized(winner,realized);
//					System.out.println(wr.print());
					// store them, and map them back and forth into indices
					ret.add(wr);
					WR2idx.put(wr, i);
					idx2WR.put(i, wr);
					i++;
				}
			}
			allWR = ret;
		}
		return allWR;
	}

	// Map from WinnerAndRealized to index
	static Integer getWRidx(WinnerAndRealized past) {
		return WR2idx.get(past);
	}

	// Map from index to WinnerAndRealized
	static WinnerAndRealized getWRfromidx(Integer i){
		return idx2WR.get(i);
	}
	
	// CACHE: Cartesian product over prices
	static HashMap<DoubleArray, Set<double[]>>[] cartesian_prices;	
	static Set<double[]> getCartesianProduct(double[] prices, int times) {
		DoubleArray tmp = new DoubleArray(prices);
		
		Set<double[]> ret = cartesian_prices[times].get(tmp);
		
		if (ret == null) {
			ret = CartesianProduct.generate(prices, times);
			cartesian_prices[times].put(tmp, ret);
		}
		
		return ret;
	}

	// CACHE: Cartesian product over prices
	static HashMap<DoubleArray, Set<DoubleArray>>[] cartesian_prices2;
	static Set<DoubleArray> getCartesianProduct(DoubleArray prices, int times) {	
		Set<DoubleArray> ret = cartesian_prices2[times].get(prices);
		
		if (ret == null) {
			ret = CartesianProduct.generate(prices, times);
			cartesian_prices2[times].put(prices, ret);
		}
		
		return ret;
	}
	
	// CACHE: Cartesian product over bins
	static HashMap<IntegerArray, Set<int[]>>[] cartesian_bins;
	static Set<int[]> getCartesianProduct(int[] bins, int times) {
		IntegerArray tmp = new IntegerArray(bins);
		
		Set<int[]> ret = cartesian_bins[times].get(tmp);
		
		if (ret == null) {
			ret = CartesianProduct.generate(bins, times);
			cartesian_bins[times].put(tmp, ret);
		}
		
		return ret;
	}
	
	// CACHE: Cartesian product over bins
	static HashMap<IntegerArray, Set<IntegerArray>>[] cartesian_bins2;
	static Set<IntegerArray> getCartesianProduct(IntegerArray bins, int times) {
		Set<IntegerArray> ret = cartesian_bins2[times].get(bins);
		
		if (ret == null) {
			ret = CartesianProduct.generate(bins, times);
			cartesian_bins2[times].put(bins, ret);
		}
		
		return ret;
	}

	// Cache: Cartesian product over {true, false}
	static Set<BooleanArray>[] possible_winning_history;
	static Set<BooleanArray> getWinningHistory(int times) {
		Set<BooleanArray> ret = possible_winning_history[times];
		
		if (ret.size() == 0) {
			ret = CartesianProduct.generate(times);
			possible_winning_history[times] = ret;
		}
		
		return ret;
	}

	// CACHE: SET OF ALL GOODS 
	// i.e., no_goods == 4, return value == {0, 1, 2, 3, 4}
	static Set<Integer>[] set_of_all_goods;
	
	// CACHE: POWERSET OF ALL GOODS
	// i.e., no_goods == 2, return value == {{}, {0}, {1}, {0, 1}}
	public static Set<Set<Integer>>[] powerset_of_all_goods;
	
	@SuppressWarnings("unchecked")
	public static void init() {
		
		// Mapping between WinnerAndRealized and indices
		allWR = new HashSet<WinnerAndRealized>();
		WR2idx = new HashMap<WinnerAndRealized,Integer>();
		idx2WR = new HashMap<Integer,WinnerAndRealized>();
		
		// Cartesian product over prices
		cartesian_prices = new HashMap[max_goods];
		for (int i = 0; i<max_goods; i++)
			cartesian_prices[i] = new HashMap<DoubleArray, Set<double[]>>();

		// Cartesian product over prices
		cartesian_prices2 = new HashMap[max_goods];
		for (int i = 0; i<max_goods; i++)
			cartesian_prices2[i] = new HashMap<DoubleArray, Set<DoubleArray>>();
		
		// Cartesian product over bins
		cartesian_bins = new HashMap[max_goods];
		for (int i = 0; i<max_goods; i++)
			cartesian_bins[i] = new HashMap<IntegerArray, Set<int[]>>();
		
		// Cartesian product over bins
		cartesian_bins2 = new HashMap[max_goods];
		for (int i = 0; i<max_goods; i++)
			cartesian_bins2[i] = new HashMap<IntegerArray, Set<IntegerArray>>();

		// Cartesian product over {true, false} histories
		possible_winning_history = new Set[max_goods];
		for (int i = 0; i<max_goods; i++)
			possible_winning_history[i] = new HashSet<BooleanArray>();

		
		// Set Of All Goods
		set_of_all_goods = new Set[max_goods];
		for (int i = 0; i<max_goods; i++) {
			set_of_all_goods[i] = new HashSet<Integer>(i);

			for (int j = 0; j<i; j++)
				set_of_all_goods[i].add(j);
		}
		
		// Power Set of All Goods
		powerset_of_all_goods = new Set[max_goods];
		for (int i = 0; i<max_goods; i++)
			powerset_of_all_goods[i] = PowerSet.generate(set_of_all_goods[i]);
	}
	
	public static void main(String args[]) {
		long start = System.currentTimeMillis();
		Cache.init();
		long finish = System.currentTimeMillis();
		
		// Let's test WinnerAndRealized utilities
		int no_goods = 2;
		double max_price = 1.0;
		double precision = 0.5;
		int idx;
		
		Set<WinnerAndRealized> all = Cache.GenarateAllWR(no_goods, max_price, precision);
		System.out.println("all.size() = " + all.size() + ". Include: ");
		for (WinnerAndRealized wr : all){
			idx = Cache.getWRidx(wr);
			System.out.println(wr.print() + "    , index = " + idx + ", which maps back to " + Cache.getWRfromidx(idx).print());
		}
		
		System.out.println();
		
		for (BooleanArray ba : Cache.getWinningHistory(3)){
			System.out.print("element = {");
			for (int i = 0; i < ba.d.length; i++)
				System.out.print(ba.d[i] + ",");
			System.out.println("}");
		}
		
		System.out.println("Initialized in " + (finish-start) + " milliseconds.");
	}
}
