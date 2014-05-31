package com.manning.nettyinaction;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class TestAutoBoxUnBox {
	public static void main(String[] args) throws Exception {
		Map<Integer, A> map = new HashMap<Integer, A>();
		// 往map里放1w个从1开始的A对象
		for(int i = 0; i < 10000; i++){
			map.put(i, new A(i));
		}
		
		while (true) {
			Collection<A> values = map.values();
			for (A a : values) {
				System.out.println(a);
				if (!map.containsKey(a.get())) {
					// 不可能发生，所以可以随便打点什么
				}
				Thread.sleep(1);
			}
		}
	}
}

class A {
	private int code;

	public A(int code) {
		this.code = code;
	}

	public int get() {
		return code;
	}
}
