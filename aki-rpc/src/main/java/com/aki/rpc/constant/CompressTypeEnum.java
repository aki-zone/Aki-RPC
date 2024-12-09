package com.aki.rpc.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用于读取协议中的压缩类型，并与此枚举进行匹配
 */
@AllArgsConstructor
@Getter
public enum CompressTypeEnum {
	//读取协议这的压缩类型，来此枚举进行匹配
    GZIP((byte) 0x01, "gzip"),       // gzip压缩类型
    OTHER((byte) 0x02, "other");     // 拓展位，其他压缩类型

    private final byte code;
    private final String name;

    // 将字节码解析为响应类型字符串
    public static String getName(byte code) {
        for (CompressTypeEnum c : CompressTypeEnum.values()) {
            if (c.getCode() == code) {
                return c.name;
            }
        }
        return null;
    }

}
