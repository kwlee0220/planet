package etri.planet;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.concurrent.GuardedBy;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class PlanetClassLoader {
	private final ConcurrentMap<String,Class<?>> m_lazyClasses
													= new ConcurrentHashMap<String,Class<?>>();
	@GuardedBy("m_classLoaders")
	private final Map<String,ClassLoader> m_classLoaders
													= new LinkedHashMap<String,ClassLoader>();
	
	public synchronized Class<?> loadClass(String className) throws ClassNotFoundException {
		ClassNotFoundException raised = null;
		
		try {
			return Class.forName(className, true, getClass().getClassLoader());
		}
		catch ( ClassNotFoundException e ) {
			raised = e;
		}
		
		Class<?> clazz = m_lazyClasses.get(className);
		if ( clazz != null ) {
			return clazz;
		}
		
		synchronized ( m_classLoaders ) {
			for ( Map.Entry<String,ClassLoader> entry: m_classLoaders.entrySet() ) {
				try {
					clazz = Class.forName(className, true, entry.getValue());
					m_lazyClasses.put(className, clazz);
					
					return clazz;
				}
				catch ( ClassNotFoundException e ) { }
			}
		}
		
		throw raised;
	}
	
	public void addClassLoader(String id, ClassLoader loader) {
		synchronized ( m_classLoaders ) {
			m_classLoaders.put(id, loader);
		}
	}
	
	public void removeClassLoader(String id) {
		synchronized ( m_classLoaders ) {
			m_classLoaders.remove(id);
		}
	}
}
