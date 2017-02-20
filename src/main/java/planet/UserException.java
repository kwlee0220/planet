package planet;


/**
 * 
 * @author Kang-Woo Lee
 */
public class UserException extends PlanetException {
	private static final long serialVersionUID = 8931808805258993595L;

	public UserException() {
	}

	public UserException(String details) {
		super(details);
	}
}
