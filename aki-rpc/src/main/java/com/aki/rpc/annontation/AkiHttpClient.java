package com.aki.rpc.annontation;

import java.lang.annotation.*;

// 定义单例模式bean管理的注解

@Target({ElementType.TYPE})
//定义作用范围:ElementType.TYPE表示可以标记在类型声明上

@Retention(RetentionPolicy.RUNTIME)
//定义生命周期:运行期可用，允许反射处理，允许bean管理

@Documented
//用于JavaDoc文档生成

@Inherited
//表示这个注解可以 被子类继承

// 注解：定义使用AkiRpc的实例
public @interface AkiHttpClient {

    // bean的名称
    // 无默认值，为必填
    String value();
}
