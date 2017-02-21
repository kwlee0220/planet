package etri.planet.thread;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
class InterceptingTask implements Runnable {
	private final AbstractTimedExecutor m_executor;
	private volatile TimedThread m_thread;
	private final Runnable m_task;
	
	InterceptingTask(AbstractTimedExecutor executor, Runnable task) {
		m_executor = executor;
		m_task = task;
	}
	
	void setTimeThread(TimedThread thread) {
		m_thread = thread;
	}

	public void run() {
		try {
			m_task.run();
		}
		finally {
			m_executor.threadDestroyed(m_thread);
		}
	}
}
