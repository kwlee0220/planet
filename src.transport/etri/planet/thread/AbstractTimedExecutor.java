package etri.planet.thread;


/**
 * 
 * @author Kang-Woo Lee
 */
public abstract class AbstractTimedExecutor {
	private volatile TimedThreadListener m_listener;
	
	public abstract long getThreadTimeout();
	public abstract void setThreadTimeout(long timeout);
	public abstract int getThreadCount();
	public abstract int getActiveThreadCount();
	public abstract int getMaxThreadCount();
	
	protected abstract void threadDestroyed(TimedThread thread);
	
	public TimedThreadListener getThreadListener() {
		return m_listener;
	}
	
	public void setThreadListener(TimedThreadListener listener) {
		m_listener = listener;
	}
	
	void beforeTaskStarted(TimedTask task) {
		if ( m_listener != null ) {
			try {
				m_listener.beforeStarted();
			}
			catch ( Throwable e ) { }
		}
	}
	
	void afterTaskFinished(TimedTask task) {
		if ( m_listener != null ) {
			try {
				m_listener.afterFinished();
			}
			catch ( Throwable e ) { }
		}
	}
}
