package speed;

import java.util.Random;

// created for testing purposes
public class SimpleValue extends Value{

	double[] v;
	
	public SimpleValue(int no_goods) {
		v = new double[no_goods+1];
		for (int i = 0; i < no_goods; i++) {
			v[i] = (double) 5*i;
		}
	}

	@Override
	public double getValue(int no_goods_won) {
		return v[no_goods_won];
	}

}