package legacy;

// Simply testing "getPMF" in JointDistributionEmpirical. 
public class TestgetPMF {
	public static void main(String args[])
	{
		// Create a simple joint empirical distribution
		int no_goods = 2;
		double precision = 1.0;
		double max_price = 2.0;
		JointDistributionEmpirical jde = new JointDistributionEmpirical(no_goods, precision, max_price);

		double[] obs1=new double[2], obs2 = new double[2], obs3 = new double[2];
		
		obs1[0] = 0.0;
		obs1[1] = 0.0;
		obs2[0] = 1.0;
		obs2[1] = 1.0;
		obs3[0] = 2.0;
		obs3[1] = 2.0;

		jde.populate(obs1);
		jde.populate(obs2);
		jde.populate(obs3);
		
		jde.normalize();
		
		double[] realized1, realized2, result1, result2;
		realized1 = new double[0];
		realized2 = new double[1];
		realized2[0] = 1.0;
		
		result1 = jde.getPMF(realized1);
		result2 = jde.getPMF(realized2);
	
		System.out.print("result1 = ");
		for (int i = 0; i < result1.length; i++)
			System.out.print(result1[i]+" ");
		System.out.println();
		
		System.out.print("result2 = ");
		for (int i = 0; i < result2.length; i++)
			System.out.print(result2[i]+" ");
		System.out.println();
	}
}
