package com.manning.nettyinaction.chapter2;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * Listing 2.4 of <i>Netty in Action</i>
 * 
 * @author <a href="mailto:nmaurer@redhat.com">Norman Maurer</a>
 */
@Sharable// 此annotation标识可以在channel间共享
public class EchoServerHandler extends ChannelInboundHandlerAdapter {

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		ByteBuf in = (ByteBuf) msg;
		System.out.println("Server received: " + ByteBufUtil.hexDump(in));
		// 把接收的消息写回去 但是不会flush
		ctx.write(in);
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		//把所有写的消息全部flush到远程客户端 并且关闭channel
		ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(
				ChannelFutureListener.CLOSE);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		//  记录日志 关闭channel
		cause.printStackTrace();
		ctx.close();
	}
}
