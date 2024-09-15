package com.aki.rpc.provider.service.impl;

import com.aki.rpc.provider.service.GoodsService;
import com.aki.rpc.provider.service.model.Goods;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class GoodsServiceImpl implements GoodsService {

    public Goods findGoods(Long id) {
        return new Goods(id,"服务提供方商品", BigDecimal.valueOf(100));
    }
}
