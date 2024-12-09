package com.aki.rpc.netty.client;
import com.aki.rpc.message.AkiRequest;

/**
 * @Auther akizora
 * netty 客户端接口
 */

public interface AkiClient {


    /**
     * 发送请求，并接收数据
     * @param akiRequest
     * @return
     */
    Object sendRequest(AkiRequest akiRequest);
}
