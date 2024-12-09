package com.aki.rpc.netty;

import com.aki.rpc.netty.handler.AkiRpcThreadFactory;
import com.aki.rpc.netty.handler.NettyServerInitiator;
import com.aki.rpc.server.AkiServiceProvider;
import com.aki.rpc.utils.RuntimeUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import lombok.extern.slf4j.Slf4j;

/**
 * @Auther akizora
 * netty 服务端的启动主类
 */
@Slf4j
public class NettyServer implements AkiServer {

    public final static int PORT = 13567;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    private AkiServiceProvider akiServiceProvider;
    private DefaultEventExecutorGroup eventExecutors;

    private boolean isRunning;

    public NettyServer() {
    }

    @Override
    public void run() {
        //1.开辟boos线程 工作线程
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        try {
            // 2.创建服务器引导类
            ServerBootstrap bootstarp = new ServerBootstrap();

            // 3.事件执行器组
            // 使用AkiRpcThreadFactory的newThread重写方法创建自定义线程

            eventExecutors = new DefaultEventExecutorGroup(RuntimeUtil.cpus() * 2,new AkiRpcThreadFactory(akiServiceProvider));

            // 4.服务器引导配置
            bootstarp.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    // TCP默认开启了 Nagle 算法，该算法的作用是尽可能的发送大数据快，减少网络传输。TCP_NODELAY 参数的作用就是控制是否启用 Nagle 算法。
                    .childOption(ChannelOption.TCP_NODELAY,true)
                    //是否开启 TCP 底层心跳机制 KEEPALIVE 保活
                    .childOption(ChannelOption.SO_KEEPALIVE,true)
                    //表示系统用于临时存放已完成三次握手的请求的队列的最大长度,如果连接建立频繁，服务器处理创建新连接较慢，可以适当调大这个参数
                    .childOption(ChannelOption.SO_BACKLOG,1024)
                    //添加日志处理
                    .handler(new LoggingHandler(LogLevel.INFO))
                    // 当客户端第一次进行请求的时候才会进行初始化
                    .childHandler(new NettyServerInitiator(eventExecutors));

            // 5.绑定端口，同步等待绑定成功
            bootstarp.bind(akiServiceProvider
                            .getAkiRpcConfig()
                            .getProviderPort())     // 读取配置类的端口号
                            .sync()                 // 同步等待绑定完成
                            .channel();
            isRunning = true;
            Runtime.getRuntime().addShutdownHook(new Thread(){
                @Override
                public void run() {
                    stopServer();
                }
            });

        }catch (InterruptedException e){
            log.error("netty server服务启动异常:",e);
        }

    }

    /**
     * 有序关闭Netty相关资源
     */
    private void stopNettyServer() {
        if (eventExecutors != null){
            eventExecutors.shutdownGracefully();
        }
        if (bossGroup != null){
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null){
            workerGroup.shutdownGracefully();
        }
    }
    @Override
    public void stopServer() {
        stopNettyServer();
        isRunning = false;
    }

    public void setAkiServiceProvider(AkiServiceProvider akiServiceProvider) {
        this.akiServiceProvider = akiServiceProvider;
    }

    public boolean isRunning() {
        return isRunning;
    }
}
