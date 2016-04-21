package com.erkoppel.fjframework.forkjoin.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Test;

public class ThreadStatisticsTest {

	@Test
	public void testThreadStatistics() {
		ThreadStatistics<Long> s = new ThreadStatistics<Long>(1L, Thread.currentThread());
		s.setEnabled(true);
		IntStream.rangeClosed(1, 10).forEach(i -> s.set("foo", v -> v * i));
		Assert.assertEquals(3628800, (long) s.get("foo"));
	}

	@Test
	public void testIdentity() {
		ThreadStatistics<String> s = new ThreadStatistics<String>("hallå!", Thread.currentThread());
		s.setEnabled(true);
		Assert.assertEquals("hallå!", s.get("foo"));
	}
	
	@Test(expected = RuntimeException.class)
	public void testOwner() throws InterruptedException, ExecutionException {
		FutureTask<ThreadStatistics<Long>> task = new FutureTask<ThreadStatistics<Long>>(() -> {
			ThreadStatistics<Long> s = new ThreadStatistics<Long>(0L, Thread.currentThread());
			s.setEnabled(true);
			return s;
		});

		Thread t = new Thread(task);
		t.setName("executor");
		t.start();

		ThreadStatistics<Long> stats = task.get();
		Assert.assertEquals(t, stats.getOwner());
		stats.set("foo", c -> c + 1);
	}
}