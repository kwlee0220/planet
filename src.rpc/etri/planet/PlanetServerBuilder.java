package etri.planet;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import etri.planet.thread.TimedExecutorService;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class PlanetServerBuilder {
	private static final String PROPS_HOST = "host";
	private static final String PROPS_PORT = "port";
	private static final String PROP_HB_INTERVAL = "heartbeat.interval";
	private static final String PROP_CONNECT_TIMEOUT = "connect.timeout";
	private static final String PROP_CALL_TIMEOUT = "call.timeout";
	private static final String PROP_THREAD_NAME = "thread.name";
	private static final String PROP_THREAD_CORECOUNT = "thread.count.core";
	private static final String PROP_THREAD_MAXCOUNT = "thread.count.max";
	private static final String PROP_THREAD_KEEPALIVE = "thread.keepalive";
	private static final String PROP_THREAD_TIMEOUT = "thread.timeout";
	
	private static final String DEFAULT_THREAD_NAME = "planet";
	
	private Properties m_props;
	private String m_prefix;
	
	private int m_defaultPort = -1;
	private int m_defaultConnectTimeout = 2;// 2 seconds
	private long m_defaultHbInterval = -1;	// 3 minutes
	private long m_defaultCallTimeout = -1;	// 10 seconds
	private int m_defaultCoreThreadCount = 4;
	private int m_defaultMaxThreadCount = -1;
	private int m_defaultKeepAlive = 60;	// 60 seconds
	private long m_defaultThreadTimeout = -1;
	private String m_threadName = DEFAULT_THREAD_NAME;
	
	public PlanetServerBuilder(Properties props, String prefix) {
		m_props = props;
		m_prefix =prefix;
	}
	
	public void setDefaultPlanetServerPort(int port) {
		m_defaultPort = port;
	}
	
	public void setDefaultHeartbeatInterval(long interval) {
		m_defaultHbInterval = interval;
	}
	
	public void setConnectTimeout(int timeout) {
		m_defaultConnectTimeout = timeout;
	}
	
	public void setDefaultCallTimeout(long timeout) {
		m_defaultCallTimeout = timeout;
	}
	
	public void setDefaultThreadCount(int count) {
		m_defaultCoreThreadCount = count;
	}
	
	public void setDefaultThreadTimeout(long timeout) {
		m_defaultThreadTimeout = timeout;
	}
	
	public void setThreadNamePrefix(String prefix) {
		m_threadName = prefix;
	}
	
	public PlanetServerImpl build() {
		PlanetServerImpl planet = new PlanetServerImpl();
		
		String host = getPropertyAsString(m_prefix + PROPS_HOST, null);
		if ( host != null ) {
			planet.setPlanetServerHost(host);
		}
		
		int port = getPropertyAsInt(m_prefix + PROPS_PORT, m_defaultPort);
		if ( port >= 0 ) {
			planet.setPlanetServerPort(port);
		}
	    
	    long interval = getPropertyAsDuration(m_prefix + PROP_HB_INTERVAL, m_defaultHbInterval);
	    planet.setHeartbeatInterval(interval);

	    long timeout;
		ExecutorService executor;

		int coreCount = getPropertyAsInt(m_prefix + PROP_THREAD_CORECOUNT, m_defaultCoreThreadCount);
		int maxCount = getPropertyAsInt(m_prefix + PROP_THREAD_MAXCOUNT, m_defaultMaxThreadCount);
		int keepAlive = getPropertyAsInt(m_prefix + PROP_THREAD_KEEPALIVE, m_defaultKeepAlive);
	    timeout = getPropertyAsDuration(m_prefix + PROP_THREAD_TIMEOUT, m_defaultThreadTimeout);
	    if ( timeout > 0 ) {
	    	String name = getPropertyAsString(m_prefix + PROP_THREAD_NAME, m_threadName);
			
			if ( maxCount < 0 || maxCount == Integer.MAX_VALUE ) {
				executor = new TimedExecutorService(name, coreCount, keepAlive, timeout);
			}
			else {
				executor = new TimedExecutorService(name, coreCount, maxCount, keepAlive, timeout);
			}
	    }
	    else {
			if ( maxCount < 0 || maxCount == Integer.MAX_VALUE ) {
				executor = new ThreadPoolExecutor(coreCount, Integer.MAX_VALUE, keepAlive,
											TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
			}
			else {
				executor = new ThreadPoolExecutor(coreCount, maxCount, keepAlive,
											TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
			}
	    }
	    planet.setPlanetExecutor(executor);
	    
	    timeout = getPropertyAsDuration(m_prefix + PROP_CALL_TIMEOUT, m_defaultCallTimeout);
	    planet.setDefaultCallTimeout(timeout);
	    
	    timeout = getPropertyAsDuration(m_prefix + PROP_CONNECT_TIMEOUT, m_defaultConnectTimeout,
	    								TimeUnit.SECONDS);
	    planet.setConnectTimeout((int)timeout);
		
	    return planet;
	}
	
	private String getPropertyAsString(String key, String defValue) {
		String value = m_props.getProperty(key);
		if ( value == null ) {
			return defValue;
		}
		else {
			return value;
		}
	}
	
	private int getPropertyAsInt(String key, int defValue) {
		String value = m_props.getProperty(key);
		if ( value == null ) {
			return defValue;
		}
		else {
			return Integer.parseInt(value);
		}
	}
    
	private long getPropertyAsDuration(String key, long defValue, TimeUnit tu) {
    	return TimeUnit.MILLISECONDS.convert(getPropertyAsDuration(key, defValue), tu);
    }
    
	private long getPropertyAsDuration(String key, long defValue) {
    	return parseDuration(getPropertyAsString(key, "" + defValue));
    }
    
    private static long parseDuration(String durStr) {
    	if ( durStr.endsWith("s") ) {
	    	int seconds = Integer.parseInt(durStr.substring(0, durStr.length()-1));
	    	return seconds * 1000;
	    }
	    else if ( durStr.endsWith("m") ) {
	    	int mimutes = Integer.parseInt(durStr.substring(0, durStr.length()-1));
	    	return (long)mimutes * 1000 * 60;
	    }
	    else if ( durStr.endsWith("h") ) {
	    	int hours = Integer.parseInt(durStr.substring(0, durStr.length()-1));
	    	return (long)hours * 1000 * 60 * 60;
	    }
	    else {
	    	return Long.parseLong(durStr.substring(0, durStr.length()));
	    }
    }
}
