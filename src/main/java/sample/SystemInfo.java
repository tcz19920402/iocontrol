package sample;


import com.sun.management.OperatingSystemMXBean;
import org.ini4j.Wini;
import util.Log;
import util.ReconnectSocketHandler;
import util.SingleLineFormatter;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.logging.Handler;
import java.util.logging.SocketHandler;


/**
 * Created by Yongtao on 9/19/2015.
 *
 * This is demo for how to periodically get system load info.
 * Network static is acquired using technique in SimpleRawLogger.
 * You can move code in main to your dedicate thread.
 *
 * Alternative choice is SIGAR@hyperic if you want more complex solution.
 */
public class SystemInfo{
	private static final Log log=Log.get();
	private static final long mb=1024*1024;

	public static void main(String args[]) throws InterruptedException, IOException{
		Wini conf=new Wini(new File("conf/sample/sample.ini"));
		int port=conf.get("server","port",int.class);
		String logServer=conf.get("log","ip");
		int logPort=conf.get("log","port",int.class);
		//  set log to socket log server
		Handler handler=new ReconnectSocketHandler(logServer,logPort);
		handler.setFormatter(new SingleLineFormatter());
		log.getParent().addHandler(handler);

		OperatingSystemMXBean os=ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
		Runtime runtime=Runtime.getRuntime();

		for(;;){
			// Please note that cpu usage may be negative,
			// because jvm takes time to calculate used cpu time over total cpu time to get ratio.
			log.i("System load: "+os.getSystemCpuLoad()+"\tProcess load: "+os.getProcessCpuLoad()+
					"\tFree memory: "+runtime.freeMemory()+"MB/"+runtime.totalMemory()+"MB");
			Thread.sleep(2*1000);
		}
	}

}
