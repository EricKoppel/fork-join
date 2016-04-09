package com.erkoppel.fjframework.forkjoin.util;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Test;

public class RoundRobingThreadPickerTest {

	@Test
	public void testRoundRobinThreadPicker() {
		int numThreads = 10;
		List<Thread> threads = IntStream.range(0, numThreads).mapToObj(i -> {
			Thread t = new Thread(String.valueOf(i));
			return t;
		}).collect(Collectors.toList());

		RoundRobinThreadPicker<Thread> threadPicker = new RoundRobinThreadPicker<>(threads);

		for (int i = 0; i < numThreads * new Random().nextInt(100); i++) {
			Assert.assertEquals(i % numThreads, Integer.parseInt(threadPicker.nextThread().getName()));
		}
	}
}