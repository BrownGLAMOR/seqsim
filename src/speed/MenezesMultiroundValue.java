package speed;

import java.util.Random;

// Extend MenezesValue to multirounds
public class MenezesMultiroundValue extends Value {

		double max_value, precision, x;
		boolean decreasing;
		Random rng;

		public MenezesMultiroundValue(double max_value, Random rng, boolean decreasing) {
			this.max_value = max_value;
			this.rng = rng;
			this.decreasing = decreasing;
			reset();
		}

		@Override
		public void reset() {
			this.x = rng.nextDouble()*max_value;
		}
		
		@Override
		public double getValue(int no_goods_won) {

			// first marginal value is x
			if (no_goods_won == 0)
				return 0.0;
			else if (no_goods_won == 1)
				return x;
			// later marginal values depend on scheme
			else if (decreasing == true){
				if (no_goods_won == 2)
					return x + x*x/max_value;
				else
					return x + x*x/max_value + x*x*x/max_value;
			}
			else{
				if (no_goods_won == 2)
					return x + Math.pow((x/max_value),0.5)*max_value;
				else
					return x + Math.pow((x/max_value),0.5)*max_value + Math.pow((x/max_value),(double) (1.0/3.0))*max_value;
			}
		}
		
		// Test
		public static void main(String args[]) {

			// parameters
			double max_value = 1.0;
			Random rng = new Random();
			boolean decreasing = true;
			Value v;
			
			// decreasing vs. increasing scheme
			v = new MenezesMultiroundValue(max_value, rng, decreasing);			
			v.reset();
			System.out.println("decreasing scheme: value(0,1,2,3 goods) = [" + v.getValue(0) + "," + v.getValue(1) + "," + v.getValue(2) + "," + v.getValue(3) + "]");
			
			v = new MenezesMultiroundValue(max_value, rng, false);
			v.reset();
			System.out.println("increasing scheme: value(0,1,2,3 goods) = [" + v.getValue(0) + "," + v.getValue(1) + "," + v.getValue(2) + "," + v.getValue(3) + "]");
			
			// GOOD, works. 
		}
}
