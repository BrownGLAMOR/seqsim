package speed;

// Sets value as the user's input
public class SetValue extends Value{

	double[] v;
	public SetValue(double[] v) {
		this.v = v;
	}

	// returns user's valuation input
	@Override
	public double getValue(int no_goods_won) {
		return v[no_goods_won];
	}

	@Override
	public void reset() {
	}
}
