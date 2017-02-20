package planet.transport;

import planet.SystemException;


/**
 * <code>ProtocolException</code>는 원격 호출시 호출 제한 시간이 초과된 경우 발생되는
 * 예외 클래스이다. 
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class ProtocolException extends SystemException {
	private static final long serialVersionUID = -6170852737356528885L;

	public ProtocolException(String msg) {
		super(msg);
	}
}
