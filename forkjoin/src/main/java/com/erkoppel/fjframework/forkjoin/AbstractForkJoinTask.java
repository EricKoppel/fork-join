package com.erkoppel.fjframework.forkjoin;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractForkJoinTask<T> implements ForkJoinableTask<T> {
	private AtomicBoolean done = new AtomicBoolean(false);

	protected abstract void run();
	protected abstract void setResult(T result);
	protected abstract T getResult();
	
	protected boolean isDone() {
		return done.get();
	}

	protected void setDone() {
		done.set(true);
	}

	@Override
	public void fork() {
		if (Thread.currentThread() instanceof ForkJoinThread) {
			((ForkJoinThread) Thread.currentThread()).fork(this);
		} else {
			throw new IllegalStateException("Cannot fork. Thread is not of type ForkJoinThread!");
		}
	}

	@Override
	public T join() {
		if (isDone()) {
			return getResult();
		}

		if (Thread.currentThread() instanceof ForkJoinThread) {
			((ForkJoinThread) Thread.currentThread()).join(this);
		} else {
			throw new IllegalStateException("Cannot do join. Thread is not of type ForkJoinThread!");
		}

		return getResult();
	}

	static final class TerminalForkJoinTask<T> extends AbstractForkJoinTask<T> {
		private final CountDownLatch done = new CountDownLatch(1);
		private AbstractForkJoinTask<T> task;

		public TerminalForkJoinTask(AbstractForkJoinTask<T> runnable) {
			this.task = runnable;
		}

		@Override
		public T join() {
			while (!isDone()) {
				try {
					done.await();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}

			return task.getResult();
		}

		@Override
		protected void run() {
			try {
				task.run();
			} finally {
				setDone();
				done.countDown();
			}
		}

		@Override
		protected void setResult(T result) {
			task.setResult(result);
		}

		@Override
		protected T getResult() {
			return task.getResult();
		}
	}
}