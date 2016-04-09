package com.erkoppel.fjframework.forkjoin;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

import com.erkoppel.fjframework.forkjoin.AbstractForkJoinTask.TerminalForkJoinTask;
import com.erkoppel.fjframework.forkjoin.interfaces.ThreadPicker;
import com.erkoppel.fjframework.forkjoin.util.RoundRobinThreadPicker;

public class WorkStealingThread extends ForkJoinThread {
	private Deque<AbstractForkJoinTask<?>> deq = new ConcurrentLinkedDeque<AbstractForkJoinTask<?>>();
	private ThreadPicker<ForkJoinThread> threadPicker = new RoundRobinThreadPicker<ForkJoinThread>(service.getThreads());

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
		service.signalMoreWork();
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
		AbstractForkJoinTask<?> task = null;
		int stealAttempts = 0;

		do {
			if (waiting != null && waiting.isDone()) {
				return;
			}

			if (waiting == null && ++stealAttempts >= service.getThreads().size()) {
				try {
					service.awaitMoreWork();
					stealAttempts = 0;
				} catch (InterruptedException e) {
					return;
				}
			}

			if (!service.getThreads().isEmpty()) {
				WorkStealingThread randomThread = (WorkStealingThread) threadPicker.nextThread();
				task = randomThread.deq.pollFirst();
			}
		} while (!isInterrupted() && task == null && !service.isShutdown());

		if (task != null && !task.isDone()) {
			task.run();
		}
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
		List<AbstractForkJoinTask<?>> drained = new ArrayList<>();
		while (!deq.isEmpty()) {
			drained.add(deq.poll());
		}
		return drained;
	}
}
