package req.Rand;

import req.Request;

public class RequestGenerator{
	Request.ReqType[] types;
	int len;
	UniformGenerator gen;
	double[] steps;

	public RequestGenerator(Request.ReqType[] types,double[] ratio){
		this.types=types;
		len=types.length;
		gen=new UniformGenerator(1);
		double sum=0.0;
		for(int i=0;i<len;++i) sum+=ratio[i];
		steps=new double[len];
		steps[0]=ratio[0]/sum;
		for(int i=1;i<len;++i) steps[i]=steps[i-1]+ratio[i]/sum;
	}

	Request.ReqType bisec(int a,int b,double rd){
		if(b-a==1) return types[a];
		int half=(a+b)/2;
		if(rd>steps[half-1])
			return bisec(half,b,rd);
		else return bisec(a,half,rd);
	}

	public Request.ReqType next(){
		return bisec(0,len,gen.nextDouble());
	}
}
