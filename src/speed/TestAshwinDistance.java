package speed;

import java.io.IOException;
import java.util.Random;

// Tests whether AshwinDistance.java works
public class TestAshwinDistance {

	public static void main(String[] args) throws IOException {
		Cache.init();
		
		Random rng = new Random();
		double precision = 1.0;
		int no_samples = 1000, no_goods = 2, max_price = 10;
		
		// create distributions
		double[][] realized_old = new double[no_samples][no_goods];
		double[][] realized_new = new double[no_samples][no_goods];
		JointDistributionEmpirical jde_old = new JointDistributionEmpirical(no_goods, precision, (double) max_price);
		JointDistributionEmpirical jde_new = new JointDistributionEmpirical(no_goods, precision, (double) max_price);		
		for (int i = 0; i < no_samples; i++){
			for (int j = 0; j < no_goods; j++){
				realized_old[i][j] = (double) rng.nextInt(max_price);
				realized_new[i][j] = (double) rng.nextGaussian()+5;
			}
			jde_old.populate(realized_old[i]);
			jde_new.populate(realized_new[i]);
		}
		jde_old.normalize();
		jde_new.normalize();
		
		double distance = 0;
		AshwinDistance ad = new AshwinDistance(distance, realized_old, realized_new, jde_old, jde_new);
		System.out.print("distance = " + distance);
	}

}
