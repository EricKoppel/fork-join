package com.erkoppel.fjframework.forkjoin;

public interface ForkJoinThreadFactory {

	public ForkJoinThread newThread(ForkJoinExecutorService service);
}
