package etri.planet.thread;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;


/**
 * 
 * @author Kang-Woo Lee
 */
public final class TimedExecutorService extends AbstractTimedExecutor
									implements ExecutorService {
	private static final Logger s_logger = Logger.getLogger("PLANET.EXECUTOR");
	
	private final CopyOnWriteArrayList<TimedThread> m_threadList;
	private final ThreadPoolExecutor m_executor;
	
	private volatile TimedThreadInspector m_checker;
	private volatile long m_timeout;
	
	class Factory implements ThreadFactory {
		String m_prefix;
		int m_counter;
		
		Factory(String prefix) {
			m_prefix = prefix;
			m_counter = 0;
		}
		
		public Thread newThread(Runnable command) {
			InterceptingTask task = new InterceptingTask(TimedExecutorService.this, command);
			TimedThread thread = new TimedThread(task, m_prefix + ":" + (m_counter++));
			task.setTimeThread(thread);
			
			m_threadList.add(thread);
			
			if ( s_logger.isDebugEnabled() ) {
				s_logger.debug("created: thread=" + thread);
			}
			
			return thread;
		}
	}
	
	public TimedExecutorService(String name, int coreThreads, long keepAliveSeconds, long timeout) {
		m_threadList = new CopyOnWriteArrayList<TimedThread>();
		m_executor = new ThreadPoolExecutor(coreThreads, Integer.MAX_VALUE, keepAliveSeconds,
											TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
											new Factory(name));
		m_timeout = timeout;
		if ( m_timeout > 0 ) {
			m_checker = new TimedThreadInspector();
			m_checker.start();
		}
		
		if ( s_logger.isInfoEnabled() ) {
			s_logger.info(String.format("create %s[core=%d max=%d, keepalive=%d, timeout=%d]",
							getClass().getSimpleName(), coreThreads, -1, keepAliveSeconds, timeout));	
		}
	}
	
	public TimedExecutorService(String name, int coreThreads, int maxThreads, long keepAliveSeconds,
								long timeout) {
		m_threadList = new CopyOnWriteArrayList<TimedThread>();
		m_executor = new ThreadPoolExecutor(coreThreads, maxThreads, keepAliveSeconds,
											TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
											new Factory(name));
		m_timeout = timeout;
		if ( m_timeout > 0 ) {
			m_checker = new TimedThreadInspector();
			m_checker.start();
		}
		
		if ( s_logger.isInfoEnabled() ) {
			s_logger.info(String.format("create %s[core=%d max=%d, keepalive=%d, timeout=%d]",
										getClass().getSimpleName(), coreThreads, maxThreads,
										keepAliveSeconds, timeout));	
		}
	}
	
	@Override
	public int getThreadCount() {
		return m_executor.getPoolSize();
	}

	@Override
	public int getActiveThreadCount() {
		return m_executor.getActiveCount();
	}

	@Override
	public int getMaxThreadCount() {
		return m_executor.getMaximumPoolSize();
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
		m_threadList.remove(thread);
		
		if ( s_logger.isDebugEnabled() ) {
			s_logger.debug("destroyed: thread=" + thread);
		}
	}

	public void execute(Runnable task) {
		m_executor.execute(new TimedRunnable(this, task));
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
		long currentMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
		
		if ( s_logger.isInfoEnabled() ) {
			s_logger.info(String.format("started: %s[count=%d, active=%d] scanning threads",
										getClass().getSimpleName(), getThreadCount(),
										getActiveThreadCount()));
		}
		
		for ( TimedThread thread: m_threadList ) {
			if ( thread != null && thread.isAlive() ) {
				TimedTask task = thread.getCurrentTask();
				
				if ( task != null ) {
					if ( (currentMillis - task.m_startedTime) >= m_timeout ) {
						task.m_interrupted = true;
						thread.interrupt();
						
						String desc = thread.getDescription();
						s_logger.warn(String.format(
										"interrupted: long running task[%s](%.02fsecs), desc=%s",
										thread.getName(), (currentMillis - task.m_startedTime)/1000.0,
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
