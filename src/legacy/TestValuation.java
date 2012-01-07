package legacy;
import java.util.Set;

public class TestValuation extends Valuation {
	// Only used for testing purposes!!! v({})=0, otherwise v = 1. 
	
	double[] values;
	
	TestValuation(int n) {
		super(n);		
		values = new double[no_valuations];
	}

	
	@Override
	public double getValue(Set<Integer> basket) {
		if (basket.size() == 2)
			return 10.0;
		else if (basket.size() == 1)
			return 5.0;
		else
			return 0.0;
	}
	
	// Useless overrides... for formality purposes
	@Override
	public double getValue(int n) {
		return values[n];
	}
	
	@Override
	public String getInfo(){
		return "Test Valuation";
	}


}
