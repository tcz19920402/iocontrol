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

		public NullCall(){
			for(int i=1;i<10;++i) order.add(i);
		}

		@Override
		public List<Integer> call(Request request){
			StaticTree.plainShuffle(order,uniform);
			int find=uniform.nextInt(6)+1; //  1~6
			System.out.println(request+" : "+find);
			return order.subList(0,find);
		}
	}

	public static void main(String args[]) throws IOException{
		parse("files/test.txt","files/rank.txt",new NullCall());
	}
}
