package etri.planet;


import java.io.IOException;

import planet.PlanetContext;
import planet.PlanetUtils;
import planet.ServantNotFoundException;
import planet.transport.Connection;
import planet.transport.InputChannel;

import org.apache.log4j.MDC;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
class PlanetMessageHandler {
	public static void execute(final PlanetSessionImpl session, final InputChannel ichannel,
								boolean nonblocking) throws IOException {
		PlanetServerImpl planet = session.m_planet;
		final PlanetReader input = new PlanetReader(session, ichannel);
		final PlanetHeader header = PlanetHeader.deserialize(input);

		boolean execDirect = false;
		AbstractPlanetMessage msg = null;
		switch ( header.m_code ) {
			case AbstractPlanetMessage.MSG_CALL:
				msg = new CallMessage(planet, header);
				break;
			case AbstractPlanetMessage.MSG_NOTIFY:
				msg = new NotifyMessage(planet, header);
				break;
			case AbstractPlanetMessage.MSG_REPLY:
				msg = new ReplyMessage(header);
				execDirect = nonblocking;
				break;
			case AbstractPlanetMessage.MSG_ERROR:
				msg = new ErrorMessage(header);
				execDirect = nonblocking;
				break;
			case AbstractPlanetMessage.MSG_STREAM:
				session.registerInputChannel(ichannel);
				return;
			default:
				throw new IOException("unknown message: header=" + header);
		}
		
		if ( execDirect ) {
			handleMessage(input, msg);
		}
		else {
			final AbstractPlanetMessage fmsg = msg;
			planet.getPlanetExecutor().execute(new Runnable() {
				public void run() {
					handleMessage(input, fmsg);
				}
			});
		}
	}
	
	private static void handleMessage(PlanetReader input, AbstractPlanetMessage msg) {
		InputChannel ichannel = input.getInputChannel();
		
		Connection conn = ichannel.getConnection();
		String desc = conn.getDescription();
		if ( desc != null ) {
			MDC.put("conn", "C[" + desc + "]");
		}
		else {
			MDC.put("conn", "C[" + conn.getId() + "]");
		}

		final PlanetContext context = PlanetServerImpl.s_context.get();
		try {
			msg.readPayload(input);
			
			context.setPlanetSession(input.getPlanetSession());
//			context.setMessage(msg);
			
			try {
				msg.onReceived(input.getPlanetSession());
			}
			catch ( Throwable e ) {
				RpcLoggers.MSG_HANDLE.error("fails in handling msg=" + msg + ", cause=" + e, e);
			}
		}
		catch ( ServantNotFoundException e ) {
			RpcLoggers.MSG_HANDLE.warn("servant was invalid in the incoming message: path="
									+ e.getMessage());
			msg.onErrorReadingPayload(input.getPlanetSession(), e);
		}
		catch ( Throwable e ) {
			e = PlanetUtils.unwrapThrowable(e);
			RpcLoggers.MSG_HANDLE.error("fails in reading msg=" + msg.getClass().getName()
										+ ", cause=" + e);
			msg.onErrorReadingPayload(input.getPlanetSession(), e);
		}
		finally {
			context.setPlanetSession(null);
//			context.setMessage(null);
			
			try {
				ichannel.close();
			}
			catch ( IOException ignored ) { }
			
			MDC.remove("conn");
		}
	}
}
