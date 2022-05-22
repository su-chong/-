package com.atguigu.gulimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.ware.dao.WareInfoDao;
import com.atguigu.gulimall.ware.entity.WareInfoEntity;
import com.atguigu.gulimall.ware.feign.MemberFeignService;
import com.atguigu.gulimall.ware.service.WareInfoService;
import com.atguigu.gulimall.ware.vo.FareVo;
import com.atguigu.gulimall.ware.vo.MemberAddressVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Map;


@Service("wareInfoService")
public class WareInfoServiceImpl extends ServiceImpl<WareInfoDao, WareInfoEntity> implements WareInfoService {

    @Autowired
    MemberFeignService memberFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareInfoEntity> wrapper = new QueryWrapper<>();

        String key = (String) params.get("key");
        if(!StringUtils.isEmpty(key)) {
            wrapper.eq("id",key)
                    .or().like("name",key)
                    .or().like("address",key)
                    .or().like("areacode",key);
        }

        IPage<WareInfoEntity> page = this.page(
                new Query<WareInfoEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    /**
     * 返回到某地址的运费
     * @param addrId
     * @return
     */
    @Override
    public FareVo getfare(Long addrId) {
        FareVo fareVo = new FareVo();
        // 调用gulimall-member查memberAddressVo
        R r = memberFeignService.addrInfo(addrId);
        MemberAddressVo addressVo = r.getData("memberReceiveAddress", new TypeReference<MemberAddressVo>() {
        });
        if(addressVo != null) {
            // 由地址计算fare
            String phone = addressVo.getPhone();
            String fare = phone.substring(phone.length()-1, phone.length());
            // 填fareVo的fare字段+address字段
            fareVo.setFare(new BigDecimal(fare));
            fareVo.setAddress(addressVo);
            return fareVo;
        }
        return null;
    }

}