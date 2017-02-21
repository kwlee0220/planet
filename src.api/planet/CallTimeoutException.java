package planet;


/**
 * <code>CallTimeoutException</code>는 원격 호출시 호출 제한 시간이 초과된 경우 발생되는
 * 예외 클래스이다. 
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class CallTimeoutException extends SystemException {
	private static final long serialVersionUID = 512366510719106553L;

	public CallTimeoutException(String details) {
		super(details);
	}
}
