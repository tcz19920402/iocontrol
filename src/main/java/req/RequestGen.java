package req;

import req.Rand.*;
import util.Log;

import java.io.IOException;
import java.util.Formatter;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RequestGen{
	static Log log=Log.get();
	final int numThreads;
	UniformGenerator uniformGen;
	ExpGenerator expGen;
	RequestGenerator reqGen;
	double alpha;
	ExecutorService pool;
	final CountDownLatch start=new CountDownLatch(1);
	volatile boolean cont=true;
	Appendable logfiles[];
	long maxAppend;
	int unit;
	int max_rand;
	int max_seq;


	StaticTree staticTree;
	DynamicTree dynamicTree;

	RequestGen(String staticFileName,   //  immutable files
	           String staticRankFileName,     //  for static file
	           String dynamicFileName,  //  mutable files, nullable

	           //   number of threads
	           int threads,
	           Appendable[] logfiles,
	           //   parameter for exponential distribution, this affects inter-arrival time
	           double lambda,
	           int time,
	           //   parameter for zipf distribution, normally around 0.9.
	           //   This affects how concentrated your requests are.
	           double alpha,
	           //   R/W parameters
	           //   unit is in bytes, max_rand is how big can a random R/W be, in unit.
	           int unit,
	           int max_rand,
	           int max_seq,

	           //   percentage of each request type, do not need to sum to 1
	           Map<Request.ReqType, Double> ratio,
	           //   your handle code for each request
	           Map<Request.ReqType, RequestCallback> callbacks) throws IOException{
		if(ratio.size()!=callbacks.size())
			throw new IllegalArgumentException("Length of request ratio and callback do not match.");
		if(logfiles!=null && logfiles.length!=threads)
			throw new IllegalArgumentException("Length of log files and number of threads do not match.");
		if(logfiles!=null) this.logfiles=logfiles;
		uniformGen=new UniformGenerator();
		expGen=new ExpGenerator(lambda,time,uniformGen);
		reqGen=new RequestGenerator(ratio,uniformGen);
		numThreads=threads;
		this.pool=Executors.newFixedThreadPool(numThreads);
		this.alpha=alpha;
		staticTree=StaticTree.getStaticTree(staticFileName,uniformGen);
		staticTree.shuffleFiles(staticRankFileName);
		this.unit=unit;
		this.max_rand=max_rand;
		this.max_seq=max_seq;
		if(dynamicFileName!=null)
			dynamicTree=DynamicTree.getDynamicTree(dynamicFileName,uniformGen);

		for(int i=0;i<numThreads;++i){
			RequestThread t=new RequestThread(callbacks,logfiles[i]);
			pool.execute(t);
		}
	}

	class RequestThread implements Runnable{
		RandomGenerator zipf;
		UniformGenerator uniform;
		long startTime;
		Map<Request.ReqType, RequestCallback> callback;
		Formatter fmt;
		long counter=0;
		long reqAcc=0;
		long overheadAcc=0;

		RequestThread(Map<Request.ReqType, RequestCallback> callback,Appendable logfile){
			this.callback=callback;
			this.zipf=new ZipfGenerator(alpha,staticTree.getFileSize(),uniform);
			this.fmt=new Formatter(logfile);
		}

		@Override
		public void run(){
			try{
				start.wait();
			}catch(InterruptedException e){
				log.s(e);
			}
			long stopExpect=startTime=System.currentTimeMillis();
			while(cont){
				try{
					long interval=expGen.nextInt();
					long t1=System.currentTimeMillis();
					stopExpect+=interval;
					Request.ReqType type=reqGen.next();
					Request rq=null;
					if(dynamicTree==null){
						if(type==Request.ReqType.LS)
							rq=staticTree.ls(zipf.nextInt(staticTree.getNonEmptyDirSize()));
						else rq=staticTree.fileInfo(zipf.nextInt(staticTree.getFileSize()));
					}else{
						if(Request.immutable.contains(type)){
							if(type==Request.ReqType.LS){
								int index=zipf.nextInt(staticTree.getNonEmptyDirSize()+dynamicTree.getNonEmptyDirSize());
								if(index<staticTree.getNonEmptyDirSize()) rq=staticTree.ls(index);
								else rq=dynamicTree.ls(index-staticTree.getNonEmptyDirSize());
							}else{
								int index=zipf.nextInt(staticTree.getFileSize()+dynamicTree.getFileSize());
								if(index<staticTree.getFileSize()) rq=staticTree.fileInfo(index);
								else rq=dynamicTree.fileInfo(index-staticTree.getFileSize());
							}
						}else{
							if(type==Request.ReqType.CREATE_DIR){
								rq=dynamicTree.createDir(zipf.nextInt(dynamicTree.getAllDirSize()));
							}else if(type==Request.ReqType.CREATE_FILE){
								rq=dynamicTree.createFile(zipf.nextInt(dynamicTree.getAllDirSize()));
							}else if(type==Request.ReqType.DELETE){
								rq=dynamicTree.delete(zipf.nextInt(dynamicTree.getFileSize()));
							}else if(type==Request.ReqType.RMDIR){
								rq=dynamicTree.rmdir(zipf.nextInt(dynamicTree.getEmptyDirSize()));
							}
						}
					}
					rq.type=type;
					if(type==Request.ReqType.SEQ_READ || type==Request.ReqType.SEQ_WRITE){
						if(rq.end>unit){
							rq.start=uniform.nextInt((int)(rq.end-unit));
							rq.end=rq.start+unit*uniform.nextInt(Math.min((int)(rq.end-rq.start)/unit,max_seq));
						}
					}else if(type==Request.ReqType.RANDOM_READ || type==Request.ReqType.RANDOM_WRITE){
						if(rq.end>unit){
							rq.start=uniform.nextInt((int)(rq.end-unit));
							rq.end=rq.start+unit*uniform.nextInt(Math.min((int)(rq.end-rq.start)/unit,max_rand));
						}
					}else if(type==Request.ReqType.APPEND){
						int delta=uniform.nextInt(max_seq)*unit;
						rq.start=rq.end;
						rq.end+=delta;
					}
					long t2=System.currentTimeMillis();
					callback.get(type).call(rq);
					long t3=System.currentTimeMillis()-t2;
					fmt.format("%s %s: %d %d %d %d: %d",rq.type,rq.path,rq.start,rq.end,rq.start,rq.end,t3);
					reqAcc+=t3;
					counter+=1;
					long t4=System.currentTimeMillis();
					overheadAcc+=(t4-t1-t3);
					long t5=stopExpect-t4;
					if(t5>0)
						Thread.sleep(t5);
				}catch(InterruptedException e){
					log.s(e);
				}
			}
			log.i("Total req: "+counter+". Real interval: "+(System.currentTimeMillis()-startTime)/counter
					+", average request time: "+reqAcc/counter+"ms, overhead: "+overheadAcc/counter);
		}
	}

	void start(){
		start.countDown();
	}

	void stop(){
		cont=false;
		try{
			pool.wait();
		}catch(InterruptedException e){
			log.s(e);
		}
	}
}
