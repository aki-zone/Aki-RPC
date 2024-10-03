package com.aki.rpc.consumer.controller;

import com.aki.rpc.annontation.AkiReference;
import com.aki.rpc.consumer.rpc.GoodsHttpRpc;
import com.aki.rpc.provider.service.GoodsService;
import com.aki.rpc.provider.service.model.Goods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("consumer")
public class ConsumerController {

//    基础HTTP方式调用
//    @Autowired
//    private RestTemplate restTemplate;
//
//    @GetMapping("/find/{id}")
//    public Goods find(@PathVariable Long id){
//        Goods goods = restTemplate.getForObject("http://localhost:7777/provider/goods/"+id,Goods.class);
//        return goods;
//    }


//    // HTTP模式RPC调用
//    // 使用HTTP方式，是采用暴露一个controller接口，然后通过http调用
//    @Autowired
//    private GoodsHttpRpc goodsHttpRpc;
//
//    @GetMapping("/find/{id}")
//    public Goods find(@PathVariable Long id){
//        return goodsHttpRpc.findGoods(id);
//    }



    // TCP模式RPC调用，隐藏RPC层，直接调用远程服务goodsService
    @AkiReference(uri = "http://localhost:7777/", resultType = Goods.class)
    private GoodsService goodsService;
    @GetMapping("/find/{id}")
    public Goods find(@PathVariable Long id){
        return goodsService.findGoods(id);
    }


}
