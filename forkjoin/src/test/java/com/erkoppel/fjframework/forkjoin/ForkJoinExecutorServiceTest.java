package com.erkoppel.fjframework.forkjoin;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class ForkJoinExecutorServiceTest {

	@BeforeClass
	public static void setSystemProperties() {
		System.setProperty("forkjoin.enableStatistics", "true");
	}
	
	@Test
	public void testStartWorkStealing() {
		ForkJoinExecutorService service = new ForkJoinExecutorService();
		Assert.assertTrue(service.getThreads().stream().allMatch(t -> t.getState() == Thread.State.NEW || t.getState() == Thread.State.RUNNABLE || t.getState() == Thread.State.WAITING && t instanceof WorkStealingThread));
		service.shutdown();
	}

	@Test
	public void testStartWorkShrugging() {
		ForkJoinExecutorService service = new ForkJoinExecutorService(new WorkShruggingThreadFactory());
		Assert.assertTrue(service.getThreads().stream().allMatch(t -> t.getState() == Thread.State.WAITING || t.getState() == Thread.State.NEW && t instanceof WorkShruggingThread));
		service.shutdown();
	}

	@Test
	public void testShutdown() throws InterruptedException {
		ForkJoinExecutorService service = new ForkJoinExecutorService();

		ReentrantLock lock = new ReentrantLock();
		Condition cond = lock.newCondition();
		
		long numTasks = (long) (100 + Math.random() * 100);
		
		Stream.generate(() -> {
			Runnable c = () -> {
				lock.lock();
				try {
					while (!service.isShutdown()) {cond.await();}
				} catch(InterruptedException e) {
				} finally {lock.unlock();}
			};
			return c;
		}).limit(numTasks).forEach(c -> service.execute(c));

		service.shutdown();
		
		lock.lock();
		try {cond.signalAll();
		} finally {lock.unlock();}
		
		service.getStopLatch().awaitAdvance(0);
		long numTasksRun = service.getThreads().stream().map(t -> t.getThreadStatistics().get("tasksRun") + t.getThreadStatistics().get("tasksStolen")).mapToLong(Long::valueOf).sum();
		long numTasksRemaining = service.getThreads().stream().map(ForkJoinThread::drainTasks).flatMap(List::stream).count();

		Assert.assertTrue(service.isShutdown());
		Assert.assertEquals(numTasks, numTasksRun);
		Assert.assertEquals(0, numTasksRemaining);
		Assert.assertTrue(service.getThreads().stream().allMatch(t -> t.getState() == Thread.State.TERMINATED));
		Assert.assertTrue(service.isTerminated());
	}

	@Test
	public void testAwaitTermination() throws InterruptedException {
		ForkJoinExecutorService service = new ForkJoinExecutorService();

		ReentrantLock lock = new ReentrantLock();
		Condition cond = lock.newCondition();

		long numTasks = (long) (100 + Math.random() * 100);
		
		Stream.generate(() -> {
			Runnable c = () -> {
				lock.lock();
				try {
					while (!service.isShutdown()) {cond.await();}
				} catch(InterruptedException e) {
				} finally {lock.unlock();}
			};
			return c;
		}).limit(numTasks).forEach(c -> service.execute(c));
		
		System.out.println(service.getStopLatch().getArrivedParties());
		service.shutdown();
		
		lock.lock();
		try {cond.signalAll();
		} finally {lock.unlock();}
		
		service.awaitTermination();
		
		long numTasksRun = service.getThreads().stream().map(t -> t.getThreadStatistics().get("tasksRun") + t.getThreadStatistics().get("tasksStolen")).mapToLong(Long::valueOf).sum();
		long numTasksRemaining = service.getThreads().stream().map(ForkJoinThread::drainTasks).flatMap(List::stream).count();

		Assert.assertTrue(service.isShutdown());
		Assert.assertEquals(0, numTasksRemaining);
		Assert.assertEquals(numTasks, numTasksRun);
		Assert.assertTrue(service.getThreads().stream().allMatch(t -> t.getThreadStatistics().isEnabled()));
		Assert.assertTrue(service.getThreads().stream().allMatch(t -> t.getState() == Thread.State.TERMINATED));
		Assert.assertTrue(service.isTerminated());
	}

	@Test
	public void testShutdownNow() throws InterruptedException {
		ForkJoinExecutorService service = new ForkJoinExecutorService();
		long numTasks = (long) (100 + Math.random() * 100);

		ReentrantLock lock = new ReentrantLock();
		Condition cond = lock.newCondition();
		
		Stream.generate(() -> {
			Runnable c = () -> {
				lock.lock();
				try {
					while (!service.isShutdown()) {cond.await();}
				} catch(InterruptedException e) {
				} finally {lock.unlock();}
			};
			return c;
		}).limit(numTasks).forEach(c -> service.execute(c));

		long numTasksNotRun = service.shutdownNow().size();
		
		lock.lock();
		try {cond.signalAll();
		} finally {lock.unlock();}
		
		service.awaitTermination();
		long numTasksRun = service.getThreads().stream().map(t -> t.getThreadStatistics().get("tasksRun") + t.getThreadStatistics().get("tasksStolen")).mapToLong(Long::valueOf).sum();
		
		Assert.assertTrue(service.isShutdown());
		Assert.assertTrue(service.getThreads().stream().allMatch(t -> t.getState() == Thread.State.TERMINATED));
		Assert.assertEquals(numTasks, numTasksNotRun + numTasksRun);
		Assert.assertTrue(service.isTerminated());
	}
	
	@Test(expected = CancellationException.class)
	public void testExecuteAfterShutdown() {
		ForkJoinExecutorService service = new ForkJoinExecutorService();
		service.shutdown();
		Assert.assertTrue(service.isShutdown());
		service.execute(() -> {});
	}
	
	@Test(expected = CancellationException.class)
	public void testSubmitAfterShutdown() throws InterruptedException, ExecutionException {
		ForkJoinExecutorService service = new ForkJoinExecutorService();
		service.shutdown();
		Assert.assertTrue(service.isShutdown());
		service.submit(() -> {});
	}
	
	@Test(expected = CancellationException.class)
	public void testInvokeAfterShutdown() throws InterruptedException, ExecutionException {
		ForkJoinExecutorService service = new ForkJoinExecutorService();
		service.shutdown();
		Assert.assertTrue(service.isShutdown());
		service.invoke(new AbstractForkJoinRunnable() {protected void solve() {}});
	}
}
