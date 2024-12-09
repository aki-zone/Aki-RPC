package com.aki.rpc.provider;

import com.aki.rpc.annotation.EnableRpc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableRpc()
public class ProviderApp02 {

    public static void main(String[] args) {
        SpringApplication.run(ProviderApp02.class,args);
    }
}
