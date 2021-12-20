package com.atguigu.gulimall.member.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.common.utils.HttpUtils;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.gulimall.member.dao.MemberDao;
import com.atguigu.gulimall.member.dao.MemberLevelDao;
import com.atguigu.gulimall.member.entity.MemberEntity;
import com.atguigu.gulimall.member.entity.MemberLevelEntity;
import com.atguigu.gulimall.member.exception.PhoneExistException;
import com.atguigu.gulimall.member.exception.UsernameExistException;
import com.atguigu.gulimall.member.service.MemberService;
import com.atguigu.gulimall.member.vo.MemberLoginVo;
import com.atguigu.gulimall.member.vo.MemberRegistVo;
import com.atguigu.gulimall.member.vo.SocialUser;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Autowired
    MemberLevelDao memberLevelDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void regist(MemberRegistVo vo) {
        MemberDao memberDao = this.baseMapper;
        MemberEntity entity = new MemberEntity();

        // 设会员等级level_id
        MemberLevelEntity defaultLevelEntity = memberLevelDao.getDefaultLevel();
        entity.setLevelId(defaultLevelEntity.getId());


        // 设用户名username和phone
        checkUsernameUnique(vo.getUsername());
        checkPhoneUnique(vo.getPhone());
        entity.setUsername(vo.getUsername());
        entity.setMobile(vo.getPhone());
        entity.setNickname(vo.getUsername());

        // 设密码password
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String s = encoder.encode(vo.getPassword());
        entity.setPassword(s);

        // 保存
        memberDao.insert(entity);
    }

    @Override
    public void checkUsernameUnique(String username) throws UsernameExistException {
        MemberDao memberDao = this.baseMapper;
        Integer count = memberDao.selectCount(new QueryWrapper<MemberEntity>().eq("username", username));
        if(count > 0) {
            throw new UsernameExistException();
        }
    }

    @Override
    public void checkPhoneUnique(String phone) throws PhoneExistException{
        MemberDao memberDao = this.baseMapper;
        Integer count = memberDao.selectCount(new QueryWrapper<MemberEntity>().eq("mobile", phone));
        if(count > 0) {
            throw new PhoneExistException();
        }
    }

    @Override
    public MemberEntity login(MemberLoginVo vo) {
        String loginacct = vo.getLoginacct();
        String password = vo.getPassword();

        // 获得loginacct对应的MemberEntity
        MemberDao memberDao = this.baseMapper;
        MemberEntity entity = memberDao.selectOne(new QueryWrapper<MemberEntity>().eq("username", loginacct).
                or().eq("mobile", loginacct));

        // 1. 检查entity是否为null。若为null，则返回null
        if(entity == null) {
            return null;
        } else {
            // 2. 检查password是否正确
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            boolean matches = encoder.matches(password, entity.getPassword());
            if(matches) {
                // 2.1 若password相符，返回entity
                return entity;
            } else {
                // 2.2 若password不符，返回null
                return null;
            }
        }

    }

    @Override
    public MemberEntity login(SocialUser socialUser) {
        String uid = socialUser.getUid();
        String token = socialUser.getAccess_token();
        Long expires = socialUser.getExpires_in();

        MemberDao baseMapper = this.baseMapper;
        MemberEntity entity = baseMapper.selectOne(new QueryWrapper<MemberEntity>().eq("social_uid",uid));

        if(entity == null) {
            // 如果entity为空，注册
            MemberEntity regist = new MemberEntity();
            regist.setSocialUid(uid);
            regist.setAccessToken(token);
            regist.setExpiresIn(expires);

            // 用token向weibo拿些信息
            try {
                // 用来搞query的map
                Map<String, String> query = new HashMap<>();
                query.put("uid", uid);
                query.put("access_token", token);
                // 向weibo接口发请求
                HttpResponse response = HttpUtils.doGet("https://api.weibo.com", "/2/users/show.json", "get", new HashMap<String, String>(), query);
                if(response.getStatusLine().getStatusCode() == 200) {
                    String s = EntityUtils.toString(response.getEntity());
                    JSONObject json = JSON.parseObject(s);
                    String name = json.getString("name");
                    String gender = json.getString("gender");
                    regist.setNickname(name);
                    regist.setGender("m".equals(gender) ? 1 : 0);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            baseMapper.insert(regist);

            return regist;
        } else {
            // 如果entity不为空，更新accessToken和expiresIn
            MemberEntity update = new MemberEntity();
            update.setSocialUid(uid);
            update.setAccessToken(token);
            update.setExpiresIn(expires);
            baseMapper.updateById(update);

            entity.setAccessToken(token);
            entity.setExpiresIn(expires);
            return entity;
        }
    }
}