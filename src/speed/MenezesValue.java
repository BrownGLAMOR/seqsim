package speed;

import java.util.Random;

public class MenezesValue extends Value {

		double max_value, precision, x, delta_x;
		boolean decreasing;
		Random rng;

		public MenezesValue(double max_value, Random rng, boolean decreasing) {
			this.max_value = max_value;
			this.rng = rng;
			this.decreasing = decreasing;
			reset();
		}

		@Override
		public void reset() {
			this.x = rng.nextDouble()*max_value;
			if (decreasing == true)
				this.delta_x = x + x*x/max_value;		// Assume that delta(x) = x + x^2/max_value, so that delta(x) <= 2x
			else
				this.delta_x = x + java.lang.Math.pow(x,0.5);
//			this.delta_x = x + java.lang.Math.sqrt(x);
		}
		
		@Override
		public double getValue(int no_goods_won) {
			if (no_goods_won == 0)
				return 0.0;
			else if (no_goods_won == 1)
				return x;
			else
				return delta_x;
		}

}
