package speed;

public class MathOps {
	// performs integer exponentiation using the squaring technique.
	public static int ipow(int base, int exp) {
	    int result = 1;
	    
	    while (exp != 0) {
	        if ((exp & 1) != 0)
	            result *= base;
	        
	        exp >>= 1;
	        base *= base;
	    }

	    return result;
	}
	
	// performs integer exponentiation using the squaring technique.
	public static long lpow(long base, long exp) {
	    long result = 1;
	    
	    while (exp != 0) {
	        if ((exp & 1) != 0)
	            result *= base;
	        
	        exp >>= 1;
	        base *= base;
	    }

	    return result;
	}
}
