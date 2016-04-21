package com.erkoppel.fjframework.forkjoin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import com.erkoppel.fjframework.forkjoin.AbstractForkJoinTask.TerminalForkJoinTask;
import com.erkoppel.fjframework.forkjoin.interfaces.ForkJoinThreadFactory;
import com.erkoppel.fjframework.forkjoin.util.RoundRobinThreadPicker;

public class ForkJoinExecutorService extends AbstractExecutorService {

	private List<ForkJoinThread> threads = new ArrayList<ForkJoinThread>();
	private RoundRobinThreadPicker<ForkJoinThread> threadPicker;
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
			throw new CancellationException(getClass().getCanonicalName() + " is shutting down!");
		}
	}

	public <T> T invoke(AbstractForkJoinTask<T> command) throws InterruptedException, ExecutionException {
		if (!shutdown.get()) {
			return threadPicker.nextThread().fork(new TerminalForkJoinTask<T>(command)).join();
		} else {
			throw new CancellationException(getClass().getCanonicalName() + " is shutting down!");
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

	public void awaitTermination() throws InterruptedException {
		stopLatch.awaitAdvance(0);
	}

	protected List<ForkJoinThread> getThreads() {
		return threads;
	}

	protected Phaser getStopLatch() {
		return stopLatch;
	}

	public void awaitMoreWork() throws InterruptedException {
		moreWorkLock.lock();
		try {
			moreWork.await();
		} finally {
			moreWorkLock.unlock();
		}
	}

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
