package com.erkoppel.fjframework.forkjoin;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class TimerUtil {
	public static <T> long time(Consumer<T> c, Supplier<T> s, int iterations) {
		long average = 0;

		for (int i = 0; i < iterations; i++) {
			T input = s.get();
			long start = System.nanoTime();
			c.accept(input);
			long end = System.nanoTime();
			long result = end - start;
			System.out.println("Test run " + i + ": " + result / 1000000000d + " s");
			average = (average * i + result) / (i + 1);
		}
		
		System.out.println("Average execution time: " + average / 1000000000d + " s");
		return average;
	}
}
