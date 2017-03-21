package etri.planet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * @author Kang-Woo Lee
 */
public class TransportLoggers {
	private TransportLoggers() { }

	public static final Logger TRANSPORT = LoggerFactory.getLogger("PLANET.TRANS");
	public static final Logger CONN = LoggerFactory.getLogger("PLANET.TRANS.CONN");
	public static final Logger CHANNEL = LoggerFactory.getLogger("PLANET.TRANS.CH");
	public static final Logger MSG = LoggerFactory.getLogger("PLANET.TRANS.MSG");
	public static final Logger IO = LoggerFactory.getLogger("PLANET.TRANS.IO");
	public static final Logger SELECTOR = LoggerFactory.getLogger("PLANET.TRANS.IO.SLCTR");
	public static final Logger ACCEPTOR = LoggerFactory.getLogger("PLANET.TRANS.IO.ACCPT");
	public static final Logger IDLECHKR = LoggerFactory.getLogger("PLANET.TRANS.IO.IDLECHKR");
}
