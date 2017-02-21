package etri.planet;


/**
 * 본 클래스는 ThreadSafe 하지 않다.
 * 
 * @author Kang-Woo Lee
 */
class EventPropertyDesc {
	final String m_name;
	final Object m_value;
	
	EventPropertyDesc(String name, Object value) {
		m_name = name;
		m_value = value;
	}
}