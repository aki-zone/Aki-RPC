package com.aki.rpc.serialize;


/**
 * @Auther akizora
 * 序列化接口，所有序列化类都要实现这个接口
 */
public interface Serializer {
    /**
     * 使用的序列化名称
     * @return
     */
    String name();
    /**
     * 序列化
     *
     * @param obj 要序列化的对象
     * @return 字节数组
     */
    byte[] serialize(Object obj);

    /**
     * 反序列化
     *
     * @param bytes 序列化后的字节数组
     * @param clazz 目标类
     * @return 反序列化的对象
     */
    Object deserialize(byte[] bytes, Class<?> clazz);
}
