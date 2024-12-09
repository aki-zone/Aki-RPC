package com.aki.rpc.provider.service.impl;

import com.aki.rpc.annotation.AkiService;
import com.aki.rpc.provider.service.OrderService;
import com.aki.rpc.provider.service.model.Goods;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
//把GoodsService这个服务 发布，消费方 就可以进行调用了
@AkiService(version="1.0")
public class OrderServiceImpl implements OrderService {
    @Value("${server.port}")
    private int port;

    public Goods findGoods(Long id) {
        return new Goods(id,port+"服务提供方订单商品", BigDecimal.valueOf(99));
    }
}
