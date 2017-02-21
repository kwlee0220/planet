package planet;


/**
 * <code>ArgumentException</code>는 메소드 호출시 인자 오류와 관련된 예외 클래스를 정의한다. 
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class InvalidArgumentException extends PlanetRuntimeException {
	private static final long serialVersionUID = -3564962871276510221L;

	public InvalidArgumentException(String details) {
		super(details);
	}
}
