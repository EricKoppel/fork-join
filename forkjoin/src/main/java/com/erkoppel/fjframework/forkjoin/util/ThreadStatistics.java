package com.erkoppel.fjframework.forkjoin.util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

public class ThreadStatistics<T> {
	private Map<String, T> map = new HashMap<String, T>();
	private boolean enabled;
	private T identity;
	private final Thread owner;

	public ThreadStatistics(T identity, Thread owner) {
		this.identity = identity;
		this.owner = owner;
	}

	public void set(String key, UnaryOperator<T> f) throws RuntimeException {
		if (!isEnabled()) {
			return;
		}
		
		if (Thread.currentThread() != owner) {
			throw new RuntimeException("Only owner thread can modify statistics!");
		}

		if (!map.containsKey(key)) {
			map.put(key, f.apply(identity));
		} else {
			map.put(key, f.apply(map.get(key)));
		}
	}

	public T get(String key) {
		if (map.containsKey(key)) {
			return map.get(key);
		} else {
			return identity;
		}
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void print() {
		if (isEnabled()) {
			System.out.println("ThreadStatistics [map=" + map + "]");
		}
	}

	public Thread getOwner() {
		return owner;
	}
}