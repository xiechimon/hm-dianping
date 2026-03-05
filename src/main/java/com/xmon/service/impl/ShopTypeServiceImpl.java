package com.xmon.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.xmon.dto.Result;
import com.xmon.entity.ShopType;
import com.xmon.mapper.ShopTypeMapper;
import com.xmon.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xmon.utils.RedisConstants;
import io.netty.util.internal.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryTypeList() {
        // 1. 查询缓存
        String key = RedisConstants.CACHE_SHOP_TYPE_KEY;
        String shopTypeJson = stringRedisTemplate.opsForValue().get(key);
        log.debug(shopTypeJson);
        // 2. 命中，返回
        if (StrUtil.isNotBlank(shopTypeJson)) {
            List<ShopType> typeList = JSONUtil.toList(shopTypeJson, ShopType.class);
            return Result.ok(typeList);
        }
        // 3. 未命中，查数据库
        List<ShopType> typeList = query().orderByAsc("sort").list();
        // 4. 数据库中不存在，返回
        if (typeList == null || typeList.isEmpty()) {
            return Result.fail("分类不存在");
        }
        // 5. 存在，写入缓存
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(typeList));
        // 6. 返回
        return Result.ok(typeList);
    }
}
