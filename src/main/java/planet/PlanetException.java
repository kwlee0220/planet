package planet;


/**
 * <code>PlanetException</code>는 PlanetServer를 발생되는 checked 예외의
 * 최상위 클래스이다.
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class PlanetException extends Exception {
	private static final long serialVersionUID = -4548754097645291222L;
	
	private String m_details;

	public PlanetException() {
		m_details = null;
	}

	public PlanetException(String details) {
		super(details);
		
		m_details = details;
	}

	public PlanetException(String details, Throwable cause) {
		super(details, cause);
		
		m_details = details + ", cause=" + cause;
	}
	
	public PlanetException(Throwable cause) {
		super(cause);
		
		m_details = "cause=" + cause;
	}
	
	public String getDetails() {
		return m_details;
	}
}
