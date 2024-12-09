package com.aki.rpc.consumer.controller;

import com.aki.rpc.annotation.AkiReference;
import com.aki.rpc.provider.service.GoodsService;
import com.aki.rpc.provider.service.model.Goods;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("consumer")
public class ConsumerController {

    // TCP模式RPC调用，隐藏RPC层，直接调用远程服务goodsService
    @AkiReference(version = "1.0")
    private GoodsService goodsService;
    @GetMapping("/find/{id}")
    public Goods find(@PathVariable Long id){
        return goodsService.findGoods(id);
    }
}
