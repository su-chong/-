package com.atguigu.gulimall.cart.service;

import com.atguigu.gulimall.cart.vo.Cart;
import com.atguigu.gulimall.cart.vo.CartItem;

import java.util.List;
import java.util.concurrent.ExecutionException;

public interface CartService {
    /**
     * 添加cartItem至cart
     * @param skuId
     * @param num
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException;

    /**
     * 获取cart中的某个cartItem
     * @param skuId
     * @return
     */
    CartItem getCartItem(Long skuId);

    /**
     * 获取cart
     * @return
     */
    Cart getCart() throws ExecutionException, InterruptedException;

    /**
     * 清空cart
     * @param cartKey
     */
    void clearCartInfo(String cartKey);

    /**
     * 改check状态
     * @param skuId
     * @param check
     */
    void checkItem(Long skuId, Integer check);

    /**
     * 改count
     * @param skuId
     * @param num
     */
    void changeItemCount(Long skuId, Integer num);

    /**
     * 删除购物项
     * @param skuId
     */
    void deleteItem(Long skuId);

    /**
     * 返回check=true的所有CartItems。注意要更新price。
     * cartItem里的price是加入购物车时的price，结算时要用skuInfoEntity里的最新price。
     * @return
     */
    List<CartItem> getUserCartItems();
}
