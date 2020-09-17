package com.leyou.cart.service;

import com.leyou.cart.client.GoodsClient;
import com.leyou.cart.interceptor.LoginInterceptor;
import com.leyou.cart.pojo.Cart;
import com.leyou.common.pojo.UserInfo;
import com.leyou.common.utils.JsonUtils;
import com.leyou.item.pojo.Sku;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartService {

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private StringRedisTemplate template;

    private static final String KEY_PREFIX = "user:cart:";

    public void addCart(Cart cart) {
        //获取用户信息
        UserInfo userInfo = LoginInterceptor.getUserInfo();

        //查询购物车记录
        BoundHashOperations<String, Object, Object> hashOperations = this.template.boundHashOps(KEY_PREFIX + userInfo.getId());

        String key = cart.getSkuId().toString();
        Integer num = cart.getNum();

        //判断当前的商品是否在购物车中
        if (hashOperations.hasKey(key)) {
            //在,更新数量
            String cartJson = hashOperations.get(key).toString();//字符类型的json对象

            //对cartJson进行反序列化
            cart = JsonUtils.parse(cartJson, Cart.class);
            cart.setNum(cart.getNum() + num);
        } else {
            //不在，新增购物车
            Sku sku = this.goodsClient.querySkuBySkuId(cart.getSkuId());

            cart.setUserId(userInfo.getId());
            cart.setTitle(sku.getTitle());
            cart.setOwnSpec(sku.getOwnSpec());
            cart.setImage(StringUtils.isBlank(sku.getImages()) ? "" : StringUtils.split(sku.getImages(), ",")[0]);
            cart.setPrice(sku.getPrice());
        }
        hashOperations.put(key, JsonUtils.serialize(cart));
    }

    public List<Cart> queryCarts() {
        UserInfo userInfo = LoginInterceptor.getUserInfo();

        //判断用户是否有购物车记录
        if (this.template.hasKey(KEY_PREFIX + userInfo.getId())) {
            return null;
        }
        //获取用户的购物车记录
        BoundHashOperations<String, Object, Object> hashOperations = this.template.boundHashOps(KEY_PREFIX + userInfo.getId());

        //获取购物车所有map中的Cart集合
        List<Object> cartsJson = hashOperations.values();

        //如果购物车集合为空，直接返回空
        if (CollectionUtils.isEmpty(cartsJson)) {
            return null;
        }

        //把List<Object>集合转化为List<Cart>集合
        return cartsJson.stream().map(cartJson -> JsonUtils.parse(cartsJson.toString(), Cart.class)).collect(Collectors.toList());
    }
}
