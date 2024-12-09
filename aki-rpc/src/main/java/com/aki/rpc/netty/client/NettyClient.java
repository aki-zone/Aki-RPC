package com.aki.rpc.netty.client;

import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.common.utils.RandomUtils;
import com.aki.rpc.config.AkiRpcConfig;
import com.aki.rpc.constant.CompressTypeEnum;
import com.aki.rpc.constant.MessageTypeEnum;
import com.aki.rpc.constant.SerializationTypeEnum;
import com.aki.rpc.exception.AkiRpcException;
import com.aki.rpc.factory.SingletonFactory;
import com.aki.rpc.message.AkiMessage;
import com.aki.rpc.message.AkiRequest;
import com.aki.rpc.message.AkiResponse;
import com.aki.rpc.netty.client.cache.ChannelCache;
import com.aki.rpc.netty.client.handler.AkiNettyClientHandler;
import com.aki.rpc.message.UnprocessedRequests;
import com.aki.rpc.netty.client.idle.ConnectionWatchdog;
import com.aki.rpc.netty.codec.AkiRpcDecoder;
import com.aki.rpc.netty.codec.AkiRpcEncoder;
import com.aki.rpc.register.nacos.NacosTemplate;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.HashedWheelTimer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

/**
 * @Auther akizora
 * netty 客户端的启动主类
 */
@Slf4j
public class NettyClient implements AkiClient {

    private AkiRpcConfig akiRpcConfig;   // 用于存储与 RPC 配置相关的信息。
    private final Bootstrap bootstrap;  // 用于配置和启动 Netty 客户端的引导类
    private final EventLoopGroup eventLoopGroup;    // 用于处理 Netty 中的事件循环。事件循环组负责处理网络事件、IO操作等。
    private final UnprocessedRequests unprocessedRequests;     //存储尚未处理的请求，用于异步响应返回，全局唯一
    private final NacosTemplate nacosTemplate;      // Nacos工具类

    private final static Set<String> SERVICES = new CopyOnWriteArraySet<>();    // 用于存储Netty服务的 IP 地址和端口信息（ip,port）。CopyOnWriteArraySet 是线程安全的集合，适用于高并发场景。
    protected final HashedWheelTimer timer = new HashedWheelTimer();         // Netty 中的定时器类，使用分桶轮算法来处理定时任务，适用于定时任务的管理。
    private final ChannelCache channelCache;            // 用于缓存 ip-Channel 对象

    public NettyClient(){
        this.channelCache = SingletonFactory.getInstance(ChannelCache.class);
        this.unprocessedRequests = SingletonFactory.getInstance(UnprocessedRequests.class);
        this.nacosTemplate = SingletonFactory.getInstance(NacosTemplate.class);
        eventLoopGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                //超时时间设置
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,5000);
    }

    /**
     * 客户端发送请求并返回调用结果
     * @param akiRequest
     * @return
     */
    @Override
    public Object sendRequest(AkiRequest akiRequest) {

        // 1. 判断是否已经配置了 Rpc 配置，如果没有则抛出异常
        if (akiRpcConfig == null){
            throw new AkiRpcException("必须启用Rpc相关配置！");
        }

        // 2. 创建 CompletableFuture封装异步响应体，用于异步返回请求结果
        CompletableFuture<AkiResponse<Object>> resultCompletableFuture = new CompletableFuture<>();

        // 3. 连接 netty服务，获取到 channel（网络通道）
        // 3.1 初始化服务地址和端口
        InetSocketAddress inetSocketAddress = null;
        String ipPort = null;

        // 3.1 判断 SERVICES 是否为空（服务列表），如果不为空则随机选择一个服务地址
        if (!SERVICES.isEmpty()){

            int size = SERVICES.size();
            // 使用随机的负载均衡算法获取服务地址
            int nextInt = RandomUtils.nextInt(0, size - 1);
            // 获取IP-PORT值
            Optional<String> optional = SERVICES.stream().skip(nextInt).findFirst();
            if (optional.isPresent()){

                ipPort = optional.get();

                // 构造 InetSocketAddress 对象用于连接
                new InetSocketAddress(ipPort.split(",")[0], Integer.parseInt(ipPort.split(",")[1]));
                log.info("使用了本地缓存，省去了连接nacos的开销...");
            }
        }
        // *dubbo-rpc ： 注册中心挂掉之后，服务调用还能否正常？ 回答 正常，第一次调用之后，缓存服务提供方的地址，直接发起调用
        // 3.2 从 nacos 获取服务提供方的 ip 和端口，若Nacos宕机则依赖于3.1步骤的SERVICES本地缓存
        Instance oneHealthyInstance = null;
        try {
            // 从Nacos中获取一个健康服务器实例，读取其地址/端口
            oneHealthyInstance = nacosTemplate.getOneHealthyInstance(akiRpcConfig.getNacosGroup(),akiRequest.getInterfaceName() + akiRequest.getVersion());
            inetSocketAddress = new InetSocketAddress(oneHealthyInstance.getIp(),oneHealthyInstance.getPort());
            ipPort = oneHealthyInstance.getIp() + "," + oneHealthyInstance.getPort();
            SERVICES.add(ipPort);
        } catch (Exception e) {
            log.error("获取nacos实例 出错:",e);
            resultCompletableFuture.completeExceptionally(e);
            return resultCompletableFuture;
        }

        // 3.3 创建一个 CompletableFuture，用于异步获取 netty 服务的 channel
        CompletableFuture<Channel> channelCompletableFuture = new CompletableFuture<>();

        // 3.4 创建一个 ConnectionWatchdog（连接监视器），用于连接时监控状态
        ConnectionWatchdog watchdog = new ConnectionWatchdog(bootstrap, timer, inetSocketAddress, channelCompletableFuture, true,channelCache) {

            /**
             * 匿名重写CacheClearHandler.clear
             * @param inetSocketAddress
             */
            @Override
            public void clear(InetSocketAddress inetSocketAddress) {
                SERVICES.remove(inetSocketAddress.getHostName()+","+inetSocketAddress.getPort());
                log.info("超过最大限次未重连上，进行缓存清理...");
            }

            /**
             * 匿名重写
             * @return
             */
            @Override
            public ChannelHandler[] handlers() {
                return new ChannelHandler[]{
                        this,
                        new IdleStateHandler(0, 3, 0, TimeUnit.SECONDS),
                        new AkiRpcDecoder(),
                        new AkiRpcEncoder(),
                        new AkiNettyClientHandler()
                };
            }
        };

        // 4. 将在watchdog缓存的处理器都导入启动依赖中。
        // *每当一个新的网络连接建立时，这个方法都会被调用。
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(watchdog.handlers());
            }
        });

//        String finalIpPort = ipPort;
//        bootstrap.connect(inetSocketAddress).addListener(new ChannelFutureListener() {
//            @Override
//            public void operationComplete(ChannelFuture future) throws Exception {
//                //连接是否完成
//                if (future.isSuccess()){
//                    channelCompletableFuture.complete(future.channel());
//                }else{
//                    //从缓存当中进行剔除
//                    SERVICES.remove(finalIpPort);
//                    channelCompletableFuture.completeExceptionally(future.cause());
//                    log.info("连接netty服务失败");
//                }
//            }
//        });

        // 5. 存入一个请求相关的异步任务
        unprocessedRequests.put(akiRequest.getRequestId(),resultCompletableFuture);

        // 6. 获取网络通道，传入目标网络地址和通道完成future
        Channel channel = getChannel(inetSocketAddress,channelCompletableFuture);
        if (!channel.isActive()){
            throw new AkiRpcException("连接异常");
        }

        // 7. 构建Aki消息体，包含以下信息：
        // - 序列化方式（使用Protostuff）
        // - 压缩类型（使用GZIP压缩）
        // - 消息类型（请求类型）
        // - 实际传输的数据（akiRequest）
        AkiMessage akiMessage = AkiMessage.builder()
                .codec(SerializationTypeEnum.PROTO_STUFF.getCode())
                .compress(CompressTypeEnum.GZIP.getCode())
                .messageType(MessageTypeEnum.REQUEST.getCode())
                .data(akiRequest)
                .build();

        // 8. 将消息写入通道并立即刷新，添加通道future监听器
        channel.writeAndFlush(akiMessage).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()){
                    log.info("请求完成");
                }else {
                    log.error("发送请求数据失败");
                    // 关闭channel，resultCompletableFuture以异常方式完成，传播错误
                    future.channel().close();
                    resultCompletableFuture.completeExceptionally(future.cause());
                }
            }
        });

        return resultCompletableFuture;
    }

    @SneakyThrows
    private Channel getChannel(InetSocketAddress inetSocketAddress, CompletableFuture<Channel> channelCompletableFuture) {
        Channel channel = channelCache.get(inetSocketAddress);
        if (channel == null){
            //进行连接...
            doConnect(inetSocketAddress,channelCompletableFuture);
            channel = channelCompletableFuture.get();
            channelCache.set(inetSocketAddress,channel);
            return channel;
        }else{
            log.info("channel是从缓存中获取的，性能进步一步做了提升...");
            return channel;
        }
    }

    public void doConnect(InetSocketAddress inetSocketAddress, CompletableFuture<Channel> channelCompletableFuture){
        bootstrap.connect(inetSocketAddress).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                //连接是否完成
                if (future.isSuccess()){
                    channelCompletableFuture.complete(future.channel());
                }else{
                    //从缓存当中进行剔除
                    SERVICES.remove(inetSocketAddress.getHostName()+","+inetSocketAddress.getPort());
                    channelCompletableFuture.completeExceptionally(future.cause());
                    log.info("连接netty服务失败");
                }
            }
        });
    }

    public AkiRpcConfig getAkiRpcConfig() {
        return akiRpcConfig;
    }

    public void setAkiRpcConfig(AkiRpcConfig akiRpcConfig) {
        this.akiRpcConfig = akiRpcConfig;
    }
}
