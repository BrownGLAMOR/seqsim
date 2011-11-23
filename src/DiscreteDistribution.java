import java.util.ArrayList;


public abstract class DiscreteDistribution {
	protected ArrayList<Double> f;
	protected double precision;
	
	// F is a vector of probabilities. The sum(F) must equal 1.
	// precision is how prices are mapped to index of the vector.
	// for example:
	//    precision=0.1  ==> one decimal place (e.g., 1.23/0.1 == 12.3 == idx 12)
	//    precision=1.0  ==> integer (e.g. 14.67/1.0 == 14.67 == idx 15)
	//    precision=10.0 ==> ten's place (e.g., 12.44/10 == 1.244 == idx 1)
	// V is an upper bound on prices.
	public DiscreteDistribution(ArrayList<Double> F, double precision) {
		this.f = F;
		this.precision = precision;
	}
	
	// returns the probability that p is the final price, given that a bid of b has already been observed. Pr(p|b)
	public double getProb(double p, double b) {
		return getProb(bin(p), b);
	}

	// returns the expected final price, given a current bid b
	// ala wellman section 4.2 page 12
	public double getExpectedFinalPrice(double b) {
		double price = 0;
		
		for (int i = bin(b); i<f.size(); i++)
			price += getProb(i, b) * val(i);
		
		return price;
	}
	
	// returns the Kolmogorov-Smirnov (KS) statistic between this DiscreteDistribution
	// and another. NOTE: both distrbutions must use the same precision!
	public double getKSStatistic(DiscreteDistribution f_prime) {
		double ks = 0;
		
		// we compute the CDF on the fly.
		double F = 0;			// CDF of f
		double F_prime = 0;		// CDF of f_prime
		
		int max_i = Math.max(f.size(), f_prime.f.size());
		
		for (int i = 0; i<max_i; i++) {
			F += i < f.size() ? f.get(i) : 0;
			F_prime += i < f_prime.f.size() ? f_prime.f.get(i) : 0;
			
			if (Math.abs(F - F_prime) > ks)
				ks = Math.abs(F - F_prime);
		}
		
		return ks;
	}
	
	// computes the CDF at the given price, given the that a bid of b has already been observed
	public double getCDF(double p, double b) {
		double F = 0; // CDF of F
		
		for (int i = 0; i<=bin(p); i++)
			F += getProb(i, b);
		
		return F;
	}
	
	// prints out some debugging information
	public void print(double b) {
		for (int i = 0; i<f.size(); i++)
			System.out.println((i*precision) + ": " + (getProb((int)i*precision, b) * 100) + "%");
	}
	
	public double getPrecision() {
		return precision;
	}
	
	protected abstract double getProb(int idx, double b);

	protected int bin(double p) {
		return (int) Math.round(p / precision);
	}
	
	protected double val(int idx) {
		return idx * precision;
	}
}