package com.erkoppel.fjframework.forkjoin;

public abstract class AbstractForkJoinRunnable extends AbstractForkJoinTask<Void> {
	protected abstract void solve();

	@Override
	public final void run() {
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
