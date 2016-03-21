package com.erkoppel.fjframework.forkjoin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.erkoppel.fjframework.forkjoin.AbstractForkJoinTask.TerminalForkJoinTask;

public class WorkShruggingThread extends ForkJoinThread {
	private BlockingQueue<AbstractForkJoinTask<?>> tasks = new LinkedBlockingQueue<AbstractForkJoinTask<?>>();

	public WorkShruggingThread(ForkJoinExecutorService service) {
		super(service);
	}

	@Override
	public void run() {
		try {
			while (!interrupted()) {
				AbstractForkJoinTask<?> task = tasks.take();

				if (!task.isDone()) {
					task.run();
				}
			}
		} catch (InterruptedException e) {
		} finally {
			forkJoinPool.getCountDownLatch().countDown();
		}
	}

	@Override
	public <T> T submit(AbstractForkJoinTask<T> task) {
		return fork(new TerminalForkJoinTask<T>(task)).join();
	}

	@Override
	public <T> AbstractForkJoinTask<T> fork(AbstractForkJoinTask<T> task) {
		WorkShruggingThread t = (WorkShruggingThread) forkJoinPool.getThreads().get(new Random().nextInt(forkJoinPool.getThreads().size()));
		return t.transfer(task);
	}

	@Override
	public void join(AbstractForkJoinTask<?> task) {
		while (!task.isDone()) {
			AbstractForkJoinTask<?> t = tasks.poll();
			if (t != null && !t.isDone()) {
				t.run();

				if (t == task) {
					return;
				}
			}
		}
	}

	public <T> AbstractForkJoinTask<T> transfer(AbstractForkJoinTask<T> task) {
		tasks.offer(task);
		return task;
	}

	@Override
	public List<AbstractForkJoinTask<?>> drainTasks() {
		List<AbstractForkJoinTask<?>> l = new ArrayList<AbstractForkJoinTask<?>>();
		tasks.drainTo(l);
		return l;
	}
}
