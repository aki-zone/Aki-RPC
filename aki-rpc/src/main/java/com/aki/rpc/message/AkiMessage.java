package com.aki.rpc.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Auther akizora
 * Rpc-TCP链路层消息传输体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AkiMessage {
    // RPC调用消息类型
    private byte messageType;
    // 序列化类型
    private byte codec;
    // 压缩类型
    private byte compress;
    // 请求ID
    private int requestId;
    // 请求数据
    private Object data;

}
