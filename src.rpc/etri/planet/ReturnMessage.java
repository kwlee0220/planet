package etri.planet;




/**
 * 
 * @author Kang-Woo Lee
 */
public abstract class ReturnMessage extends AbstractPlanetMessage {
	private Throwable m_readError;
	
	public ReturnMessage(PlanetHeader header) {
		super(header);
		
		m_readError = null;
	}
	
	public Throwable getReadError() {
		return m_readError;
	}

	@Override
	public void onReceived(PlanetSessionImpl session) {
		session.notifyReplyReceived(this);
	}

	@Override
	public void onErrorReadingPayload(PlanetSessionImpl session, Throwable cause) {
		m_readError = cause;
	}
}