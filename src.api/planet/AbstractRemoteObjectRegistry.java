package planet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.concurrent.GuardedBy;

import planet.transport.Connection;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public abstract class AbstractRemoteObjectRegistry<K,V> {
	private final PlanetServer m_planet;
	
	private final ReentrantLock m_lock = new ReentrantLock();
	@GuardedBy("m_lock") private final Map<K,Desc> m_map;
	
	protected abstract void onObjectAdded(K key, V value, ReentrantLock lock);
	protected abstract void onObjectRemoved(K key, V value, ReentrantLock lock);
	
	protected AbstractRemoteObjectRegistry(PlanetServer planet) {
		m_planet = planet;
		m_map = new HashMap<K,Desc>();
	}
	
	public V get(K key) {
		m_lock.lock();
		try {
			Desc desc = (Desc)m_map.get(key);
			return (desc != null) ? (V)desc.m_value : null;
		}
		finally {
			m_lock.unlock();
		}
	}
	
	public boolean containsKey(K key) {
		m_lock.lock();
		try {
			return m_map.containsKey(key);
		}
		finally {
			m_lock.unlock();
		}
	}
	
	public int size() {
		m_lock.lock();
		try {
			return m_map.size();
		}
		finally {
			m_lock.unlock();
		}
	}
	
	public boolean putIfAbsent(K key, V value)
		throws NullPointerException, IOException {
		Desc desc = new Desc(key, value);

		m_lock.lock();
		try {
			Desc prev = (Desc)m_map.put(key, desc);
			if ( prev != null && (prev.m_conn == null
								|| (prev.m_conn != null && !prev.m_conn.isClosed())) ) {
				m_map.put(key, prev);
				
				return false;
			}
			else {
				if ( value instanceof Remote ) {
					try {
						desc.m_conn = m_planet.getConnection(((Remote)value).getPlanetId(), true);
						desc.m_conn.addDisconnectionHandler(desc);
					}
					catch ( IOException e ) {
						m_map.put(key, prev);
						
						throw e;
					}
					catch ( InterruptedException e ) {
						m_map.put(key, prev);
						
						throw new RuntimeException(e);
					}
					catch ( RuntimeException e ) {
						m_map.put(key, prev);
						
						throw e;
					}
				}
			}
			
			onObjectAdded(key, value, m_lock);
			
			return true;
		}
		finally {
			m_lock.unlock();
		}
	}
	
	public V remove(K key) {
		m_lock.lock();
		try {
			Desc desc = (Desc)m_map.remove(key);
			if ( desc != null ) {
				onObjectRemoved(key, desc.m_value, m_lock);
				
				return (V)desc.m_value;
			}
			else {
				return null;
			}
		}
		finally {
			m_lock.unlock();
		}
	}
	
	public List<V> getValues() {
		List<V> values = new ArrayList<V>();
		
		m_lock.lock();
		try {
			for ( Object desc : m_map.values() ) {
				values.add((V)((Desc)desc).m_value);
			}
		}
		finally {
			m_lock.unlock();
		}
		
		return values;
	}
	
	public void fillValues(List<? super V> list) {
		m_lock.lock();
		try {
			for ( Object desc : m_map.values() ) {
				list.add((V)((Desc)desc).m_value);
			}
		}
		finally {
			m_lock.unlock();
		}
	}
	
	public ReentrantLock getLock() {
		return m_lock;
	}
	
	class Desc implements DisconnectionHandler {
		final K m_key;
		final V m_value;
		volatile Connection m_conn;
		
		Desc(K key, V value) {
			m_key = key;
			m_value = value;
		}
		
		public void onDisconnected(Connection conn) {
			remove(m_key);
		}
	}
}
