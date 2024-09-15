package com.aki.rpc.annontation;

import com.aki.rpc.bean.AkiBeanDefinitionRegistry;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

// RPC扫包注解
@Target({ElementType.TYPE})
//定义作用范围:ElementType.TYPE表示可以标记在类型声明上

@Retention(RetentionPolicy.RUNTIME)
//定义生命周期:运行期可用，允许反射处理，允许bean管理

@Documented
//用于JavaDoc文档生成

@Inherited
@Import(AkiBeanDefinitionRegistry.class)
public @interface EnableHttpClient {
    // 扫包路径
    String basePackage();
}
