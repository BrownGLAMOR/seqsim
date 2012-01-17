package speed;

import java.util.Random;

public class KatzHLValue extends Value {
	int N;
	double H, L, max_value, precision;
	Random rng;

	public KatzHLValue(int N, double max_value, double precision, Random rng) {
		this.N = N;
		this.max_value = max_value;
		this.precision = precision;
		this.rng = rng;
		
		reset();
	}
	
	@Override
	public double getValue(int no_goods_won) {
		switch (no_goods_won) {
			case 0: return 0.0;
			case 1: return H;
			case 2: return L+H;
			default:
				System.out.println("ERROR: KatzHLValue cannot give value for no_goods_won = " + no_goods_won);
				return -1;
		}
	}

	@Override
	public void reset() {
		// draw a new valuation
		L = rng.nextDouble() * max_value;
		H = rng.nextDouble() * max_value;
		
		if (L > H) {
			// swap H and L s.t. L < H
			double temp = H;
			H = L;
			L = temp;
		}
	}

}
