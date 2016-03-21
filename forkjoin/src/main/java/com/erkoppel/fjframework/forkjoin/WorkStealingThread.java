package com.erkoppel.fjframework.forkjoin;

import java.util.Deque;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedDeque;

import com.erkoppel.fjframework.forkjoin.AbstractForkJoinTask.TerminalForkJoinTask;

public class WorkStealingThread extends ForkJoinThread {
	private Deque<AbstractForkJoinTask<?>> deq = new ConcurrentLinkedDeque<AbstractForkJoinTask<?>>();
	private Random rnd = new Random();

	public WorkStealingThread(ForkJoinExecutorService pool) {
		super(pool);
	}

	@Override
	public void run() {
		try {
			while (!interrupted()) {
				if (forkJoinPool.isShutdown() && deq.isEmpty()) {
					break;
				}
				
				AbstractForkJoinTask<?> task = deq.poll();

				if (task != null && !task.isDone()) {
					task.run();
				} else {
					steal(null);
				}
			}
		} finally {
			System.out.println("Shutting down.");
			forkJoinPool.getCountDownLatch().countDown();
			
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

				if (!forkJoinPool.getThreads().isEmpty()) {
					WorkStealingThread randomThread = (WorkStealingThread) forkJoinPool.getThreads().get(rnd.nextInt(forkJoinPool.getThreads().size()));
					task = randomThread.take();
				}
			} while (!isInterrupted() && task == null && !forkJoinPool.isShutdown());

			if (task != null && !task.isDone()) {
				task.run();
			}
		} finally {
			if (getPriority() == Thread.MIN_PRIORITY) {
				setPriority(Thread.NORM_PRIORITY);
			}
		}
	}

	private AbstractForkJoinTask<?> take() {
		return deq.pollFirst();
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

	@Override
	public List<AbstractForkJoinTask<?>> drainTasks() {
		// TODO Auto-generated method stub
		return null;
	}
}
