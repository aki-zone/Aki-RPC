package com.aki.rpc.utils;

/**
 * @Auther Akizora
 *  获取当前系统硬件层核心数的工具类
 */
public class RuntimeUtil {

    public static int cpus() {
        return Runtime.getRuntime().availableProcessors();
    }
}
