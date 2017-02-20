package planet;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


/**
 * <code>RemoteSystemException</code>는 원격 메소드 호출시 원격 처리 중 메소드에
 * 지정되지 않은 예외가 발생된 경우 발생되는 예외 클래스 이다.
 * <p>
 * 발생된 실제 예외는 {@link #getCause()}를 호출하여 얻을 수 있다.
 * 
 * @author Kang-Woo Lee (Kang-Woo Lee)
 */
public class RemoteSystemException extends SystemException {
	private static final long serialVersionUID = -7026547024128189151L;
	public static final String ID = RemoteSystemException.class.getName();
	
	private String m_causeId;
	private String m_causeDetails;
	private SystemException m_cause;

	public RemoteSystemException(SystemException cause) {
		super(createDetails(cause));
		
		m_causeId = cause.getClass().getName();
		m_causeDetails = cause.getDetails();
		m_cause = cause;
	}

	public RemoteSystemException(String details) {
		super(details);
		
		int begin = details.indexOf('[') + 1;
		int end = details.indexOf(':', begin);
		
		m_causeId = details.substring(begin, end);
		m_causeDetails = details.substring(end+1, details.length());
	}
	
	private static String createDetails(SystemException e) {
		return String.format("cause=[%s:%s]", e.getClass().getName(), e.getDetails());
	}

	private static final Class<?>[] THROWABLE_CTOR_TYPES = new Class[]{String.class};
	public SystemException getCauseException(PlanetServer planet) {
		if ( m_cause == null ) {
			try {
				Class<?> cls = planet.loadClass(m_causeId);
				Constructor<?> ctor = cls.getConstructor(THROWABLE_CTOR_TYPES);
				m_cause = (SystemException)ctor.newInstance(new Object[]{m_causeDetails});
			}
			catch ( InvocationTargetException e ) {
				throw new SystemException("" + e.getTargetException());
			}
			catch ( Exception e ) {
				throw new SystemException("" + e);
			}
		}
		
		return m_cause;
	}
}
