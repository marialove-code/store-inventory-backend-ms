package com.inventory.filter;

import cn.hutool.core.util.StrUtil;
import com.inventory.common.utils.JwtUtil;
import com.inventory.constant.RedisConstants;
import com.inventory.context.LoginUserContext;
import com.inventory.entity.login.LoginUserVO;
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
 *
 * 所有请求都会经过这里
 *
 * 负责：
 * 1. 解析JWT
 * 2. 校验JWT合法性
 * 3. 校验Redis登录态
 * 4. 设置SpringSecurity认证信息
 * 5. 设置当前登录用户上下文
 * 6. 新增：JWT临近过期自动续期
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /**
     * JWT工具类
     */
    private final JwtUtil jwtUtil;

    /**
     * Redis
     */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 请求头key
     */
    @Value("${jwt.header:Authorization}")
    private String header;

    /**
     * Token前缀
     * 例如：Bearer
     */
    @Value("${jwt.token-prefix:Bearer }")
    private String tokenPrefix;

    /**
     * Token过期错误码（与前端约定）
     */
    private static final int TOKEN_EXPIRED_CODE = 1102;

    // ===================== 新增续期配置 =====================
    /**
     * 触发自动续期阈值：剩余过期时间小于该秒数 自动续期
     * 例如：30分钟令牌，剩余5分钟就自动续期
     */
    private static final long RENEW_THRESHOLD_SECOND = 300;
    /**
     * 响应头存放新令牌key，前端从这里拿新token替换本地旧token
     */
    private static final String NEW_TOKEN_HEADER = "new-access-token";
    // ======================================================

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            /**
             * 1. 获取请求头中的Token
             */
            String token = request.getHeader(header);

            /**
             * 2. Token不存在或格式不对，直接放行
             *
             * 后续由SpringSecurity决定是否拦截
             */
            if (StrUtil.isBlank(token) || !token.startsWith(tokenPrefix)) {
                filterChain.doFilter(request, response);
                return;
            }

            /**
             * 3. 去掉 Bearer 前缀
             */
            token = token.substring(tokenPrefix.length()).trim();

            /**
             * 4. 校验Token是否过期
             *
             * 【优化】如果过期，返回1102错误码，前端自动刷新
             */
            if (jwtUtil.isTokenExpired(token)) {
                writeErrorResponse(response, TOKEN_EXPIRED_CODE, "Token已过期");
                return;
            }

            /**
             * 5. 校验Token签名是否有效
             */
            if (!jwtUtil.validateToken(token)) {
                writeErrorResponse(response, TOKEN_EXPIRED_CODE, "Token无效");
                return;
            }

            // ===================== 新增自动续期逻辑开始 =====================
            // 获取当前token剩余有效时长（秒）
            long remainExpireSecond = jwtUtil.getRemainExpireTime(token);
            // 判断是否达到续期条件：剩余时间小于阈值
            if (remainExpireSecond < RENEW_THRESHOLD_SECOND) {
                // 解析当前用户信息
                Long userId = jwtUtil.getUserId(token);
                String username = jwtUtil.getUsername(token);
                // 生成全新accessToken
                String newAccessToken = jwtUtil.createAccessToken(userId, username);
                // 将新令牌放入响应头返回给前端
                response.setHeader(NEW_TOKEN_HEADER, tokenPrefix + newAccessToken);
                log.info("用户{}令牌临近过期，已自动完成续期", username);
            }
            // ===================== 新增自动续期逻辑结束 =====================


            /**
             * 6. Redis登录态校验
             *
             * 防止：
             * - 退出登录后JWT仍然有效
             * - 用户被踢下线
             * - 用户被禁用
             */
            String redisKey = RedisConstants.LOGIN_TOKEN_KEY + token;
            Object loginUserObj = redisTemplate.opsForValue().get(redisKey);

            if (loginUserObj == null) {
                log.warn("登录态已失效: {}", token);
                writeErrorResponse(response, TOKEN_EXPIRED_CODE, "登录态已失效");
                return;
            }

            /**
             * 7. 防止重复认证
             */
            if (SecurityContextHolder.getContext().getAuthentication() == null) {

                /**
                 * 8. 从JWT解析用户信息
                 */
                Long userId = jwtUtil.getUserId(token);
                String username = jwtUtil.getUsername(token);

                /**
                 * 9. 从Redis获取真实登录用户
                 */
                LoginUserVO loginUser = (LoginUserVO) loginUserObj;

                /**
                 * 10. 存入ThreadLocal
                 *
                 * 后续业务代码可直接获取：
                 * LoginUserContext.getUserId()
                 */
                LoginUserContext.setUser(loginUser);

                /**
                 * 11. 权限列表
                 */
                List<SimpleGrantedAuthority> authorities = Collections.emptyList();

                /**
                 * 12. SpringSecurity用户对象
                 */
                User user = new User(username, "", authorities);

                /**
                 * 13. 构建认证对象
                 */
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                user,
                                null,
                                authorities
                        );

                /**
                 * 14. 设置请求详情
                 */
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                /**
                 * 15. 存入SpringSecurity上下文
                 */
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            /**
             * 16. 放行请求
             */
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            /**
             * 【优化】捕获异常，返回统一错误格式
             * 原因：Filter抛的异常@ControllerAdvice捕获不到
             */
            log.error("JWT认证失败: {}", e.getMessage(), e);
            writeErrorResponse(response, TOKEN_EXPIRED_CODE, "认证失败: " + e.getMessage());

        } finally {
            /**
             * 必须清理ThreadLocal
             *
             * 防止线程复用导致用户数据串号
             */
            LoginUserContext.clear();
        }
    }

    /**
     * ============================================
     * 写入错误响应
     * ============================================
     *
     * 【关键】Filter中必须手动写响应，因为@ControllerAdvice捕获不到Filter的异常
     *
     * @param response HTTP响应
     * @param code     错误码（1102表示Token过期，前端自动刷新）
     * @param msg      错误信息
     */
    private void writeErrorResponse(HttpServletResponse response, int code, String msg) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        String json = String.format("{\"code\":%d,\"msg\":\"%s\"}", code, msg);
        response.getWriter().write(json);
    }
}