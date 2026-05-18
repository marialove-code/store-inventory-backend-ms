package com.inventory.common.utils;

import com.inventory.common.exception.BusinessException;
import com.inventory.common.result.ResultCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 工具类
 * 支持双Token：AccessToken 业务鉴权、RefreshToken 无感刷新
 */
@Slf4j
@Component
public class JwtUtil {

    // 自定义载荷Key常量
    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_TOKEN_TYPE = "tokenType";

    // Token类型标识常量
    public static final String ACCESS_TOKEN = "access";
    public static final String REFRESH_TOKEN = "refresh";

    // 从配置文件读取JWT秘钥
    @Value("${jwt.secret}")
    private String secret;

    // AccessToken过期时间 单位分钟
    @Value("${jwt.access-expire}")
    private Long accessExpire;

    // RefreshToken过期时间 单位分钟
    @Value("${jwt.refresh-expire}")
    private Long refreshExpire;

    /**
     * 根据配置的秘钥生成加密签名密钥
     * 采用HMAC SHA256算法
     */
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 访问令牌 AccessToken
     * 用于接口正常鉴权，有效期短
     */
    public String createAccessToken(Long userId, String username) {
        return createToken(userId, username, ACCESS_TOKEN, accessExpire);
    }

    /**
     * 生成 刷新令牌 RefreshToken
     * 用于AccessToken过期后换新令牌，有效期长
     */
    public String createRefreshToken(Long userId, String username) {
        return createToken(userId, username, REFRESH_TOKEN, refreshExpire);
    }

    /**
     * 通用创建Token方法
     * @param userId 用户ID
     * @param username 用户名
     * @param tokenType 令牌类型 access/refresh
     * @param expireMinutes 过期时间(分钟)
     * @return 生成的JWT令牌字符串
     */
    private String createToken(Long userId,
                               String username,
                               String tokenType,
                               Long expireMinutes) {
        // 当前时间
        Date now = new Date();
        // 计算过期时间
        Date expireDate = new Date(now.getTime() + expireMinutes * 60 * 1000);

        // 构建JWT
        return Jwts.builder()
                // 设置JWT唯一ID
                .setId(UUID.randomUUID().toString())
                // 自定义载荷存放用户信息和令牌类型
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                // 签发时间
                .setIssuedAt(now)
                // 过期时间
                .setExpiration(expireDate)
                // 加密签名
                .signWith(getSecretKey(), SignatureAlgorithm.HS256)
                // 压缩为字符串
                .compact();
    }

    /**
     * ============================================
     * 【新增】判断Token是否过期
     * ============================================
     *
     * 用途：Filter中先判断过期，再决定是否返回1102错误码
     *
     * @param token JWT令牌
     * @return true=已过期 false=未过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSecretKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            Date expiration = claims.getExpiration();
            return expiration != null && expiration.before(new Date());

        } catch (ExpiredJwtException e) {
            // 明确过期
            return true;
        } catch (Exception e) {
            // 其他异常（签名错误、格式错误等）也视为过期
            log.warn("Token解析失败，视为过期: {}", e.getMessage());
            return true;
        }
    }


    /**
     * 获取Token剩余有效时间（单位：秒）
     * 用于自动续期判断：剩余时间 < 阈值 → 自动续期
     */
    public long getRemainExpireTime(String token) {
        try {
            // 解析token（不校验过期，只拿过期时间）
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSecretKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            Date expireDate = claims.getExpiration();
            if (expireDate == null) {
                return 0;
            }

            // 当前时间毫秒 - 过期时间毫秒 → 剩余毫秒
            long now = System.currentTimeMillis();
            long expireMills = expireDate.getTime();
            long remainMills = expireMills - now;

            // 小于0 说明已过期
            if (remainMills <= 0) {
                return 0;
            }

            // 转成秒返回
            return remainMills / 1000;

        } catch (ExpiredJwtException e) {
            // 已经过期 → 剩余0秒
            return 0;
        } catch (Exception e) {
            // 解析失败 → 视为0
            return 0;
        }
    }

    /**
     * ============================================
     * 【新增】验证Token签名是否有效（不抛异常）
     * ============================================
     *
     * 用途：Filter中验证token是否被篡改
     *
     * @param token JWT令牌
     * @return true=有效 false=无效
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSecretKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            log.warn("Token验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 解析JWT令牌，获取载荷Claims
     * 捕获各类异常并打印日志
     * @param token JWT令牌
     * @return 载荷信息 解析失败返回null
     */
    public Claims parseToken(String token) {
        try {
            // 新版JJWT标准解析方式
            return Jwts.parserBuilder()
                    .setSigningKey(getSecretKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            // 1102 → 前端无感刷新 核心触发码
            log.error("Token已过期：{}", e.getMessage());
            throw new BusinessException(ResultCode.TOKEN_EXPIRED);
        } catch (UnsupportedJwtException e) {
            log.error("Token格式不支持");
            throw new BusinessException(ResultCode.TOKEN_INVALID); // 1101
        } catch (MalformedJwtException e) {
            log.error("Token非法篡改或格式错误");
            throw new BusinessException(ResultCode.TOKEN_INVALID); // 1101
        } catch (SignatureException e) {
            log.error("Token签名校验失败");
            throw new BusinessException(ResultCode.TOKEN_INVALID); // 1101
        } catch (IllegalArgumentException e) {
            log.error("Token为空字符串");
            throw new BusinessException(ResultCode.TOKEN_EMPTY); // 1103
        } catch (Exception e) {
            log.error("Token解析异常：{}", e.getMessage());
            throw new BusinessException(ResultCode.TOKEN_PARSE_ERROR); // 1104
        }
    }

    /**
     * 校验是否为合法的AccessToken
     * @param token 令牌
     * @return 合法true 非法false
     * old
     */
   /* public boolean validateAccessToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return false;
        }
        // 校验令牌类型必须是access
        return ACCESS_TOKEN.equals(claims.get(CLAIM_TOKEN_TYPE));
    }*/

    /**
     * 校验是否为合法的RefreshToken
     * @param token 令牌
     * @return 合法true 非法false
     */
    public boolean validateRefreshToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return false;
        }
        // 校验令牌类型必须是refresh
        return REFRESH_TOKEN.equals(claims.get(CLAIM_TOKEN_TYPE));
    }

    /**
     * 从Token中获取用户ID
     * @param token 令牌
     * @return 用户ID 解析失败返回null
     */
    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }
        return Long.valueOf(claims.get(CLAIM_USER_ID).toString());
    }

    /**
     * 从Token中获取用户名
     * @param token 令牌
     * @return 用户名 解析失败返回null
     */
    public String getUsername(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }
        return claims.get(CLAIM_USERNAME).toString();
    }

}
