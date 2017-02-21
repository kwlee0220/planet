package etri.planet.servant;

import planet.PlanetSession;


/**
 * 
 * @author Kang-Woo Lee
 */
public abstract class AbstractSessionBoundServant implements SessionBoundServant {
	private PlanetSession m_session;

	public synchronized void bindToSession(PlanetSession session) {
		m_session = session;
	}

	public synchronized void unbindFromSession() {
		m_session = null;
	}

	public synchronized PlanetSession getBoundSession() {
		return m_session;
	}
}
