package sample;

import net.*;
import org.ini4j.Wini;
import util.Log;
import util.ReconnectSocketHandler;
import util.SingleLineFormatter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.util.logging.Handler;

/**
 * Created by Yongtao on 9/10/2015.
 * <p/>
 * This is demo for using IOControl as server.
 */
public class SimpleServer{
	private static Log log=Log.get();

	/**
	 * Deal with echo and exit msg
	 */
	static class Echo implements MsgHandler{
		private IOControl control;

		Echo(IOControl control){
			this.control=control;
		}

		@Override
		public boolean process(Session session) throws IOException{
			if(session.getType()==SampleEchoMsgType.EXIT_SERVER){
				control.quitServer();
				return false;
			}else{
				control.response(new Session(SampleEchoMsgType.ACK),session);
				return false;
			}
		}
	}

	/**
	 * Handle file read.
	 * From here you know how to DMA copy file directly to socket without extra memory copying.
	 */
	static class FileServer implements MsgHandler{
		private IOControl control;

		FileServer(IOControl control){
			this.control=control;
		}

		@Override
		public boolean process(Session session) throws IOException{
			MsgType type=session.getType();
			String path=session.getString("path");
			File file;
			Session error=new Session(SampleReadFileMsgType.READ_FILE_ERROR);
			try{
				file=new File(path);
				FileInputStream fis=new FileInputStream(file);
				Session reply=new Session(SampleReadFileMsgType.READ_FILE_OK);
				long fileSize=file.length();
				reply.set("size",fileSize);
				reply.set("modify",file.lastModified());
				control.response(reply,session);
				long position=0;
				SocketChannel channel=session.getSocketChannel();
				FileChannel fc=fis.getChannel();
				//  here is DMA copying utilizing sendfile system call.
				while(position<fileSize){
					position+=fc.transferTo(position,fileSize,channel);
					log.i("position: "+position+" size: "+fileSize);
				}
			}catch(NullPointerException e){
				error.set("comment","Path name is null");
				control.response(error,session);
			}catch(FileNotFoundException e){
				error.set("comment","File not found");
				control.response(error,session);
			}catch(Exception e){
				error.set("comment",e.getMessage());
				control.response(error,session);
			}
			return false;
		}
	}

	public static void main(String args[]){
		try{
			//  read conf
			Wini conf=new Wini(new File("conf/sample/sample.ini"));
			int port=conf.get("server","port",int.class);
			String logServer=conf.get("log","ip");
			int logPort=conf.get("log","port",int.class);
			//  set log to socket log server
			Handler handler=new ReconnectSocketHandler(logServer,logPort);
			handler.setFormatter(new SingleLineFormatter());
			log.getParent().addHandler(handler);


			try{
				IOControl server=new IOControl();
				//  register echo handlers
				MsgHandler logger=new Echo(server);
				server.registerMsgHandlerLast(logger,new SampleEchoMsgType[]{SampleEchoMsgType.ECHO,SampleEchoMsgType.EXIT_SERVER});

				// register filters
				MsgFilter stat=new SimpleRawLogger();
				server.registerMsgFilterHead(stat);

				// register file read handler
				MsgHandler fileRead=new FileServer(server);
				server.registerMsgHandlerHead(fileRead,SampleReadFileMsgType.READ_FILE);

				// start server
				server.startServer(port);

				// blocking until asked to quit (see SimpleEchoClient)
				server.waitForServer();
			}catch(IOException e){
				log.w(e);
			}
		}catch(IOException e){
			log.w(e);
		}
	}
}
