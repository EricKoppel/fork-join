package com.erkoppel.fjframework.forkjoin;


public abstract class AbstractForkJoinCallable<T> extends AbstractForkJoinTask<T> {

	private T result;

	/**
	 * When a {@link ForkJoinThread} receives an {@link AbstractForkJoinCallable} to execute,
	 * the solve method of the implementing class instance is executed.
	 * 
	 * @return T the result of the computation
	 * @see #run()
	 */
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