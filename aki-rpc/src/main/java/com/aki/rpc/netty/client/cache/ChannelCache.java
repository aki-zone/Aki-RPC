package com.aki.rpc.netty.client.cache;

import io.netty.channel.Channel;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Auther akizora
 * 通道缓存管理类，存储【服务器实例地址 address】 - 【通信管道地址 channel】 的映射缓存
 * 应在单Netty-client范围单例使用
 */
public class ChannelCache {

    // 使用线程安全的ConcurrentHashMap存储通道，例如：
    // Key: "127.0.0.1:8080"
    // Value: NioSocketChannel{id=xxxx}
    private final Map<String, Channel> channelMap;

    public ChannelCache(){
        channelMap = new ConcurrentHashMap<>();
    }

    /**
     * 根据网络地址获取缓存的通道
     * @param address 网络套接字地址
     * @return 活跃的通道，如果不存在或已失效则返回null
     */
    public Channel get(InetSocketAddress address){
        String key = address.toString();
        // *. 检查键是否非空
        if (key != null){
            Channel channel = channelMap.get(key);
            // *. 检查通道是否存在且处于活跃状态
            if (channel != null && channel.isActive()){
                return channel;
            }else {
                // *. 如果通道不存在或已失效，从缓存中移除
                remove(address);
            }
        }

        // *. 未找到有效通道，返回null
        return null;
    }

    // 存入通道
    public void set(InetSocketAddress address,Channel channel){
        channelMap.put(address.toString(),channel);
    }

    // 根据地址删除通道
    public void remove(InetSocketAddress address){
        channelMap.remove(address.toString());
    }
}
