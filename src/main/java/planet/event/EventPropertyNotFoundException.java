package planet.event;

import planet.PlanetRuntimeException;


/**
 * <code>EventPropertyNotFoundException</code>는 지정된 속성 이름에 해당하는 속성 값이
 * 없는 경우 발생되는 예외를 정의한 클래스이다.
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class EventPropertyNotFoundException extends PlanetRuntimeException {
	private static final long serialVersionUID = -4930203009116607674L;

	public EventPropertyNotFoundException(String msg) {
		super(msg);
	}
}
