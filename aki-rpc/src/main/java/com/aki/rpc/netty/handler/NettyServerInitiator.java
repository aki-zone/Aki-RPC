package com.aki.rpc.netty.handler;

import com.aki.rpc.netty.codec.AkiRpcDecoder;
import com.aki.rpc.netty.codec.AkiRpcEncoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;

import java.util.concurrent.TimeUnit;


public class NettyServerInitiator extends ChannelInitializer<SocketChannel> {
    private DefaultEventExecutorGroup eventExecutors;

    public NettyServerInitiator(DefaultEventExecutorGroup eventExecutors) {
        this.eventExecutors = eventExecutors;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        //定义 TCP协议 数据报文格式
        //10s中 没有读请求 认为该触发心跳检测
        ch.pipeline().addLast(new IdleStateHandler(10,0,0, TimeUnit.SECONDS));
        //解码器
        ch.pipeline ().addLast ( "decoder",new AkiRpcDecoder() );
        //编码器
        ch.pipeline ().addLast ( "encoder",new AkiRpcEncoder());
        //消息处理器，线程池处理
        ch.pipeline ().addLast ( eventExecutors,"handler",new AkiNettyServerHandler());
    }
}
