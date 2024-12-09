package com.aki.rpc.provider.service.impl;

import com.aki.rpc.annotation.AkiService;
import com.aki.rpc.provider.service.GoodsService;
import com.aki.rpc.provider.service.model.Goods;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

//在service的实现类加上注解，代表将GoodsService下的所有方法发布为函数服务
@Service
@AkiService(version = "1.0")
public class GoodsServiceImpl implements GoodsService {

    @Value("${server.port}")
    private int port;

    public Goods findGoods(Long id) {
        return new Goods(id,port+"服务提供方商品 : " ,BigDecimal.valueOf(100));
    }
}
