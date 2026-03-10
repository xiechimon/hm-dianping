package com.xmon.service.impl;

import com.xmon.dto.Result;
import com.xmon.entity.SeckillVoucher;
import com.xmon.entity.Shop;
import com.xmon.entity.VoucherOrder;
import com.xmon.mapper.VoucherOrderMapper;
import com.xmon.service.ISeckillVoucherService;
import com.xmon.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xmon.service.IVoucherService;
import com.xmon.utils.RedisIdWorker;
import com.xmon.utils.UserHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Override
    @Transactional
    public Result seckillVoucher(Long voucherId) {
        // 1. 查询优惠券
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);

        // 2. 判断秒杀是否结束
        if (LocalDateTime.now().isAfter(voucher.getEndTime())) {
            return Result.fail("秒杀已经结束！");
        }

        // 3. 是否开始
        if (LocalDateTime.now().isBefore(voucher.getBeginTime())) {
            return Result.fail("秒杀尚未开始！");
        }
        // 4. 开始，判断库存是否充足
        if (voucher.getStock() < 1) {
            return Result.fail("库存不足");
        }
        // 5 充足，扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1 ")
                .eq("voucher_id", voucherId).update();
        if (!success) {
            // 扣减失败
            return Result.fail("库存不足");
        }

        // 6. 创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        // 订单ID
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        // 用户ID
        Long userId = UserHolder.getUser().getId();
        voucherOrder.setUserId(userId);
        // 代金券ID
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);

        // 7. 返回订单ID
        return Result.ok(orderId);
    }
}
