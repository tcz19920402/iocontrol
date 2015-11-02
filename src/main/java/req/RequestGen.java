package req;

import req.Rand.RandomGenerator;
import req.Rand.RequestGenerator;
import req.Rand.UniformGenerator;
import req.Rand.ZipfGenerator;
import util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RequestGen{
	static Log log=Log.get();
	final int numThreads;
	Request.ReqType[] types;
	double[] ratio;
	int interval;
	double alpha;
	ExecutorService pool;
	final CountDownLatch start=new CountDownLatch(1);
	volatile boolean cont=true;
	RequestCallback callbacks[];
	Appendable logfiles[];
	LineReader reader;
	long maxAppend;

	interface LineReader{
		String read(long offset);
		long len();
	}

	static class FileLineReader implements LineReader{
		List<Long> index=new ArrayList<>();
		RandomAccessFile raf;

		public FileLineReader(String fileName,String indexName) throws IOException{
			try(FileInputStream fis=new FileInputStream(new File(indexName));
			    RandomAccessFile rf=new RandomAccessFile(new File(fileName),"r")){
				Scanner sc=new Scanner(fis);
				while(sc.hasNextLong()) index.add(sc.nextLong());
				raf=rf;
			}catch(IOException e){
				throw e;
			}
		}

		@Override
		public String read(long offset){
			try{
				raf.seek(index.get((int)offset));
				return raf.readLine();
			}catch(IOException e){
				log.s(e);
			}
			return null;
		}

		@Override
		public long len(){
			return index.size();
		}
	}

	static class RamLineReader implements LineReader{
		List<String> lines;

		public RamLineReader(String fileName) throws IOException{
			lines=Files.readAllLines(Paths.get(fileName),Charset.forName("UTF-8"));
		}

		@Override
		public String read(long offset){
			return lines.get((int)offset);
		}

		@Override
		public long len(){
			return lines.size();
		}
	}

	RequestGen(int intervalInMilliseconds,
	           long maxAppend,
	           LineReader reader,double alpha,
	           Request.ReqType[] types,double[] ratio,
	           RequestCallback[] callbacks,
	           Appendable[] logfiles){
		if(types.length!=ratio.length)
			throw new IllegalArgumentException("Length of Request types and ratio do not match.");
		if(callbacks.length!=logfiles.length)
			throw new IllegalArgumentException("Length of callback and log files do not match.");
		this.logfiles=logfiles;
		this.numThreads=callbacks.length;
		this.maxAppend=maxAppend;
		this.callbacks=callbacks;
		this.interval=intervalInMilliseconds;
		this.pool=Executors.newFixedThreadPool(numThreads);
		this.types=types;
		this.ratio=ratio;
		this.alpha=alpha;
		this.reader=reader;

		for(int i=0;i<numThreads;++i){
			RequestThread t=new RequestThread(callbacks[i],logfiles[i]);
			pool.execute(t);
		}
	}

	class RequestThread implements Runnable{
		RandomGenerator zipf;
		UniformGenerator uniform;
		long startTime;
		RequestCallback callback;
		RequestGenerator reqGen=new RequestGenerator(types,ratio);
		Formatter fmt;
		long counter=0;
		long reqAcc=0;
		long overheadAcc=0;

		RequestThread(RequestCallback callback,Appendable logfile){
			this.callback=callback;
			long len=reader.len();
			uniform=new UniformGenerator(len);
			this.zipf=new ZipfGenerator(uniform,len,alpha);
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
					long t1=System.currentTimeMillis();
					stopExpect+=interval;
					String line=reader.read(zipf.nextLong());
					String[] tokens=line.split("[\\s\\[\\]]");
					int l=tokens.length;
					Request rq=new Request();
					String path=tokens[l-1];
					rq.path=path;
					if(path.endsWith("/")) rq.isDir=true;
					if((l==3 || l==2) && !rq.isDir){
						rq.size=Long.parseLong(tokens[l-2]);
					}
					if(rq.isDir){
						do{
							rq.type=reqGen.next();
						}while(rq.type!=Request.ReqType.CREATE && rq.type!=Request.ReqType.DELETE);
					}else{
						rq.type=reqGen.next();
						if(rq.size>0){
							if(rq.type==Request.ReqType.READ || rq.type==Request.ReqType.WRITE){
								rq.start=uniform.nextLong(rq.size);
								rq.end=uniform.nextLong(rq.size-rq.start)+rq.start;
							}else if(rq.type==Request.ReqType.APPEND)
								rq.end=uniform.nextLong(maxAppend);
						}
					}
					long t2=System.currentTimeMillis();
					callback.call(rq);
					long t3=System.currentTimeMillis()-t2;
					fmt.format("%s %s: %d %d %d: %d",rq.type,rq.path,rq.size,rq.start,rq.end,t3);
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
			log.i("Total req: "+counter+", average request time: "+reqAcc/counter+"ms, overhead: "+overheadAcc/counter);
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
