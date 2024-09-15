package com.aki.rpc.provider.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Goods {

    //商品id
    private Long id;
    //商品名称
    private String goodsName;
    //商品价格
    private BigDecimal price;
}
