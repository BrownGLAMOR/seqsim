package speed;

import java.util.Random;

public abstract class JointDistribution {
	// get the probability of the good numbered "realized.length" taking on price "price",
	// given the list of realized prices.
	public abstract double getProb(double price, double[] realized);

	// get the probability mass function of the good numbered "realized.length",
	// given the list of realized prices.
	public abstract double[] getPMF(double[] realized);
		
	// get the expected price of the good numbered "realized.length", given list of
	// realized prices.
	public abstract double getExpectedFinalPrice(double[] realized);
	
	// samples the joint distribution and returns an array of prices, one per good.
	public abstract double[] getSample(Random rng);
	
	// debug: get a print out of the JointDistribution
	public abstract void output();
	
	//----- STATIC UTILITY FUNCTIONS BELOW THIS POINT -------
	
	// get the KS statistic between two distributions
	public static double getKSStatistic(double[] a, double[] b) {
		assert (a.length == b.length);
		
		double a_cdf = 0.0;
		double b_cdf = 0.0;
		double max_diff = 0.0;
		
		for (int i = 0; i<a.length; i++) {
			// compute cdf
			a_cdf += a[i];
			b_cdf += b[i];
			
			double diff = a_cdf - b_cdf;		// find difference
			diff = diff < 0 ? -diff : diff; 	// take abs value
			
			if (diff > max_diff)
				max_diff = diff;
		}
		
		return max_diff;
	}
	
	// given a price and precision, return the bin in the histogram
	public static int bin(double p, double precision) {
		return (int)(.5 + p / precision);		
	}

	// given a bin in the histogram and precision, return the price
	public static double val(int idx, double precision) {
		return idx * precision;
	}
}
