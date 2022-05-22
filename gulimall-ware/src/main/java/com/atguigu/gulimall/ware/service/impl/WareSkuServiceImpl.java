package com.atguigu.gulimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.to.es.SkuHasStockVo;
import com.atguigu.common.to.mq.OrderTo;
import com.atguigu.common.to.mq.StockDetailTo;
import com.atguigu.common.to.mq.StockLockedTo;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.ware.dao.WareSkuDao;
import com.atguigu.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.atguigu.gulimall.ware.entity.WareOrderTaskEntity;
import com.atguigu.gulimall.ware.entity.WareSkuEntity;
import com.atguigu.gulimall.ware.exception.NoStockException;
import com.atguigu.gulimall.ware.feign.OrderFeignService;
import com.atguigu.gulimall.ware.feign.ProductFeignService;
import com.atguigu.gulimall.ware.service.WareOrderTaskDetailService;
import com.atguigu.gulimall.ware.service.WareOrderTaskService;
import com.atguigu.gulimall.ware.service.WareSkuService;
import com.atguigu.gulimall.ware.vo.OrderItemVo;
import com.atguigu.gulimall.ware.vo.OrderVo;
import com.atguigu.gulimall.ware.vo.WareSkuLockVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    OrderFeignService orderFeignService;

    @Autowired
    WareOrderTaskDetailService wareOrderTaskDetailService;

    @Autowired
    WareOrderTaskService wareOrderTaskService;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    WareOrderTaskDetailServiceImpl taskDetailService;

    @Autowired
    WareSkuDao wareSkuDao;

    @Autowired
    ProductFeignService productFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareSkuEntity> wrapper = new QueryWrapper<>();

        String skuId = (String) params.get("skuId");
        if (!StringUtils.isEmpty(skuId)) {
            wrapper.eq("sku_id", skuId);
        }

        String wareId = (String) params.get("wareId");
        if (!StringUtils.isEmpty(wareId)) {
            wrapper.eq("ware_id", wareId);
        }

        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        List<WareSkuEntity> wareSkuEntities = wareSkuDao.selectList(new QueryWrapper<WareSkuEntity>().eq("sku_id", skuId).eq("ware_id", wareId));
        if (wareSkuEntities == null || wareSkuEntities.size() == 0) {
            WareSkuEntity wareSkuEntity = new WareSkuEntity();
            wareSkuEntity.setWareId(wareId);
            wareSkuEntity.setSkuId(skuId);
            wareSkuEntity.setStock(skuNum);
            wareSkuEntity.setStockLocked(0);

            try {
                R info = productFeignService.info(skuId);
                Map<String, Object> data = (Map<String, Object>) info.get("skuInfo");
                if (info.getCode() == 0) {
                    wareSkuEntity.setSkuName((String) data.get("skuName"));
                }
            } catch (Exception e) {

            }

            wareSkuDao.insert(wareSkuEntity);
        } else {
            wareSkuDao.addStock(skuId, wareId, skuNum);
        }
    }

    @Override
    public List<SkuHasStockVo> getSkusHasStock(List<Long> skuIds) {
        List<SkuHasStockVo> collect = skuIds.stream().map(item -> {
            Long count = baseMapper.getSkuStock(item);
            SkuHasStockVo vo = new SkuHasStockVo();
            vo.setSkuId(item);
            vo.setHasStock(count == null ? false : count > 0);
            return vo;
        }).collect(Collectors.toList());
        return collect;
    }

    /**
     * 为order锁库存
     * @param vo
     * @return
     */
    @Transactional(rollbackFor = NoStockException.class)
    @Override
    public Boolean orderLockStock(WareSkuLockVo vo) {
        List<OrderItemVo> orderItemVos = vo.getLocks();

        // 0. [记录1] 记至WareOrderTaskEntity
        WareOrderTaskEntity taskEntity = new WareOrderTaskEntity();
        taskEntity.setOrderSn(vo.getOrderSn());
        wareOrderTaskService.save(taskEntity);


        // 1. 为各OrderItem查哪些ware里有货
        List<SkuWareHasStock> stockVos = orderItemVos.stream().map(item -> {
            Long skuId = item.getSkuId();

            SkuWareHasStock skuWareHasStockVo = new SkuWareHasStock();
            // 填skuId字段
            skuWareHasStockVo.setSkuId(skuId);
            // 填num字段
            skuWareHasStockVo.setNum(item.getCount());
            // 填wareId字段
            List<Long> wareIds = wareSkuDao.listWareIdHasStock(skuId);
            skuWareHasStockVo.setWareIds(wareIds);

            return skuWareHasStockVo;
        }).collect(Collectors.toList());

        // 2. 为各OrderItem找存货充足的ware，并锁库存
        for (SkuWareHasStock stockVo : stockVos) {
            Boolean locked = false;
            Long skuId = stockVo.getSkuId();
            List<Long> wareIds = stockVo.getWareIds();
            // 若各ware均无货，则报错
            if(wareIds == null || wareIds.size() == 0) {
                throw new NoStockException(skuId);
            }

            for (Long wareId : wareIds) {
                Long count = wareSkuDao.lockSkuStock(skuId,wareId,stockVo.getNum());
                if(count == 1) {
                    // 锁库存成功，继续下一个stockVo
                    locked = true;

                    // [记录2] 记至WareOrderTaskDetailEntity
                    WareOrderTaskDetailEntity taskDetailEntity = new WareOrderTaskDetailEntity(null, skuId, null, stockVo.getNum(), taskEntity.getId(), wareId, 1);
                    taskDetailService.save(taskDetailEntity);

                    // [记录3] 发给RabbitMQ
                    StockLockedTo stockLockedTo = new StockLockedTo();
                    // ① 写id字段
                    stockLockedTo.setId(taskEntity.getId());
                    // ② 写stockDetailTo字段
                    StockDetailTo detailTo = new StockDetailTo();
                    BeanUtils.copyProperties(taskDetailEntity, detailTo);
                    stockLockedTo.setDetailTo(detailTo);
                    // 发送
                    rabbitTemplate.convertAndSend("stock-event-exchange", "stock.locked", stockLockedTo);

                    break;
                } else {
                    // 锁库存失败，尝试下一个ware
                }
            }
            if(!locked) {
                // 当前商品锁库存失败
                throw new NoStockException(skuId);
            }
        }

        // 运行到这里表示锁库存成功
        return true;
    }

    @Data
    class SkuWareHasStock {
        private Long skuId;
        private List<Long> wareIds;
        private Integer num;
    }

    /**
     * [解锁库存1] 源于定时解锁库存的需求
     * @param lockedTo
     */
    @Override
    public void unlockStock(StockLockedTo lockedTo) {
        Long id = lockedTo.getId();
        StockDetailTo detailTo = lockedTo.getDetailTo();
        Long detailId = detailTo.getId();

        // 1. 判断WareOrderTaskDetailEntity是否存在。
        WareOrderTaskDetailEntity detailEntity = wareOrderTaskDetailService.getById(detailId);
        if(detailEntity != null) {
            // 1.1 WareOrderTaskDetailEntity存在

            // 2. 判断OrderEntity是否存在。
            WareOrderTaskEntity taskEntity = wareOrderTaskService.getById(id);
            String orderSn = taskEntity.getOrderSn();
            // 利用orderSn得到OrderEntity
            R r = orderFeignService.getOrderByOrderSn(orderSn);
            if(r.getCode() == 0) {
                OrderVo orderVo = r.getData(new TypeReference<OrderVo>() {
                });

                if(orderVo == null || orderVo.getStatus() == 4) {
                    // 2.1 OrderEntity不存在或已被取消
                    // >>> [第3种处理] 接收消息，并解锁库存
                    if(detailEntity.getLockStatus() == 1) {
                        unlockStock(detailEntity.getSkuId(),detailEntity.getWareId(), detailEntity.getSkuNum(),detailId);
                    }

                } else {
                    // 2.2 OrderEntity存在且未被取消
                    // >>> 接收消息，什么都不用做
                }
            } else {
                // 远程调用失败
                // >>> [第2种处理] 不接收消息
                throw new RuntimeException("解锁库存时,远程服务失败");
            }

        } else {
            // 1.2 WareOrderTaskDetailEntity不存在，说明锁库存失败，已自动回滚。
            // >>> [第1种处理] 接收消息，什么都不用做
        }
    }


    /**
     * [解锁库存2] 源于关闭订单时的要求
     * 需求：防止因卡顿导致"定时解锁库存"早于"定时关闭订单"发生，导致库存永远无法解锁
     * @param orderTo
     */
    @Override
    public void unlockStock(OrderTo orderTo) {
        String orderSn = orderTo.getOrderSn();
        WareOrderTaskEntity taskEntity = wareOrderTaskService.getByOrderSn(orderSn);
        List<WareOrderTaskDetailEntity> detailEntities = wareOrderTaskDetailService.list(new QueryWrapper<WareOrderTaskDetailEntity>()
                .eq("task_id", taskEntity.getId())
                .eq("lock_status", 1));
        for (WareOrderTaskDetailEntity entity : detailEntities) {
            unlockStock(entity.getSkuId(), entity.getWareId(), entity.getSkuNum(), entity.getId());
        }
    }


    /**
     * [解锁库存之本解] 实质地做，用以辅助前两个方法
     * 做法：① 减stock_locked ② 改WareOrderTaskDetailEntity.lockStatus
     * @param skuId
     * @param wareId
     * @param num
     * @param taskDetailId
     */
    private void unlockStock(Long skuId, Long wareId, Integer num, Long taskDetailId) {
        wareSkuDao.unlockStock(skuId, wareId, num);

        WareOrderTaskDetailEntity updateEntity = new WareOrderTaskDetailEntity();
        updateEntity.setId(taskDetailId);
        updateEntity.setLockStatus(2);
        wareOrderTaskDetailService.updateById(updateEntity);
    }

}

