package com.atguigu.gulimall.auth.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.common.utils.HttpUtils;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberRespVo;
import com.atguigu.gulimall.auth.feign.MemberFeignService;
import com.atguigu.gulimall.auth.vo.SocialUser;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
public class OAuth2Controller {

    @Autowired
    MemberFeignService memberFeignService;

    @GetMapping("/oauth2.0/weibo/success")
    public String weibo(@RequestParam("code") String code, HttpSession session) throws Exception {

        // 向weibo要Access Token
        Map<String, String> map = new HashMap<>();
        map.put("client_id", "3942904027");
        map.put("client_secret", "01881fee4971193eafa699c07bf8157a");
        map.put("grant_type", "authorization_code");
        map.put("redirect_uri", "http://auth.gulimall.com/oauth2.0/weibo/success");
        map.put("code", code);

        HttpResponse response = HttpUtils.doPost("https://api.weibo.com", "/oauth2/access_token", "post", new HashMap<String,String>(), null, map);

        // 1. 上面的post请求有没有成功拿到信息？
        // 1.1 若顺利拿到user信息，注册或登录
        if (response.getStatusLine().getStatusCode() == 200) {

            // 封装user信息为SocialUser
            HttpEntity entity = response.getEntity();
            String json = EntityUtils.toString(entity);
            SocialUser socialUser = JSON.parseObject(json, SocialUser.class);

            // 拿SocialUser到gulimall-member注册/登录
            R oauthLogin = memberFeignService.oauthlogin(socialUser);

            // 2. 有没有gulimall-member模块是否成功注册/登录？
            // 2.1 若成功注册或登录，回到首页
            if (oauthLogin.getCode() == 0) {
                MemberRespVo vo = oauthLogin.getData("data", new TypeReference<MemberRespVo>() {
                });
                log.info("登录成功：用户信息：{}",vo);
                session.setAttribute(AuthServerConstant.LOGIN_USER,vo);
                return "redirect:http://gulimall.com";
            } else {
                // 2.2 若未成功注册或登录，回到登录页
                return "redirect:http://auth.gulimall.com/login.html";
            }

        } else {
            // 1.2 若未拿到token信息，回登录页
            return "redirect:http://auth.gulimall.com/login.html";
        }
    }

}
