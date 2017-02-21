package etri.planet.servant;

import java.util.List;

import planet.Directory;
import planet.PersistentServant;
import planet.PlanetServer;
import planet.PlanetUtils;
import planet.Servant;
import planet.ServantExistsException;
import planet.ServantNotFoundException;
import planet.SystemException;

import etri.planet.RpcLoggers;
import etri.planet.UpdatableDirectory;


/**
 * 
 * @author Kang-Woo Lee
 */
public class ServantManager {
	private Directory m_root;
	
	public ServantManager(Directory rootDir) {
		if ( !rootDir.getServantPath().equals(PlanetServer.ROOT_PATH) ) {
			throw new IllegalArgumentException("Invalid root servant path=" + rootDir.getServantPath());
		}
		
		m_root = rootDir;
	}
	
	/**
	 * 루트 Servant를 반환한다.
	 * 
	 * @return	루트 servant 객체.
	 */
	public Directory getRootDirectory() {
		return m_root;
	}
	
	/**
	 * 주어진 servant path에 해당하는 Servant 객체를 반환한다.
	 * 
	 * @param path		대상 servant의 path
	 * @return 검색된 Servant 객체.
	 * @throws ServantNotFoundException	path에 해당하는 Servant 객체가 없는 경우.
	 */
	public Servant getServant(String path) throws ServantNotFoundException {
		if ( path == null ) {
			throw new NullPointerException("path was null");
		}
		
		path = path.trim();
		if ( path.equals(PlanetServer.ROOT_PATH) ) {
			return m_root;
		}
		
		List<String> nodeList = ServantUtils.parseServantPath(path);
		return (Servant)traverse(nodeList, nodeList.size());
	}
	
	/**
	 * 주어진 servant path에 해당하는 Servant 객체의 존재 여부를 반환한다.
	 * <p><code>path</code>가 null이거나 길이 0 배열인 경우는 루트 servant의 존재 여부를 반환한다.
	 * 
	 * @param	path	대상 servant의 path.
	 * @return	Servant 객체의 존재 여부.
	 */
	public synchronized boolean existsServant(String path) {
		if ( path == null ) {
			throw new NullPointerException("path was null");
		}
		
		path = path.trim();
		if ( path.equals(PlanetServer.ROOT_PATH) ) {
			return true;
		}

		List<String> nodeList = ServantUtils.parseServantPath(path);
		try {
			traverse(nodeList, nodeList.size());
			
			return true;
		}
		catch ( ServantNotFoundException e ) {
			return false;
		}
	}
	
	public synchronized void addServant(PersistentServant servant)
		throws ServantNotFoundException, ServantExistsException {
		if ( servant == null ) {
			throw new NullPointerException("servant was null");
		}
		
		String path = servant.getServantPath();
		if ( path == null ) {
			throw new NullPointerException("servant path was null");
		}
		path = path.trim();
		if ( path.length() == 0 || path.charAt(0) != '/' ) {
			throw new IllegalArgumentException("servant path=" + path);
		}
		if ( path.length() == 1 ) {
			throw new ServantExistsException("Cannot add servant at root position");
		}

		List<String> nodeList = ServantUtils.parseServantPath(path);
		Directory dir = traverseToDirectory(nodeList, nodeList.size()-1);
		if ( !(dir instanceof UpdatableDirectory) ) {
			throw new SystemException("non-updatable directory=" + path);
		}
		
		UpdatableDirectory udir = (UpdatableDirectory)dir;
		String id = nodeList.get(nodeList.size()-1);
		PersistentServant prev = udir.addServant(id, servant);
		if ( prev != null ) {
			udir.addServant(id, prev);
			
			throw new ServantExistsException("path=" + path);
		}
		
		if ( RpcLoggers.SERVANT.isInfoEnabled() ) {
			RpcLoggers.SERVANT.info("added: " + toString(path, servant) + " object=" + servant);
		}
	}
	
	public synchronized boolean removeServant(String path) {
		if ( path == null ) {
			throw new SystemException("path was null");
		}
		
		path = path.trim();
		if ( path.equals(PlanetServer.ROOT_PATH) ) {
			throw new SystemException("Cannot remove root servant");
		}

		List<String> nodeList = ServantUtils.parseServantPath(path);
		try {
			Directory dir = traverseToDirectory(nodeList, nodeList.size()-1);
			if ( !(dir instanceof UpdatableDirectory) ) {
				throw new SystemException("non-updatable directory=" + path);
			}
			
			Servant removed = ((UpdatableDirectory)dir).removeServant(nodeList.get(nodeList.size()-1));
			if ( removed == null ) {
				return false;
			}
			
			if ( RpcLoggers.SERVANT.isInfoEnabled() ) {
				RpcLoggers.SERVANT.info("removed: " + toString(nodeList, nodeList.size()-1, removed));
			}
		}
		catch ( ServantNotFoundException e ) {
			return false;
		}
		
		return true;
	}
	
	private synchronized Object traverse(List<String> path, int depth)
		throws ServantNotFoundException {
		Servant current = m_root;
		for ( int i =0; i < depth; ++i ) {
			if ( current instanceof Directory ) {
				current = ((Directory)current).getServant(path.get(i));
				if ( current == null ) {
					throw new ServantNotFoundException("path="
												+ PlanetUtils.newServantPath(path, i+1));
				}
			}
			else {
				throw new ServantNotFoundException("not directory: path="
													+ PlanetUtils.newServantPath(path, i));
			}
		}
		
		return current;
	}
	
	private Directory traverseToDirectory(List<String> path, int depth)  {
		Object node = traverse(path, depth);
		if ( node instanceof Directory ) {
			return (Directory)node;
		}
		else {
			return null;
		}
	}
	
	public static String toString(PersistentServant servant) {
		return "Servant[path=" + servant.getServantPath() + ", class="
				+ servant.getClass().getSimpleName() + "]";
	}
	
	public static String toString(String path, Servant servant) {
		return "Servant[path=" + path + ", class="
				+ servant.getClass().getSimpleName() + "]";
	}
	
	public static String toString(List<String> nodeList, int nodeLength, Servant servant) {
		return "Servant[path=" + toPathString(nodeList, nodeLength)
				+ ", class=" + servant.getClass().getSimpleName() + "]";
	}
	
	public static String toPathString(List<String> nodeList, int length) {
		if ( nodeList.size() == 0 ) {
			return "/";
		}
		
		StringBuilder builder = new StringBuilder();
		for ( int i =0; i < length; ++i ) {
			builder.append('/').append(nodeList.get(i));
		}
		
		return builder.toString();
	}
}
