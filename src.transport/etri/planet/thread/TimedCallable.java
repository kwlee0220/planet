package etri.planet.thread;

import java.util.concurrent.Callable;


/**
 * 
 * @author Kang-Woo Lee
 */
class TimedCallable<T> extends TimedTask implements Callable<T> {
	private final Callable<T> m_worker;
	
	TimedCallable(AbstractTimedExecutor executor, Callable<T> work) {
		super(executor);
		
		m_worker = work;
	}
	
	public T call() throws Exception {
		try {
			beforeStarted();
			
			return m_worker.call();
		}
		finally {
			afterFinished();
		}
	}
}
