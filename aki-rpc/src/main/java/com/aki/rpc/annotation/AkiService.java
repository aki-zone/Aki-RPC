package com.aki.rpc.annotation;

import java.lang.annotation.*;

//@AkiService来标识需要发布的服务
@Documented
@Inherited                           // 备注解的子类将继承该注解
@Retention(RetentionPolicy.RUNTIME)  // 限制注解只能用于构造器和字段上。
@Target({ElementType.TYPE})          // 注解在运行时依然有效，可通过反射访问。
public @interface AkiService {
    String version() default "1.0";
}
