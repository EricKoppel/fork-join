package com.erkoppel.fjframework.forkjoin;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedDeque;

import com.erkoppel.fjframework.forkjoin.AbstractForkJoinTask.TerminalForkJoinTask;

public class WorkStealingThread extends ForkJoinThread {
	private Deque<AbstractForkJoinTask<?>> deq = new ConcurrentLinkedDeque<AbstractForkJoinTask<?>>();
	private Random rnd = new Random();

	public WorkStealingThread(ForkJoinExecutorService service) {
		super(service);
	}

	@Override
	public void run() {
		try {
			while (!(shutdownNow.get() || (shutdown.get() && deq.isEmpty()))) {
				AbstractForkJoinTask<?> task = deq.poll();

				if (task != null && !task.isDone()) {
					task.run();
				} else {
					steal(null);
				}
			}
		} finally {
			service.getStopLatch().countDown();
		}
	}

	@Override
	public <T> T submit(AbstractForkJoinTask<T> task) {
		return fork(new TerminalForkJoinTask<T>(task)).join();
	}

	@Override
	public <T> AbstractForkJoinTask<T> fork(AbstractForkJoinTask<T> task) {
		deq.push(task);
		return task;
	}

	@Override
	public void join(AbstractForkJoinTask<?> task) {
		while (!task.isDone()) {
			AbstractForkJoinTask<?> t = deq.poll();
			if (t != null && !t.isDone()) {
				t.run();
			} else {
				steal(task);
			}
		}
	}

	private void steal(AbstractForkJoinTask<?> waiting) {
		try {
			AbstractForkJoinTask<?> task = null;

			do {
				if (waiting != null && waiting.isDone()) {
					return;
				}

				if (getPriority() != Thread.MIN_PRIORITY) {
					setPriority(Thread.MIN_PRIORITY);
				} else if (getPriority() == Thread.MIN_PRIORITY) {
					yield();
				}

				if (!service.getThreads().isEmpty()) {
					WorkStealingThread randomThread = (WorkStealingThread) service.getThreads().get(rnd.nextInt(service.getThreads().size()));
					task = randomThread.deq.pollFirst();
				}
			} while (!isInterrupted() && task == null && !service.isShutdown());

			if (task != null && !task.isDone()) {
				task.run();
			}
		} finally {
			if (getPriority() == Thread.MIN_PRIORITY) {
				setPriority(Thread.NORM_PRIORITY);
			}
		}
	}

	@Override
	protected void onShutdown() {
	}

	@Override
	protected void onShutdownNow() {
	}

	@Override
	protected List<AbstractForkJoinTask<?>> drainTasks() {
		List<AbstractForkJoinTask<?>> drained = new ArrayList<>();
		while (!deq.isEmpty()) {
			drained.add(deq.poll());
		}
		return drained;
	}
}
