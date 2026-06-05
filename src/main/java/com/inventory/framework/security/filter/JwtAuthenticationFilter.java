package com.inventory.framework.security.filter;

import cn.hutool.core.util.StrUtil;
import com.inventory.framework.security.jwt.JwtUtil;
import com.inventory.common.constants.RedisConstants;
import com.inventory.framework.security.context.LoginUserContext;
import com.inventory.modules.auth.vo.LoginUserVO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * JWT认证过滤器
 * 所有请求都会经过这里
 * 负责：
 * 1. 解析JWT
 * 2. 校验JWT合法性
 * 3. 校验Redis登录态
 * 4. 设置SpringSecurity认证信息
 * 5. 设置当前登录用户上下文
 * 6. JWT临近过期自动续期
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** JWT工具类：生成、解析、校验、过期判断 */
    private final JwtUtil jwtUtil;

    /** Redis操作模板：校验登录态、获取用户信息 */
    private final RedisTemplate<String, Object> redisTemplate;

    /** 请求头key：从header获取token，默认Authorization */
    @Value("${jwt.header:Authorization}")
    private String header;

    /** Token前缀：默认Bearer  */
    @Value("${jwt.token-prefix:Bearer }")
    private String tokenPrefix;

    /** Token过期错误码：与前端约定统一返回码 */
    private static final int TOKEN_EXPIRED_CODE = 1102;

//    // ===================== 自动续期配置 =====================
//    /** 自动续期阈值：剩余过期时间小于该值自动续期（秒） */
//    private static final long RENEW_THRESHOLD_SECOND = 300;
//    /** 响应头存放新令牌：前端从该头获取新token替换本地 */
//    private static final String NEW_TOKEN_HEADER = "new-access-token";
//    // ======================================================

    /**
     * 过滤器核心方法：每个请求只执行一次
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            // 获取当前请求URI，便于日志排查
            String requestURI = request.getRequestURI();
            System.out.println("当前请求URI：" + requestURI);

            // 免过滤路径：上传头像接口直接放行，不做JWT校验
            if (requestURI.startsWith("/api/upload/avatar/")) {
                filterChain.doFilter(request, response);
                return;
            }

            // 1. 从请求头获取token
            String token = request.getHeader(header);

            // 2. token为空或格式不正确，直接放行，由后续Security处理
            if (StrUtil.isBlank(token) || !token.startsWith(tokenPrefix)) {
                filterChain.doFilter(request, response);
                return;
            }

            // 3. 去除Bearer前缀，获取纯token
            token = token.substring(tokenPrefix.length()).trim();

            // 4. 校验token是否过期，过期直接返回错误
            if (jwtUtil.isTokenExpired(token)) {
                writeErrorResponseWithRedirect(response, TOKEN_EXPIRED_CODE, "登录已过期，请重新登录");
                return;
            }

            // 5. 校验token签名是否合法，无效直接返回错误
            if (!jwtUtil.validateToken(token)) {
                writeErrorResponseWithRedirect(response, TOKEN_EXPIRED_CODE, "Token无效，请重新登录");
                return;
            }

//            // ===================== 自动续期逻辑 =====================
//            // 获取token剩余过期时间
//            long remainExpireSecond = jwtUtil.getRemainExpireTime(token);
//            // 剩余时间小于阈值，触发自动续期
//            if (remainExpireSecond < RENEW_THRESHOLD_SECOND) {
//                // 从旧token解析用户信息
//                Long userId = jwtUtil.getUserId(token);
//                String username = jwtUtil.getUsername(token);
//                // 生成新的accessToken和refreshToken
//                String newAccessToken = jwtUtil.createAccessToken(userId, username);
//                String newRefreshToken = jwtUtil.createRefreshToken(userId, username);
//                // 将新token放入响应头，前端自动替换
//                response.setHeader(NEW_TOKEN_HEADER, tokenPrefix + newAccessToken);
//                log.info("用户{}令牌临近过期，已自动续期", username);
//            }

            // 6. Redis校验登录态：拼接RedisKey，获取登录用户信息
            Long userId = jwtUtil.getUserId(token);
            String redisKey = RedisConstants.LOGIN_TOKEN_PREFIX + userId + ":access:" + token;
            Object loginUserObj = redisTemplate.opsForValue().get(redisKey);

            // Redis中无登录态，说明已被踢下线或登录失效
            if (loginUserObj == null) {
                log.warn("登录态已失效或被踢下线: {}", token);
                writeErrorResponseWithRedirect(response, TOKEN_EXPIRED_CODE, "权限已变更或登录失效，即将跳转登录页");
                return;
            }

            // 7. 防止重复认证：上下文已有认证信息则跳过
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                // 8. 从Redis强转为登录用户VO
                LoginUserVO loginUser = (LoginUserVO) loginUserObj;

                // 9. 将用户信息存入ThreadLocal上下文，供全链路使用
                LoginUserContext.setUser(loginUser);
                LoginUserContext.setAccessToken(token);

                // 10. SpringSecurity权限集合：本项目权限由业务控制，此处设为空
                List<SimpleGrantedAuthority> authorities = Collections.emptyList();

                // 11. 构建SpringSecurity内置User对象
                User user = new User(loginUser.getUsername(), "", authorities);

                // 12. 构建认证对象：已认证状态
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);

                // 13. 设置请求详情信息（IP、Session等）
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 14. 将认证信息存入SpringSecurity上下文
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            // 15. 放行请求，进入后续过滤器/控制器
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            // 捕获所有异常，统一返回认证失败
            log.error("JWT认证失败: {}", e.getMessage(), e);
            writeErrorResponseWithRedirect(response, TOKEN_EXPIRED_CODE, "认证失败: " + e.getMessage());
        } finally {
            // 必须清理ThreadLocal，防止线程池复用导致用户信息串号
            LoginUserContext.clear();
        }
    }

    /**
     * 统一写入错误响应JSON（Filter中无法走全局异常，必须手动返回）
     * 携带跳转登录页标记，前端根据字段自动跳转
     */
    private void writeErrorResponseWithRedirect(HttpServletResponse response, int code, String msg) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        // 构造前端约定的JSON格式：code、msg、是否跳转、延迟时间
        String json = String.format("{\"code\":%d,\"msg\":\"%s\",\"redirectLogin\":true,\"redirectDelay\":2000}", code, msg);
        response.getWriter().write(json);
    }
}