package com.erkoppel.fjframework.forkjoin;

import java.util.List;


public abstract class ForkJoinThread extends Thread {
	protected ForkJoinExecutorService forkJoinPool;

	public ForkJoinThread(ForkJoinExecutorService pool) {
		this.forkJoinPool = pool;
	}

	public abstract void join(AbstractForkJoinTask<?> task);
	public abstract <T> AbstractForkJoinTask<T> fork(AbstractForkJoinTask<T> task);
	public abstract <T> T submit(AbstractForkJoinTask<T> task);
	public abstract List<AbstractForkJoinTask<?>> drainTasks();
}
