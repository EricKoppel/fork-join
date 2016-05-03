package com.erkoppel.fjframework.forkjoin;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public abstract class AbstractForkJoinTask<T> {
	private AtomicBoolean done = new AtomicBoolean(false);

	
	/**
	 * A {@link ForkJoinThread} that receives a {@link AbstractForkJoinTask} to execute
	 * will run this method.
	 */
	protected abstract void run();

	protected abstract void setResult(T result);

	protected abstract T getResult();

	protected boolean isDone() {
		return done.get();
	}

	protected void setDone() {
		done.set(true);
	}

	
	/**
	 * Arrange for the task to be asynchronously executed.
	 * 
	 * @return the task being forked
	 */
	public AbstractForkJoinTask<T> fork() {
		if (Thread.currentThread() instanceof ForkJoinThread) {
			return ((ForkJoinThread) Thread.currentThread()).fork(this);
		} else {
			throw new IllegalStateException("Cannot fork. Thread is not of type ForkJoinThread!");
		}
	}

	
	/**
	 * Returns the result of the forkjoin task.
	 * 
	 * @return the computed result
	 */
	public T join() {
		if (Thread.currentThread() instanceof ForkJoinThread) {
			return ((ForkJoinThread) Thread.currentThread()).join(this);
		} else {
			throw new IllegalStateException("Cannot do join. Thread is not of type ForkJoinThread!");
		}
	}

	/**
	 * A forkjoin task that uses {@link ReentrantLock} and {@link Condition} 
	 * in order block submitting thread until task is done.
	 *
	 * @param <T> the result type
	 */
	static final class TerminalForkJoinTask<T> extends AbstractForkJoinTask<T> {
		private final ReentrantLock doneLock = new ReentrantLock();
		private final Condition done = doneLock.newCondition();

		private AbstractForkJoinTask<T> task;

		public TerminalForkJoinTask(AbstractForkJoinTask<T> runnable) {
			this.task = runnable;
		}

		@Override
		public T join() {
			doneLock.lock();
			try {
				while (!isDone()) {
					done.await();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				doneLock.unlock();
			}

			return task.getResult();
		}

		@Override
		protected void run() {
			doneLock.lock();
			try {
				task.run();
				setDone();
				done.signalAll();
			} finally {
				doneLock.unlock();
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