package legacy;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

// Create a cartesian product of array of prices
public class CartesianProduct{	
	
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
	}
}
	 
