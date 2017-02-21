package etri.planet.thread;


/**
 * 
 * @author Kang-Woo Lee
 */
public class TimedThread extends Thread {
	private volatile TimedTask m_worker;
	private volatile String m_desc;
	
	public TimedThread(Runnable command, String name) {
		super(command, name);
	}
	
	public static TimedThread getCurrentThread() {
		Thread current = Thread.currentThread();
		return ( current instanceof TimedThread ) ? (TimedThread)current : null;
	}
	
	public static void setTaskDescription(String desc) {
		TimedThread current = getCurrentThread();
		if ( current != null ) {
			current.setDescription(desc);
		}
	}
	
	public static void resetTimer() {
		TimedThread current = getCurrentThread();
		if ( current != null && current.m_worker != null ) {
			current.m_worker.m_startedTime = System.currentTimeMillis();
		}
	}
	
	public TimedTask getCurrentTask() {
		return m_worker;
	}
	
	public void setCurrentTask(TimedTask task) {
		m_worker = task;
	}
	
	public String getDescription() {
		return m_desc;
	}
	
	public void setDescription(String desc) {
		m_desc = desc;
	}
}
