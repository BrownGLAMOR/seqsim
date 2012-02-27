package speed;

public class Statistics {
// Implements some simple statistical computations

	// Calculates the mean
	public static double mean(double[] x) {
		double ave = 0.0;
		for (int i = 0; i < x.length; i++)
			ave += x[i];
		ave /= x.length;
		return ave;
	}
	
	// Standard deviation
	public static double stdev(double[] x) {
		double mean = Statistics.mean(x);
		double stdev = 0.0;
		for (int i = 0; i < x.length; i++)
			stdev += (x[i] - mean)*(x[i] - mean);
		stdev /= x.length;
		stdev = java.lang.Math.sqrt(stdev);
		return stdev;
	}

	// testing
	public static void main(String[] args){
		double[] x = new double[3];
		x[0] = 1;
		x[1] = 2;
		x[2] = 3;
		System.out.println("mean(x) = " + Statistics.mean(x) + ", stdev = " + Statistics.stdev(x));
	}
}
