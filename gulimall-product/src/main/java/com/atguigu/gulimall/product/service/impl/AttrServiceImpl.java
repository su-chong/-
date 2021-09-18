package com.atguigu.gulimall.product.service.impl;

import com.atguigu.common.constant.ProductConstant;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.gulimall.product.dao.AttrAttrgroupRelationDao;
import com.atguigu.gulimall.product.dao.AttrDao;
import com.atguigu.gulimall.product.dao.AttrGroupDao;
import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.AttrAttrgroupRelationEntity;
import com.atguigu.gulimall.product.entity.AttrEntity;
import com.atguigu.gulimall.product.entity.AttrGroupEntity;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.AttrService;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.AttrVo;
import com.atguigu.gulimall.product.vo.AttroRespVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service("attrService")
public class AttrServiceImpl extends ServiceImpl<AttrDao, AttrEntity> implements AttrService {

    @Autowired
    AttrAttrgroupRelationDao relationDao;

    @Autowired
    AttrGroupDao attrGroupDao;

    @Autowired
    CategoryDao categoryDao;

    @Autowired
    CategoryService categoryService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                new QueryWrapper<AttrEntity>()
        );

        return new PageUtils(page);
    }

    @Transactional
    @Override
    public void saveAttr(AttrVo attr) {
        // 保存attrEntity的信息
        AttrEntity attrEntity = new AttrEntity();
        BeanUtils.copyProperties(attr,attrEntity);
        this.save(attrEntity);

        // 保存attrgroup的信息
        if(attr.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode() && attr.getAttrGroupId()!=null) {
            AttrAttrgroupRelationEntity relation = new AttrAttrgroupRelationEntity();
            relation.setAttrId(attrEntity.getAttrId()); // 此处用attr.getAttrId()会出错
            relation.setAttrGroupId(attr.getAttrGroupId());
            relationDao.insert(relation);
        }

    }

    @Override
    public PageUtils queryBaseAttrPage(Map<String, Object> params, Long catelogId, String type) {
        QueryWrapper<AttrEntity> wrapper = new QueryWrapper<AttrEntity>().eq("attr_type","base".equalsIgnoreCase(type)?ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode():ProductConstant.AttrEnum.ATTR_TYPE_SALE.getCode());

        // 如果 catelogId != 0，给wrapper加上此条件
        if(catelogId != 0) {
            wrapper.eq("catelog_id",catelogId);
        }

        // 如果 key非空，给wrapper加上key此条件
        String key = (String) params.get("key");
        if(!Strings.isEmpty(key)) {
            wrapper.and( (w) -> {
                w.eq("attr_id",key).or().like("attr_name",key);
            });
        }

        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                wrapper
        );

        PageUtils pageUtils = new PageUtils(page);
        List<AttrEntity> records = page.getRecords();
        List<AttroRespVo> respVos = records.stream().map((attrEntity) -> {
            AttroRespVo attroRespVo = new AttroRespVo();
            BeanUtils.copyProperties(attrEntity, attroRespVo);

            // 加groupName
            if("base".equalsIgnoreCase(type)){
                AttrAttrgroupRelationEntity relationEntity = relationDao.selectOne(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrEntity.getAttrId()));
                if (relationEntity != null) {
                    AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(relationEntity.getAttrGroupId());
                    attroRespVo.setGroupName(attrGroupEntity.getAttrGroupName());
                }
            }

            // 加catelogName
            CategoryEntity categoryEntity = categoryDao.selectById(attrEntity.getCatelogId());
            if (categoryEntity != null) {
                attroRespVo.setCatelogName(categoryEntity.getName());
            }

            return attroRespVo;
        }).collect(Collectors.toList());

        pageUtils.setList(respVos);

        return pageUtils;
    }

    @Override
    public AttroRespVo getAttrInfo(Long attrId) {
        AttroRespVo attroRespVo = new AttroRespVo();

        // 把attrEntity里的property交给目标
        AttrEntity attrEntity = this.getById(attrId);
        BeanUtils.copyProperties(attrEntity,attroRespVo);

        if(attrEntity.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()){
            // 把AttrGroupId交给目标
            AttrAttrgroupRelationEntity relationEntity = relationDao.selectOne(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrEntity.getAttrId()));
            if(relationEntity != null) {
                Long groupId = relationEntity.getAttrGroupId();
                attroRespVo.setAttrGroupId(groupId);

                // 把attrgroupName交给目标
                AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(groupId);
                if(attrGroupEntity != null) {
                    attroRespVo.setGroupName(attrGroupEntity.getAttrGroupName());
                }
            }
        }

        // 把catelogName交给目标
        Long catelogId = attrEntity.getCatelogId();
        CategoryEntity categoryEntity = categoryDao.selectById(catelogId);
        if(categoryEntity != null) {
            attroRespVo.setCatelogName(categoryEntity.getName());
        }

        // 把catelogPath交给目标
        Long[] categoryPath = categoryService.findCategoryPath(catelogId);
        attroRespVo.setCatelogPath(categoryPath);

        return attroRespVo;
    }

    @Override
    public void updateAttr(AttrVo attr) {
        // 更新AttrEntity的自有属性
        AttrEntity attrEntity = new AttrEntity();
        BeanUtils.copyProperties(attr,attrEntity);
        this.updateById(attrEntity);

        if(attrEntity.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()){
            // 把attrGroupId交给目标
            Long attrId = attrEntity.getAttrId();
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            relationEntity.setAttrId(attrId);
            relationEntity.setAttrGroupId(attr.getAttrGroupId());

            Integer count = relationDao.selectCount(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrId));
            if(count > 0) {
                relationDao.update(relationEntity,new UpdateWrapper<AttrAttrgroupRelationEntity>().eq("attr_id",attrId));
            } else {
                relationDao.insert(relationEntity);
            }
        }

    }

    @Override
    public List<AttrEntity> getRelationAttr(Long attrGroupId) {
        List<AttrAttrgroupRelationEntity> entities = relationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_group_id", attrGroupId));
        List<Long> attrs = entities.stream().map((attr) -> {
            return attr.getAttrId();
        }).collect(Collectors.toList());

        if(attrs == null || attrs.size()==0) {
            return null;
        }
        List<AttrEntity> attrEntities = this.listByIds(attrs);
        return attrEntities;
    }

    @Override
    public PageUtils getNoRelationAttr(Map<String, Object> params, Long attrgroupId) {
        QueryWrapper<AttrEntity> wrapper = new QueryWrapper<AttrEntity>();

        // 条件1：catelogId要与当前attrgroup的catelogId相同
        AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrgroupId);
        Long catelogId = attrGroupEntity.getCatelogId();
        wrapper.eq("catelog_id", catelogId);

        // 条件2:尚未归入某个attrgroup
        // ① 拿到当前catelogId下的所有attrgroupId
        List<AttrGroupEntity> attrGroupEntities = attrGroupDao.selectList(new QueryWrapper<AttrGroupEntity>().eq("catelog_id", catelogId));
        List<Long> attrGroupIds = attrGroupEntities.stream().map((item) -> {
            return item.getAttrGroupId();
        }).collect(Collectors.toList());

        // ② 拿到这些attrgroupId下的所有attrId
        List<AttrAttrgroupRelationEntity> relationEntities = relationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>().in("attr_group_id", attrGroupIds));
        List<Long> attrIds = relationEntities.stream().map((item) -> {
            return item.getAttrId();
        }).collect(Collectors.toList());

        // ③ 排除这些attrId，拿到剩余的attr
        if(attrIds != null && attrIds.size()>0) {
            wrapper.notIn("attr_id",attrIds);
        }

        // 条件3:若key里有内容，加入模糊搜索条件
        String key = (String) params.get("key");
        if(!Strings.isEmpty(key)) {
            wrapper.and( (w) -> {
                w.eq("attr_id",key).or().like("attr_name",key);
            });
        }

        IPage<AttrEntity> page = this.page(new Query<AttrEntity>().getPage(params), wrapper);
        PageUtils pageUtils = new PageUtils(page);
        return pageUtils;
    }

}