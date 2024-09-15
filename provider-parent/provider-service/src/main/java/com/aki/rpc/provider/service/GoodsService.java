package com.aki.rpc.provider.service;

import com.aki.rpc.provider.service.model.Goods;

public interface GoodsService {

    /**
     * 根据商品id 查询商品
     * @param id
     * @return
     */

    // 此为生产者远程提供的服务函数
    Goods findGoods(Long id);
}
