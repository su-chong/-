package com.atguigu.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.to.es.SkuHasStockVo;
import com.atguigu.common.to.mq.OrderTo;
import com.atguigu.common.to.mq.SeckillOrderTo;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberRespVo;
import com.atguigu.gulimall.order.constant.OrderConstant;
import com.atguigu.gulimall.order.dao.OrderDao;
import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.entity.OrderItemEntity;
import com.atguigu.gulimall.order.entity.PaymentInfoEntity;
import com.atguigu.gulimall.order.enume.OrderStatusEnum;
import com.atguigu.gulimall.order.feign.CartFeignService;
import com.atguigu.gulimall.order.feign.MemberFeignService;
import com.atguigu.gulimall.order.feign.ProductFeignService;
import com.atguigu.gulimall.order.feign.WareFeignService;
import com.atguigu.gulimall.order.interceptor.LoginUserInterceptor;
import com.atguigu.gulimall.order.service.OrderItemService;
import com.atguigu.gulimall.order.service.OrderService;
import com.atguigu.gulimall.order.service.PaymentInfoService;
import com.atguigu.gulimall.order.to.OrderCreateTo;
import com.atguigu.gulimall.order.vo.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Slf4j
@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    @Autowired
    PaymentInfoService paymentInfoService;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    OrderItemService orderItemService;

    @Autowired
    ProductFeignService productFeignService;

    private ThreadLocal<OrderSubmitVo> submitVoThreadLocal = new ThreadLocal<>();

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    WareFeignService wareFeignService;

    @Autowired
    MemberFeignService memberFeignService;

    @Autowired
    CartFeignService cartFeignService;

    @Autowired
    ThreadPoolExecutor executor;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    // 生成结算页(confirm.html)需要的数据
    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        MemberRespVo member = LoginUserInterceptor.loginUser.get();
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        // 新建OrderConfirmVo
        OrderConfirmVo confirmVo = new OrderConfirmVo();

        CompletableFuture<Void> getAddressFuture = CompletableFuture.runAsync(() -> {
            // 获取主线程的request context
            RequestContextHolder.setRequestAttributes(requestAttributes);
            // 填List<MemberAddressVo>字段
            List<MemberAddressVo> address = memberFeignService.getAddress(member.getId());
            confirmVo.setAddress(address);
        }, executor);


        CompletableFuture<Void> cartFuture = CompletableFuture.runAsync(() -> {
            RequestContextHolder.setRequestAttributes(requestAttributes);
            // 填List<OrderItemVo>字段
            List<OrderItemVo> currentUserCartItems = cartFeignService.getCurrentUserCartItems();
            confirmVo.setItems(currentUserCartItems);
        }, executor).thenRunAsync(() -> {
            // 查stocks字段
            List<Long> skuIds = confirmVo.getItems().stream().map(OrderItemVo::getSkuId).collect(Collectors.toList());
            RequestContextHolder.setRequestAttributes(requestAttributes);
            R r = wareFeignService.getSkusHasStock(skuIds);
            List<SkuHasStockVo> stockVos = r.getData(new TypeReference<List<SkuHasStockVo>>() {
            });
            Map<Long, Boolean> collect = stockVos.stream().collect(Collectors.toMap(SkuHasStockVo::getSkuId, SkuHasStockVo::getHasStock));
            confirmVo.setStocks(collect);
        }, executor);

        // 填integration字段
        Integer integration = member.getIntegration();
        confirmVo.setIntegration(integration);

        // total字段自动计算
        // payPrice字段自动计算

        // 填token字段
        String token = UUID.randomUUID().toString().replace("-", "");
        confirmVo.setOrderToken(token);
        redisTemplate.opsForValue().set(OrderConstant.SUBMIT_ORDER_TOKEN_PREFIX + member.getId(), token);

        CompletableFuture.allOf(getAddressFuture, cartFuture).get();

        return confirmVo;
    }

    //    @GlobalTransactional
    @Transactional
    @Override
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo vo) {
        // 把OrderSubmitVo放到ThreadLocal里
        submitVoThreadLocal.set(vo);

        SubmitOrderResponseVo response = new SubmitOrderResponseVo();
        response.setCode(0);

        // 1. 原子验证token
        // ① 从Redis里查token
        MemberRespVo member = LoginUserInterceptor.loginUser.get();
        String cartKey = OrderConstant.SUBMIT_ORDER_TOKEN_PREFIX + member.getId();
        String orderToken1 = redisTemplate.opsForValue().get(cartKey);

        // ② 结算页提交来的token
        String orderToken2 = vo.getOrderToken();

        // ③ redis脚本
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

        // ④ 原子验证orderToken
        Long result = redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList(cartKey), orderToken2);

        if (result == 0L) {
            // token验证失败，错误代码为1
            response.setCode(1);
            return response;
        } else {
            // token验证成功

            // 2. 生成订单(OrderCreateTo)
            OrderCreateTo order = createOrder();

            // 3. 验价
            BigDecimal payPrice = vo.getPayPrice();
            BigDecimal payAmount = order.getOrderEntity().getPayAmount();
            if (Math.abs(payAmount.subtract(payPrice).doubleValue()) < 0.01) {
                // 3.1 验价成功

                // 4. 保存到数据库
                saveOrder(order);

                // 5. 锁库存
                WareSkuLockVo lockVo = new WareSkuLockVo();
                lockVo.setOrderSn(order.getOrderEntity().getOrderSn());
                List<OrderItemVo> orderItemVos = order.getEntities().stream().map(item -> {
                    OrderItemVo orderItemVo = new OrderItemVo();
                    orderItemVo.setSkuId(item.getSkuId());
                    orderItemVo.setCount(item.getSkuQuantity());
                    orderItemVo.setTitle(item.getSpuName());
                    return orderItemVo;
                }).collect(Collectors.toList());
                lockVo.setLocks(orderItemVos);
                R r = wareFeignService.orderLockStock(lockVo);

                if (r.getCode() == 0) {
                    // 锁定成功
                    response.setOrderEntity(order.getOrderEntity());
                    // 出错
                    // int i = 10 / 0;

                    // 6. 给RabbitMQ发消息
                    rabbitTemplate.convertAndSend("order-event-exchange", "order.create.order", order.getOrderEntity());
                    return response;
                } else {
                    // 锁定失败
                    response.setCode(3);
                    return response;
                }

            } else {
                // 3.2 验价失败，错误代码为2
                response.setCode(2);
                return response;
            }
        }
    }

    @Override
    public OrderEntity getOrderByOrderSn(String orderSn) {
        OrderEntity orderEntity = this.getOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));
        return orderEntity;
    }

    /**
     * 关闭订单
     * 条件：订单状态是"未付款"(即OrderEntity.status=1)
     * 做法：① 改OrderEntity.status;② 发消息给order.release.order.queue
     *
     * @param orderEntity
     */
    @Override
    public void closeOrder(OrderEntity orderEntity) {
        // 只在订单状态是"待付款"时关单
        Long id = orderEntity.getId();
        OrderEntity byId = this.getById(id);
        if (byId.getStatus() == OrderStatusEnum.CREATE_NEW.getCode()) {
            OrderEntity update = new OrderEntity();
            update.setId(id);
            update.setStatus(OrderStatusEnum.CANCLED.getCode());
            this.updateById(update);
        }
        // 发消息给order.release.other
        OrderTo orderTo = new OrderTo();
        BeanUtils.copyProperties(byId, orderTo);
        rabbitTemplate.convertAndSend("order-event-exchange", "order.release.other", orderTo);
    }


    // 方法4:保存OrderEntity和OrderItemEntity到数据库
    private void saveOrder(OrderCreateTo order) {
        // 保存OrderEntity
        OrderEntity orderEntity = order.getOrderEntity();
        orderEntity.setModifyTime(new Date());
        this.save(orderEntity);

        // 保存OrderItemEntity
        List<OrderItemEntity> entities = order.getEntities();
        orderItemService.saveBatch(entities);
    }

    // 此method用来建OrderCreateTo
    private OrderCreateTo createOrder() {
        OrderCreateTo orderCreateTo = new OrderCreateTo();
        // 填orderEntity字段
        OrderEntity orderEntity = buildOrderEntity();
        orderCreateTo.setOrderEntity(orderEntity);

        // 填List<OrderItemEntity>字段
        List<OrderItemEntity> orderItemEntities = buildOrderItemEntities(orderEntity.getOrderSn());
        orderCreateTo.setEntities(orderItemEntities);

        // 补填orderEntity
        computePrice(orderEntity, orderItemEntities);

        // 填payPrice字段

        // 填fare字段

        return orderCreateTo;
    }


    // 子方法1:此method用来建OrderEntity
    private OrderEntity buildOrderEntity() {
        OrderEntity orderEntity = new OrderEntity();

        // 填memberId字段
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        orderEntity.setMemberId(memberRespVo.getId());

        // 填orderSn字段
        String timeId = IdWorker.getTimeId();
        orderEntity.setOrderSn(timeId);

        // 填freightAmount字段
        OrderSubmitVo orderSubmitVo = submitVoThreadLocal.get();
        R fare = wareFeignService.getFare(orderSubmitVo.getAddrId());
        FareVo fareVo = fare.getData(new TypeReference<FareVo>() {
        });
        orderEntity.setFreightAmount(fareVo.getFare());

        // 填receiveName,receivePhone,receivePostCode,receiveProvince,receiveCity,receiveRegion,receiveDetailAddress字段
        MemberAddressVo address = fareVo.getAddress();
        orderEntity.setReceiverName(address.getName());
        orderEntity.setReceiverPhone(address.getPhone());
        orderEntity.setReceiverPostCode(address.getPostCode());
        orderEntity.setReceiverProvince(address.getProvince());
        orderEntity.setReceiverCity(address.getCity());
        orderEntity.setReceiverCity(address.getRegion());
        orderEntity.setReceiverDetailAddress(address.getDetailAddress());

        // 填autoConfirmDay字段
        orderEntity.setAutoConfirmDay(7);

        // 填status字段(订单状态)
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());

        // 填deleteStatus字段
        orderEntity.setDeleteStatus(0);

        return orderEntity;
    }

    // 子方法2:此method用来建List<OrderItemEntity>
    private List<OrderItemEntity> buildOrderItemEntities(String orderSn) {
        List<OrderItemVo> cartItems = cartFeignService.getCurrentUserCartItems();
        if (cartItems != null && cartItems.size() > 0) {
            return cartItems.stream().map(item -> buildOrderItemEntity(item, orderSn)).collect(Collectors.toList());
        }
        return null;
    }

    // 子方法2.1:此method用来建OrderItemEntity,辅助子方法2
    private OrderItemEntity buildOrderItemEntity(OrderItemVo orderItemVo, String orderSn) {

        OrderItemEntity orderItemEntity = new OrderItemEntity();

        // 1. 填orderSn
        orderItemEntity.setOrderSn(orderSn);

        // 2. 填spuId,spuName,spuPic,spuBrand,categoryId
        R r = productFeignService.getSpuInfoBySkuId(orderItemVo.getSkuId());
        SpuInfoVo spuInfoVo = r.getData(new TypeReference<SpuInfoVo>() {
        });
        orderItemEntity.setSpuId(spuInfoVo.getId());
        orderItemEntity.setSpuName(spuInfoVo.getSpuName());
        orderItemEntity.setSpuPic(spuInfoVo.getSpuDescription());
        orderItemEntity.setSpuBrand(spuInfoVo.getBrandId().toString());
        orderItemEntity.setCategoryId(spuInfoVo.getCatalogId());

        // 3. 填skuId,skuName,skuPic,skuPrice,skuQuantity,skuAttrsVals
        orderItemEntity.setSkuId(orderItemVo.getSkuId());
        orderItemEntity.setSkuName(orderItemVo.getTitle());
        orderItemEntity.setSkuPic(orderItemVo.getImage());
        orderItemEntity.setSkuPrice(orderItemVo.getPrice());
        orderItemEntity.setSkuQuantity(orderItemVo.getCount());
        String s = StringUtils.collectionToDelimitedString(orderItemVo.getSkuAttr(), ";");
        orderItemEntity.setSkuAttrsVals(s);

        //  4. 填promotionAmount,couponAmount,integrationAmount,realAmount
        orderItemEntity.setPromotionAmount(new BigDecimal(0));
        orderItemEntity.setCouponAmount(new BigDecimal(0));
        orderItemEntity.setIntegrationAmount(new BigDecimal(0));
        orderItemEntity.setRealAmount(orderItemVo.getPrice()
                .subtract(orderItemEntity.getPromotionAmount())
                .subtract(orderItemEntity.getCouponAmount())
                .subtract(orderItemEntity.getIntegrationAmount()));

        // 5. 填giftIntegration,giftGrowth
        BigDecimal total = orderItemVo.getPrice().multiply(new BigDecimal(orderItemVo.getCount().toString()));
        orderItemEntity.setGiftGrowth(total.intValue());
        orderItemEntity.setGiftIntegration(total.intValue());

        return orderItemEntity;
    }

    // 子方法3:此method用来填OrderEntity里与钱和积分相关的字段
    private void computePrice(OrderEntity orderEntity, List<OrderItemEntity> orderItemEntities) {
        BigDecimal promotionAmount = new BigDecimal(0);
        BigDecimal couponAmount = new BigDecimal(0);
        BigDecimal integrationAmount = new BigDecimal(0);
        BigDecimal totalAmount = new BigDecimal(0);

        Integer growth = 0;
        Integer integration = 0;

        if (orderItemEntities != null && orderItemEntities.size() > 0) {
            for (OrderItemEntity item : orderItemEntities) {
                promotionAmount = promotionAmount.add(item.getPromotionAmount());
                couponAmount = couponAmount.add(item.getCouponAmount());
                integrationAmount = integrationAmount.add(item.getIntegrationAmount());
                totalAmount = totalAmount.add(item.getSkuPrice().multiply(new BigDecimal(item.getSkuQuantity().toString())));

                growth += item.getGiftGrowth();
                integration += item.getGiftIntegration();
            }
        }

        // 填promotionAmount,couponAmount,integrationAmount,totalAmount字段
        orderEntity.setPromotionAmount(promotionAmount);
        orderEntity.setCouponAmount(couponAmount);
        orderEntity.setIntegrationAmount(integrationAmount);
        orderEntity.setTotalAmount(totalAmount);

        // 填payAmount字段(减运费)
        orderEntity.setPayAmount(totalAmount.add(orderEntity.getFreightAmount()));

        // 填growth,integration字段
        orderEntity.setGrowth(growth);
        orderEntity.setIntegration(integration);

    }

    // 用orderSn得到PayVo
    @Override
    public PayVo getPayOrder(String orderSn) {
        OrderEntity order = this.getOrderByOrderSn(orderSn);

        PayVo payVo = new PayVo();
        // 设out_trade_no字段
        payVo.setOut_trade_no(orderSn);
        // 设amount字段
        BigDecimal bigDecimal = order.getPayAmount().setScale(2, BigDecimal.ROUND_UP);
        payVo.setTotal_amount(bigDecimal.toString());
        // 设subject字段
        List<OrderItemEntity> items = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", orderSn));
        OrderItemEntity item = items.get(0);
        payVo.setSubject(item.getSkuName());
        // 设body字段
        payVo.setBody(item.getSkuAttrsVals());

        return payVo;
    }

    @Override
    public PageUtils queryPageWithItem(Map<String, Object> params) {
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>().eq("member_id", memberRespVo.getId()).orderByDesc("id"));

        // 给OrderEntity加List<OrderItemEntity>的field
        List<OrderEntity> orderEntities = page.getRecords().stream().map(order ->
                {
                    List<OrderItemEntity> order_sn = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", order.getOrderSn()));
                    order.setItemEntities(order_sn);
                    return order;
                }
        ).collect(Collectors.toList());

        page.setRecords(orderEntities);

        return new PageUtils(page);
    }

    @Override
    @Transactional
    public String handlePayResult(PayAsyncVo vo) {
        // 1. 记录至payment_info表
        PaymentInfoEntity paymentInfo = new PaymentInfoEntity();
        paymentInfo.setOrderSn(vo.getOut_trade_no());
        paymentInfo.setAlipayTradeNo(vo.getTrade_no());
        paymentInfo.setCallbackTime(vo.getNotify_time());
        paymentInfo.setPaymentStatus(vo.getTrade_status());

        // 把payment_nfo表的orderSn和alipayTradeNo字段设为unique
        paymentInfoService.save(paymentInfo);

        // 2. 更新订单状态
        if (vo.getTrade_status().equals("TRADE_SUCCESS") || vo.getTrade_status().equals("TRADE_FINISHED")) {
            this.baseMapper.updateOrderStatus(vo.getOut_trade_no(), OrderStatusEnum.PAYED.getCode());
        }

        return "success";
    }

    @Override
    public void createSeckillOrder(SeckillOrderTo orderTo) {
        log.info("创建秒杀订单");

        // 新建并保存OrderEntity
        OrderEntity entity = new OrderEntity();
        entity.setOrderSn(orderTo.getOrderSn());
        entity.setMemberId(orderTo.getMemberId());
        entity.setCreateTime(new Date());
        entity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        BigDecimal price = orderTo.getSeckillPrice().multiply(new BigDecimal("" + orderTo.getNum()));
        entity.setPayAmount(price);
        this.save(entity);

        // 新建并保存OrderItemEntity
        OrderItemEntity itemEntity = new OrderItemEntity();
        itemEntity.setOrderSn(orderTo.getOrderSn());
        itemEntity.setRealAmount(price);
        itemEntity.setSkuId(orderTo.getSkuId());
        itemEntity.setSkuQuantity(orderTo.getNum());
        orderItemService.save(itemEntity);
    }

}