package etri.planet.servant;

import java.util.ArrayList;
import java.util.List;


/**
 * 
 * @author Kang-Woo Lee
 */
public class ServantUtils {
	private ServantUtils() { }
	
	public static List<String> parseServantPath(String path) {
		List<String> nodeList = new ArrayList<String>();
		ServantPathParser parser = new ServantPathParser(path);
		
		String node;
		while ( (node = parser.parseNext()) != null ) {
			nodeList.add(node);
		}
		
		return nodeList;
	}
	
	public static String toPathString(List<String> nodeList) {
		return toPathString(nodeList, nodeList.size());
	}
	
	public static String toPathString(List<String> nodeList, int depth) {
		if ( nodeList == null ) {
			throw new NullPointerException("path was null");
		}
		else if ( nodeList.size() < depth ) {
			throw new IllegalArgumentException("path=" + toPathString(nodeList)
												+ ", depth=" + depth);
		}
		else if ( nodeList.size() == 0 ) {
			return "/";
		}
		
		StringBuilder builder = new StringBuilder();
		for ( int i =0; i < depth; ++i ) {
			builder.append('/').append(nodeList.get(i));
		}
		
		return builder.toString();
	}
}
