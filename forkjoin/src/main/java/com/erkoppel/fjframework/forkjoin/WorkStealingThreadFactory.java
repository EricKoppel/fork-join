package com.erkoppel.fjframework.forkjoin;


public class WorkStealingThreadFactory implements ForkJoinThreadFactory {
	@Override
	public ForkJoinThread newThread(ForkJoinExecutorService service) {
		return new WorkStealingThread(service);
	}
}
