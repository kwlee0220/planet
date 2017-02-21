package etri.planet.transport;


import java.io.IOException;
import java.nio.ByteBuffer;

import planet.transport.Connection;
import planet.transport.OutputChannel;

import etri.planet.TransportLoggers;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class DataControlMessage extends TransportMessage {
	public static final int NEXT_DATA = 0x00000000;
	private static final int CLOSE_DATA = 0xFFFFFFFF;
	
	public int m_channelId;
	public int m_control;
	
	public static DataControlMessage newCloseChannelMessage(int channelId) {
		return new DataControlMessage(channelId, CLOSE_DATA);
	}
	
	public DataControlMessage(int channelId, int control) {
		super(new TransportHeader(TransportHeader.CODE_DATA_CTRL, -1));
		
		m_channelId = channelId;
		m_control = control;
	}
	
	public DataControlMessage(TransportHeader header) {
		super(header);
	}

	@Override
	public void handle(ConnectionImpl conn) throws Exception {
		if ( conn.isClosed() ) {
			if ( TransportLoggers.TRANSPORT.isDebugEnabled() ) {
				TransportLoggers.TRANSPORT.debug("discarded: msg of closed " + conn);
			}
			
			return;
		}
		
		if ( TransportLoggers.CHANNEL.isDebugEnabled() ) {
			TransportLoggers.CHANNEL.debug("rcvd: " + this);
		}
		
		switch ( m_control ) {
			case NEXT_DATA:
				conn.unpendingOutputChannel(m_channelId);
				break;
			case CLOSE_DATA:
				OutputChannel ochannel = conn.getOutputChannel(m_channelId);
				if ( ochannel != null ) {
					ochannel.closeByOther();
					
					if ( TransportLoggers.CHANNEL.isInfoEnabled() ) {
						TransportLoggers.CHANNEL.info("closed by reader: " + ochannel + ", "
														+ ochannel.getConnection());
					}
				}
				break;
		}
	}

	@Override
	public void readPayload(Connection conn, ByteBuffer buf) throws IOException {
		m_channelId = buf.getInt();
		m_control = buf.getInt();
	}

	@Override
	public void writePayload(ByteBuffer buf) throws IOException {
		buf.putInt(m_channelId);
		buf.putInt(m_control);
	}
	
	public String toString() {
		String ctrlStr;
		switch ( m_control ) {
			case NEXT_DATA:
				ctrlStr = "NEXT";
				break;
			case CLOSE_DATA:
				ctrlStr = "CLOSE";
				break;
			default:
				ctrlStr = "UNKNOWN[control=" + m_control + "]";
				break;
		}
		
		return String.format("%s[ch=%d]", ctrlStr, m_channelId);
	}
}
