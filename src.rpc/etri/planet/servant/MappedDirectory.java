package etri.planet.servant;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import planet.Directory;
import planet.PersistentServant;

import etri.planet.RpcLoggers;
import etri.planet.UpdatableDirectory;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class MappedDirectory implements UpdatableDirectory {
	private final String m_path;
	private final Map<String,PersistentServant> m_servants;
	
	public MappedDirectory(String path) {
		m_path = path;
		m_servants = new HashMap<String,PersistentServant>();
	}

	public synchronized PersistentServant getServant(String id) {
		return m_servants.get(id);
	}

	public synchronized PersistentServant addServant(String id, PersistentServant servant) {
		String path = servant.getServantPath();
		List<String> nodeList = ServantUtils.parseServantPath(path);
		if ( !id.equals(nodeList.get(nodeList.size()-1)) ) {
			throw new IllegalArgumentException("Invalid servant path=" + path);
		}
		
		PersistentServant prev = m_servants.put(id, servant);
		
		if ( RpcLoggers.SERVANT.isDebugEnabled() ) {
			RpcLoggers.SERVANT.debug("added: " + ServantManager.toString(path, servant));
		}
		
		return prev;
	}

	public synchronized PersistentServant removeServant(String id) {
		PersistentServant servant = m_servants.remove(id);
		if ( servant != null && RpcLoggers.SERVANT.isDebugEnabled() ) {
			RpcLoggers.SERVANT.debug("removed: " + ServantManager.toString(servant));
		}
		
		return servant;
	}

	public String getServantPath() {
		return m_path;
	}

	private static final Class<?>[] REMOTE_INTERFACES = new Class[]{Directory.class};
	public Class<?>[] getRemoteInterfaces() {
		return REMOTE_INTERFACES;
	}
}
