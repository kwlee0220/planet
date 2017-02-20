package planet;



/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class IllegalOperationException extends SystemException {
	private static final long serialVersionUID = 8030288752629978366L;
	
	private String m_opId;

	public IllegalOperationException(String opId) {
		super("opId=" + opId);
		
		m_opId = opId;
	}
	
	public String getOperationId() {
		return m_opId;
	}
}
