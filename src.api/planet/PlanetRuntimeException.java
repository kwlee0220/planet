package planet;


/**
 * <code>PlanetRuntimeException</code>는 PlanetServer를 발생되는 unchecked 예외의
 * 최상위 클래스이다.
 * 
 * @author Kang-Woo Lee
 */
public class PlanetRuntimeException extends RuntimeException {
	private static final long serialVersionUID = 3796564718283343921L;
	
	private String m_details;

	public PlanetRuntimeException(String details) {
		super(details);
		
		m_details = details;
	}

	public PlanetRuntimeException(String details, Throwable cause) {
		super(details, cause);
		
		m_details = details + ", cause=" + cause;
	}

	public PlanetRuntimeException(Throwable cause) {
		super(cause);
		
		m_details = "cause=" + cause;
	}
	
	public String getDetails() {
		return m_details;
	}
}
