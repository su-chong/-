package com.atguigu.gulimall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.cart.feign.ProductFeignService;
import com.atguigu.gulimall.cart.interceptor.CartInterceptor;
import com.atguigu.gulimall.cart.service.CartService;
import com.atguigu.gulimall.cart.vo.Cart;
import com.atguigu.gulimall.cart.vo.CartItem;
import com.atguigu.gulimall.cart.vo.SkuInfoVo;
import com.atguigu.gulimall.cart.vo.UserInfoTo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CartServiceImpl implements CartService {

    @Autowired
    ThreadPoolExecutor executor;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;

    private final String CART_PREFIX = "gulimall:cart:";

    @Override
    public CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException {
        // 1. 把CartItem存到redis里
        // 1.1 往redis里存时，用的key是什么
        BoundHashOperations<String, Object, Object> boundHashOps = getCartOps();

        // 1.2 往redis里存时，用的value是什么。需要先查redis里是否已有这个东西
        String res= (String) boundHashOps.get(skuId.toString());

        // 1.2.case1 购物车本来没有这个东西
        if(StringUtils.isEmpty(res)) {
            CartItem cartItem = new CartItem();

            // (1) skuId, count, check → 已有
            cartItem.setSkuId(skuId);
            cartItem.setCount(num);
            cartItem.setCheck(true);

            // (2) title, image, price → 查SkuInfo表
            CompletableFuture<Void> getSkuInfoTask = CompletableFuture.runAsync(() -> {
                R r = productFeignService.getSkuInfo(skuId);
                SkuInfoVo skuInfo = r.getData("skuInfo", new TypeReference<SkuInfoVo>() {});
                cartItem.setTitle(skuInfo.getSkuTitle());
                cartItem.setImage(skuInfo.getSkuDefaultImg());
                cartItem.setPrice(skuInfo.getPrice());

            }, executor);

            // (3) skuAttr → 查SkuSaleAttrValue表
            CompletableFuture<Void> getSaleAttrValues = CompletableFuture.runAsync(() -> {
                List<String> values = productFeignService.getSaleAttrValuesAsStringList(skuId);
                cartItem.setSkuAttr(values);
            }, executor);

            // (4) 等这两个异步完成
            CompletableFuture.allOf(getSkuInfoTask,getSaleAttrValues).get();

            // 1.3 把(skuId,cartItem)存到redis里
            String s = JSON.toJSONString(cartItem);
            boundHashOps.put(skuId.toString(), s);

            // 2. 返回CartItem
            return cartItem;

        } else {
            // 1.2.case2 购物车本来有这个东西
            CartItem cartItem = JSON.parseObject(res, CartItem.class);
            cartItem.setCount(cartItem.getCount() + num);

            boundHashOps.put(skuId.toString(), JSON.toJSONString(cartItem));
            return cartItem;
        }

    }

    @Override
    public CartItem getCartItem(Long skuId) {
        BoundHashOperations<String, Object, Object> ops = getCartOps();
        String str = (String) ops.get(skuId.toString());
        CartItem cartItem = JSON.parseObject(str, CartItem.class);
        return cartItem;
    }

    /**
     * 获取cart。若未登录获取临时购物车，若已登录合临时购物车至帐号购物车
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Override
    public Cart getCart() throws ExecutionException, InterruptedException {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        Cart cart = new Cart();

        if(userInfoTo.getUserId() != null) {
            // 1. 已登录
            // 1.1 加临时购物车里的items至帐号购物车
            String tempCartKey = CART_PREFIX + userInfoTo.getUserKey();
            List<CartItem> tempCartItems = getCartItems(tempCartKey);
            if(tempCartItems != null && tempCartItems.size() > 0) {
                for (CartItem cartItem : tempCartItems) {
                    addToCart(cartItem.getSkuId(), cartItem.getCount());
                }
            }
            // 1.2 清空临时购物车
            clearCartInfo(tempCartKey);

            String cartKey = CART_PREFIX + userInfoTo.getUserId();
            List<CartItem> cartItems = getCartItems(cartKey);
            cart.setItems(cartItems);
        } else {
            // 2. 未登录
            String tempCartKey = CART_PREFIX + userInfoTo.getUserKey();
            List<CartItem> cartItems = getCartItems(tempCartKey);
            cart.setItems(cartItems);
        }
        return cart;
    }

    private BoundHashOperations<String, Object, Object> getCartOps() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();

        String cartKey = "";

        if(userInfoTo.getUserId() != null) {
            cartKey = CART_PREFIX + userInfoTo.getUserId();
        } else {
            cartKey = CART_PREFIX + userInfoTo.getUserKey();
        }

        BoundHashOperations<String, Object, Object> boundHashOps = redisTemplate.boundHashOps(cartKey);
        return boundHashOps;
    }

    /**
     * 获取某cartKey的购物车数据
     * @param cartKey
     * @return
     */
    private List<CartItem> getCartItems(String cartKey) {
        BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(cartKey);
        List<Object> values = ops.values();
        if(values != null && values.size()>0) {
            List<CartItem> collect = values.stream().map(obj -> {
                String s = (String) obj;
                CartItem item = JSON.parseObject(s, new TypeReference<CartItem>() {
                });
                return item;
            }).collect(Collectors.toList());
            return collect;
        }
        return null;
    }

    @Override
    public void clearCartInfo(String cartKey) {
        redisTemplate.delete(cartKey);
    }

    @Override
    public void checkItem(Long skuId, Integer check) {
        CartItem cartItem = getCartItem(skuId);
        cartItem.setCheck(check==1?true:false);
        String s = JSON.toJSONString(cartItem);
        BoundHashOperations<String, Object, Object> ops = getCartOps();
        ops.put(skuId.toString(), s);
    }

    @Override
    public void changeItemCount(Long skuId, Integer num) {
        CartItem cartItem = getCartItem(skuId);
        cartItem.setCount(num);
        BoundHashOperations<String, Object, Object> ops = getCartOps();
        ops.put(skuId.toString(), JSON.toJSONString(cartItem));
    }

    @Override
    public void deleteItem(Long skuId) {
        BoundHashOperations<String, Object, Object> ops = getCartOps();
        ops.delete(skuId.toString());
    }

    @Override
    public List<CartItem> getUserCartItems() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();

        if(userInfoTo.getUserId() == null) {
            return null;
        } else {
            String cartKey = CART_PREFIX + userInfoTo.getUserId();
            List<CartItem> cartItems = getCartItems(cartKey);
            List<CartItem> collect = cartItems.stream()
                    .filter(item -> item.getCheck())
                    .map(item -> {
                        // 更新最新price
                        BigDecimal price = productFeignService.getPrice(item.getSkuId());
                        item.setPrice(price);
                        return item;
                    }).collect(Collectors.toList());
            return collect;
        }
    }
}
