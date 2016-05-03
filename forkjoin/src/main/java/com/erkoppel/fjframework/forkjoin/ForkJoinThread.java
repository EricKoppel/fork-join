package com.erkoppel.fjframework.forkjoin;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import com.erkoppel.fjframework.forkjoin.util.ThreadStatistics;

/**
 * {@link ForkJoinThread} extends {@link Thread} by allowing tasks executing on the thread
 * to spawn new tasks by way of {@link #fork(AbstractForkJoinTask)} and to wait on spawned 
 * tasks by way of {@link #join(AbstractForkJoinTask)}. 
 *
 */
public abstract class ForkJoinThread extends Thread {
	protected ForkJoinExecutorService service;
	protected AtomicBoolean shutdown = new AtomicBoolean(false);
	protected AtomicBoolean shutdownNow = new AtomicBoolean(false);
	protected ThreadStatistics<Long> statistics = new ThreadStatistics<Long>(0L, this);

	public ForkJoinThread(ForkJoinExecutorService service) {
		this.service = service;
		statistics.setEnabled(service.isStatisticsEnabled());
	}

	/**
	 * This method should arrange for the task to be asynchronously executed.
	 * 
	 * @param task
	 * @return the task being forked
	 */
	public abstract <T> AbstractForkJoinTask<T> fork(AbstractForkJoinTask<T> task);

	/**
	 * This method should return the result of the computed task.
	 * 
	 * @param task the task whose result is being returned
	 * @return the computed result of task
	 */
	public abstract <T> T join(AbstractForkJoinTask<T> task);
	
	/**
	 * Hook method called by {@link #shutdown()} when {@link ForkJoinExecutorService#shutdown()} is called.
	 */
	protected abstract void onShutdown();

	/**
	 * Hook method called by {@link #shutdownNow()} when {@link ForkJoinExecutorService#shutdownNow()} is called.
	 */
	protected abstract void onShutdownNow();

	protected abstract List<AbstractForkJoinTask<?>> drainTasks();
	
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
}
