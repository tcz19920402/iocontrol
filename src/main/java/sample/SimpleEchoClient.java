package sample;

import net.IOControl;
import net.Session;
import org.ini4j.Wini;
import util.Log;
import util.ReconnectSocketHandler;
import util.SingleLineFormatter;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.logging.Handler;

/**
 * Created by Yongtao on 9/10/2015.
 * <p/>
 * This is demo how to use IOControl as client.
 */
public class SimpleEchoClient{
	private static Log log=Log.get();

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
						String test=cmd.toLowerCase().trim();
						//  ask server to quit
						if(test.equals("quit") || test.equals("exit") || test.equals("q") || test.equals("e")){
							control.send(new Session(SampleEchoMsgType.EXIT_SERVER),serverIP,serverPort);
							break;
						}
					}
					// else just send plain ping msg.
					Session session=new Session(SampleEchoMsgType.ECHO);
					session.set("Comment",cmd);
					Session ping=control.request(session,serverIP,serverPort);
					log.i("Heard: "+ping.getType());
				}
			}catch(Exception e){
				log.w(e);
			}
		}catch(IOException e){
			log.w(e);
		}
	}
}
