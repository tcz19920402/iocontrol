package sample.Request;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ReqGeneratorAdapter implements LListenRequests{

	List<LListenRequests> list = new ArrayList<>();
	
		
	
	public void registerListener(LListenRequests obj){
		list.add(obj);
	}
	
	public void execute(String requests){
		for(LListenRequests o:list)
			o.listenRequests(requests);
	}
	
	public void init(String[] input,int thread){
		//String[] input = new String[2];			
		//input[0]="/home/tongxin/Dropbox/UTDallas/cloud computing/danei-iocontrol/files/test2.txt";
		//input[1]="/home/tongxin/Dropbox/UTDallas/cloud computing/danei-iocontrol/files/rank.txt";
	    //FSPropagate.run(input,this);
		try {
			ReqGenerator.threads=thread;
			ReqGenerator.run(input[0], input[1], this);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
  
	}

	@Override
	public void listenRequests(String requests) {
		execute(requests);
		
	}
	
	

}
