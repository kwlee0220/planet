package etri.planet.transport;

import java.nio.ByteBuffer;


/**
 * 
 * @author Kang-Woo Lee
 */
public class TransportUtils {
	public static void copyTo(ByteBuffer src, ByteBuffer tar, int length) {
		if ( tar.remaining() >= src.remaining() ) {
			tar.put(src);
		}
		else {
			int orgLimit = src.limit();
			src.limit(src.position() + length);
			tar.put(src);
			src.limit(orgLimit);
		}
	}
}
