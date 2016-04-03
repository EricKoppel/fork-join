package com.erkoppel.fjframework.forkjoin;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

public class ForkJoinExecutorServiceTest {

	@Test
	public void testStartWorkStealing() {
		ForkJoinExecutorService service = new ForkJoinExecutorService();
		Assert.assertTrue(service.getThreads().stream().allMatch(t -> t.getState() == Thread.State.RUNNABLE && t instanceof WorkStealingThread));
		service.shutdown();
	}

	@Test
	public void testStartWorkShrugging() {
		ForkJoinExecutorService service = new ForkJoinExecutorService(new WorkShruggingThreadFactory());
		Assert.assertTrue(service.getThreads().stream().allMatch(t -> t.getState() == Thread.State.WAITING && t instanceof WorkShruggingThread));
		service.shutdown();
	}

	@Test
	public void testShutdown() throws InterruptedException {
		ForkJoinExecutorService service = new ForkJoinExecutorService();

		Stream.generate(() -> {
			Runnable c = () -> {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}
			};
			return c;
		}).limit(10).forEach(c -> {
			service.execute(c);
		});

		service.shutdown();
		service.getCountDownLatch().await();
		long numTasksRemaining = service.getThreads().stream().map(ForkJoinThread::drainTasks).flatMap(List::stream).count();

		Assert.assertEquals(0, numTasksRemaining);
		Assert.assertTrue(service.getThreads().stream().allMatch(t -> t.getState() == Thread.State.TERMINATED));
	}

	@Test
	public void testAwaitTermination() throws InterruptedException {
		ForkJoinExecutorService service = new ForkJoinExecutorService();

		Stream.generate(() -> {
			Runnable c = () -> {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}
			};
			return c;
		}).limit(10).forEach(c -> {
			service.execute(c);
		});

		service.shutdown();
		service.awaitTermination(10000, TimeUnit.MILLISECONDS);
		long numTasksRemaining = service.getThreads().stream().map(ForkJoinThread::drainTasks).flatMap(List::stream).count();

		Assert.assertEquals(0, numTasksRemaining);
		Assert.assertTrue(service.getThreads().stream().allMatch(t -> t.getState() == Thread.State.TERMINATED));
	}

	@Test
	public void testShutdownNow() throws InterruptedException {
		ForkJoinExecutorService service = new ForkJoinExecutorService();

		int diff = 2;
		int numThreads = service.getThreads().size();
		int numTasks = numThreads + diff;

		Stream.generate(() -> {
			Runnable c = () -> {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
			};
			return c;
		}).limit(numTasks).forEach(c -> {
			service.execute(c);
		});

		List<Runnable> r = service.shutdownNow();
		service.awaitTermination(10000, TimeUnit.MILLISECONDS);
		Assert.assertEquals(diff, r.size());
		Assert.assertTrue(service.getThreads().stream().allMatch(t -> t.getState() == Thread.State.TERMINATED));
	}
	
	@Test(expected = CancellationException.class)
	public void testExecuteAfterShutdown() {
		ForkJoinExecutorService service = new ForkJoinExecutorService();
		service.shutdown();
		service.execute(() -> {});
	}
	
	@Test(expected = CancellationException.class)
	public void testSubmitAfterShutdown() throws InterruptedException, ExecutionException {
		ForkJoinExecutorService service = new ForkJoinExecutorService();
		service.shutdown();
		service.submit(() -> {});
	}
	
	@Test(expected = CancellationException.class)
	public void testInvokeAfterShutdown() throws InterruptedException, ExecutionException {
		ForkJoinExecutorService service = new ForkJoinExecutorService();
		service.shutdown();
		service.invoke(new AbstractForkJoinRunnable() {protected void solve() {}});
	}
}
