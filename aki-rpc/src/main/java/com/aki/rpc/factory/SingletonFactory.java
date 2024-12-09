package com.aki.rpc.factory;

import com.aki.rpc.server.AkiServiceProvider;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Auther Akizora
 * 对象的缓存列表，获取单例对象的工厂类
 */
public final class SingletonFactory {

    // 线程安全的对象池，Key采用对象序列值存储
    private static final Map<String, Object> OBJECT_MAP = new ConcurrentHashMap<>();

    private SingletonFactory() {
    }

    /**
     * 单例模式对象池的工厂方法
     * @param c 单例泛型类型
     * @return
     * @param <T>
     */
    public static <T> T getInstance(Class<T> c) {
        // 1.判断传入的类类型 c 是否为 null，如果是 null，则抛出异常
        if (c == null) {
            throw new IllegalArgumentException();
        }

        // 2.将传入的类对象转换为字符串形式，作为 key 使用
        String key = c.toString();

        // 3. 检查对象池中是否已存在该类的实例
        if (OBJECT_MAP.containsKey(key)) {
            // 3.1 如果已经存在，则直接从 OBJECT_MAP 中获取并返回该实例
            return c.cast(OBJECT_MAP.get(key));
        } else {
            // 3.2如果 OBJECT_MAP 中没有该类的实例，则尝试创建一个新的实例
            // 边缘修订，这里再做一次computeIfAbsent查重判断，保证实例绝对的单例
            return c.cast(OBJECT_MAP.computeIfAbsent(key, k -> {
                try {
                    // 使用反射调用无参构造函数来创建该类的新实例
                    return c.getDeclaredConstructor().newInstance();
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }));
        }
    }

//    public static void main(String[] args) {
//        //测试并发下 生成的单例是否唯一
//        ExecutorService executorService = Executors.newFixedThreadPool(100);
//
//        for (int i = 0 ; i< 100; i++) {
//            executorService.execute(new Runnable() {
//                @Override
//                public void run() {
//                    AkiServiceProvider instance = SingletonFactory.getInstance(AkiServiceProvider.class);
//                    System.out.println(instance);
//                }
//            });
//        }
//        while (true){
//
//        }
//    }
}
