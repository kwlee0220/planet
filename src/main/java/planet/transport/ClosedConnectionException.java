package planet.transport;

import planet.SystemException;



/**
 * <code>ClosedConnectionException</code>는 원격 메소드 호출시 대상 PlanetServer와의
 * 연결을 얻을 수 없는 경우 발생되는 예외이다.
 * 
 * @author Kang-Woo Lee
 */
public class ClosedConnectionException extends SystemException {
	private static final long serialVersionUID = -7127065601227012305L;

	public ClosedConnectionException(String details) {
		super(details);
	}
}
