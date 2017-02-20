package planet;



/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class SimpleIntegerEvent implements Event {
	public static final String PROP_VALUE = "value";
	private static final String[] PROPERTIES = new String[]{PROP_VALUE};
	private static final String[] EMPTY = new String[0];
	
	private volatile int m_value;
	
	public SimpleIntegerEvent(int value) {
		m_value = value;
	}
	
	@Override
	public String[] getEventTypeIds() {
		return EMPTY;
	}

	@Override
	public Object getProperty(String name) {
		if ( PROP_VALUE.equals(name) ) {
			return m_value;
		}
		
		throw new InvalidArgumentException("property name=" + name);
	}

	@Override
	public String[] getPropertyNameAll() {
		return PROPERTIES;
	}

	public int getValue() {
		return m_value;
	}
}
