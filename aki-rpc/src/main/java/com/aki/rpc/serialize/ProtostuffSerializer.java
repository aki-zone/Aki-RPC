package com.aki.rpc.serialize;

import com.aki.rpc.constant.SerializationTypeEnum;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Auther Akizora
 * @description Protostuff序列化器
 *
 */
public class ProtostuffSerializer implements Serializer{

    /**
     * LinkedBuffer 大小默认为512，按等容分节点式扩容
     * 避免每次序列化都重新申请Buffer空间,用来暂时存放对象序列化之后的数据
     * 如果你设置的空间不足，会自动扩展的，但这个大小还是要设置一个合适的值，设置大了浪费空间，设置小了会自动扩展浪费时间
     */
    private LinkedBuffer buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);

    /**
     * Schema记录一个对象的全方位具体信息
     * 缓存类对应的Schema，由于构造schema需要获得对象的类和字段信息，会用到反射机制
     * 这是一个很耗时的过程，因此进行缓存很有必要，下次遇到相同的类直接从缓存中get就行了
     * 存在则直接获取，不存在先生成再获取
     */
    private Map<Class<?>, Schema<?>> schemaCache = new ConcurrentHashMap<>();

    @Override
    public String name() {
        return SerializationTypeEnum.PROTO_STUFF.getName();
    }

    public byte[] serialize(Object obj) {
        // 1.获取对象的运行时类型
        Class clazz = obj.getClass();

        // 2.根据对象的类类型获取对应的 Schema。
        Schema schema = getSchema(clazz);

        // 3.创建序列化数组
        byte[] data;

        try {
            //4.序列化操作，将对象转换为字节数组
            data = ProtostuffIOUtil.toByteArray(obj, schema, buffer);
        } finally {
            //5.使用完清空buffer
            buffer.clear();
        }
        return data;
    }

    public Object deserialize(byte[] bytes, Class<?> clazz) {
        Schema schema = getSchema(clazz);
        Object obj = schema.newMessage();
        //反序列化操作，将字节数组转换为对应的对象
        ProtostuffIOUtil.mergeFrom(bytes, obj, schema);
        return obj;
    }

    /**
     * @description 获取Schema，若池中不存在则创建存入
     * @param clazz
     * @return [io.protostuff.Schema]
     */
    private Schema getSchema(Class clazz) {
        //首先尝试从Map缓存中获取类对应的schema
        Schema schema = schemaCache.get(clazz);
        if(Objects.isNull(schema)) {
            //新创建一个schema，RuntimeSchema就是将schema繁琐的创建过程封装了起来
            //它的创建过程是线程安全的,采用懒创建的方式，即当需要schema的时候才创建
            schema = RuntimeSchema.getSchema(clazz);
            if(Objects.nonNull(schema)) {
                //缓存schema，方便下次直接使用
                schemaCache.put(clazz, schema);
            }
        }
        return schema;
    }
}
