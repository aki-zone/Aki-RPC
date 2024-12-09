package com.aki.rpc.netty.client.handler;

import com.aki.rpc.constant.CompressTypeEnum;
import com.aki.rpc.constant.MessageTypeEnum;
import com.aki.rpc.constant.AkiRpcConstants;
import com.aki.rpc.constant.SerializationTypeEnum;
import com.aki.rpc.factory.SingletonFactory;
import com.aki.rpc.message.AkiMessage;
import com.aki.rpc.message.AkiResponse;
import com.aki.rpc.message.UnprocessedRequests;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AkiNettyClientHandler extends ChannelInboundHandlerAdapter {
    private UnprocessedRequests unprocessedRequests;

    public AkiNettyClientHandler(){
        unprocessedRequests = SingletonFactory.getInstance(UnprocessedRequests.class);
    }
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            //一旦客户端发出消息，在这就得等待接收
            if (msg instanceof AkiMessage) {
                AkiMessage akiMessage = (AkiMessage) msg;
                Object data = akiMessage.getData();
                if (MessageTypeEnum.RESPONSE.getCode() == akiMessage.getMessageType()) {
                    AkiResponse akiResponse = (AkiResponse) data;
                    unprocessedRequests.complete(akiResponse);
                }
                //
            }
        }catch (Exception e){
            log.error("客户端读取消息出错:",e);
        }finally {
            //释放 以防内存泄露
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
       if (evt instanceof IdleStateEvent){
           IdleStateEvent stateEvent = (IdleStateEvent) evt;
           if (stateEvent.state() == IdleState.WRITER_IDLE){
               log.info("客户端发送了心跳包...");
               //进行心跳检测，发送一个心跳包去服务端
               AkiMessage akiMessage = AkiMessage.builder()
                       .messageType(MessageTypeEnum.HEARTBEAT_PING.getCode())
                       .compress(CompressTypeEnum.GZIP.getCode())
                       .codec(SerializationTypeEnum.PROTO_STUFF.getCode())
                       .data(AkiRpcConstants.HEART_PING)
                       .build();
               ctx.channel().writeAndFlush(akiMessage).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
           }
       }else {
           super.userEventTriggered(ctx,evt);
       }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        log.info("客户端连接上了...连接正常");
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //如果触发了这个方法 代表服务端 关闭连接了
        super.channelInactive(ctx);
        log.info("服务端关闭了连接....");
        //清除对应的缓存

        ctx.fireChannelInactive();
    }
}
