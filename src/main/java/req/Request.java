package req;

import java.io.Serializable;

public class Request implements Serializable{


	static public final double[] ratio=new double[]{0.7,0.2,0.03,0.04,0.03};

	public enum ReqType{
		READ,WRITE,DELETE,APPEND,CREATE
	}

	ReqType type=null;
	public long start=0;    //start position for read, write
	public long end=-1;     //end position for read, write; append size for append
	public boolean isDir=false;
	public String path=null;
	public long size=-1;    //file size
}
