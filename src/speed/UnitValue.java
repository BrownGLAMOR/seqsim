package speed;

import java.util.Random;

// implements unit-demand valuation, simple a draw of x ~ unif(0,max_value)
public class UnitValue extends Value {
	
	double max_value, x;
	Random rng;

	public UnitValue(double max_value, Random rng) {
		this.max_value = max_value;
		this.rng = rng;
		reset();
	}
	
	@Override
	public double getValue(int no_goods_won) {
		if (no_goods_won >= 1)
			return x;
		else
			return 0.0;
	}

	@Override
	public void reset() {
		// draw a new valuation
		this.x = rng.nextDouble() * max_value;
//		System.out.println("reset, x = " + x);
	}

}
