package com.aki.rpc.register.nacos;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;

import java.util.List;

/**
 * Nacos注册器模板
 */
public class NacosTemplate {

    private ConfigService configService;    //主要用作配置方面的管理功能
    private NamingService namingService;    //主要用作服务方面的管理功能

    public NacosTemplate(){

    }

    /**
     * 根据地址和端口初始化指定Nacos服务空间
     * @param host
     * @param port
     */
    public void init(String host,int port){
        try {
            namingService = NacosFactory.createNamingService(host+":"+port);
        } catch (NacosException e) {
            e.printStackTrace();
        }
    }
    
    //注册服务，服务组是DEFAULT_GROUP
    public void registerServer(Instance instance) throws Exception{
        namingService.registerInstance(instance.getServiceName(),instance);
    }

    //注册服务，指定服务组
    public void registerServer(String groupName,Instance instance) throws Exception{
        namingService.registerInstance(instance.getServiceName(),groupName,instance);
    }

    //删除服务
    public void deleteServer(Instance instance) throws Exception{
        namingService.deregisterInstance(instance.getServiceName(),instance.getIp(),instance.getPort());
    }

    //随机全部（有可能获取到的不健康）。拿到全部实例后，我们可以按照自己的负载均衡算法进行调用。
    public List<Instance> getAllServer(String serverName) throws Exception{
        return namingService.getAllInstances(serverName);
    }

    //根据负载均衡算法获取一个健康的实例
    public Instance getOneHealthyInstance(String serverName) throws Exception{
        return namingService.selectOneHealthyInstance(serverName);
    }

    //根据负载均衡算法获取一个健康的实例
    public Instance getOneHealthyInstance(String groupName,String serverName) throws Exception{
        return namingService.selectOneHealthyInstance(serverName,groupName);
    }

}
