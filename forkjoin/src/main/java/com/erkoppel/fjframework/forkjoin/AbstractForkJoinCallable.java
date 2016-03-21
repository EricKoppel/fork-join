package com.erkoppel.fjframework.forkjoin;


public abstract class AbstractForkJoinCallable<T> extends AbstractForkJoinTask<T> {

	private T result;

	protected abstract T solve();
	
	@Override
	protected final void run() {
		setResult(solve());
		setDone();
	}

	@Override
	protected final void setResult(T result) {
		this.result = result;
	}

	@Override
	protected final T getResult() {
		return result;
	}
}