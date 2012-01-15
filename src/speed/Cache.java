package speed;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import legacy.CartesianProduct;
import legacy.PowerSet;

public class Cache {
	// Configuration
	static final int max_goods = 11;

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
	
	// CACHE: SET OF ALL GOODS 
	// i.e., no_goods == 4, return value == {0, 1, 2, 3, 4}
	static Set<Integer>[] set_of_all_goods;
	
	// CACHE: POWERSET OF ALL GOODS
	// i.e., no_goods == 2, return value == {{}, {0}, {1}, {0, 1}}
	public static Set<Set<Integer>>[] powerset_of_all_goods;
	
	@SuppressWarnings("unchecked")
	public static void init() {
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
		
		System.out.println("Initialized in " + (finish-start) + " milliseconds.");
	}
}
