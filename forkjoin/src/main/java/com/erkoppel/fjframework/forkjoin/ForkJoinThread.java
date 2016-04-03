package com.erkoppel.fjframework.forkjoin;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public abstract class ForkJoinThread extends Thread {
	protected ForkJoinExecutorService service;
	protected AtomicBoolean shutdown = new AtomicBoolean(false);
	protected AtomicBoolean shutdownNow = new AtomicBoolean(false);

	public ForkJoinThread(ForkJoinExecutorService service) {
		this.service = service;
	}

	public void shutdown() {
		shutdown.set(true);
		onShutdown();
	}

	public List<Runnable> shutdownNow() {
		shutdownNow.set(true);
		onShutdownNow();
		return drainTasks().stream().map(t -> {
			Runnable r = () -> t.run();
			return r;
		}).collect(Collectors.toList());
	}

	public abstract <T> T submit(AbstractForkJoinTask<T> task);
	
	public abstract <T> AbstractForkJoinTask<T> fork(AbstractForkJoinTask<T> task);
	
	public abstract void join(AbstractForkJoinTask<?> task);

	protected abstract void onShutdown();

	protected abstract void onShutdownNow();
	
	protected abstract List<AbstractForkJoinTask<?>> drainTasks();
}
