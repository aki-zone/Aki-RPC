package com.aki.rpc.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用于读取消息体中的数据类型，并与此枚举进行匹配
 */
@AllArgsConstructor
@Getter
public enum MessageTypeEnum {

    REQUEST((byte) 0x01, "request"),            // 请求消息类型
    RESPONSE((byte) 0x02, "response"),          // 响应消息类型
    HEARTBEAT_PING((byte) 0x03, "heart ping"),  // 心跳检测请求消息类型
    HEARTBEAT_PONG((byte) 0x04, "heart pong");  // 心跳检测响应消息类型

    private final byte code;
    private final String name;

    // 将字节码解析为响应类型字符串
    public static String getName(byte code) {
        for (MessageTypeEnum c : MessageTypeEnum.values()) {
            if (c.getCode() == code) {
                return c.name;
            }
        }
        return null;
    }

}
