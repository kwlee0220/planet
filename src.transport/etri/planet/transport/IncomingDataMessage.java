package etri.planet.transport;


import java.io.IOException;
import java.nio.ByteBuffer;

import planet.SystemException;
import planet.transport.Connection;
import planet.transport.InputChannel;

import etri.planet.TransportLoggers;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class IncomingDataMessage extends DataMessage {
	public IncomingDataMessage(TransportHeader header) {
		super(header);
	}

	@Override
	public void handle(ConnectionImpl conn) throws Exception {
		conn.updateDataAccessTime();
		
		if ( TransportLoggers.CHANNEL.isDebugEnabled() ) {
			TransportLoggers.CHANNEL.debug("rcvd: " + this);
		}
		
		if ( m_header.m_blockNum == 0 ) {
			InputChannel ichannel = null;
			if ( m_header.m_final == 1 ) {
				ichannel = new SingleBlockInputChannel(conn, m_header.m_chId, m_buffer);
				
				if ( TransportLoggers.CHANNEL.isDebugEnabled() ) {
					TransportLoggers.CHANNEL.debug("opened: " + ichannel + ", " + conn);
				}
			}
			else {
				// channel의 첫 메시지인 경우는 InputChannel 객체를 생성하여 Connection에 등록시키고,
				// consumer 쓰레드를 가동시킨다.
				ichannel = new MultiBlockInputChannel(conn, m_header.m_chId);
				conn.registerInputChannel((MultiBlockInputChannel)ichannel);
				
				if ( TransportLoggers.CHANNEL.isInfoEnabled() ) {
					TransportLoggers.CHANNEL.info("opened: " + ichannel + ", " + conn);
				}
				
				((MultiBlockInputChannel)ichannel).appendBlock(m_buffer, false);
			}
			
			final InputChannel fic = ichannel;
			final TransportManagerImpl transport = conn.m_transport;
			transport.m_executor.execute(new Runnable() {
				public void run() {
					transport.m_listener.onInputChannelCreated(fic, m_header.m_final == 1);
				}
			});
		}
		else {
			// 등록된 PlanetInputStream 객체를 찾는다.
			MultiBlockInputChannel ichannel = conn.lookupInputChannel(m_header.m_chId);
			if ( ichannel != null ) {
				ichannel.appendBlock(m_buffer, m_header.m_final == 1);
			}
			else {
				// 제거된 스트림의 데이타를 읽어서 버린다.
				if ( TransportLoggers.CHANNEL.isDebugEnabled() ) {
					TransportLoggers.CHANNEL.debug("discarded msg of unknown channel[" + m_header.m_chId
											+ "]: msg=" + this);
				}
			}
		}
		
		// 다중 블럭 채널의 마지막 블럭인 경우 해당  채널을 연결에서 등록 해제시킨다.
		if ( m_header.m_final == 1 && m_header.m_blockNum > 0 ) {
			conn.unregisterInputChannel(m_header.m_chId);
		}
	}

	@Override
	public void readPayload(Connection conn, ByteBuffer buf) throws IOException {
		int length = m_header.m_length - TransportHeader.SIZE;
		m_buffer = ByteBuffer.allocate(length);
		if ( buf.remaining() > length ) {
			TransportUtils.copyTo(buf, m_buffer, length);
		}
		else {
			m_buffer.put(buf);
		}
		m_buffer.flip();
	}

	@Override
	public void encode(ByteBuffer buf) throws IOException {
		throw new SystemException("Should not be called!!");
	}

	@Override
	public void writePayload(ByteBuffer buf) throws IOException {
		throw new SystemException("Should not be called!!");
	}

	@Override
	public String toString() {
		if ( m_header.m_final == 0 ) {
			return "DATA_CONT[in:" + m_header.toShortString() + "]";
		}
		else {
			return "DATA_LAST[in:" + m_header.toShortString() + "]";
		}
	}
}
