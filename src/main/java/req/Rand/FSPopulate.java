package req.Rand;

import req.Request;
import req.RequestCallback;
import req.StaticTree;

import java.io.*;
import java.util.List;

public class FSPopulate{
	StaticTree tree;
	static UniformGenerator uniform=new UniformGenerator();

	static String printLine(List list){
		String lstring=list.toString();
		return lstring.substring(1,lstring.length()-1)+"\n";
	}

	public FSPopulate(String filename,String output,RequestCallback create) throws IOException{
		tree=StaticTree.getStaticTree(filename,uniform);
		try(Writer out=new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(output),"UTF-8"))){
			for(int i=0;i<tree.getFileSize();++i){
				Request request=tree.fileInfo(i);
				request.type=Request.ReqType.CREATE_FILE;
				List<Integer> line=create.call(request);
				out.write(printLine(line));
			}
		}
	}
}
