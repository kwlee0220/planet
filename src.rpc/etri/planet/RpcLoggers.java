package etri.planet;

import org.apache.log4j.Logger;


/**
 * 
 * @author Kang-Woo Lee
 */
public class RpcLoggers {
	private RpcLoggers() { }
	
	public static final Logger SERVANT = Logger.getLogger("PLANET.RPC.SERVANT");
	public static final Logger PROXY = Logger.getLogger("PLANET.RPC.PROXY");
	public static final Logger SESSION = Logger.getLogger("PLANET.RPC.SESSION");
	public static final Logger MSG_HANDLE = Logger.getLogger("PLANET.RPC.MSG_HANDLE");
	public static final Logger CALL = Logger.getLogger("PLANET.RPC.CALL");
	public static final Logger NOTI = Logger.getLogger("PLANET.RPC.NOTI");
	public static final Logger CODEC = Logger.getLogger("PLANET.RPC.CODEC");
	public static final Logger STREAM = Logger.getLogger("PLANET.RPC.STREAM");
	public static final Logger STRM_WORKER = Logger.getLogger("PLANET.RPC.STREAM.WORK");
	public static final Logger CLSLDR = Logger.getLogger("PLANET.RPC.CLASS");
}
