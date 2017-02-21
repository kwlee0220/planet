package etri.planet;

import org.apache.log4j.Logger;


/**
 * 
 * @author Kang-Woo Lee
 */
public class TransportLoggers {
	private TransportLoggers() { }

	public static final Logger TRANSPORT = Logger.getLogger("PLANET.TRANS");
	public static final Logger CONN = Logger.getLogger("PLANET.TRANS.CONN");
	public static final Logger CHANNEL = Logger.getLogger("PLANET.TRANS.CH");
	public static final Logger MSG = Logger.getLogger("PLANET.TRANS.MSG");
	public static final Logger IO = Logger.getLogger("PLANET.TRANS.IO");
	public static final Logger SELECTOR = Logger.getLogger("PLANET.TRANS.IO.SLCTR");
	public static final Logger ACCEPTOR = Logger.getLogger("PLANET.TRANS.IO.ACCPT");
	public static final Logger IDLECHKR = Logger.getLogger("PLANET.TRANS.IO.IDLECHKR");
}
