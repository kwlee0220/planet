package planet;



/**
 * 
 * @author Kang-Woo Lee
 */
public abstract class AbstractDirectory implements Directory {
	private final String m_path;
	
	protected AbstractDirectory(String path) {
		m_path = path;
	}

	public String getServantPath() {
		return m_path;
	}

	private static final Class<?>[] REMOTE_INTERFACES = new Class[]{Directory.class};
	public Class<?>[] getRemoteInterfaces() {
		return REMOTE_INTERFACES;
	}
}
