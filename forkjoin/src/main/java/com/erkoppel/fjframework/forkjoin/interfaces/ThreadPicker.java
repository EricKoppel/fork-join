package com.erkoppel.fjframework.forkjoin.interfaces;

public interface ThreadPicker<T extends Thread> {
	public T nextThread();
}
