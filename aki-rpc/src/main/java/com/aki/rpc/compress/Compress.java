package com.aki.rpc.compress;

/**
 * @Auther akizora
 * 压缩类接口
 */
public interface Compress {
    /**
     * 压缩方法名称
     * @return
     */
     String name();
    /**
     * 压缩
     * @param bytes
     * @return
     */
    byte[] compress(byte[] bytes);

    /**
     * 解压缩
     * @param bytes
     * @return
     */
    byte[] decompress(byte[] bytes);
}
