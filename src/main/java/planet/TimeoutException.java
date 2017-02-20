package planet;




/**
 * <code>TimeoutException</code>는 지정된 시간 내에 작업 처리하는 못하는 경우
 * 발생되는 예외 클래스를 정의한다.
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class TimeoutException extends PlanetException {
	private static final long serialVersionUID = 8579988220954615753L;

	public TimeoutException(String details) {
		super(details);
	}
}
