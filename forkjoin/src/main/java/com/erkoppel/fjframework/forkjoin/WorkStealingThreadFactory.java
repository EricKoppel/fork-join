package com.erkoppel.fjframework.forkjoin;

import com.erkoppel.fjframework.forkjoin.interfaces.ForkJoinThreadFactory;

public class WorkStealingThreadFactory implements ForkJoinThreadFactory {
	@Override
	public ForkJoinThread newThread(ForkJoinExecutorService service) {
		return new WorkStealingThread(service);
	}
}
