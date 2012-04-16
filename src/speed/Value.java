package speed;

public abstract class Value {
	// call this to get the valuation
	public abstract double getValue(int no_goods_won);
	
//	// call this to get a binned value
//	public abstract int getDiscreteValue(int no_goods_won);
	
	// call this to force the Value class to generate a new valuation
	public abstract void reset();
}
