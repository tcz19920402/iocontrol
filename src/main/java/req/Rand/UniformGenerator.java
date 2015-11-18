package req.Rand;

import java.util.concurrent.ThreadLocalRandom;

public class UniformGenerator implements RandomGenerator{
	protected int upper;

	public UniformGenerator(int upper){
		this.upper=upper;
	}

	public UniformGenerator(){
		this.upper=Integer.MAX_VALUE;
	}

	@Override
	public int nextInt(){
		return nextInt(this.upper);
	}

	@Override
	public int nextInt(int upper){
		return ThreadLocalRandom.current().nextInt(upper);
	}

	@Override
	public double nextDouble(){
		return ThreadLocalRandom.current().nextDouble();
	}
}
