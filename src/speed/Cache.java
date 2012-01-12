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
	static HashMap<DoubleArray, Set<double[]>>[] cartesian;
	static Set<double[]> getCartesianProduct(double[] prices, int times) {
		DoubleArray tmp = new DoubleArray(prices);
		
		Set<double[]> ret = cartesian[times].get(tmp);
		
		if (ret == null) {
			ret = CartesianProduct.generate(prices, times);
			cartesian[times].put(tmp, ret);
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
		cartesian = new HashMap[max_goods];
		for (int i = 0; i<max_goods; i++)
			cartesian[i] = new HashMap<DoubleArray, Set<double[]>>();
		
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
