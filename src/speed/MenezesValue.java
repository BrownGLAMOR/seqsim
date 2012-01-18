package speed;

import java.util.Random;

public class MenezesValue extends Value {

		double max_value, precision, x, delta_x;
		Random rng;

		public MenezesValue(double max_value, double precision, Random rng) {
			this.max_value = max_value;
			this.precision = precision;
			this.rng = rng;
			reset();
		}

		@Override
		public void reset() {
			this.x = rng.nextDouble()*max_value;
			this.delta_x = x + x*x/max_value;		// Assume that delta(x) = x + x^2/max_value, so that delta(x) <= 2x
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
