package io.agust.payment.controller;


import io.agust.common.pojo.po.GoodPO;
import io.agust.common.pojo.po.OrderPO;
import io.agust.payment.mapper.GoodsMapper;
import io.agust.payment.mapper.OrdersMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TradeController {

    private final GoodsMapper goodsMapper;
    private final OrdersMapper ordersMapper;

    /**
     * 获取商品列表
     *
     * @return
     */
    @GetMapping("/goods")
    public List<GoodPO> getGoods() {
        return goodsMapper.selectList(null);
    }

    /**
     * 获取订单列表
     *
     * @return
     */
    @GetMapping("/orders")
    public List<OrderPO> getOrders() {
        return ordersMapper.selectList(null);
    }

    @Transactional
    @PostMapping("/buy")
    public boolean buy(@RequestParam Integer goodsId) {
        GoodPO goodPO = goodsMapper.selectById(goodsId);
        int store = goodPO.getStore() - 1;
        if (store < 0) {
            return false;
        }
        Date date = new Date();
        OrderPO orderPO = new OrderPO();
        orderPO.setGoodsId(goodsId);
        orderPO.setCreateTime(date);
        orderPO.setName("购买" + goodPO.getName() + "订单");
        orderPO.setOrderId(new SimpleDateFormat("yyyyMMdd").format(date) + System.currentTimeMillis());
        orderPO.setTotal(goodPO.getPrice().multiply(BigDecimal.ONE));

        goodPO.setStore(store);
        return ordersMapper.insert(orderPO) > 0 && goodsMapper.updateById(goodPO) > 0;
    }
}
