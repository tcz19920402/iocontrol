package req.Rand;

import java.util.Random;

public class UniformGenerator implements RandomGenerator{
	protected Random rand=new Random();
	protected long upper;

	public UniformGenerator(long upper){
		this.upper=upper;
	}

	@Override
	public long nextLong(){
		return nextLong(this.upper);
	}

	public long nextLong(long upper){
		//  http://stackoverflow.com/a/2546186
		long bits, val;
		do{
			bits=(rand.nextLong()<<1)>>>1;
			val=bits%upper;
		}while(bits-val+(upper-1)<0L);
		return val;
	}

	@Override
	public double nextDouble(){
		return rand.nextDouble();
	}
}
