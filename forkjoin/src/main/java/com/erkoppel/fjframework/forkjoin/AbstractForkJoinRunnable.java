package com.erkoppel.fjframework.forkjoin;

public abstract class AbstractForkJoinRunnable extends AbstractForkJoinTask<Void> {
	
	/**
	 * When a {@link ForkJoinThread} receives an {@link AbstractForkJoinRunnable} to execute,
	 * the solve method of the implementing class instance is executed.
	 * 
	 * @see #run()
	 */
	protected abstract void solve();

	@Override
	protected final void run() {
		solve();
		setDone();
	}

	@Override
	protected final void setResult(Void result) {
	}

	@Override
	protected final Void getResult() {
		return null;
	}
}
