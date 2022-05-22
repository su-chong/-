package com.atguigu.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.Catalog3Vo;
import com.atguigu.gulimall.product.vo.Catelog2Vo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    RedissonClient redissonClient;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        // 1. 查出所有分类
        List<CategoryEntity> entities = baseMapper.selectList(null); // baseMapper其实是CategoryDao
        List<CategoryEntity> level1Menus = entities.stream().filter
                (categoryEntity -> categoryEntity.getParentCid() == 0).map((menu) -> {
            menu.setChildren(getChildren(menu, entities));
            return menu;
        }).sorted((menu1, menu2) -> {
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());
        return level1Menus;
    }

    public List<CategoryEntity> getChildren(CategoryEntity root, List<CategoryEntity> all) {
        List<CategoryEntity> children = all.stream().filter(entity -> {
            return entity.getParentCid() == root.getCatId();
        }).map(categoryEntity -> {
            categoryEntity.setChildren(getChildren(categoryEntity, all));
            return categoryEntity;
        }).sorted((menu1, menu2) -> {
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());
        return children;
    }

    @Override
    public Long[] findCategoryPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        Long point = catelogId;
        while (point != 0) {
            paths.add(point);
            point = this.getById(point).getParentCid();
        }
        Collections.reverse(paths);
        return paths.toArray(new Long[paths.size()]);
    }

    @Caching(evict = {
            @CacheEvict(value = "category", key = "'getLevel1Categorys'"),
            @CacheEvict(value = "category", key = "'getCatelogJson'")
    })
//    @CacheEvict(value = "category",allEntries = true)
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());

        // TODO 更新其他关联
    }

    @Cacheable(value = {"category"}, key = "#root.methodName")
//    @Cacheable(value = {"category"},key = "'level1Category'")
    @Override
    public List<CategoryEntity> getLevel1Categorys() {
        System.out.println("getLevel1Categorys...");
//        long l = System.currentTimeMillis();
        List<CategoryEntity> entities = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("cat_level", 1));
//        System.out.println("消耗时间" + (System.currentTimeMillis() - l));
        return entities;
    }

    /**
     * 这是最早版本，没有抽取方法，多次查数据库，不用缓存
     *
     * @return
     */
    @Deprecated
    @Override
    public Map<String, List<Catelog2Vo>> getCatelogJson9() {

        List<CategoryEntity> level1Categorys = getLevel1Categorys();

        // 用List<1级categoryEntity> → Map<id,List<catelog2Vo>>：
        Map<String, List<Catelog2Vo>> result = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            List<CategoryEntity> level2Categorys = this.list(new QueryWrapper<CategoryEntity>().eq("parent_cid", v.getCatId()));

            // 用List<2级categoryEntity> → List<catelog2vo>
            List<Catelog2Vo> catelog2Vos = level2Categorys.stream().map(item -> {
                Catelog2Vo catelog2Vo = new Catelog2Vo(item.getCatId().toString(), item.getName(), v.getCatId().toString(), null);

                List<CategoryEntity> level3Categorys = this.list(new QueryWrapper<CategoryEntity>().eq("parent_cid", v.getCatId()));
                // 用List<3级categoryEntity> → List<catelog3vo>
                List<Catalog3Vo> catalog3Vos = level3Categorys.stream().map(c -> {
                    return new Catalog3Vo(c.getCatId().toString(), c.getName(), c.getParentCid().toString());
                }).collect(Collectors.toList());

                catelog2Vo.setCatalog3List(catalog3Vos);
                return catelog2Vo;
            }).collect(Collectors.toList());

            return catelog2Vos;
        }));
        return result;
    }

    /**
     * 这是从getCatalogJson里的某一句抽取的一个方法
     *
     * @param selectList
     * @param parentId
     * @return
     */
    public List<CategoryEntity> getParent_cid(List<CategoryEntity> selectList, Long parentId) {
        List<CategoryEntity> collect = selectList.stream().filter(item -> item.getParentCid() == parentId).collect(Collectors.toList());
        return collect;
//        return this.list(new QueryWrapper<CategoryEntity>().eq("parent_cid", v.getCatId()));
    }

    /**
     * 这是用getCatelogJson整体抽取而成的方法。只查一次数据库。用到了getParent_cid()
     *
     * @return
     */
    @Override
    public Map<String, List<Catelog2Vo>> getCatelogJsonFromDb() {
        List<CategoryEntity> selectList = baseMapper.selectList(null);

        List<CategoryEntity> level1Categorys = getParent_cid(selectList, 0L);

        // 用List<1级categoryEntity> → Map<id,List<catelog2Vo>>：
        Map<String, List<Catelog2Vo>> result = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            List<CategoryEntity> level2Categorys = getParent_cid(selectList, v.getCatId());

            // 用List<2级categoryEntity> → List<catelog2vo>
            List<Catelog2Vo> catelog2Vos = level2Categorys.stream().map(item -> {
                Catelog2Vo catelog2Vo = new Catelog2Vo(item.getCatId().toString(), item.getName(), v.getCatId().toString(), null);

                List<CategoryEntity> level3Categorys = getParent_cid(selectList, item.getCatId());
                // 用List<3级categoryEntity> → List<catelog3vo>
                List<Catalog3Vo> catalog3Vos = level3Categorys.stream().map(c -> {
                    return new Catalog3Vo(c.getCatId().toString(), c.getName(), c.getParentCid().toString());
                }).collect(Collectors.toList());

                catelog2Vo.setCatalog3List(catalog3Vos);
                return catelog2Vo;
            }).collect(Collectors.toList());

            return catelog2Vos;
        }));
        return result;
    }

    /**
     * 引入redis缓存。有调用getCatelogJsonFromDb()
     *
     * @return
     */
    @Override
    public Map<String, List<Catelog2Vo>> getCatelogJson8() {
        String catalogJson = redisTemplate.opsForValue().get("catalogJSON");

        if (StringUtils.isEmpty(catalogJson)) {
            Map<String, List<Catelog2Vo>> catelogJsonFromDb = getCatelogJsonFromDb();
            redisTemplate.opsForValue().set("catalogJSON", JSON.toJSONString(catelogJsonFromDb));
            return catelogJsonFromDb;
        }

        return JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catelog2Vo>>>() {
        });
    }

    /**
     * 给getCatelogFromDb()加锁
     *
     * @return
     */
    @Override
    public Map<String, List<Catelog2Vo>> getCatelogJsonFromDbWithLock() {
        synchronized (this) {
            String catalogJson = redisTemplate.opsForValue().get("catalogJSON");

            if (!StringUtils.isEmpty(catalogJson)) {
                return JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catelog2Vo>>>() {
                });
            }

            System.out.println("查询了数据库...");

            List<CategoryEntity> selectList = baseMapper.selectList(null);

            List<CategoryEntity> level1Categorys = getParent_cid(selectList, 0L);

            // 用List<1级categoryEntity> → Map<id,List<catelog2Vo>>：
            Map<String, List<Catelog2Vo>> result = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
                List<CategoryEntity> level2Categorys = getParent_cid(selectList, v.getCatId());

                // 用List<2级categoryEntity> → List<catelog2vo>
                List<Catelog2Vo> catelog2Vos = level2Categorys.stream().map(item -> {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(item.getCatId().toString(), item.getName(), v.getCatId().toString(), null);

                    List<CategoryEntity> level3Categorys = getParent_cid(selectList, item.getCatId());
                    // 用List<3级categoryEntity> → List<catelog3vo>
                    List<Catalog3Vo> catalog3Vos = level3Categorys.stream().map(c -> {
                        return new Catalog3Vo(c.getCatId().toString(), c.getName(), c.getParentCid().toString());
                    }).collect(Collectors.toList());

                    catelog2Vo.setCatalog3List(catalog3Vos);
                    return catelog2Vo;
                }).collect(Collectors.toList());

                return catelog2Vos;
            }));
            return result;
        }

    }

    /**
     * redis缓存 + getCatelogJsonFromDbWithLock()
     *
     * @return
     */
    @Override
    public Map<String, List<Catelog2Vo>> getCatelogJson7() {
        String catalogJson = redisTemplate.opsForValue().get("catalogJSON");

        if (StringUtils.isEmpty(catalogJson)) {
            System.out.println("缓存未命中，查数据库...");
            Map<String, List<Catelog2Vo>> catelogJsonFromDb = getCatelogJsonFromDbWithLock();
            redisTemplate.opsForValue().set("catalogJSON", JSON.toJSONString(catelogJsonFromDb));
            return catelogJsonFromDb;
        }

        System.out.println("缓存命中，直接返回...");
        return JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catelog2Vo>>>() {
        });

    }

    /**
     * 改动：查完后添加至redis
     *
     * @return
     */
    @Override
    public Map<String, List<Catelog2Vo>> getCatelogJsonFromDbWithLockUpdateRedis() {
        synchronized (this) {
            return getTemp();
        }
    }

    private Map<String, List<Catelog2Vo>> getTemp() {
        String catalogJson = redisTemplate.opsForValue().get("catalogJSON");

        if (!StringUtils.isEmpty(catalogJson)) {
            return JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catelog2Vo>>>() {
            });
        }

        System.out.println("查询了数据库...");

        List<CategoryEntity> selectList = baseMapper.selectList(null);

        List<CategoryEntity> level1Categorys = getParent_cid(selectList, 0L);

        // 用List<1级categoryEntity> → Map<id,List<catelog2Vo>>：
        Map<String, List<Catelog2Vo>> result = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            List<CategoryEntity> level2Categorys = getParent_cid(selectList, v.getCatId());

            // 用List<2级categoryEntity> → List<catelog2vo>
            List<Catelog2Vo> catelog2Vos = level2Categorys.stream().map(item -> {
                Catelog2Vo catelog2Vo = new Catelog2Vo(item.getCatId().toString(), item.getName(), v.getCatId().toString(), null);

                List<CategoryEntity> level3Categorys = getParent_cid(selectList, item.getCatId());
                // 用List<3级categoryEntity> → List<catelog3vo>
                List<Catalog3Vo> catalog3Vos = level3Categorys.stream().map(c -> {
                    return new Catalog3Vo(c.getCatId().toString(), c.getName(), c.getParentCid().toString());
                }).collect(Collectors.toList());

                catelog2Vo.setCatalog3List(catalog3Vos);
                return catelog2Vo;
            }).collect(Collectors.toList());
            return catelog2Vos;
        }));
        redisTemplate.opsForValue().set("catalogJSON", JSON.toJSONString(result));
        return result;
    }

    /**
     * redis缓存 + getCatelogJsonFromDbWithLockUpdateRedis()
     *
     * @return
     */
    @Override
    public Map<String, List<Catelog2Vo>> getCatelogJson6() {
        String catalogJson = redisTemplate.opsForValue().get("catalogJSON");

        if (StringUtils.isEmpty(catalogJson)) {
            System.out.println("缓存未命中，查数据库...");
            Map<String, List<Catelog2Vo>> catelogJsonFromDb = getCatelogJsonFromDbWithLockUpdateRedis();
            return catelogJsonFromDb;
        }

        System.out.println("缓存命中，直接返回...");
        return JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catelog2Vo>>>() {
        });
    }

    /**
     * 用Redis锁控制查数据库的操作
     *
     * @return
     */
    @Override
    public Map<String, List<Catelog2Vo>> getCatelogJsonFromDbWithRedisLock() {
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", "1111");
        if (lock) {
            Map<String, List<Catelog2Vo>> result = getTemp();
            redisTemplate.delete("lock");
            return result;
        } else {
            return getCatelogJsonFromDbWithRedisLock(); // 自旋的方式
        }
    }

    /**
     * redis缓存 + getCatelogJsonFromDbWithRedisLock()
     *
     * @return
     */
    @Override
    public Map<String, List<Catelog2Vo>> getCatelogJson5() {
        String catalogJson = redisTemplate.opsForValue().get("catalogJSON");

        if (StringUtils.isEmpty(catalogJson)) {
            System.out.println("缓存未命中，查数据库...");
            Map<String, List<Catelog2Vo>> catelogJsonFromDb = getCatelogJsonFromDbWithRedisLock();
            return catelogJsonFromDb;
        }

        System.out.println("缓存命中，直接返回...");
        return JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catelog2Vo>>>() {
        });
    }

    /**
     * 用Redis锁控制查数据库的操作
     *
     * @return
     */
    @Override
    public Map<String, List<Catelog2Vo>> getCatelogJsonFromDbWithRedisLock2() {
        String uuid = UUID.randomUUID().toString();
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", uuid, 300, TimeUnit.SECONDS);
        if (lock) {
            System.out.println("获取分布式锁成功...");
            Map<String, List<Catelog2Vo>> result;
            try {
                result = getTemp();
            } finally {
                String script = "if redis.call(\"get\",KEYS[1]) == ARGV[1]\n" +
                        "then\n" +
                        "    return redis.call(\"del\",KEYS[1])\n" +
                        "else\n" +
                        "    return 0\n" +
                        "end";
                Long lock1 = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), Arrays.asList("lock"), uuid);
            }
            return result;
        } else {
            System.out.println("获取分布式锁失败，重试中...");
            try {
                Thread.sleep(200);
            } catch (Exception e) {

            }
            return getCatelogJsonFromDbWithRedisLock2(); // 自旋的方式
        }
    }

    /**
     * redis缓存 + getCatelogJsonFromDbWithRedisLock()
     *
     * @return
     */
    @Override
    public Map<String, List<Catelog2Vo>> getCatelogJson4() {
        String catalogJson = redisTemplate.opsForValue().get("catalogJSON");

        if (StringUtils.isEmpty(catalogJson)) {
            System.out.println("缓存未命中，查数据库...");
            Map<String, List<Catelog2Vo>> catelogJson = getCatelogJsonFromDbWithRedisLock2();
            return catelogJson;
        }

        System.out.println("缓存命中，直接返回...");
        return JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catelog2Vo>>>() {
        });
    }

    @Override
    public Map<String, List<Catelog2Vo>> getCatelogJsonFromDbWithRedissonLock() {
        RLock Lock = redissonClient.getLock("CatalogJson-lock");
        Lock.lock();

        Map<String, List<Catelog2Vo>> result;
        try {
            result = getTemp();
        } finally {
            Lock.unlock();
        }
        return result;
    }

    /**
     * redis缓存 + getCatelogJsonFromDbWithRedissonLock()
     *
     * @return
     */
    @Override
    public Map<String, List<Catelog2Vo>> getCatelogJson3() {
        String catalogJson = redisTemplate.opsForValue().get("catalogJSON");

        if (StringUtils.isEmpty(catalogJson)) {
            System.out.println("缓存未命中，查数据库...");
            Map<String, List<Catelog2Vo>> catelogJson = getCatelogJsonFromDbWithRedissonLock();
            return catelogJson;
        }

        System.out.println("缓存命中，直接返回...");
        return JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catelog2Vo>>>() {
        });
    }

    @Cacheable(value = {"category"}, key = "#root.methodName")
    @Override
    public Map<String, List<Catelog2Vo>> getCatelogJson() {

        System.out.println("查询了数据库...");

        List<CategoryEntity> selectList = baseMapper.selectList(null);

        List<CategoryEntity> level1Categorys = getParent_cid(selectList, 0L);

        // 用List<1级categoryEntity> → Map<id,List<catelog2Vo>>：
        Map<String, List<Catelog2Vo>> result = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            List<CategoryEntity> level2Categorys = getParent_cid(selectList, v.getCatId());

            // 用List<2级categoryEntity> → List<catelog2vo>
            List<Catelog2Vo> catelog2Vos = level2Categorys.stream().map(item -> {
                Catelog2Vo catelog2Vo = new Catelog2Vo(item.getCatId().toString(), item.getName(), v.getCatId().toString(), null);

                List<CategoryEntity> level3Categorys = getParent_cid(selectList, item.getCatId());
                // 用List<3级categoryEntity> → List<catelog3vo>
                List<Catalog3Vo> catalog3Vos = level3Categorys.stream().map(c -> {
                    return new Catalog3Vo(c.getCatId().toString(), c.getName(), c.getParentCid().toString());
                }).collect(Collectors.toList());

                catelog2Vo.setCatalog3List(catalog3Vos);
                return catelog2Vo;
            }).collect(Collectors.toList());
            return catelog2Vos;
        }));
        return result;
    }

}

