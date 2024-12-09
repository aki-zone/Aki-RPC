package com.aki.rpc.constant;

// 定义了一些用于通信协议的数据常量，这些常量通常用于定义消息头部的信息，确保消息在传输时可以被正确解析。
public class AkiRpcConstants {

    /**
     * 消息头的总长度（单位：字节）。
     * 4B  magic code（魔法数）   1B version（版本）     4B full length（消息长度）    1B messageType（消息类型）
     * 1B compress（压缩类型）    1B codec（序列化类型）  4B  requestId（请求的Id）
     * 总长度为 16 字节，包括魔法数 (MAGIC_NUMBER)、版本号 (VERSION)、消息长度字段等。
     * 用于确保在接收消息时可以完整解析消息头部。
     */
    public static final int TOTAL_LENGTH = 16;

    /**
     * 魔法数，用于标识消息的合法性。
     * - 值为 "kira" 的字节数组，即 4 个字节。
     * - 接收方解析消息时，首先检查前 4 个字节是否匹配该魔法数。
     * - 如果不匹配，通常认为消息非法或不符合该协议。
     */
    public static final byte[] MAGIC_NUMBER = {(byte)'k',(byte)'i',(byte)'r',(byte)'a'};

    /**
     * 协议版本号。
     * - 当前版本号为 1。
     * - 该字段可以用于区分不同版本的协议，便于后续扩展和兼容性处理。
     */
    public static final byte VERSION = 1;

    /**
     * 消息头部的固定长度（单位：字节）。
     * - 消息头包含魔数 (4 字节)、版本号 (1 字节)、预留字段或长度字段。
     * - 确保解析时可以确定消息体的位置。
     */
    public static final int HEAD_LENGTH = 16;

    /**
     * 心跳消息内容，用于维持长连接的活跃状态。
     * - "ping" 表示客户端向服务端发送的心跳检测包。
     * - 服务端接收到 "ping" 后通常会回复 "pong"。
     * - 这种机制可以防止连接因长时间无数据传输而被中断。
     */
    public static final String HEART_PING = "ping";

    /**
     * 心跳响应消息内容，用于维持长连接的活跃状态。
     * - "pong" 表示服务端对客户端心跳检测的回复。
     * - 客户端接收到 "pong" 后，可以确认连接仍然正常。
     */
    public static final String HEART_PONG = "pong";
}
