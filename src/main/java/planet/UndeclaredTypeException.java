package planet;



/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class UndeclaredTypeException extends SystemException {
	private static final long serialVersionUID = -1566943903819910905L;

	public UndeclaredTypeException(String typeId) {
		super("typeId=" + typeId);
	}
}
