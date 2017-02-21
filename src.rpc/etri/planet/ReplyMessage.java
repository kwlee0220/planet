package etri.planet;

import java.io.IOException;
import java.lang.reflect.Method;

import etri.planet.serializer.TypeCode;


public class ReplyMessage extends ReturnMessage {
	private Method m_method;
	private Object m_result;
	
	public ReplyMessage(int reqId, Method method, Object result) {
		super(new PlanetHeader(MSG_REPLY, reqId));
		
		m_method = method;
		m_result = result;
	}
	
	public ReplyMessage(PlanetHeader header) {
		super(header);
	}
	
	public Object getResult() {
		return m_result;
	}
	
	public void setResult(Object result) {
		m_result = result;
	}

	public void readPayload(PlanetReader reader) throws IOException {
		m_result = reader.readObject(null);
	}

	public void writePayload(PlanetWriter writer) throws IOException {
		if ( m_method.getReturnType() != void.class ) {
			writer.writeObjectWithCode(m_result);
		}
		else {
			writer.writeByte(TypeCode.VOID);
		}
	}
	
	public String toString() {
		if ( getReadError() == null ) {
			return "Reply[result=" + m_result + "]";
		}
		else {
			return "Reply[corrupted:cause=" + getReadError() + "]";
		}
	}
}