package planet;



/**
 * 
 * @author Kang-Woo Lee
 */
public class PlanetContext {
	private PlanetSession m_session;
	private long m_callTimeout;
	
	public PlanetContext() {
		m_callTimeout = -1;
	}
	
	public void reset() {
		m_session = null;
		m_callTimeout = -1;
	}
	
	public long getCallTimeout() {
		return m_callTimeout;
	}
	
	public void setCallTimeout(long timeout) {
		if ( timeout == 0 ) {
			throw new IllegalArgumentException("Call timeout should not be zero");
		}
		
		m_callTimeout = timeout;
	}
	
	public PlanetSession getPlanetSession() {
		return m_session;
	}
	
	public void setPlanetSession(PlanetSession session) {
		m_session = session;
		
		if ( m_callTimeout == 0 ) {
			
		}
	}
}
