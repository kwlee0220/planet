package planet;



/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class NullLocalEvent implements Event {
	private static final String[] EMPTY = new String[0];
	
	@Override
	public String[] getEventTypeIds() {
		return EMPTY;
	}

	@Override
	public Object getProperty(String name) {
		return null;
	}

	@Override
	public String[] getPropertyNameAll() {
		return EMPTY;
	}

}
