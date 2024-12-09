package com.aki.rpc.annotation;

import java.lang.annotation.*;

//
@Documented
@Inherited                                           // 备注解的子类将继承该注解
@Target({ElementType.CONSTRUCTOR,ElementType.FIELD}) // 限制注解只能用于构造器和字段上。
@Retention(RetentionPolicy.RUNTIME)                  // 注解在运行时依然有效，可通过反射访问。
public @interface AkiReference {
//    String host();                                 // 动态获取相关服务地址/端口
//    int port();
    String version() default "1.0";
}
