package com.erkoppel.fjframework.forkjoin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import com.erkoppel.fjframework.forkjoin.interfaces.ForkJoinThreadFactory;
import com.erkoppel.fjframework.forkjoin.util.RoundRobinThreadPicker;

public class ForkJoinExecutorService extends AbstractExecutorService {

	private List<ForkJoinThread> threads = new ArrayList<ForkJoinThread>();
	private RoundRobinThreadPicker<ForkJoinThread> threadPicker;
	private AtomicBoolean shutdown = new AtomicBoolean(false);
	private AtomicBoolean terminated = new AtomicBoolean(false);
	private CountDownLatch stopLatch;
	private ReentrantLock moreWorkLock = new ReentrantLock();
	private Condition moreWork = moreWorkLock.newCondition();

	public ForkJoinExecutorService(ForkJoinThreadFactory factory) {
		int numThreads = Runtime.getRuntime().availableProcessors();
		stopLatch = new CountDownLatch(numThreads);
		for (int i = 0; i < numThreads; i++) {
			ForkJoinThread t = factory.newThread(this);
			t.setName("forkjoin-thread-" + i);
			threads.add(t);
		}
		
		threads.parallelStream().forEach(Thread::start);
		threadPicker = new RoundRobinThreadPicker<ForkJoinThread>(threads);
	}

	public ForkJoinExecutorService() {
		this(new WorkStealingThreadFactory());
	}

	@Override
	public void execute(Runnable command) {
		if (!shutdown.get()) {
			ForkJoinThread t = threadPicker.nextThread();
			t.fork(new AbstractForkJoinRunnable() {
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
			ForkJoinThread t = threadPicker.nextThread();
			return t.submit(command);
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
		return terminated.get();
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		return stopLatch.await(timeout, unit);
	}

	protected List<ForkJoinThread> getThreads() {
		return threads;
	}

	protected CountDownLatch getStopLatch() {
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
		moreWorkLock.lock();
		try {
			moreWork.signalAll();
		} finally {
			moreWorkLock.unlock();
		}
	}
}
