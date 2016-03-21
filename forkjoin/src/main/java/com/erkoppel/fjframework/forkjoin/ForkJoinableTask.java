package com.erkoppel.fjframework.forkjoin;

public interface ForkJoinableTask<T> {

	void fork();

	T join();

}
