package etri.planet.thread;

import planet.PlanetUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * @author Kang-Woo Lee
 */
class TimedRunnable extends TimedTask implements Runnable {
	private static final Logger s_logger = LoggerFactory.getLogger("PLANET.SCHEDULER");
	
	private final Runnable m_worker;
	
	TimedRunnable(AbstractTimedExecutor executor, Runnable work) {
		super(executor);
		
		m_worker = work;
	}
	
	public void run() {
		try {
			beforeStarted();
			
			m_worker.run();
		}
		catch ( Throwable e ) {
			s_logger.warn("uncaught exception=", PlanetUtils.unwrapThrowable(e));
		}
		finally {
			afterFinished();
		}
	}
}
