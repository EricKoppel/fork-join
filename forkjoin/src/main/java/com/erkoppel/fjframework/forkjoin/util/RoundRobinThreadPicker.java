package com.erkoppel.fjframework.forkjoin.util;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.erkoppel.fjframework.forkjoin.interfaces.ThreadPicker;

public class RoundRobinThreadPicker<T extends Thread> implements ThreadPicker<T> {
	private Iterator<T> iterator;

	public RoundRobinThreadPicker(List<T> threads) {
		this.iterator = new CyclicalListIterator<T>(threads);
	}

	@Override
	public T nextThread() {
		return iterator.next();
	}

	static class CyclicalListIterator<T> implements Iterator<T> {
		private List<T> list;
		private AtomicInteger index = new AtomicInteger(0);

		public CyclicalListIterator(List<T> collection) {
			this.list = collection;
		}

		@Override
		public boolean hasNext() {
			return !list.isEmpty();
		}

		@Override
		public T next() {
			return list.get(index.getAndUpdate(i -> (i + 1) % list.size()));
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
