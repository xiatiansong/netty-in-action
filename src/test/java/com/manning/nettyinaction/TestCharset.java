package com.manning.nettyinaction;

import java.nio.charset.Charset;

public class TestCharset {

	public static void main(String[] args) throws Exception {
		System.out.println(Charset.defaultCharset().name());
		System.out.println(System.getProperty("file.encoding"));
	}

}
