package com.erkoppel.fjframework.forkjoin.interfaces;

import com.erkoppel.fjframework.forkjoin.ForkJoinExecutorService;
import com.erkoppel.fjframework.forkjoin.ForkJoinThread;

public interface ForkJoinThreadFactory {

	public ForkJoinThread newThread(ForkJoinExecutorService service);
}
