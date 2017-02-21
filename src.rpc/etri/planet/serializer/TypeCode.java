package etri.planet.serializer;

/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class TypeCode {
	private TypeCode() { }
	
	public static final byte NULL = 0x00;
	
	public static final byte BYTE = 0x01;
	public static final byte SHORT = 0x02;
	public static final byte INT = 0x03;
	public static final byte LONG = 0x04;
	public static final byte FLOAT = 0x05;
	public static final byte DOUBLE = 0x06;
	public static final byte BOOLEAN = 0x07;
	public static final byte STRING = 0x08;
	public static final byte BINARY = 0x09;
	public static final byte ENUM = 0x0A;
	public static final byte EXCEPTION = 0x0B;
	public static final byte TYPE = 0x0C;

	public static final byte REMOTE = 0x0D;
	public static final byte VALUE = 0x0E;
	public static final byte EVENT = 0x0F;
	public static final byte STREAM = 0x10;
	public static final byte SEQUENCE = 0x11;

	public static final byte VOID = 0x12;
	public static final byte REFERENCE = 0x13;

	public static final byte MAP = 0x14;
	public static final byte BYTE_BUFFER = 0x15;
}