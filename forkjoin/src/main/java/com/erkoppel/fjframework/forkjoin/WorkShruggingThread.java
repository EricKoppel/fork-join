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
			while (!(shutdownNow.get() || (shutdown.get() && tasks.isEmpty()))) {
				try {
					AbstractForkJoinTask<?> task = tasks.take();

					if (!task.isDone()) {
						task.run();
					}
				} catch (InterruptedException e) {
				}
			}
		} finally {
			service.getCountDownLatch().countDown();
		}
	}

	@Override
	public <T> T submit(AbstractForkJoinTask<T> task) {
		return fork(new TerminalForkJoinTask<T>(task)).join();
	}

	@Override
	public <T> AbstractForkJoinTask<T> fork(AbstractForkJoinTask<T> task) {
		WorkShruggingThread t = (WorkShruggingThread) service.getThreads().get(new Random().nextInt(service.getThreads().size()));
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
	protected void onShutdown() {
		interrupt();
	}

	@Override
	protected void onShutdownNow() {
		interrupt();
	}

	@Override
	protected List<AbstractForkJoinTask<?>> drainTasks() {
		List<AbstractForkJoinTask<?>> t = new ArrayList<>();
		tasks.drainTo(t);
		return t;
	}
}
