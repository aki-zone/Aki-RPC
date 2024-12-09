package com.aki.rpc.message;

import lombok.*;

import java.io.Serializable;

/**
 * @Auther akizora
 * RPC调用方请求消息体
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@ToString
public class AkiRequest implements Serializable {
    private String requestId;        // 请求唯一标识，用于追踪或匹配请求与响应
    private String interfaceName;    // 接口名称，表示被调用的接口全限定名（例如：com.example.MyService）
    private String methodName;       // 方法名称，表示接口中的具体方法名（例如：getUserById）
    private Object[] parameters;     // 方法参数列表，表示调用方法时传递的参数
    private Class<?>[] paramTypes;   // 方法参数类型列表，用于服务端根据类型进行方法匹配
    private String version;          // 版本号，用于区分接口的不同版本
    private String group;            // 分组标识，用于区分接口的不同分组，便于服务隔离或多实现区分
}
