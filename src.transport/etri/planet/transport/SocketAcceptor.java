package etri.planet.transport;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import planet.SystemException;

import etri.planet.TransportLoggers;

/**
 * 
 * 본 클래스는 ThreadSafe하도록 구현되었다.
 * 
 * @author Kang-Woo Lee
 */
class SocketAcceptor implements Runnable {
	private final TransportManagerImpl m_transport;
	private volatile ServerSocketChannel m_server;	// guarded by "this"
	private final Thread m_thread;
	
	SocketAcceptor(TransportManagerImpl transport) {
		m_transport = transport;
		m_thread = new Thread(this, "planet:accept");
	}
	
	ServerSocketChannel getServerSocketChannel() {
		return m_server;
	}
	
	int start(int listenerPort) throws IOException {
		synchronized ( this ) {
			if ( m_server != null ) {
				throw new SystemException(getClass().getSimpleName() + " started already");
			}
			
			m_server = ServerSocketChannel.open();
		}
		
		ServerSocket socket = m_server.socket();
		socket.setReuseAddress(true);
		socket.bind(new InetSocketAddress(listenerPort));
		m_server.configureBlocking(true);
		
		m_thread.start();
		
		return socket.getLocalPort();
	}
	
	void stop() {
		try {
			m_server.close();
			if ( TransportLoggers.ACCEPTOR.isInfoEnabled() ) {
				TransportLoggers.ACCEPTOR.info("stopped: " + getClass().getSimpleName());
			}
		}
		catch ( IOException e ) {
			TransportLoggers.ACCEPTOR.error("fails to stop " + getClass().getSimpleName(), e);
		}
	}
	
	public void run() {
		while ( true ) {
			try {
				SocketChannel sockChannel = m_server.accept();
				Socket socket = sockChannel.socket();
				
				String remoteHost = socket.getInetAddress().getHostAddress();
				int remotePort = socket.getPort();
				
				if ( remoteHost.equals("211.38.47.6") ) {
					sockChannel.close();
					continue;
				}
				
				if ( TransportLoggers.ACCEPTOR.isDebugEnabled() ) {
					TransportLoggers.ACCEPTOR.debug("accepting: socket=" + remoteHost + ":" + remotePort);
				}

				ConnectionImpl conn = new ConnectionImpl(m_transport, sockChannel);
				if ( TransportLoggers.ACCEPTOR.isInfoEnabled() ) {
					TransportLoggers.ACCEPTOR.info("acceptted: " + conn);
				}
				
				conn.submitForRead();
			}
			catch ( AsynchronousCloseException e ) {
				return;
			}
			catch ( IOException e ) {
				TransportLoggers.ACCEPTOR.error("failed: in accepting connections (in ConnectionAcceptor)", e);
				return;
			}
		}
	}
}
