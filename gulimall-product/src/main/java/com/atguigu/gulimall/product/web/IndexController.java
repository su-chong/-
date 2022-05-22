package com.atguigu.gulimall.product.web;

import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.Catelog2Vo;
import org.redisson.api.RLock;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
public class IndexController {

    @Autowired
    CategoryService categoryService;

    @Autowired
    RedissonClient redissonClient;

    @RequestMapping({"/","/index.html"})
    public String indexPage(Model model) {
        List<CategoryEntity> categorys =  categoryService.getLevel1Categorys();
        model.addAttribute("categorys",categorys);
        System.out.println(categorys);
        return "index";
    }

    @ResponseBody
    @RequestMapping("index/catalog.json")
    public Map<String, List<Catelog2Vo>> getCatlogJson() {

        Map<String, List<Catelog2Vo>> catelogJson = categoryService.getCatelogJson();
        return catelogJson;
    }

    @ResponseBody
    @RequestMapping("/hello")
    public String hello() {
        RLock lock = redissonClient.getLock("my-lock");
//        lock.lock(); // 阻塞式等待
        // 设置过期时间
        lock.lock(10, TimeUnit.SECONDS);
        try {
            System.out.println("加锁成功，执行业务..."+Thread.currentThread().getId());
            Thread.sleep(30000);
        } catch (Exception e) {

        } finally {
            lock.unlock();
            System.out.println("解锁成功..."+Thread.currentThread().getId());
        }
        return "hello";
    }

    @RequestMapping("/park")
    @ResponseBody
    public String park() throws InterruptedException {
        RSemaphore park = redissonClient.getSemaphore("park");
        park.acquire();
        return "停了一辆";
    }

    @RequestMapping("/tryPark")
    @ResponseBody
    public String tryPark() throws InterruptedException {
        RSemaphore park = redissonClient.getSemaphore("park");
        boolean b = park.tryAcquire();
        if(b) {
            // 执行业务
        } else {
            return "error";
        }
        return "ok";
    }

    @RequestMapping("/go")
    @ResponseBody
    public String go() throws InterruptedException {
        RSemaphore park = redissonClient.getSemaphore("park");
        park.release();
        return "走了一辆";
    }

}
