package com.aki.rpc.provider.controller;

import com.aki.rpc.provider.service.GoodsService;
import com.aki.rpc.provider.service.model.Goods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("provider")
public class ProviderController {

    @Autowired
    private GoodsService goodsService;

    @GetMapping("goods/{id}")
    public Goods findGood(@PathVariable Long id){
        return goodsService.findGoods(id);
    }

}
