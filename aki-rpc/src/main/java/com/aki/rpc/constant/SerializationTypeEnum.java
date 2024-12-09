package com.aki.rpc.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 读取协议这的序列化类型，来此枚举进行匹配
 */
@AllArgsConstructor
@Getter
public enum SerializationTypeEnum {
    PROTO_STUFF((byte) 0x01, "protoStuff");     // ProtoStuff序列化类型

    private final byte code;
    private final String name;

    // 根据编码获取序列化类型的名称
    public static String getName(byte code) {
        for (SerializationTypeEnum c : SerializationTypeEnum.values()) {
            if (c.getCode() == code) {
                return c.name;
            }
        }
        return null;
    }

}
