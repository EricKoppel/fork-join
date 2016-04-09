package com.erkoppel.fjframework.forkjoin;

import com.erkoppel.fjframework.forkjoin.interfaces.ForkJoinThreadFactory;

public class WorkShruggingThreadFactory implements ForkJoinThreadFactory {
	@Override
	public ForkJoinThread newThread(ForkJoinExecutorService service) {
		return new WorkShruggingThread(service);
	}
}
