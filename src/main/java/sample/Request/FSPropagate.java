package sample.Request;

import req.Rand.RandomGenerator;
import req.Rand.UniformGenerator;
import req.Request;
import req.RequestCallback;
import req.StaticTree;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FSPropagate{
	public static String printLine(List list){
		String lstring=list.toString();
		return lstring.substring(1,lstring.length()-1)+"\n";
	}

	public static void parse(String input,String output,RequestCallback call) throws IOException{
		try(Writer out=new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(output),"UTF-8"))){
			StaticTree tree=StaticTree.getStaticTree(input,new UniformGenerator());
			for(int i=0;i<tree.getFileSize();++i){
				Request req=tree.fileInfo(i);
				req.type=Request.ReqType.CREATE_FILE;
				out.write(printLine(call.call(req)));
			}
		}
	}

	public static class NullCall implements RequestCallback{
		List<Integer> order=new ArrayList<>();
		RandomGenerator uniform=new UniformGenerator();
		LListenRequests listener;
		public NullCall(LListenRequests listener){
			this.listener=listener;
			for(int i=1;i<10;++i) order.add(i);
			
		}

		public NullCall(){
			this.listener=listener;
			for(int i=1;i<10;++i) order.add(i);
			
		}
		
		@Override
		public List<Integer> call(Request request){
			StaticTree.plainShuffle(order,uniform);
			int find=uniform.nextInt(6)+1; //  1~6
			if(listener==null)
				System.out.println(request+" : "+find);
			else
<<<<<<< HEAD
				//System.out.println(request+" : "+find);
=======
>>>>>>> aa1f01ef43ebb0da3df4514791682560ac9cc046
				listener.listenRequests(request+" : "+find);
			return order.subList(0,find);
		}
	}
	
	public static void run(String[] args,LListenRequests listener){
		try {
			parse(args[0],args[1],new NullCall(listener));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

	public static void main(String args[]) throws IOException{
<<<<<<< HEAD
		parse("files/test.txt","files/rank.txt",new NullCall());
=======
		parse("files/test2.txt","files/rank.txt",new NullCall());
>>>>>>> aa1f01ef43ebb0da3df4514791682560ac9cc046
		
	}
}
