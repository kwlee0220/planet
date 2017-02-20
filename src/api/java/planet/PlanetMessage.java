package planet;

/**
 * 
 * @author Kang-Woo Lee
 */
public interface PlanetMessage {
	public static final byte MSG_CALL = 0;
	public static final byte MSG_REPLY = 1;
	public static final byte MSG_ERROR = 2;
	public static final byte MSG_NOTIFY = 3;
	public static final byte MSG_STREAM = 4;
}