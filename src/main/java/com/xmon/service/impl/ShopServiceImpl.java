package com.xmon.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.xmon.dto.Result;
import com.xmon.entity.Shop;
import com.xmon.mapper.ShopMapper;
import com.xmon.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xmon.utils.CacheClient;
import com.xmon.utils.RedisConstants;
import com.xmon.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    @Override
    public Result queryById(Long id) {
        // 缓存穿透
        //Shop shop = cacheClient.queryWithPassThrough(
        //        RedisConstants.CACHE_SHOP_KEY,
        //        id, Shop.class,
        //        this::getById,
        //        RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES
        //);

        // 互斥锁解决缓存击穿
        //Shop shop = queryWithMutex(id);

        // 逻辑过期解决缓存击穿
        Shop shop = cacheClient.queryWithLogicalExpire(RedisConstants.CACHE_SHOP_KEY, id, Shop.class, this::getById,
                20L, TimeUnit.SECONDS);

        if (shop == null) {
            return Result.fail("店铺不存在");
        }

        // 返回
        return Result.ok(shop);
    }

    //private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    //private Shop queryWithLogicalExpire(Long id) {
    //    // 1. 从 Redis 查询商户缓存
    //    String key = RedisConstants.CACHE_SHOP_KEY + id;
    //    String shopJson = stringRedisTemplate.opsForValue().get(key);
    //    // 2. 判断是否存在，因为热点key我们设置了逻辑过期，所以热点key不会有空的情况存在，无须阻塞
    //    if (StrUtil.isBlank(shopJson)) {
    //        // 3. 不存在，直接返回空
    //        return null;
    //    }
    //
    //    // 4. 命中，将 JSON 反序列化为对象后判断缓存是否过期
    //    RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
    //    Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
    //    LocalDateTime expireTime = redisData.getExpireTime();
    //
    //    if (expireTime.isAfter(LocalDateTime.now())) {
    //        // 4.1 未过期，返回商铺信息
    //        return shop;
    //    }
    //
    //    // 4.2 过期，需要缓存重建
    //    // 5.1 获取互斥锁
    //    String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
    //    boolean isLock = tryLock(lockKey);
    //    // 5.2 判断是否获取锁成功
    //    if (isLock) {
    //        // 5.3 成功，开启独立线程，实现缓存重建
    //        CACHE_REBUILD_EXECUTOR.execute(() -> {
    //            try {
    //                // 重建缓存
    //                saveData2Redis(id, 20L);
    //            } catch (Exception e) {
    //                throw new RuntimeException(e);
    //            } finally {
    //                // 释放锁
    //                unLock(lockKey);
    //            }
    //        });
    //    }
    //    // 5.4 失败，返回过期的商铺信息
    //    return shop;
    //}

    //private Shop queryWithMutex(Long id) {
    //    // 1. 从 Redis 查询商户缓存
    //    String key = RedisConstants.CACHE_SHOP_KEY + id;
    //    String shopJson = stringRedisTemplate.opsForValue().get(key);
    //    // 2. 判断是否存在
    //    if (StrUtil.isNotBlank(shopJson)) {
    //        // 3. 存在，直接返回
    //        Shop shop = JSONUtil.toBean(shopJson, Shop.class);
    //        return shop;
    //    }
    //    // 判断命中的是否是空值
    //    if (shopJson != null) {
    //        return null;
    //    }
    //
    //    // 4. 不能查数据库，热点key，此时需要缓存重建
    //    // 4.1 获取互斥锁
    //    String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
    //    Shop shop = null;
    //    try {
    //        boolean isLock = tryLock(lockKey);
    //        // 4.2 判断是否获取成功
    //        if (!isLock) {
    //            // 4.3 失败，休眠并重试
    //            Thread.sleep(50);
    //            return queryWithMutex(id);
    //        }
    //        // 4.4 成功，根据id查redis
    //        shop = getById(id);
    //        // 模拟重建延时
    //        Thread.sleep(200);
    //
    //        // 5. 写入 Redis
    //        if (shop == null) {
    //            // 将空值写入 Redis
    //            stringRedisTemplate.opsForValue().set(key, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
    //            // 返回错误
    //            return null;
    //        }
    //        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop),
    //                RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
    //    } catch (InterruptedException e) {
    //        throw new RuntimeException(e);
    //    } finally {
    //        // 6. 释放互斥锁
    //        unLock(lockKey);
    //    }
    //
    //    // 7. 返回
    //    return shop;
    //}

    //private Shop queryWithPassThrough(Long id) {
    //    // 1. 从 Redis 查询商户缓存
    //    String key = RedisConstants.CACHE_SHOP_KEY + id;
    //    String shopJson = stringRedisTemplate.opsForValue().get(key);
    //    // 2. 判断是否存在
    //    if (StrUtil.isNotBlank(shopJson)) {
    //        // 3. 存在，直接返回
    //        Shop shop = JSONUtil.toBean(shopJson, Shop.class);
    //        return shop;
    //    }
    //    // 判断命中的是否是空值
    //    if (shopJson != null) {
    //        return null;
    //    }
    //
    //    // 4. 不存在，根据 id 查询数据库
    //    Shop shop = getById(id);
    //    // 4.1 不存在，返回错误
    //    if (shop == null) {
    //        // 将空值写入 Redis
    //        stringRedisTemplate.opsForValue().set(key, "",RedisConstants.CACHE_NULL_TTL,TimeUnit.MINUTES);
    //        // 返回错误信息
    //        return null;
    //    }
    //    // 4.2 存在，写入 Redis
    //    stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop),
    //            RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
    //
    //    // 5. 返回
    //    return shop;
    //}

    //// 获取锁
    //private boolean tryLock(String key) {
    //    // setnx，即SET if Not eXists set的值无法被更改
    //    Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
    //    return BooleanUtil.isTrue(flag);
    //}
    //
    //// 释放锁
    //private void unLock(String key) {
    //    stringRedisTemplate.delete(key);
    //}
    //
    //public void saveData2Redis(Long id, Long expireSeconds) throws InterruptedException {
    //    // 1. 查询店铺数据
    //    Shop shop = getById(id);
    //    Thread.sleep(200);
    //    // 2. 封装逻辑过期时间
    //    RedisData redisData = new RedisData();
    //    redisData.setData(shop);
    //    redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
    //    // 3. 写入 Redis
    //    stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id,
    //            JSONUtil.toJsonStr(redisData));
    //}

    @Override
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺id不能为空");
        }
        // 1. 先更新数据库
        updateById(shop);
        // 2. 再删除缓存
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY + id);

        return Result.ok(shop);
    }
}
