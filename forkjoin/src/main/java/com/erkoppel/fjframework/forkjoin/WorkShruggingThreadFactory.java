package com.erkoppel.fjframework.forkjoin;


public class WorkShruggingThreadFactory implements ForkJoinThreadFactory {
	@Override
	public ForkJoinThread newThread(ForkJoinExecutorService service) {
		return new WorkShruggingThread(service);
	}
}
