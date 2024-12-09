package com.aki.rpc.netty.handler;

import com.aki.rpc.exception.AkiRpcException;
import com.aki.rpc.factory.SingletonFactory;
import com.aki.rpc.message.AkiRequest;
import com.aki.rpc.server.AkiServiceProvider;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @Auther akizora
 *
 */
@Slf4j
public class AkiRequestHandler {

    private AkiServiceProvider akiServiceProvider;

    public AkiRequestHandler(){
        akiServiceProvider = SingletonFactory.getInstance(AkiServiceProvider.class);
    }

    public Object handler(AkiRequest akiRequest) {
        // 1. 从请求中获取接口名称、版本号，并获取服务实例
        String interfaceName = akiRequest.getInterfaceName();
        String version = akiRequest.getVersion();
        Object service = akiServiceProvider.getService(interfaceName + version);

        // 2.实例判空
        if (service == null){
            throw new AkiRpcException("没有找到可用的服务提供方");
        }

        //3.通过invoke返回所调用方法执行结果
        try {
            Method method = service.getClass().getMethod(akiRequest.getMethodName(), akiRequest.getParamTypes());
            return method.invoke(service, akiRequest.getParameters());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            log.info("服务提供方 方法调用 出现问题:",e);
        }

        return null;
    }
}
