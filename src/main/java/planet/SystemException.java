package planet;


/**
 * 
 * @author Kang-Woo Lee
 */
public class SystemException extends PlanetRuntimeException {
	private static final long serialVersionUID = 3796564718283343921L;

	public SystemException(String details) {
		super(details);
	}

	public SystemException(String details, Throwable cause) {
		super(details, cause);
	}
	
	public SystemException(Throwable cause) {
		super(cause);
	}
}
