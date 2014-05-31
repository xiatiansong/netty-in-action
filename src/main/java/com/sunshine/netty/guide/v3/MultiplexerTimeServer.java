package com.sunshine.netty.guide.v3;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class MultiplexerTimeServer implements Runnable {

	private Selector selector;

	private ServerSocketChannel servChannel;

	private volatile boolean stop;

	/**
	 * 初始化多路复用器、绑定监听端口
	 * 
	 * @param port
	 */
	public MultiplexerTimeServer(int port) {
		try {
			selector = Selector.open();
			servChannel = ServerSocketChannel.open();
			//设置为非阻塞模式
			servChannel.configureBlocking(false);
			servChannel.socket().bind(new InetSocketAddress(port), 1024);
			servChannel.register(selector, SelectionKey.OP_ACCEPT);
			System.out.println("The time server is start in port : " + port);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void stop() {
		this.stop = true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		while (!stop) {
			try {
				int nKeys = selector.select(1000);
				if(nKeys <= 0){
					continue;
				}
				Set<SelectionKey> selectedKeys = selector.selectedKeys();
				Iterator<SelectionKey> it = selectedKeys.iterator();
				SelectionKey key = null;
				while (it.hasNext()) {
					key = it.next();
					it.remove();
					try {
						handleInput(key);
					} catch (Exception e) {
						if (key != null) {
							key.cancel();
							if (key.channel() != null)
								key.channel().close();
						}
					}
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}

		// 多路复用器关闭后，所有注册在上面的Channel和Pipe等资源都会被自动去注册并关闭，所以不需要重复释放资源
		if (selector != null)
			try {
				selector.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
	}

	private void handleInput(SelectionKey key) throws IOException {

		if (key.isValid()) {
			// 处理发生连接的请求消息
			if (key.isAcceptable()) {
				// Accept the new connection
				ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
				SocketChannel sc = ssc.accept();
				sc.configureBlocking(false);
				// 注册感兴趣的IO读时间，通常不直接注册写事件，在发送缓冲区未满的情况下，
				//一般是可写的，因此如注册了写事件，而又不用写数据，很同意造成CPU消耗100%的现象
				sc.register(selector, SelectionKey.OP_READ);
				//完成连接建立
				sc.finishConnect();
			}
			if (key.isReadable()) {//有流可读取
				// Read the data
				SocketChannel sc = (SocketChannel) key.channel();
				ByteBuffer readBuffer = ByteBuffer.allocate(1024);
				
				int readBytes = 0;
				try{
					int ret = 0;
					try{
						while((ret = sc.read(readBuffer)) > 0){
							readBytes += ret;
						}
					}finally{
						readBuffer.flip();
					}
					if (readBytes > 0) {
						byte[] bytes = new byte[readBuffer.remaining()];
						readBuffer.get(bytes);
						String body = new String(bytes, "UTF-8");
						System.out.println("The time server receive order : " + body);
						String currentTime = "QUERY TIME ORDER".equalsIgnoreCase(body.trim()) ? 
								new java.util.Date(System.currentTimeMillis()).toString() : "BAD ORDER";
						doWrite(sc, currentTime);
					} else if (readBytes < 0) {
						// 对端链路关闭
						key.cancel();
						sc.close();
					} else
						; // 读到0字节，忽略
				}finally{
					if(readBuffer != null){
						readBuffer.clear();
					}
				}
			}else if(key.isWritable()){//可写入流
				//取消对OP_WRITE事件的注册
				key.interestOps(key.interestOps() & (~SelectionKey.OP_WRITE));
				SocketChannel sc = (SocketChannel)key.channel();
				//此步为阻塞操作，直到写入操作系统发送缓冲区或网络IO出现异常，返回的为成功写入的字节数，当操作系统的发送缓冲区已满，此处会返回0
				byte[] bytes = "QUERY TIME ORDER".getBytes();
				ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
				writeBuffer.put(bytes);
				writeBuffer.flip();
				int writtenedSize = sc.write(writeBuffer);
				if(writtenedSize == 0){
					key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
				}
			}
			selector.selectedKeys().clear();
		}
	}

	private void doWrite(SocketChannel channel, String response)
			throws IOException {
		if (response != null && response.trim().length() > 0) {
			byte[] bytes = response.getBytes();
			ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
			writeBuffer.put(bytes);
			writeBuffer.flip();
			channel.write(writeBuffer);
		}
	}
}
