package com.aki.rpc.provider;

import com.aki.rpc.annotation.EnableRpc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableRpc(
        nacosHost = "Dasd",  //
        nacosPort = 0,
        nacosGroup = "",
        serverPort = 0
)
public class ProviderApp {

    public static void main(String[] args) {
        SpringApplication.run(ProviderApp.class,args);
    }
}
