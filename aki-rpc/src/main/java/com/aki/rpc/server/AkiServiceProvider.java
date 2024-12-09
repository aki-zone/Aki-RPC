package com.aki.rpc.server;

import com.alibaba.nacos.api.naming.pojo.Instance;
import com.aki.rpc.annotation.AkiService;
import com.aki.rpc.config.AkiRpcConfig;
import com.aki.rpc.exception.AkiRpcException;
import com.aki.rpc.factory.SingletonFactory;
import com.aki.rpc.netty.NettyServer;
import com.aki.rpc.register.nacos.NacosTemplate;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Auther Akizora
 * Nacos服务注册发布类，单例使用
 */
@Slf4j
public class AkiServiceProvider {

    @Setter
    @Getter
    private AkiRpcConfig akiRpcConfig;
    private final Map<String, Object> serviceMap;       // 全局服务本地映射表查询表，用于匹配interfaceName的bean查询其相关Method进行invoke操作
    private NacosTemplate nacosTemplate;

    // 初始化并发HashMap，并单例式获取Nacos工具类
    public AkiServiceProvider(){
        serviceMap = new ConcurrentHashMap<>();
        nacosTemplate = SingletonFactory.getInstance(NacosTemplate.class);
    }

    /**
     * 服务发布，并将本服务提供者装载进nettyServer单例中
     * @param akiService
     * @param service
     */
    public void publishService(AkiService akiService, Object service) {

        // 1. 注册服务到本地服务映射表
        registerService(akiService,service);

        // 2. 获取NettyServer单例
        NettyServer nettyServer = SingletonFactory.getInstance(NettyServer.class);

        // 3. 设置服务提供者
        // 此处给nettyServer设置了第一个，也是全局单例的akiServiceProvider
        nettyServer.setAkiServiceProvider(this);

        // *启动nettyServer 这个方法可能会调用多次，服务只能启动一次
        if (!nettyServer.isRunning()){
            nettyServer.run();
        }
    }

    /**
     * 服务注册方法
     * @param akiService
     * @param service
     */
    private void registerService(AkiService akiService, Object service) {
        // 1.获取服务版本
        String version = akiService.version();

        // 2. 获取服务接口的规范名称，类似"com.aki.provider.GoodService"
        String interfaceName = service.getClass().getInterfaces()[0].getCanonicalName();


        // 3.将服务存储到本地映射表中（接口名+版本作为key）
        serviceMap.put(interfaceName+version,service);

        // 4.同步注册到Nacos中
        //group 只有在同一个组内 调用关系才能成立，不同的组之间是隔离的
        if (akiRpcConfig == null){
            throw new AkiRpcException("必须开启EnableRPC");
        }
        try {
            // 4.1 创建Nacos服务实例
            Instance instance = new Instance();
            instance.setIp(InetAddress.getLocalHost().getHostAddress());            // 本地IP
            instance.setPort(akiRpcConfig.getProviderPort());                       // 服务端口
            instance.setClusterName(akiRpcConfig.getNacosGroup());                                     // 集群名称
            instance.setServiceName(interfaceName+version);                         // 服务名称

            // 4.2 注册进Nacos,群组默认为aki-rpc
            nacosTemplate.registerServer(akiRpcConfig.getNacosGroup(),instance);
            log.info("发布了服务:{}",interfaceName+version);
            log.info("- 地址:{}，端口:{}，集群:{}",instance.getIp(),instance.getPort(),instance.getClusterName());
        }catch (Exception e){
            log.error("nacos注册失败:",e);
        }
    }

    /**
     * 根据服务名称从本地缓存表单获取服务实例
     * @param serviceName
     * @return
     */
    public Object getService(String serviceName){
        return serviceMap.get(serviceName);
    }

}
