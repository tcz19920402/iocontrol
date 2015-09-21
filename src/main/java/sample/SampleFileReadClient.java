package sample;

import net.IOControl;
import net.Session;
import org.ini4j.Wini;
import util.Log;
import util.ReconnectSocketHandler;
import util.SingleLineFormatter;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;
import java.util.logging.Handler;

/**
 * Created by Yongtao on 9/20/2015.
 *
 * Read file from server to memory. Path is read from console
 */
public class SampleFileReadClient{
	private static final Log log=Log.get();

	public static void main(String args[]){
		try{
			//  read conf file here
			Wini conf=new Wini(new File("conf/sample/sample.ini"));
			String serverIP=conf.get("server","ip");
			String logIP=conf.get("log","ip");
			int serverPort=conf.get("server","port",int.class);
			int logPort=conf.get("log","port",int.class);
			// set remote log server to forward all logs there
			Handler handler=new ReconnectSocketHandler(logIP,logPort);
			handler.setFormatter(new SingleLineFormatter());
			log.getParent().addHandler(handler);

			try{
				IOControl control=new IOControl();
				//  get what you type
				Scanner in=new Scanner(System.in);
				for(;;){
					String cmd=in.nextLine();
					if(cmd.length()>0){
						String path=cmd.trim();
						Session session=new Session(SampleReadFileMsgType.READ_FILE);
						session.set("path",path);
						long start=System.currentTimeMillis();
						Session response=control.request(session,serverIP,serverPort);
						if(response.getType()==SampleReadFileMsgType.READ_FILE_ERROR){
							log.i("File read error: "+response.getString("comment"));
						}else if(response.getType()==SampleReadFileMsgType.READ_FILE_OK){
							log.i("File size: "+response.getLong("size")+" Last modified: "+response.getLong("modify"));
							long size=response.getLong("size");
							int buffer_size=1024;
							ByteBuffer buffer=ByteBuffer.allocate(buffer_size);
							SocketChannel channel=response.getSocketChannel();
//							System.out.println(channel);
							long read=0;
							while(read<size){
								long read_once=channel.read(buffer);
								log.i("Read: "+read_once);
								if(read_once==0) buffer.clear();
								else read+=read_once;
							}
							long time=System.currentTimeMillis()-start;
							log.d("Total time: "+time);
						}else{
							log.w("Unrecognized reply: "+response.getType());
						}
					}
				}
			}catch(Exception e){
				log.w(e);
			}
		}catch(IOException e){
			log.w(e);
		}
	}
}
