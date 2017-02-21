package planet.transport;


import java.io.IOException;
import java.nio.ByteBuffer;



/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public interface InputChannel {
	public interface Listener {
		public void onClosed(InputChannel ichannel);
	}
	
	/**
	 * 입력 채널의 식별자를 반환한다.
	 * 
	 * @return	입력 채널 식별자.
	 */
	public int getId();
	
	/**
	 * 입력 채널을 닫는다.
	 * <p>
	 * 닫힌 후의 스트림을 통한 데이타 읽기 연산을 수행하는 경우 {@link java.io.IOException} 예외가
	 * 발생된다.
	 */
	public void close() throws IOException;
	
	/**
	 * 입력 채널이 속한 연결 객체를 반환한다.
	 * 
	 * @return	소속된 연결 객체.
	 */
	public Connection getConnection();
	
	/**
	 * 입력 채널에서 1바이트를 읽어 반환한다.
	 * 
	 * @return	입력 채널에서 읽혀진 1바이트 값.
	 * @throws IOException	입력 채널이 이미 닫혔거나, 데이타 읽기 중 오류가 발생된 경우. 
	 */
	public byte read1() throws IOException;
	
	/**
	 * 입력 채널에서 2바이트를 읽어 short형으로 변환하여 반환한다.
	 * <p>
	 * short형으로 변환시 big-endian 인코딩 방식을 사용한다.
	 * 
	 * @return	입력 채널에서 읽혀진 2바이트 값으로 생성된 short 값.
	 * @throws IOException	입력 채널이 이미 닫혔거나, 데이타 읽기 중 오류가 발생된 경우. 
	 */
	public short read2() throws IOException;
	
	/**
	 * 입력 채널에서 4바이트를 읽어 int형으로 변환하여 반환한다.
	 * <p>
	 * int형으로 변환시 big-endian 인코딩 방식을 사용한다.
	 * 
	 * @return	입력 채널에서 읽혀진 4바이트 값으로 생성된 int 값.
	 * @throws IOException	입력 채널이 이미 닫혔거나, 데이타 읽기 중 오류가 발생된 경우. 
	 */
	public int read4() throws IOException;
	
	/**
	 * 입력 채널에서 8바이트를 읽어 long형으로 변환하여 반환한다.
	 * <p>
	 * long형으로 변환시 big-endian 인코딩 방식을 사용한다.
	 * 
	 * @return	입력 채널에서 읽혀진 8바이트 값으로 생성된 long 값.
	 * @throws IOException	입력 채널이 이미 닫혔거나, 데이타 읽기 중 오류가 발생된 경우. 
	 */
	public long read8() throws IOException;
	
	/**
	 * 입력 채널에서 데이타를 읽어 주어진 바이트 버퍼를 채운다.
	 * 
	 * @param buf	읽은 데이타를 저장할 바이트 버퍼 객체.
	 * @return TODO
	 * @throws IOException	입력 채널이 이미 닫혔거나, 데이타 읽기 중 오류가 발생된 경우. 
	 */
	public int readByteBuffer(ByteBuffer buf) throws IOException;
	
	/**
	 * 입력 채널에서 데이타를 읽어 주어진 바이트 배열에 채운다.
	 * 
	 * @param bytes	읽은 데이타를 저장할 바이트 배열.
	 * @param offset	저장될 시작 배열 인덱스.
	 * @return TODO
	 * @throws IOException	입력 채널이 이미 닫혔거나, 데이타 읽기 중 오류가 발생된 경우.
	 * @throws IllegalArgumentException	<code>offset</code>, 또는 <code>length</code>가 음수인 경우.
	 */
	public int readBytes(byte[] bytes, int offset, int length) throws IOException;
	
	public void setListener(Listener listener);
}
