package com.aki.rpc.consumer.controller;

import com.aki.rpc.consumer.rpc.GoodsHttpRpc;
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


    // RPC模式调用
    @Autowired
    private GoodsHttpRpc goodsHttpRpc;

    @GetMapping("/find/{id}")
    public Goods find(@PathVariable Long id){
        return goodsHttpRpc.findGoods(id);
    }

}
