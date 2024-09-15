package com.aki.rpc.consumer.config;

import com.aki.rpc.annontation.EnableHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

// 在配置类中传递扫包基路径给Rpc模块,具体传递使用@AkiHttpClient的注解
@EnableHttpClient(basePackage = "com.aki.rpc.consumer.rpc")
@Configuration
public class RestConfig {

    //定义restTemplate，spring提供
    ////发起http请求，传递参数，解析返回值（ Class<T> responseType）
    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
        // 当 Spring 启动时，会自动创建并管理一个 RestTemplate 对象。
        // 其他地方的 Spring 组件可以通过依赖注入来使用这个 RestTemplate。
    }

}
