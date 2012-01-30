package legacy;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import speed.BooleanArray;
import speed.DoubleArray;
import speed.IntegerArray;

// Create a cartesian product of array of prices
public class CartesianProduct {
	// FOR DOUBLE ARRAYS
	
	public static Set<double[]> generate(double[] prices, int times) {
		// iteratively generate the cartesian product
		Set<double[]> ret;
		if (times == 0) {
			 ret = new HashSet<double[]>();
			 ret.add(new double[0]);
			return ret;
		} else {
			return _cartesianProduct(0, times, prices);
		}
	}
	
	private static Set<double[]> _cartesianProduct(int index, int times, double[] prices) {	    
		Set<double[]> ret = new HashSet<double[]>();
	    
	    if (index == times) {
	    	// initialize the iteration
	        ret.add(new double[times]);
	    } else {
	    	// iteratively add new elements
	        for (double p : prices) {
	            for (double[] array : _cartesianProduct(index+1, times, prices)) {
	                array[index]=p;
	                ret.add(array);
	            }
	        }
	    }
	    return ret;
	}

	// FOR DOUBLE ARRAYS
	
	public static Set<DoubleArray> generate(DoubleArray prices, int times) {
		// iteratively generate the cartesian product
		Set<DoubleArray> ret;
		if (times == 0) {
			 ret = new HashSet<DoubleArray>();
			 ret.add(new DoubleArray(0));
			return ret;
		} else {
			return _cartesianProduct(0, times, prices);
		}
	}
	
	private static Set<DoubleArray> _cartesianProduct(int index, int times, DoubleArray prices) {	    
		Set<DoubleArray> ret = new HashSet<DoubleArray>();
	    
	    if (index == times) {
	    	// initialize the iteration
	        ret.add(new DoubleArray(times));
	    } else {
	    	// iteratively add new elements
	        for (double p : prices.d) {
	            for (DoubleArray array : _cartesianProduct(index+1, times, prices)) {
	                array.d[index] = p;
	                ret.add(array);
	            }
	        }
	    }
	    return ret;
	}

	// FOR INTEGER ARRAYS
	
	public static Set<int[]> generate(int[] bins, int times) {
		// iteratively generate the cartesian product
		Set<int[]> ret;
		if (times == 0) {
			 ret = new HashSet<int[]>();
			 ret.add(new int[0]);
			return ret;
		} else {
			return _cartesianProduct(0, times, bins);
		}
	}
	
	private static Set<int[]> _cartesianProduct(int index, int times, int[] bins) {	    
		Set<int[]> ret = new HashSet<int[]>();
	    
	    if (index == times) {
	    	// initialize the iteration
	        ret.add(new int[times]);
	    } else {
	    	// iteratively add new elements
	        for (int p : bins) {
	            for (int[] array : _cartesianProduct(index+1, times, bins)) {
	                array[index]=p;
	                ret.add(array);
	            }
	        }
	    }
	    return ret;
	}
	
	
	// FOR INTEGER ARRAYS
	
	public static Set<IntegerArray> generate(IntegerArray prices, int times) {
		// iteratively generate the cartesian product
		Set<IntegerArray> ret;
		if (times == 0) {
			 ret = new HashSet<IntegerArray>();
			 ret.add(new IntegerArray(0));
			return ret;
		} else {
			return _cartesianProduct(0, times, prices);
		}
	}
	
	private static Set<IntegerArray> _cartesianProduct(int index, int times, IntegerArray prices) {	    
		Set<IntegerArray> ret = new HashSet<IntegerArray>();
	    
	    if (index == times) {
	    	// initialize the iteration
	        ret.add(new IntegerArray(times));
	    } else {
	    	// iteratively add new elements
	        for (int p : prices.d) {
	            for (IntegerArray array : _cartesianProduct(index+1, times, prices)) {
	                array.d[index] = p;
	                ret.add(array);
	            }
	        }
	    }
	    return ret;
	}



	// FOR BOOLEAN ARRAYS
//	
//	public static Set<boolean[]> generate(int times) {
//		// iteratively generate the cartesian product
//		Set<boolean[]> ret;
//		if (times == 0) {
//			 ret = new HashSet<boolean[]>();
//			 ret.add(new boolean[] {}); 
//			return ret;
//		} else {
//			return _cartesianProduct(0, times);
//		}
//	}
//	
//	private static Set<boolean[]> _cartesianProduct(int index, int times) {	    
//		Set<boolean[]> ret = new HashSet<boolean[]>();
//	    
//	    if (index == times) {
//	    	// initialize the iteration
//	        ret.add(new boolean[times]);
//	    } else {
//	    	// iteratively add new elements (true, false)
//            for (boolean[] array : _cartesianProduct(index+1, times)) {
//                array[index] = true;
//                ret.add(array);
//            }
//            for (boolean[] array : _cartesianProduct(index+1, times)) {
//                array[index] = false;
//                ret.add(array);
//            }
//	    }
//	    return ret;
//	}

	// FOR BOOLEAN ARRAYS
	
	public static Set<BooleanArray> generate(int times) {
		// iteratively generate the cartesian product
		Set<BooleanArray> ret;
		if (times == 0) {
			 ret = new HashSet<BooleanArray>();
			 ret.add(new BooleanArray(0)); 
			return ret;
		} else {
			return _cartesianProduct(0, times);
		}
	}
	
	private static Set<BooleanArray> _cartesianProduct(int index, int times) {	    
		Set<BooleanArray> ret = new HashSet<BooleanArray>();
	    
	    if (index == times) {
	    	// initialize the iteration
	        ret.add(new BooleanArray(times));
	    } else {
	    	// iteratively add new elements (true, false)
            for (BooleanArray array : _cartesianProduct(index+1, times)) {
                array.d[index] = true;
                ret.add(array);
            }
            for (BooleanArray array : _cartesianProduct(index+1, times)) {
                array.d[index] = false;
                ret.add(array);
            }
	    }
	    return ret;
	}

	
	
	public static void main(String args[]) {
		// TESTING / EXAMPLE

		// a list of possible prices
		int size = 2;
		double[] prices = new double[size];
		for (int i = 0; i < size; i++)
			prices[i]=i;
		
		// enumerate cartesian product of prices
		Set<double[]> enumeration = CartesianProduct.generate(prices, 3);
		
		// print out enumerate prices
		System.out.println("enumerated prices include: ");
		System.out.println("size of enumeration = " + enumeration.size());
		Iterator<double[]> it = enumeration.iterator();
		while (it.hasNext()) {
			double[] p = it.next();
			
			for (int i = 0; i < p.length; i++){
				System.out.print(p[i] + " ");
			}
			System.out.println();

		}

		// -------------- testing boolean "generate"
		
		// enumerate possible winning history
		Set<BooleanArray> w = CartesianProduct.generate(3);
		
		// print out enumerate prices
		System.out.println("enumerated winning history include: ");
		System.out.println("size of winning history = " + enumeration.size());
		
		Iterator<BooleanArray> it2 = w.iterator();
		while (it2.hasNext()) {
			BooleanArray p = it2.next();
			
			for (int i = 0; i < p.d.length; i++){
				System.out.print(p.d[i] + " ");
			}
			System.out.println();

		}

	
	}
}
	 
