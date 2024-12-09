package com.aki.rpc.message;

import com.aki.rpc.exception.AkiRpcException;


import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Auther Akizora
 * 线程安全的存储请求id-异步任务的映射
 */
public class UnprocessedRequests {

    // 使用线程安全的ConcurrentHashMap来存储未处理的请求。Map的key是请求的requestId，value是一个CompletableFuture对象，用于异步获取响应结果。
    private static final Map<String, CompletableFuture<AkiResponse<Object>>> UP = new ConcurrentHashMap<>();

    // put方法用于将一个未处理的请求（及其对应的CompletableFuture）存入UP映射中。
    public void put(String requestId,CompletableFuture<AkiResponse<Object>> resultFuture){
        UP.put(requestId,resultFuture);
    }

    // complete方法用于将响应数据（AkiResponse对象）与相应的CompletableFuture关联起来，完成异步操作。
    public CompletableFuture<AkiResponse<Object>> complete(AkiResponse<Object> akiResponse){
        // 清除相应
        CompletableFuture<AkiResponse<Object>> completableFuture = UP.remove(akiResponse.getRequestId());
        if (completableFuture != null){
            completableFuture.complete(akiResponse);
            return completableFuture;
        }
        throw new AkiRpcException("获取结果数据出现问题");
    }
}
