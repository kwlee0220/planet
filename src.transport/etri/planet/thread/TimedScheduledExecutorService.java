package etri.planet.thread;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;


/**
 * 
 * @author Kang-Woo Lee
 */
public final class TimedScheduledExecutorService extends AbstractTimedExecutor
											implements ScheduledExecutorService {
	private static final Logger s_logger = Logger.getLogger("PLANET.SCHEDULER");
	
	private final TimedThread[] m_threads;
	private final ScheduledExecutorService m_executor;
	private final int m_threadCount;
	private volatile int m_pos;	// guarded by "this"
	
	private volatile TimedThreadInspector m_checker;
	private volatile long m_timeout;
	
	class Factory implements ThreadFactory {
		String m_name;
		
		Factory(String name) {
			m_name = name;
		}
		
		public Thread newThread(Runnable command) {
			synchronized ( TimedScheduledExecutorService.this ) {
				for ( int i =1; i <= m_threadCount; ++i ) {
					int idx = (m_pos + i)%m_threadCount;
					
					TimedThread thread = m_threads[idx];
					if ( thread == null || !thread.isAlive() ) {
						m_threads[idx] = new TimedThread(command, m_name + "-worker-" + (idx+1));
						m_pos = idx;
						
						return m_threads[idx];
					}
				}
			}
			
			s_logger.fatal("Should be here");
			throw new RuntimeException("Should be here");
		}
	}
	
	public TimedScheduledExecutorService(String name, int threadCount, long timeout) {
		m_pos = -1;
		m_threads = new TimedThread[threadCount];
		m_executor = Executors.newScheduledThreadPool(threadCount, new Factory(name));
		m_threadCount = threadCount;
		m_timeout = timeout;
		
		if ( m_timeout > 0 ) {
			m_checker = new TimedThreadInspector();
			m_checker.start();
		}
	}

	@Override
	public int getThreadCount() {
		return m_threadCount;
	}

	@Override
	public int getActiveThreadCount() {
		return ((ThreadPoolExecutor)m_executor).getActiveCount();
	}

	@Override
	public int getMaxThreadCount() {
		return ((ThreadPoolExecutor)m_executor).getMaximumPoolSize();
	}
	
	public long getThreadTimeout() {
		return m_timeout;
	}
	
	public synchronized void setThreadTimeout(long timeout) {
		m_checker.m_finished = true;
		m_checker.interrupt();
		
		if ( (m_timeout = timeout) > 0 ) {
			m_checker = new TimedThreadInspector();
			m_checker.start();
		}
	}

	@Override
	protected void threadDestroyed(TimedThread thread) {
	}

	public void execute(Runnable task) {
		m_executor.execute(new TimedRunnable(this, task));
	}
	
	public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
		return m_executor.schedule(new TimedRunnable(this, task), delay, unit);
	}
	
	public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
		return m_executor.schedule(new TimedCallable<V>(this, callable), delay, unit);
	}

	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long initialDelay,
													long delay, TimeUnit unit) {
		return m_executor.scheduleWithFixedDelay(new TimedRunnable(this, task), initialDelay, delay, unit);
	}

	public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay,
													long period, TimeUnit unit) {
		return m_executor.scheduleAtFixedRate(new TimedRunnable(this, command), initialDelay, period, unit);
	}

	public Future<?> submit(Runnable task) {
		return m_executor.submit(new TimedRunnable(this, task));
	}

	public <T> Future<T> submit(Runnable task, T result) {
		return m_executor.submit(new TimedRunnable(this, task), result);
	}
	
	public <T> Future<T> submit(Callable<T> task) {
		return m_executor.submit(new TimedCallable<T>(this, task));
	}
	
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
//	public <T> List<Future<T>> invokeAll(Collection<Callable<T>> tasks) throws InterruptedException {
		List<Callable<T>> taskList = new ArrayList<Callable<T>>();
		for ( Callable<T> callable: tasks ) {
			taskList.add(new TimedCallable<T>(this, callable));
		}
		
		return m_executor.invokeAll(taskList);
	}

	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
//	public <T> List<Future<T>> invokeAll(Collection<Callable<T>> tasks, long timeout, TimeUnit unit)
		throws InterruptedException {
		List<Callable<T>> taskList = new ArrayList<Callable<T>>();
		for ( Callable<T> callable: tasks ) {
			taskList.add(new TimedCallable<T>(this, callable));
		}
		
		return m_executor.invokeAll(taskList, timeout, unit);
	}

	public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
//	public <T> T invokeAny(Collection<Callable<T>> tasks) throws InterruptedException, ExecutionException {
		List<Callable<T>> taskList = new ArrayList<Callable<T>>();
		for ( Callable<T> callable: tasks ) {
			taskList.add(new TimedCallable<T>(this, callable));
		}
		
		return m_executor.invokeAny(taskList);
	}

	public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
//	public <T> T invokeAny(Collection<Callable<T>> tasks, long timeout, TimeUnit unit)
		throws InterruptedException, ExecutionException, TimeoutException {
		List<Callable<T>> taskList = new ArrayList<Callable<T>>();
		for ( Callable<T> callable: tasks ) {
			taskList.add(new TimedCallable<T>(this, callable));
		}
		
		return m_executor.invokeAny(taskList, timeout, unit);
	}

	public boolean isShutdown() {
		return m_executor.isShutdown();
	}

	public boolean isTerminated() {
		return m_executor.isTerminated();
	}

	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		return m_executor.awaitTermination(timeout, unit);
	}

	public void shutdown() {
		m_executor.shutdown();
		
		m_checker.m_finished = true;
		m_checker.interrupt();
	}

	public List<Runnable> shutdownNow() {
		List<Runnable> tasks = m_executor.shutdownNow();
		
		m_checker.m_finished = true;
		m_checker.interrupt();
		
		return tasks;
	}
	
	class TimedThreadInspector extends Thread {
		volatile boolean m_finished = false;
		
		TimedThreadInspector() {
			super("timed:scan");
		}
		
		public void run() {
			synchronized ( this ) {
				try {
					this.wait(m_timeout);
				}
				catch ( InterruptedException ignored ) { }
			}
			
			while ( !m_finished ) {
				scan();
				
				synchronized ( this ) {
					try {
						this.wait(m_timeout);
					}
					catch ( InterruptedException ignored ) { }
				}
			}
			
			if ( s_logger.isInfoEnabled() ) {
				s_logger.info("finished " + getClass().getSimpleName() + "=" + this);
			}
		}
	}
	
	private synchronized void scan() {
		long current = TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
		
		if ( s_logger.isInfoEnabled() ) {
			s_logger.info(String.format("started: %s[count=%d, active=%d] scanning threads",
								getClass().getSimpleName(), getThreadCount(), getActiveThreadCount()));
		}
		
		for ( int i =0; i < m_threadCount; ++i ) {
			TimedThread thread = m_threads[i];
			
			if ( thread != null && thread.isAlive() ) {
				TimedTask task = thread.getCurrentTask();
				
				if ( task != null ) {
					if ( (current - task.m_startedTime) >= m_timeout ) {
						task.m_interrupted = true;
						thread.interrupt();
						
						String desc = thread.getDescription();
						s_logger.warn(String.format(
										"interrupted: long running task[%s](%.02fsecs), desc=%s",
										thread.getName(), (current - task.m_startedTime)/1000.0,
										(desc != null) ? desc : "unspecified"));
					}
				}
			}
		}
		
		if ( s_logger.isInfoEnabled() ) {
			s_logger.info(String.format("finished: %s[count=%d, active=%d] scanning threads",
					getClass().getSimpleName(), getThreadCount(), getActiveThreadCount()));
		}
	}
}
