package etri.planet.transport;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * @author Kang-Woo Lee
 */
class ConnectionInspector implements Runnable {
	private static final Logger s_logger = LoggerFactory.getLogger("PLANET.HEARTBEAT");

	private final TransportManagerImpl m_transport;
	private long m_hbPeriod;
	private boolean m_activated;
	
	ConnectionInspector(TransportManagerImpl transport, long hbPeriod) {
		m_transport = transport;
		m_hbPeriod = hbPeriod;
		m_activated = true;
	}
	
	synchronized void stop() {
		m_activated = false;
		this.notifyAll();
	}
	
	public void run() {
		while ( true ) {
			if ( s_logger.isInfoEnabled() ) {
				s_logger.info("started: Idle connection inspection");
			}
			
			ConnectionImpl[] connections = m_transport.getConnections();
			for ( int i =0; i < connections.length; ++i ) {
				connections[i].inspectForIdleness();
			}
			
			if ( s_logger.isInfoEnabled() ) {
				s_logger.info("finished: Idle connection inspection");
			}
			
			synchronized ( this ) {
				try {
					this.wait(m_hbPeriod);
				}
				catch ( InterruptedException e ) {
					s_logger.warn("interrupted: " + Thread.currentThread().getName());
					
					return;
				}
				
				if ( !m_activated ) {
					s_logger.warn("stopped: " + Thread.currentThread().getName());
					
					return;
				}
			}
		}
	}
}
