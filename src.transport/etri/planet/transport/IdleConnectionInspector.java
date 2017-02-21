package etri.planet.transport;


import java.util.concurrent.DelayQueue;

import etri.planet.TransportLoggers;


/**
 * 
 * @author Kang-Woo Lee
 */
class IdleConnectionInspector implements Runnable {
	@SuppressWarnings("unused")
	private final TransportManagerImpl m_transport;
	private final DelayQueue<ConnectionImpl> m_delayeds = new DelayQueue<ConnectionImpl>();
	private volatile Thread m_thread;
	private boolean m_activated;						// guarded by 'this'
	
	IdleConnectionInspector(TransportManagerImpl transport) {
		m_transport = transport;
		m_activated = true;
	}
	
	void start() {
		m_thread = new Thread(this, "planet:idle-collector");
		m_thread.start();
	}
	
	synchronized void stop() {
		m_activated = false;
		this.notifyAll();
		
		m_thread.interrupt();
	}
	
	void schedule(ConnectionImpl conn) {
		m_delayeds.add(conn);
	}
	
	public void run() {
		while ( true ) {
			synchronized ( this ) {
				if ( !m_activated ) {
					if ( TransportLoggers.IDLECHKR.isInfoEnabled() ) {
						TransportLoggers.IDLECHKR.info("stopped: IdleConnectionCollector");
					}
					
					return;
				}
			}
			
			try {
				ConnectionImpl conn = m_delayeds.take();
				conn.onIdleTimeout();
			}
			catch ( InterruptedException expected ) { }
		}
	}
}
