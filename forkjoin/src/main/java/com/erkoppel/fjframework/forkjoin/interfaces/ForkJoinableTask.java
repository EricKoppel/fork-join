package com.erkoppel.fjframework.forkjoin.interfaces;

public interface ForkJoinableTask<T> {

	void fork();

	T join();

}
