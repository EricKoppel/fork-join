package com.erkoppel.fjframework.forkjoin.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Test;

public class CyclicalListIteratorTest {

	@Test
	public void testCyclicalListIterator() {
		List<Integer> list = IntStream.generate(() -> new Random().nextInt(100)).limit(10).boxed().collect(Collectors.toList());
		List<Integer> result = new ArrayList<Integer>();

		Iterator<Integer> iterator = new RoundRobinThreadPicker.CyclicalListIterator<Integer>(list);

		for (int i = 0; i < list.size(); i++) {
			Integer item = iterator.next();
			result.add(item);
		}

		Assert.assertEquals(list.size(), result.size());
		Assert.assertArrayEquals(list.toArray(new Integer[list.size()]), result.toArray(new Integer[list.size()]));
	}
}
