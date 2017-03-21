package etri.planet.transport;


import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;

import planet.PlanetUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;	



/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
class IoProcessor implements Runnable {
	private static final Logger s_logger = LoggerFactory.getLogger("PLANET.CHANNEL");

	private final BlockingQueue<ConnectionImpl> m_ioReadyQ;
	private final Executor m_executor;
	private volatile Thread m_myself;
	private volatile boolean m_stopRequested;
	
	IoProcessor(BlockingQueue<ConnectionImpl> queue, Executor executor) {
		m_ioReadyQ = queue;
		m_executor = executor;
	}
	
	void stop() {
		m_stopRequested = true;
		m_myself.interrupt();
	}
	
	public void run() {
		m_myself = Thread.currentThread();
		
		try {
			while ( !m_stopRequested ) {
				handleIoEvent(m_ioReadyQ.take());
			}
		}
		catch ( InterruptedException expected ) { }
	}
	
	private void handleIoEvent(final ConnectionImpl conn) {
		if ( conn.isClosed() ) {
			return;
		}
		
		try {
			conn.m_msgBuilder.fillBuffer();
		}
		catch ( IOException expected ) {
			// 소켓이 끊어져서 IO event가 발생된 경우.
			conn.close();
			
			return;
		}
		
		while ( true ) {
			TransportMessage msg;
			try {
				msg = conn.m_msgBuilder.build();
				if ( msg == null ) {
					conn.m_msgBuilder.compact();
					conn.submitForRead();
					
					return;
				}
			}
			catch ( IOException e ) {
				s_logger.error("fails to read TransportMessage: conn=" + conn
								+ ", cause=" + PlanetUtils.unwrapThrowable(e));
		
				conn.close();
				
				return;
			}

			conn.m_msgBuilder.resetHeader();
			conn.m_msgBuilder.compact();
			
			final TransportMessage fmsg = msg;
			switch ( msg.m_header.m_code ) {
				case TransportHeader.CODE_CONNECT_REPLY:
				case TransportHeader.CODE_HEARTBEAT_ACK:
				case TransportHeader.CODE_DATA:
					handleMessage(conn, msg);
					break;
				default:
					m_executor.execute(new Runnable() {
						public void run() {
							handleMessage(conn, fmsg);
						}
					});
					break;
			}
			
			if ( conn.m_msgBuilder.remaining() == 0 ) {
				conn.m_msgBuilder.compact();
				conn.submitForRead();
				
				break;
			}
		}
	}
	
	private void handleMessage(final ConnectionImpl conn, final TransportMessage msg) {
		try {
			msg.handle(conn);
		}
		catch ( Exception e ) {
			e.printStackTrace();
			s_logger.error("fails to handle TransportMessage: msg=" + msg
							+ ", conn=" + conn
							+ ", cause=" + PlanetUtils.unwrapThrowable(e));
			
			conn.close();
		}
	}
}
