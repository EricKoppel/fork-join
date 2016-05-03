package com.erkoppel.fjframework.forkjoin;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

import com.erkoppel.fjframework.forkjoin.interfaces.ThreadPicker;
import com.erkoppel.fjframework.forkjoin.util.RoundRobinThreadPicker;


/**
 * {@link WorkStealingThread} is the core implementation of {@link ForkJoinThread}.
 * It is based off of Doug Lea's "A Java Fork/Join Framework" with a few tweaks.
 * 
 */
public class WorkStealingThread extends ForkJoinThread {
	private Deque<AbstractForkJoinTask<?>> deq = new ConcurrentLinkedDeque<AbstractForkJoinTask<?>>();
	private ThreadPicker<ForkJoinThread> threadPicker = new RoundRobinThreadPicker<ForkJoinThread>(service.getThreads());

	private final int MAX_STEAL_ATTEMPTS;

	public WorkStealingThread(ForkJoinExecutorService service) {
		super(service);
		MAX_STEAL_ATTEMPTS = service.getThreads().size() << 8;
	}

	@Override
	public void run() {
		service.getStopLatch().register();
		try {
			while (!(shutdownNow.get() || (shutdown.get() && deq.isEmpty()))) {
				AbstractForkJoinTask<?> task = deq.poll();

				if (task != null && !task.isDone()) {
					task.run();
					statistics.set("tasksRun", i -> i + 1);
				} else {
					steal(null);
				}
			}
		} finally {
			service.getStopLatch().arriveAndDeregister();
		}
	}

	@Override
	public <T> AbstractForkJoinTask<T> fork(AbstractForkJoinTask<T> task) {
		deq.push(task);
		service.signalMoreWork();
		return task;
	}

	@Override
	public <T> T join(AbstractForkJoinTask<T> task) {
		while (!task.isDone()) {
			AbstractForkJoinTask<?> t = deq.poll();
			if (t != null && !t.isDone()) {
				t.run();
				statistics.set("tasksRun", i -> i + 1);
			} else {
				steal(task);
			}
		}

		return task.getResult();
	}

	/**
	 * The steal method is executed when a {@link WorkStealingThread} has no work
	 * on its deque. Tasks pulled from the back of other {@link ForkJoinThread}s' 
	 * deques, in round-robin fashion, and executed, yielding for the owner thread. 
	 * 
	 * If the {@link WorkStealingThread} is waiting for another task to complete and
	 * that task finishes, the method will return.
	 * 
	 * After a number of attempted steals, the thread will go into a waiting state
	 * until another thread pushes work onto its deque. 
	 * 
	 * @param waiting the task that {@link WorkStealingThread} is waiting for completion
	 */
	private void steal(AbstractForkJoinTask<?> waiting) {
		AbstractForkJoinTask<?> task = null;
		int stealAttempts = 0;

		do {
			if (waiting != null && waiting.isDone() || !deq.isEmpty()) {
				return;
			}

			if (waiting == null && ++stealAttempts >= MAX_STEAL_ATTEMPTS) {
				try {
					statistics.set("idleCount", i -> i + 1);
					service.awaitMoreWork();
					stealAttempts = 0;
				} catch (InterruptedException e) {
					return;
				}
			}

			yield();

			WorkStealingThread randomThread = (WorkStealingThread) threadPicker.nextThread();
			task = randomThread.deq.pollLast();

			if (task != null && !task.isDone()) {
				task.run();
				statistics.set("tasksStolen", i -> i + 1);
			}
		} while (!isInterrupted() && !service.isShutdown());
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
