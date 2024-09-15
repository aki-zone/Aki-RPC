# Aki-Rpc Http模式实现 ver1.0

## 项目简介
Aki-Rpc 是一个简易的 RPC 框架，本分支通过 HTTP 协议实现远程方法调用。本项目实现了 `aki-rpc` 包中 `AkiHttpClient`、`AkiMapping` 及 `EnableHttpClient` 的 HTTP 模式，并且无需繁琐的配置，开发者可以直接使用。

## 特性
- 注解驱动的服务定义。
- 简化的 HTTP 请求和响应处理。
- 支持使用 Spring 依赖注入。
- 提供了基本的错误处理。

## 使用说明

### 1. 项目结构
```
.
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── yourcompany
│   │   │           └── rpc
│   │   └── resources
│   │       └── application.properties
│   └── test
├── pom.xml
└── README.md
```

### 2. 依赖配置
在 `pom.xml` 中加入所需的依赖：
```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>com.aki</groupId>
        <artifactId>aki-rpc</artifactId>
        <version>1.0-SNAPSHOT</version>
        <scope>compile</scope>
    </dependency>
    <!-- 其他依赖 -->
</dependencies>
```

### 3. Controller 示例
定义 `ProviderController` 用于生产者的执行业务。

```java
// `ProviderController.java`
package com.yourcompany.rpc.controller;

import com.yourcompany.rpc.service.GoodsService;
import com.yourcompany.rpc.model.Goods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("provider")
public class ProviderController {

    @Autowired
    private GoodsService goodsService;

    @GetMapping("goods/{id}")
    public Goods findGood(@PathVariable Long id) {
        return goodsService.findGoods(id);
    }
}
```

### 4. 配置文件
在 `application.properties` 中配置服务地址等信息。

```properties
# `application.properties`
server.port=7777
```

### 5. RPC 接口定义
定义 `GoodsHttpRpc` 接口以表明远程调用的方法。

```java
// `GoodsHttpRpc.java`
package com.yourcompany.rpc.client;

import com.yourcompany.rpc.annotations.AkiHttpClient;
import com.yourcompany.rpc.annotations.AkiMapping;
import com.yourcompany.rpc.model.Goods;
import org.springframework.web.bind.annotation.PathVariable;

@AkiHttpClient(value = "goodsHttpRpc")
public interface GoodsHttpRpc {

    @AkiMapping(url = "http://localhost:7777", api = "/provider/goods/{id}")
    Goods findGoods(@PathVariable Long id);
}
```

### 6. 另一个 Controller 示例
创建另一个 Controller 实现消费者的接口调用。

```java
// `ConsumerController.java`
package com.yourcompany.rpc.controller;

import com.yourcompany.rpc.client.GoodsHttpRpc;
import com.yourcompany.rpc.model.Goods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConsumerController {

    @Autowired
    private GoodsHttpRpc goodsHttpRpc;

    @GetMapping("/find/{id}")
    public Goods find(@PathVariable Long id) {
        return goodsHttpRpc.findGoods(id);
    }
}
```

## 开始使用
1. 启动 Spring Boot 应用。

   ```
   mvn spring-boot:run
   ```

2. 发送 HTTP 请求以使用 RPC 服务。

   ```
   GET http://localhost:7777/provider/goods/{id}
   ```

## 贡献
欢迎贡献！如发现任何问题或有改进建议，请打开问题或提交拉取请求。

## 许可证
本项目遵循 [MIT 许可证](LICENSE)。

## 感谢
感谢所有为本项目做出贡献的开源社区成员。