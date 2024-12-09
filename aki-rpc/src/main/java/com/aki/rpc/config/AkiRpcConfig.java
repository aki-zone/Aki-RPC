package com.aki.rpc.config;

import lombok.Data;
// Aki-Rpc服务单例部署/集群部署相关配置类
@Data
public class AkiRpcConfig {

    private String nacosHost = "localhost";

    private int nacosPort = 8848;

    private int providerPort = 0;

    private String nacosGroup = "aki-rpc";
}
