package com.xmon.utils;

import com.xmon.dto.UserDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;

public class LoginInterceptor implements HandlerInterceptor {

    /**
     * 登录校验（进入Controller之前）
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1.获取Session
        HttpSession session = request.getSession();
        // 2.获取Session中的用户
        Object user = session.getAttribute("user");
        // 3.判断用户是否存在
        if (user == null) {
            // 4.不存在，拦截
            response.setStatus(401);
            return false;
        }
        // 5.存在，保存用户信息到ThreadLocal
        UserHolder.saveUser((UserDTO) user);

        // 6.放行
        return true;
    }

    /**
     * 销毁用户信息
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
        // 移除用户
        UserHolder.removeUser();
    }
}
