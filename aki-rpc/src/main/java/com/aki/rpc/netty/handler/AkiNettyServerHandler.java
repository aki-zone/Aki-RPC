package com.aki.rpc.netty.handler;

import com.aki.rpc.constant.MessageTypeEnum;
import com.aki.rpc.constant.AkiRpcConstants;
import com.aki.rpc.factory.SingletonFactory;
import com.aki.rpc.message.AkiMessage;
import com.aki.rpc.message.AkiRequest;
import com.aki.rpc.message.AkiResponse;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AkiNettyServerHandler extends ChannelInboundHandlerAdapter {

    private AkiRequestHandler akiRequestHandler;

    public AkiNettyServerHandler(){
        akiRequestHandler = SingletonFactory.getInstance(AkiRequestHandler.class);
    }

    /**
     * 核心处理器，对消息体进行处理
      */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //接收客户端发来的数据，数据肯定包括了 要调用的服务提供者的 接口，方法，
        //解析消息，去找到对应的服务提供者，然后调用，得到调用结果，发消息给客户端就可以了
        try {
            // 1. 检查接收到的消息是否是 AkiMessage 类型
            if (msg instanceof AkiMessage){
                // 2. 拿到请求数据 ，调用对应服务提供方方法 获取结果 给客户端返回
                AkiMessage akiMessage = (AkiMessage) msg;
                byte messageType = akiMessage.getMessageType();

                // 2.1判断消息类型是否为心跳检测请求 (PING)，是则返回(PONG)消息
                if (MessageTypeEnum.HEARTBEAT_PING.getCode() == messageType){
                    akiMessage.setMessageType(MessageTypeEnum.HEARTBEAT_PONG.getCode());
                    akiMessage.setData(AkiRpcConstants.HEART_PONG);
                }

                // 2.2 判断消息类型是否为调用业务请求
                if (MessageTypeEnum.REQUEST.getCode() == messageType){
                    // 2.2.1 通过Request处理器处理业务，使用反射找到方法 发起调用 获取结果
                    AkiRequest akiRequest = (AkiRequest) akiMessage.getData();
                    Object result = akiRequestHandler.handler(akiRequest);

                    // 2.2.2 设置消息类型
                    akiMessage.setMessageType(MessageTypeEnum.RESPONSE.getCode());

                    // 2.2.3 检查当前通道是否活跃且可写，确保可以发送响应数据
                    if (ctx.channel().isActive() && ctx.channel().isWritable()) {
                        // 活跃则设置成功响应消息，并准备发送
                        AkiResponse akiResponse = AkiResponse.success(result, akiRequest.getRequestId());
                        akiMessage.setData(akiResponse);
                        log.info("服务端收到数据，并处理完成{}:",akiMessage);
                    }else{
                        // 否则设置失败消息
                        akiMessage.setData(AkiResponse.fail("net fail"));
                    }
                }

                // 3.将消息写入到通道并刷新，发送给客户端；
                // *如果写入失败，自动关闭通道（CLOSE_ON_FAILURE）
                ctx.writeAndFlush(akiMessage).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
        }catch (Exception e){
            log.error("读取消息出错:",e);
        }finally {
            // channelRead需手动释放msg 此处需避免内存泄露
            ReferenceCountUtil.release(msg);
        }

    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent){
            IdleStateEvent stateEvent = (IdleStateEvent) evt;
            IdleState state = stateEvent.state();
            if (state == IdleState.READER_IDLE){
                log.info("收到了心跳检测，超时未读取....");
//                ctx.close();
            }else {
                super.userEventTriggered(ctx,evt);
            }

        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.info("发生了错误....");
        super.exceptionCaught(ctx, cause);
        ctx.close();
    }
}
