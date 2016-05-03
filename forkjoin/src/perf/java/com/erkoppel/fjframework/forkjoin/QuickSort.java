package com.erkoppel.fjframework.forkjoin;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class QuickSort<T extends Comparable<T>> extends AbstractForkJoinRunnable {

	private List<T> list;
	private int left;
	private int right;

	public static void main(String[] args) {
		System.setProperty("forkjoin.enableStatistics", "true");
		ForkJoinExecutorService pool = new ForkJoinExecutorService(new WorkStealingThreadFactory());

		int PROBLEM_SIZE = 10000000;
		int ITERATIONS = 20;

		Supplier<List<Integer>> inputSupplier = () -> new Random().ints(PROBLEM_SIZE).boxed().collect(Collectors.toList());
		Consumer<List<Integer>> consumer = (list) -> pool.invoke(new QuickSort<Integer>(list, 0, list.size()));
		TimerUtil.time(consumer, inputSupplier, ITERATIONS);

		// System.out.printf("Tasks per second: %d\n", Math.round((numTasks /
		// iterations / (runningTime / 1000000000d))));
		pool.getThreads().forEach(t -> t.getThreadStatistics().print());

		pool.shutdown();
		pool.awaitTermination();
	}

	public QuickSort(List<T> list, int left, int right) {
		super();
		this.list = list;
		this.left = left;
		this.right = right;
	}

	@Override
	public void solve() {
		if (right - left > list.size() / 4) {
			int pivot = (int) (left + Math.random() * (right - left));
			pivot = partition(list, left, right, pivot);
			QuickSort<T> lSort = new QuickSort<T>(list, left, pivot);
			QuickSort<T> rSort = new QuickSort<T>(list, pivot + 1, right);
			lSort.fork();
			rSort.solve();
			lSort.join();
		} else {
			Collections.sort(list.subList(left, right));
		}
	}

	static <T extends Comparable<T>> int partition(List<T> list, int left, int right, int pivot) {
		T pivotValue = list.get(pivot);
		swap(list, left, pivot);
		int i = left;

		for (int j = i; j < right; j++) {
			if (pivotValue.compareTo(list.get(j)) > 0) {
				swap(list, ++i, j);
			}
		}

		swap(list, left, i);
		return i;
	}

	static <T> void swap(List<T> list, int i, int j) {
		T tmp = list.get(i);
		list.set(i, list.get(j));
		list.set(j, tmp);
	}
}
