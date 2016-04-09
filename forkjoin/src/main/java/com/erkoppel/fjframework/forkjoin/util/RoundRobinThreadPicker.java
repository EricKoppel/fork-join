package com.erkoppel.fjframework.forkjoin.util;

import java.util.Iterator;
import java.util.List;

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
		private int index;

		public CyclicalListIterator(List<T> collection) {
			this.list = collection;
			this.index = 0;
		}

		@Override
		public boolean hasNext() {
			return !list.isEmpty();
		}

		@Override
		public T next() {
			T res = list.get(index);
			index = (index + 1) % list.size();
			return res;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
