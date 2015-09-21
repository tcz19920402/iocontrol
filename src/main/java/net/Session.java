package net;

import java.io.Serializable;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * Created by Yongtao on 8/25/2015.
 *
 * Raw control msg between socket.
 * Java serialization is used. Though heavy and less efficient than Kryo, we can put virtually any data into header.
 * File and other large byte data should be transferred with SocketChannel.transferTo/From which utilize DMA.
 */
public class Session implements Serializable{
	protected SocketChannel socketChannel=null;
	protected Socket socket=null;
	protected MsgType type;
	protected Map<String, Serializable> headerMap=new ConcurrentHashMap<>();
	protected ExecutorService pool;

	protected Session(SocketChannel socketChannel,ExecutorService pool){
		this.socketChannel=socketChannel;
		this.socket=socketChannel.socket();
		this.pool=pool;
	}

	public Session(MsgType type){
		this.type=type;
	}

	public void copy(Session session){
		this.type=session.type;
		this.headerMap=session.headerMap;
	}

	public MsgType getType(){
		return type;
	}

	public Map<String, Serializable> getHeaderMap(){
		return headerMap;
	}

	public void setHeaderMap(Map<String, Serializable> header){
		this.headerMap=header;
	}

	public Serializable get(String key){
		return headerMap.get(key);
	}

	public <T> T get(String key,Class<T> tClass){
		return (T)headerMap.get(key);
	}

	public String getString(String key){
		return get(key,String.class);
	}

	public int getInt(String key){
		return get(key,int.class);
	}

	public long getLong(String key){
		return get(key,long.class);
	}

	public boolean getBoolean(String key){
		return get(key,boolean.class);
	}

	public void set(String key,Serializable val){
		headerMap.put(key,val);
	}

	public Socket getSocket(){
		return socket;
	}

	public void setSocket(Socket socket){
		this.socket=socket;
	}

	public SocketChannel getSocketChannel(){
		return socketChannel;
	}

	public void setSocketChannel(SocketChannel socketChannel){
		this.socketChannel=socketChannel;
	}

	public ExecutorService getExecutor(){
		return pool;
	}

	public void setExecutor(ExecutorService pool){
		this.pool=pool;
	}

	public Map<String, Serializable> getKeyValuePairs(){
		return headerMap;
	}
}
