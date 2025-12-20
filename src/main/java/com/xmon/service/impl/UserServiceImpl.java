package com.xmon.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xmon.dto.LoginFormDTO;
import com.xmon.dto.Result;
import com.xmon.entity.User;
import com.xmon.mapper.UserMapper;
import com.xmon.service.IUserService;
import com.xmon.utils.RegexUtils;
import com.xmon.utils.SystemConstants;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 1.校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 2.不符合，返回错误信息
            return Result.fail("手机号格式错误！");
        }

        // 3.符合，生成验证码
        String code = RandomUtil.randomNumbers(6);

        // 4.保存验证码到session
        session.setAttribute("code", code);

        // 5.发送验证码
        log.debug("短信验证码发送成功, 验证码：{}", code);

        // 6.返回OK
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // 1.校验手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误！");
        }

        // 2.校验验证码，是否为空并且是否一致
        Object cacheCode = session.getAttribute("code");
        String code = loginForm.getCode();
        if (cacheCode == null || !cacheCode.toString().equals(code)) {
            // 3.不一致，报错
            return Result.fail("验证码为空或不一致，请重新输入！");
        }

        // 4.一致，根据手机号查询用户
        User user = query().eq("phone", phone).one();

        // 5.判断用户是否存在
        if (user == null) {
            // 6.不存在，创建新用户并保存到数据库
            user = createUserWithPhone(phone);
        }

        // 7.（不存在和存在都要）保存用户信息到session
        session.setAttribute("user", user);
        return Result.ok();
    }

    private User createUserWithPhone(String phone) {
        // 1.创建用户
        User user = new User();
        user.setPhone(phone);
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));

        // 2.保存用户
        save(user);
        return user;
    }
}
