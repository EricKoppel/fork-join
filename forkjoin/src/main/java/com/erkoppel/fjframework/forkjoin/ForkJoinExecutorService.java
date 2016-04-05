package com.erkoppel.fjframework.forkjoin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class ForkJoinExecutorService extends AbstractExecutorService {

	private List<ForkJoinThread> threads = new ArrayList<ForkJoinThread>();
	private Random rnd = new Random();
	private AtomicBoolean shutdown = new AtomicBoolean(false);
	private AtomicBoolean terminated = new AtomicBoolean(false);
	private CountDownLatch stopLatch;

	public ForkJoinExecutorService(ForkJoinThreadFactory factory) {
		int numThreads = Runtime.getRuntime().availableProcessors();
		stopLatch = new CountDownLatch(numThreads);
		for (int i = 0; i < numThreads; i++) {
			ForkJoinThread t = factory.newThread(this);
			t.setName("forkjoin-thread-" + i);
			t.start();
			threads.add(t);
		}
	}

	public ForkJoinExecutorService() {
		this(new WorkStealingThreadFactory());
	}

	@Override
	public void execute(Runnable command) {
		if (!shutdown.get()) {
			ForkJoinThread t = threads.get(rnd.nextInt(threads.size()));
			t.fork(new AbstractForkJoinRunnable() {protected void solve() {command.run();}});
		} else {
			throw new CancellationException(getClass().getCanonicalName() + " is shutting down!");
		}
	}

	public <T> T invoke(AbstractForkJoinTask<T> command) throws InterruptedException, ExecutionException {
		if (!shutdown.get()) {
			ForkJoinThread t = threads.get(rnd.nextInt(threads.size()));
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
}
