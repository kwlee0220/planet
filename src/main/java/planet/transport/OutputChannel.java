package planet.transport;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public interface OutputChannel {
	public interface Listener {
		public void beforeClosedByOther();
	}
	
	/**
	 * 입력 채널의 식별자를 반환한다.
	 * 
	 * @return	입력 채널 식별자.
	 */
	public int getId();
	
	/**
	 * 출력 채널을 닫는다.
	 * <p>
	 * 닫힌 후의 출력 채널을 통한 데이타 쓰기 연산을 수행하는 경우 {@link java.io.EOFException} 예외가
	 * 발생된다.
	 * 만일 이미 닫힌 출력 채널에 대해 본 메소드를 호출하는 경우는 호출은 무시된다.
	 * 
	 * @param flush 출력 채널 폐쇄시 버퍼에 남아있던 데이타의 flush 여부. Flush되면 버퍼에 남은
	 * 				데이타는 전송된다.
	 */
	public void close(boolean flush) throws IOException;
	
	public void closeByOther() throws IOException;
	
	/**
	 * 입력 채널이 속한 연결 객체를 반환한다.
	 * 
	 * @return	소속된 연결 객체.
	 */
	public Connection getConnection();
	
	/**
	 * 주어진 바이트 값을 출력 채널에 추가한다.
	 * 
	 * @param v	추가할 바이트 값.
	 * @throws EOFException	출력 채널이 이미 닫혔있는 경우.
	 * @throws IOException	기타 이유로 데이타 쓰기에 실패한 경우.
	 */
	public void write1(byte v) throws EOFException, IOException;
	
	/**
	 * 주어진 short 값을 출력 채널에 추가한다.
	 * <p>
	 * 삽입시 big-endian 인코딩을 사용한다.
	 * 
	 * @param v	추가할 short 값.
	 * @throws EOFException	출력 채널이 이미 닫혔있는 경우.
	 * @throws IOException	기타 이유로 데이타 쓰기에 실패한 경우.
	 */
	public void write2(short v) throws EOFException, IOException;
	
	/**
	 * 주어진 int 값을 출력 채널에 추가한다.
	 * <p>
	 * 삽입시 big-endian 인코딩을 사용한다.
	 * 
	 * @param v	추가할 int 값.
	 * @throws EOFException	출력 채널이 이미 닫혔있는 경우.
	 * @throws IOException	기타 이유로 데이타 쓰기에 실패한 경우.
	 */
	public void write4(int v) throws EOFException, IOException;
	
	/**
	 * 주어진 long 값을 출력 채널에 추가한다.
	 * <p>
	 * 삽입시 big-endian 인코딩을 사용한다.
	 * 
	 * @param v	추가할 long 값.
	 * @throws EOFException	출력 채널이 이미 닫혔있는 경우.
	 * @throws IOException	기타 이유로 데이타 쓰기에 실패한 경우.
	 */
	public void write8(long v) throws EOFException, IOException;
	
	/**
	 * 주어진 길이의 바이트 배열의 지정된 구간을 출력 채널에 추가한다.
	 * 
	 * @param bytes		추가할 바이트 배열 객체.
	 * @param offset	저장될 데이타의 배열 내의 시작 인덱스.
	 * @param length	저장된 데이타의 바이트 수.
	 * @throws EOFException	출력 채널이 이미 닫혔있는 경우.
	 * @throws IOException	기타 이유로 데이타 쓰기에 실패한 경우.
	 */
	public void writen(byte[] bytes, int offset, int length) throws EOFException, IOException;
	
	/**
	 * 주어진 길이의 바이트 배열 전체를 출력 채널에 추가한다.
	 * 
	 * @param bytes		추가할 바이트 배열 객체.
	 * @throws EOFException	출력 채널이 이미 닫혔있는 경우.
	 * @throws IOException	기타 이유로 데이타 쓰기에 실패한 경우.
	 */
	public void writen(byte[] bytes) throws EOFException, IOException;
	
	/**
	 * 주어진 길이의 바이트 버퍼를 출력 채널에 추가한다.
	 * 
	 * @param bytes		추가할 바이트 버퍼 객체.
	 * @throws EOFException	출력 채널이 이미 닫혔있는 경우.
	 * @throws IOException	기타 이유로 데이타 쓰기에 실패한 경우.
	 */
	public void writen(ByteBuffer buffer) throws EOFException, IOException;
	
	/**
	 * 출력 채널에 캐슁된 내용을 네트워크를 통해 전송한다.
	 * 
	 * @throws EOFException	출력 채널이 이미 닫혔있는 경우.
	 * @throws IOException	기타 이유로 데이타 flush에 실패한 경우.
	 */
	public void flush() throws EOFException, IOException;
	
	public void setListener(Listener listener);
}
