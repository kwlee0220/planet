package etri.planet.transport;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import planet.PlanetUtils;

import etri.planet.TransportLoggers;


/**
 * 
 * 본 클래스는 ThreadSafe하도록 구현되었다.
 * 
 * @author Kang-Woo Lee
 */
class IoScheduler implements Runnable {
	public Selector m_selector;
	private final BlockingQueue<ConnectionImpl> m_ioReadyQ;
    private final List<ConnectionImpl> m_waiters;	// guarded by "this"
    private volatile boolean m_normalShutdown;
    private final AtomicBoolean m_selecting;
	
	IoScheduler(BlockingQueue<ConnectionImpl> connQ) {
		m_ioReadyQ = connQ;
		m_waiters = new ArrayList<ConnectionImpl>();
		m_normalShutdown = false;
		m_selecting = new AtomicBoolean(false);
	}
	
	void start() throws IOException {
		m_selector = Selector.open();
		
		ThreadGroup grp = new ThreadGroup("planet:high");
		grp.setMaxPriority(Thread.MAX_PRIORITY);
		new Thread(grp, this, "planet:io-scheduler").start();
	}
	
	void shutdown() {
		try {
			m_normalShutdown = true;
			
			m_selector.close();
			
			if ( TransportLoggers.SELECTOR.isInfoEnabled() ) {
				TransportLoggers.SELECTOR.info("stopped: " + getClass().getSimpleName());
			}
		}
		catch ( IOException e ) {
			TransportLoggers.SELECTOR.warn("exception ignored while closing TransportManager", e);
		}
	}
    
	synchronized void submitForRead(ConnectionImpl conn) {
		synchronized ( conn ) {
			conn.m_ops |= SelectionKey.OP_READ;
			if ( conn.m_selectionKey != null ) {
				conn.m_selectionKey.interestOps(conn.m_ops);
			}
		}
		m_waiters.add(conn);
		
		if ( m_selecting.get() ) {
			if ( TransportLoggers.SELECTOR.isDebugEnabled() ) {
				TransportLoggers.SELECTOR.debug("interrupt selector for reader registration");
			}
			
			m_selector.wakeup();
		}
    }
    
	void submitForWrite(ConnectionImpl conn) {
		synchronized ( conn ) {
			conn.m_ops |= SelectionKey.OP_WRITE;
			conn.m_selectionKey.interestOps(conn.m_ops);
		}
    	
		if ( TransportLoggers.SELECTOR.isDebugEnabled() ) {
			TransportLoggers.SELECTOR.debug("submit for write: channel[" + conn + "]");
		}
		
		if ( m_selecting.get() ) {
			if ( TransportLoggers.SELECTOR.isDebugEnabled() ) {
				TransportLoggers.SELECTOR.debug("interrupt selector for writer registration");
			}
			
			m_selector.wakeup();
		}
    }
	
	public void run() {
		try {
			while ( true ) {
//				m_selecting.set(true);
//				StringBuilder builder = new StringBuilder();
//				for ( SelectionKey key: m_selector.keys() ) {
//					builder.append(',').append(key.interestOps());
//				}
//				String pattern = (builder.length() > 0) ? builder.substring(1) : "";
//				System.out.print("selecting (keys=" + pattern + ")..."); System.out.flush();
//				int n = m_selector.select();
//				System.out.println(" -> selected=" + n);
//				m_selecting.set(false);
				
				m_selecting.set(true);
				m_selector.select();
				m_selecting.set(false);
				
				schedule();
			}
		}
		catch ( IOException e ) {
			if ( !m_normalShutdown ) {
				TransportLoggers.SELECTOR.fatal("failed: IoScheduler (Selector closed)", e);
			}
		}
		catch ( ClosedSelectorException e ) {
			if ( !m_normalShutdown ) {
				TransportLoggers.SELECTOR.fatal("shutdown: IoScheduler (Selector closed)");
			}
		}
	}
	
	private void schedule() {
		Set<SelectionKey> selectedKeys = m_selector.selectedKeys();
		
		int remains = selectedKeys.size();
		if ( remains > 0 ) {
			for ( Iterator<SelectionKey> iter = selectedKeys.iterator(); remains > 0; --remains ) {
				SelectionKey key = iter.next();
				ConnectionImpl conn = (ConnectionImpl)key.attachment();
//				System.out.println("\tconn=" + conn.getId() + ", read=" + key.isReadable() + ",write=" + key.isWritable());

				try {
					if ( key.isReadable() ) {
						synchronized ( conn ) {
							conn.m_ops &= ~SelectionKey.OP_READ;
							conn.m_selectionKey.interestOps(conn.m_ops);
						}
						
						m_ioReadyQ.put(conn);
					}
					
					if ( key.isWritable() ) {
						synchronized ( conn ) {
							conn.m_ops &= ~SelectionKey.OP_WRITE;
							conn.m_selectionKey.interestOps(conn.m_ops);
						}

						conn.notifyWriteIsReady();
					}
				}
				catch ( CancelledKeyException ignored ) { }
				catch ( Exception e ) {
					TransportLoggers.SELECTOR.warn("" + PlanetUtils.unwrapThrowable(e));
					
					conn.close();
				}
			}
			selectedKeys.clear();
		}
		
		synchronized ( this ) {
			int size = m_waiters.size();
			for ( int i =0; i < size; ++i ) {
				ConnectionImpl conn = m_waiters.get(i);
					
				try {
					conn.m_selectionKey = conn.m_sockChannel.register(m_selector,
																conn.m_ops, conn);
			    	
					if ( TransportLoggers.SELECTOR.isDebugEnabled() ) {
						TransportLoggers.SELECTOR.debug("register for read: " + conn + "]");
					}
				}
				catch ( CancelledKeyException e ) { }
				catch ( ClosedChannelException e ) { }
			}
			
			m_waiters.clear();
		}
	}
}