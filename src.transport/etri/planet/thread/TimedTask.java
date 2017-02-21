package etri.planet.thread;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import etri.planet.PlanetServerImpl;


/**
 * 
 * @author Kang-Woo Lee
 */
class TimedTask {
	private static final Logger s_logger = Logger.getLogger("PLANET.SCHEDULER");
	
	private final AbstractTimedExecutor m_executor;
	volatile TimedThread m_thread;
	volatile long m_startedTime;
	volatile boolean m_interrupted;
	
	private final Map<String,Object> m_log4jContext;
	
	@SuppressWarnings("unchecked")
	TimedTask(AbstractTimedExecutor executor) {
		m_executor = executor;
		m_startedTime = Long.MAX_VALUE;
		
		m_log4jContext = new HashMap<String,Object>();
		if ( MDC.getContext() != null ) {
			m_log4jContext.putAll(MDC.getContext());
		}
	}
	
	protected void beforeStarted() {
		m_startedTime = System.currentTimeMillis();
		
		m_thread = (TimedThread)Thread.currentThread();
		m_thread.setCurrentTask(this);
		
		for ( Map.Entry<String,Object> entry : m_log4jContext.entrySet() ) {
			MDC.put(entry.getKey(), entry.getValue());
		}
		
		m_executor.beforeTaskStarted(this);
		
		Thread.interrupted();	// clear interruption flag, if set
	}
	
	protected void afterFinished() {
		m_executor.afterTaskFinished(this);
		
		m_thread.setCurrentTask(null);
		
		PlanetServerImpl.s_context.get().reset();
		Hashtable<?,?> map = MDC.getContext();
		if ( map != null ) {
			map.clear();
		}
		
		if ( m_interrupted ) {
			if ( s_logger.isDebugEnabled() ) {
				s_logger.debug("dispose: interrupted " + m_thread);
			}
			
			Thread.currentThread().interrupt();
		}
	}
}
