package sample.Request;

import java.util.ArrayList;
import java.util.List;

public class FSPropagateAdapter implements LListenRequests {
	
	List<LListenRequests> list = new ArrayList<>();
	
		
	
	public void registerListener(LListenRequests obj){
		list.add(obj);
	}
	
	public void execute(String requests){
		for(LListenRequests o:list)
			o.listenRequests(requests);
	}
	
	public void init(String[] input){
		//String[] input = new String[2];			
		//input[0]="/home/tongxin/Dropbox/UTDallas/cloud computing/danei-iocontrol/files/test2.txt";
		//input[1]="/home/tongxin/Dropbox/UTDallas/cloud computing/danei-iocontrol/files/rank.txt";
		FSPropagate.run(input,this);
	}

	@Override
	public void listenRequests(String requests) {
		execute(requests);
		
	}
	
	

}
