package com.aki.rpc.netty.handler;

import com.aki.rpc.server.AkiServiceProvider;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程工场
 */
public class AkiRpcThreadFactory implements ThreadFactory {

    private AkiServiceProvider akiServiceProvider;

    private static final AtomicInteger poolNumber = new AtomicInteger(1);
    private final AtomicInteger threadNumber = new AtomicInteger(1);

    private final String namePrefix;

    private final ThreadGroup threadGroup;

    public AkiRpcThreadFactory(AkiServiceProvider akiServiceProvider) {
        // 1.缓存akiServiceProvider作为上下文备用
        this.akiServiceProvider = akiServiceProvider;

        // 2.检查是否存在安全管理器，安全管理器存在，使用安全管理器获取线程组，否则使用当前线程组
        SecurityManager securityManager = System.getSecurityManager();
        threadGroup = securityManager != null ? securityManager.getThreadGroup() :Thread.currentThread().getThreadGroup();

        // 3.生成类似 "aki-rpc-0-thread-", "aki-rpc-1-thread-" 这样的线程名称前缀
        namePrefix = "aki-rpc-" + poolNumber.getAndIncrement()+"-thread-";
    }


    /**
     * 接口重写，创建的线程以“N-thread-M”命名，N是该工厂的序号，M是线程号
     * @param runnable a runnable to be executed by new thread instance
     * @return
     */
    @Override
    public Thread newThread(Runnable runnable) {
        // 1.生成名如aki-rpc-0-thread-0的线程，线程栈大小类型为0，即系统默认(一般默认为1MB)
        Thread t = new Thread(threadGroup, runnable,
                namePrefix + threadNumber.getAndIncrement(), 0);
        // 2. 设置为守护线程
        t.setDaemon(true);
        // 3.设置线程优先级为标准优先级，NORM_PRIORITY 是 5（中等优先级）
        t.setPriority(Thread.NORM_PRIORITY);
        return t;
    }
}
