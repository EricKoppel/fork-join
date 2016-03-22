package com.erkoppel.fjframework.forkjoin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ForkJoinExecutorService extends AbstractExecutorService {

	private List<ForkJoinThread> threads = new ArrayList<ForkJoinThread>();
	private Random rnd = new Random();
	private AtomicBoolean shutdown = new AtomicBoolean(false);
	private AtomicBoolean terminated = new AtomicBoolean(false);
	private CountDownLatch stopLatch;

	public ForkJoinExecutorService(ForkJoinThreadFactory factory) {
		int threads = Runtime.getRuntime().availableProcessors();
		stopLatch = new CountDownLatch(threads);
		for (int i = 0; i < threads; i++) {
			ForkJoinThread t = factory.newThread(this);
			t.setName("forkjoin-thread-" + i);
			this.threads.add(t);
			t.start();
		}
	}

	public ForkJoinExecutorService() {
		this(new WorkStealingThreadFactory());
	}

	@Override
	public void execute(Runnable command) {
		if (!shutdown.get()) {
			ForkJoinThread t = threads.get(rnd.nextInt(threads.size()));
			t.fork(new ForkJoinRunnableWrapper(command));
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
		System.out.println("Shutting down");
		shutdown.set(true);
	}

	@Override
	public List<Runnable> shutdownNow() {
		shutdown.set(true);
		threads.forEach(Thread::interrupt);
		return Collections.emptyList();
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
		shutdown.set(true);
		return stopLatch.await(timeout, unit);
	}

	public List<ForkJoinThread> getThreads() {
		return threads;
	}

	public CountDownLatch getCountDownLatch() {
		return stopLatch;
	}

	private static class ForkJoinRunnableWrapper extends AbstractForkJoinRunnable {
		private Runnable command;

		public ForkJoinRunnableWrapper(Runnable command) {
			this.command = command;
		}

		@Override
		protected void solve() {
			command.run();
		}
	}
}
