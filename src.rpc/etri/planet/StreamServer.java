package etri.planet;


import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import planet.transport.OutputChannel;


/**
 * 
 * @author Kang-Woo Lee
 */
public class StreamServer implements OutputChannel.Listener {
	private static final int STATE_NOT_STARTED =0;
	private static final int STATE_RUNNING =1;
	private static final int STATE_STOP_REQUESTED =2;
	private static final int STATE_STOPPED =3;

	private final PlanetSessionImpl m_session;
	private final InputStream m_is;
	private final OutputChannel m_ochannel;
	
	private final ReentrantLock m_lock = new ReentrantLock();
	private final Condition m_cond = m_lock.newCondition();
	private int m_status = STATE_NOT_STARTED;				// guarded by 'm_lock'
	
	public StreamServer(PlanetSessionImpl session, InputStream is) throws IOException {
		m_session = session;
		m_is = is;
		m_ochannel = session.newOutputChannel();
		m_ochannel.setListener(this);
		
		m_session.m_strmServers.put(m_ochannel.getId(), this);
	}
	
	public int getStreamId() {
		return m_ochannel.getId();
	}
	
	public void start() {
		if ( RpcLoggers.STREAM.isDebugEnabled() ) {
			RpcLoggers.STREAM.debug("fork: " + this);
		}
		
		m_session.m_planet.getPlanetExecutor().execute(new Worker());
	}
	
	public void stop() throws InterruptedException {
		if ( RpcLoggers.STREAM.isDebugEnabled() ) {
			RpcLoggers.STREAM.debug("stopping: " + this);
		}
		
		m_lock.lock();
		try {
			if ( m_status == STATE_NOT_STARTED ) {
				m_status = STATE_STOPPED;
				m_cond.signalAll();
			}
			else {
				if ( m_status == STATE_RUNNING ) {
					m_status = STATE_STOP_REQUESTED;
					m_cond.signalAll();
				}
				
				// worker 쓰레드에서 output channel에 write시 blocking되어 있는 상태가 발생될 수 있으므로,
				// 먼저 이 쓰레드를 깨우기 위해 output channel을 강제로 close시킨다.
				try {
					m_ochannel.close(false);
				}
				catch ( Throwable ignored ) { }

				// worker 쓰레드가 마무리 작업을 완료할 때까지 대기한다.
				while ( m_status != STATE_STOPPED ) {
					m_cond.await();
				}
			}
		}
		finally {
			m_lock.unlock();
		}
	}

	@Override
	public void beforeClosedByOther() {
		try {
			stop();
		}
		catch ( InterruptedException e ) { }
	}
	
	public String toString() {
		return "StreamWorker[" + m_ochannel + "], " + m_session.m_conn;
	}
	
	private int getStatus() {
		m_lock.lock();
		try {
			return m_status;
		}
		finally {
			m_lock.unlock();
		}
	}
	
	private boolean compareAndSet(int cond, int target) {
		m_lock.lock();
		try {
			if ( m_status == cond ) {
				m_status = target;
				m_cond.signalAll();
				
				return true;
			}
			else {
				return false;
			}
		}
		finally {
			m_lock.unlock();
		}
	}
	
	private void cleanup() {
		
		m_session.m_strmServers.remove(m_ochannel.getId());
		m_session.m_istreamChannels.remove(m_ochannel.getId());
		
		try {
			m_is.close();
		}
		catch ( IOException ignored ) { }

		m_lock.lock();
		try {
			m_status = STATE_STOPPED;
			m_cond.signalAll();
		}
		finally {
			m_lock.unlock();
		}
	}
	
	private class Worker implements Runnable {
		public void run() {
			if ( RpcLoggers.STRM_WORKER.isInfoEnabled() ) {
				RpcLoggers.STRM_WORKER.info("started: " + StreamServer.this);
			}
			
			try {
				if ( !compareAndSet(STATE_NOT_STARTED, STATE_RUNNING) ) {
					return;
				}
				
				PlanetHeader header = new PlanetHeader(AbstractPlanetMessage.MSG_STREAM, -1);
				PlanetWriter writer = new PlanetWriter(m_session, m_ochannel);
				header.serialize(writer);

				byte[] buffer = new byte[AbstractPlanetMessage.MAX_DATA_SIZE-1];
				while ( true ) {
					if ( getStatus() == STATE_STOP_REQUESTED ) {
						return;
					}

					int nread = m_is.read(buffer, 0, AbstractPlanetMessage.MAX_DATA_SIZE-1);
					if ( nread == -1 || getStatus() == STATE_STOP_REQUESTED ) {
						try {
							m_ochannel.close(true);
						}
						catch ( IOException ignored ) { }
						
						return;
					}

					try {
						m_ochannel.writen(buffer, 0, nread);
					}
					catch ( Exception expected ) {
						return;
					}
				}
			}
			catch ( Exception ignored ) {
				ignored.printStackTrace();
			}
			finally {
				cleanup();
				
				if ( RpcLoggers.STRM_WORKER.isInfoEnabled() ) {
					RpcLoggers.STRM_WORKER.info("stopped: " + StreamServer.this);
				}
			}
		}
	}
}
