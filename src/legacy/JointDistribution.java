package legacy;

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
	
	//----- STATIC UTILITY FUNCTIONS BELOW THIS POINT -------
	
	// given a price and precision, return the bin in the histogram
	public static int bin(double p, double precision) {
		return (int)(.5 + p / precision);		
	}

	// given a bin in the histogram and precision, return the price
	public static double val(int idx, double precision) {
		return idx * precision;
	}
}
