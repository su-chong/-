package com.atguigu.gulimall.product.service;

import com.atguigu.gulimall.product.vo.Catelog2Vo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.product.entity.CategoryEntity;

import java.util.List;
import java.util.Map;

/**
 * 商品三级分类
 *
 * @author yunuozju
 * @email 2246463432@qq.com
 * @date 2021-08-12 17:48:52
 */
public interface CategoryService extends IService<CategoryEntity> {

    PageUtils queryPage(Map<String, Object> params);

    List<CategoryEntity> listWithTree();

    /**
     * 找到catelogId的完整路径
     * @param catelogId
     * @return
     */
    Long[] findCategoryPath(Long catelogId);

    void updateCascade(CategoryEntity category);

    List<CategoryEntity> getLevel1Categorys();

    // 原生的
    Map<String, List<Catelog2Vo>> getCatelogJson9();

    // 优化逻辑，只查一遍数据库
    Map<String, List<Catelog2Vo>> getCatelogJsonFromDb();

    // 引入Redis缓存
    Map<String, List<Catelog2Vo>> getCatelogJson8();

    // 用synchronized()锁控制查数据库的操作
    Map<String, List<Catelog2Vo>> getCatelogJsonFromDbWithLock();

    // 用getCatelogJsonFromDbWithLock()，其它同8
    Map<String, List<Catelog2Vo>> getCatelogJson7();



    // 把"添加至redis"的操作放到getCatelogJsonFromDbWithLock()内
    Map<String, List<Catelog2Vo>> getCatelogJsonFromDbWithLockUpdateRedis();

    // 用getCatelogJsonFromDbWithLockUpdateRedis()，其他同8
    Map<String, List<Catelog2Vo>> getCatelogJson6();



    // 用redis锁控制查数据库的操作
    Map<String, List<Catelog2Vo>> getCatelogJsonFromDbWithRedisLock();

    // 用getCatelogJsonFromDbWithRedisLock()，其他同8
    Map<String, List<Catelog2Vo>> getCatelogJson5();



    // 把"加锁"和"设过期时间"搞成原子操作，把"判断是否是自己的锁"和"删锁"搞成原子操作
    Map<String, List<Catelog2Vo>> getCatelogJsonFromDbWithRedisLock2();

    // 用getCatelogJsonFromDbWithRedisLock()，其他同8
    Map<String, List<Catelog2Vo>> getCatelogJson4();


    // 给查数据库的操作用Redisson加锁（默认有原子性）
    Map<String, List<Catelog2Vo>> getCatelogJsonFromDbWithRedissonLock();

    // 用getCatelogJsonFromDbWithRedissonLock()，其他同8
    Map<String, List<Catelog2Vo>> getCatelogJson3();

    // 给查数据库的操作用Redisson加锁（默认有原子性）
    Map<String, List<Catelog2Vo>> getCatelogJson();

}

