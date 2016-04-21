package com.erkoppel.fjframework.forkjoin;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import com.erkoppel.fjframework.forkjoin.util.ThreadStatistics;

public abstract class ForkJoinThread extends Thread {
	protected ForkJoinExecutorService service;
	protected AtomicBoolean shutdown = new AtomicBoolean(false);
	protected AtomicBoolean shutdownNow = new AtomicBoolean(false);
	protected ThreadStatistics<Long> statistics = new ThreadStatistics<Long>(0L, this);

	public ForkJoinThread(ForkJoinExecutorService service) {
		this.service = service;
		statistics.setEnabled(service.isStatisticsEnabled());
	}

	public void shutdown() {
		shutdown.set(true);
		onShutdown();
	}

	public List<Runnable> shutdownNow() {
		shutdownNow.set(true);
		onShutdownNow();
		return drainTasks().stream().map(t -> (Runnable) () -> t.run()).collect(Collectors.toList());
	}

	public ThreadStatistics<Long> getThreadStatistics() {
		return statistics;
	}

	public abstract <T> AbstractForkJoinTask<T> fork(AbstractForkJoinTask<T> task);

	public abstract void join(AbstractForkJoinTask<?> task);

	protected abstract void onShutdown();

	protected abstract void onShutdownNow();

	protected abstract List<AbstractForkJoinTask<?>> drainTasks();
}
