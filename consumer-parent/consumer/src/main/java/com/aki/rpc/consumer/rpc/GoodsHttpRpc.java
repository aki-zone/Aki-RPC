package com.aki.rpc.consumer.rpc;

import com.aki.rpc.annontation.AkiHttpClient;
import com.aki.rpc.annontation.AkiMapping;
import com.aki.rpc.provider.service.model.Goods;
import org.springframework.web.bind.annotation.PathVariable;

@AkiHttpClient(value = "goodsHttpRpc")
public interface GoodsHttpRpc {

    //
    @AkiMapping(url = "http://localhost:7777",api = "/provider/goods/{id}")
    Goods findGoods(@PathVariable Long id);
}
