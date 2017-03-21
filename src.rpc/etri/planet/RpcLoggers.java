package etri.planet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * @author Kang-Woo Lee
 */
public class RpcLoggers {
	private RpcLoggers() { }
	
	public static final Logger SERVANT = LoggerFactory.getLogger("PLANET.RPC.SERVANT");
	public static final Logger PROXY = LoggerFactory.getLogger("PLANET.RPC.PROXY");
	public static final Logger SESSION = LoggerFactory.getLogger("PLANET.RPC.SESSION");
	public static final Logger MSG_HANDLE = LoggerFactory.getLogger("PLANET.RPC.MSG_HANDLE");
	public static final Logger CALL = LoggerFactory.getLogger("PLANET.RPC.CALL");
	public static final Logger NOTI = LoggerFactory.getLogger("PLANET.RPC.NOTI");
	public static final Logger CODEC = LoggerFactory.getLogger("PLANET.RPC.CODEC");
	public static final Logger STREAM = LoggerFactory.getLogger("PLANET.RPC.STREAM");
	public static final Logger STRM_WORKER = LoggerFactory.getLogger("PLANET.RPC.STREAM.WORK");
	public static final Logger CLSLDR = LoggerFactory.getLogger("PLANET.RPC.CLASS");
}
