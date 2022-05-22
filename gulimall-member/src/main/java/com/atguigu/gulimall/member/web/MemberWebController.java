package com.atguigu.gulimall.member.web;

import com.alibaba.fastjson.JSON;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.member.feign.OrderFeignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;

@Controller
public class MemberWebController {

    @Autowired
    OrderFeignService orderFeignService;

    @GetMapping("/memberOrder.html")
    public String getMemberOrderPage(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum, Model model) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("page", pageNum);
        R r = orderFeignService.listWithItem(params);

        model.addAttribute("orders", r);
        System.out.println(JSON.toJSONString(r));

        return "orderList";
    }

}
