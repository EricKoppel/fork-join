package com.erkoppel.fjframework.forkjoin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Phaser;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import com.erkoppel.fjframework.forkjoin.AbstractForkJoinTask.TerminalForkJoinTask;
import com.erkoppel.fjframework.forkjoin.interfaces.ForkJoinThreadFactory;
import com.erkoppel.fjframework.forkjoin.interfaces.ThreadPicker;
import com.erkoppel.fjframework.forkjoin.util.RoundRobinThreadPicker;

public class ForkJoinExecutorService extends AbstractExecutorService {

	private List<ForkJoinThread> threads = new ArrayList<ForkJoinThread>();
	private ThreadPicker<ForkJoinThread> threadPicker;
	private AtomicBoolean shutdown = new AtomicBoolean(false);
	private AtomicInteger threadsStarted = new AtomicInteger();
	private Phaser stopLatch = new Phaser();
	private ReentrantLock moreWorkLock = new ReentrantLock();
	private Condition moreWork = moreWorkLock.newCondition();
	private AtomicBoolean enabledStatistics;

	public ForkJoinExecutorService(ForkJoinThreadFactory factory) {
		enabledStatistics = new AtomicBoolean(Boolean.parseBoolean(System.getProperty("forkjoin.enableStatistics", "false")));
		int numThreads = Runtime.getRuntime().availableProcessors();
		threadPicker = new RoundRobinThreadPicker<ForkJoinThread>(threads);

		for (int i = 0; i < numThreads; i++) {
			ForkJoinThread t = factory.newThread(this);
			t.setName("forkjoin-thread-" + i);
			threads.add(t);
		}
	}

	public ForkJoinExecutorService() {
		this(new WorkStealingThreadFactory());
	}

	@Override
	public void execute(Runnable command) {
		if (!shutdown.get()) {
			threadPicker.nextThread().fork(new AbstractForkJoinRunnable() {
				protected void solve() {
					command.run();
				}
			});
		} else {
			throw new RejectedExecutionException(getClass().getCanonicalName() + " is shutting down!");
		}
	}

	
	/**
	 * Executes the given {@link AbstractForkJoinTask} and returns the result.
	 * 
	 * @param command the {@link AbstractForkJoinTask} to be executed
	 * @return computed result of the {@link AbstractForkJoinTask}
	 * @throws RejectedExecutionException if the task is submitted after {@link #shutdown()} or {@link #shutdownNow()} has been called.
	 */
	public <T> T invoke(AbstractForkJoinTask<T> command) {
		if (!shutdown.get()) {
			return threadPicker.nextThread().fork(new TerminalForkJoinTask<T>(command)).join();
		} else {
			throw new RejectedExecutionException(getClass().getCanonicalName() + " is shutting down!");
		}
	}

	@Override
	public void shutdown() {
		shutdown.set(true);
		threads.forEach(ForkJoinThread::shutdown);
	}

	@Override
	public List<Runnable> shutdownNow() {
		shutdown.set(true);
		return threads.stream().map(ForkJoinThread::shutdownNow).flatMap(List::stream).collect(Collectors.toList());
	}

	@Override
	public boolean isShutdown() {
		return shutdown.get();
	}

	@Override
	public boolean isTerminated() {
		return stopLatch.isTerminated();
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		try {
			stopLatch.awaitAdvanceInterruptibly(0, timeout, unit);
		} catch (TimeoutException e) {
			e.printStackTrace();
		}
		return stopLatch.isTerminated();
	}

	
	/**
	 * Blocks indefinitely until all tasks have completed execution after a shutdown request.
	 */
	public void awaitTermination() {
		stopLatch.awaitAdvance(0);
	}

	protected List<ForkJoinThread> getThreads() {
		return threads;
	}

	protected Phaser getStopLatch() {
		return stopLatch;
	}

	
	/**
	 * Put thread into waiting state when there is no work to do and nothing to steal.
	 * When more work is available, the thread can be notified by calling signalMoreWork()
	 * 
	 * @throws InterruptedException if the thread is interrupted.
	 * @see #signalMoreWork()
	 */
	public void awaitMoreWork() throws InterruptedException {
		moreWorkLock.lock();
		try {
			moreWork.await();
		} finally {
			moreWorkLock.unlock();
		}
	}

	/**
	 * Signals that a task has been forked onto a work queue.
	 * Waiting threads are woken up. If there are non-started threads,
	 * then one is chosen to start.
	 * 
	 * @see #awaitMoreWork()
	 */
	public void signalMoreWork() {
		if (threadsStarted.get() < threads.size()) {
			threads.get(threadsStarted.getAndIncrement()).start();
		} else {
			moreWorkLock.lock();
			try {
				moreWork.signalAll();
			} finally {
				moreWorkLock.unlock();
			}
		}
	}

	public boolean isStatisticsEnabled() {
		return enabledStatistics.get();
	}
}
