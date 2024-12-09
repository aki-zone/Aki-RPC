package com.aki.rpc.proxy;


import com.aki.rpc.annotation.AkiReference;
import com.aki.rpc.exception.AkiRpcException;
import com.aki.rpc.message.AkiRequest;
import com.aki.rpc.message.AkiResponse;
import com.aki.rpc.netty.client.NettyClient;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

//每一个动态代理类的调用处理程序都必须实现InvocationHandler接口，
// 并且每个代理类的实例都关联到了实现该接口的动态代理类调用处理程序中，
// 当我们通过动态代理对象调用一个方法时候，
// 这个方法的调用就会被转发到实现InvocationHandler接口类的invoke方法来调用
@Slf4j
public class AkiRpcClientProxy implements InvocationHandler {


    public AkiRpcClientProxy(){

    }
    private AkiReference akiReference;
    private NettyClient nettyClient;
    

    public AkiRpcClientProxy(AkiReference akiReference,NettyClient nettyClient) {
        this.akiReference = akiReference;
        this.nettyClient = nettyClient;
    }

    /**
     * proxy:代理类代理的真实代理对象com.sun.proxy.$Proxy0
     * method:我们所要调用某个对象真实的方法的Method对象
     * args:指代代理对象方法传递的参数
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //实现业务，向服务提供方发起网络请求，获取结果 并返回
        log.info("rpc 服务消费方 发起了调用..... invoke调用了");
        //1. 构建请求数据AkiRequest
        String version = akiReference.version();
        AkiRequest akiRequest = AkiRequest.builder()
                .group("aki-rpc")
                .interfaceName(method.getDeclaringClass().getName())
                .methodName(method.getName())
                .version(version)
                .parameters(args)
                .paramTypes(method.getParameterTypes())
                .requestId(UUID.randomUUID().toString())
                .build();

        //2. 通过客户端向服务端发送请求，并返回一个CompletableFuture异步结果
        Object sendRequest = nettyClient.sendRequest(akiRequest);
        CompletableFuture<AkiResponse<Object>> resultCompletableFuture = (CompletableFuture<AkiResponse<Object>>) sendRequest;

        //3. 接收数据，判定异常
        AkiResponse<Object> akiResponse = resultCompletableFuture.get();
        if (akiResponse == null){
            throw new AkiRpcException("服务调用失败");
        }
        if (!akiRequest.getRequestId().equals(akiResponse.getRequestId())){
            throw new AkiRpcException("响应结果和请求不一致");
        }
        return akiResponse.getData();
    }

    /**
     * get the proxy object
     */
    @SuppressWarnings("unchecked")
    public <T> T getProxy(Class<T> clazz) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[]{clazz}, this);
    }

}