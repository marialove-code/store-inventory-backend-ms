package com.inventory.modules.ai.support;

import com.inventory.common.constants.RedisConstants;
import com.inventory.modules.ai.config.AiChatProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis + TTL 的聊天会话存储实现（第四站 · 学习/生产可选方案）。
 * <p>
 * 与 {@link InMemoryChatSessionStore} 一样，实现 {@link ChatSessionStore} 接口，
 * 保证 {@link com.inventory.modules.ai.service.impl.AiChatServiceImpl} 只依赖抽象、不写死存储方式。
 * </p>
 * <p>
 * 生活类比：便签本从「收银台抽屉里的纸」（JVM 内存）换成「门店共用的电子记事本」（Redis）——
 * 打烊重启（JVM 重启）纸丢了，但 Redis 里的记录还在；超过 {@code session-ttl-seconds} 没人翻的页会自动撕掉（TTL）。
 * </p>
 * <p>
 * <b>如何启用（学习对比时改 yml，默认仍是 memory）：</b>
 * <pre>
 * inventory:
 *   ai:
 *     chat:
 *       session-store: redis
 *       session-ttl-seconds: 604800   # 7 天，实验可改 120（2 分钟）
 * </pre>
 * 本类带 {@link ConditionalOnProperty}，仅当 {@code session-store=redis} 时注册为 Bean；
 * 与 {@link InMemoryChatSessionStore} 互斥，同一时刻 Spring 容器里只有一个 {@link ChatSessionStore} 实现。
 * </p>
 * <p>
 * <b>Redis 数据结构：</b>
 * <ul>
 *   <li>Key：{@code inventory:ai:chat:session:{sessionId}}</li>
 *   <li>Value：{@link ChatHistoryEntry} 列表（经 {@link RedisTemplate} JSON 序列化）</li>
 *   <li>TTL：每次 {@link #appendTurn} 写入时刷新；到期 Redis 自动 DELETE 整个 Key</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "inventory.ai.chat", name = "session-store", havingValue = "redis")
public class RedisChatSessionStore implements ChatSessionStore {

    /**
     * 项目统一的 Redis 客户端（见 {@link com.inventory.config.redis.RedisConfig}）。
     * Key 用 String 序列化，Value 用 Jackson JSON 序列化。
     */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 业务配置：历史条数上限 {@link AiChatProperties#getMaxHistoryMessages()}、
     * TTL 秒数 {@link AiChatProperties#getSessionTtlSeconds()}。
     */
    private final AiChatProperties aiChatProperties;

    /**
     * 读取指定会话的历史，供 {@code callModel} 拼 Prompt 使用。
     * <p>
     * 流程：拼 Redis Key → GET → {@link #readEntries} 反序列化 → {@link #toMessages} 转成 Spring AI 的 Message。
     * Key 不存在或已 TTL 过期 → 返回空列表（等同首次对话，不会抛异常）。
     * </p>
     * <p>
     * 与内存版区别：数据在 Redis，<strong>JVM 重启后历史仍可恢复</strong>（TTL 未到期时）；
     * 多实例部署时各节点读同一 Key，<strong>会话可共享</strong>。
     * </p>
     *
     * @param sessionId 与前端/响应体中的 sessionId 一致
     * @return Spring AI Message 列表（User/Assistant），无记录时为空列表
     */
    @Override
    public List<Message> getHistory(String sessionId) {
        List<ChatHistoryEntry> entries = readEntries(redisTemplate.opsForValue().get(buildKey(sessionId)));
        return toMessages(entries);
    }

    /**
     * 追加一轮对话（用户问 + 助手答），并刷新 TTL。
     * <p>
     * 采用「读-改-写」模式（非 Redis List 原子 LPUSH），实现简单、便于学习理解：
     * <ol>
     *   <li>GET 当前列表（可能为 null）</li>
     *   <li>追加 User、Assistant 两条 {@link ChatHistoryEntry}</li>
     *   <li>{@link #trimHistory} 裁剪超长历史</li>
     *   <li>SET 回 Redis，并带上 TTL（秒）</li>
     * </ol>
     * 每次写入都会<strong>重置 TTL 倒计时</strong>，类似「每次聊天给便签本续期 7 天」。
     * </p>
     * <p>
     * 调用时机与内存版相同：仅在 {@code callModel} 成功之后，由 Service 调用；
     * 失败/降级路径不会执行 appendTurn，避免历史里留下「有问无答」。
     * </p>
     * <p>
     * 并发说明：极端情况下同一 session 并发 append 可能覆盖（后写覆盖先写）；
     * 客服场景同一用户连点较少，学习/demo 可接受；生产高并发可改用 Redis List + Lua 或分布式锁。
     * </p>
     *
     * @param sessionId       会话 ID
     * @param userText        用户本轮输入
     * @param assistantText   助手本轮回复
     */
    @Override
    public void appendTurn(String sessionId, String userText, String assistantText) {
        String key = buildKey(sessionId);

        // 1. 读出已有历史（新 session 则为空列表）
        List<ChatHistoryEntry> entries = readEntries(redisTemplate.opsForValue().get(key));

        // 2. 追加本轮一问一答（顺序：User 在前，Assistant 在后，与聊天时间线一致）
        entries.add(ChatHistoryEntry.user(userText));
        entries.add(ChatHistoryEntry.assistant(assistantText));

        // 3. 单会话最多保留 max-history-messages 条（默认 20 条 ≈ 10 轮）
        trimHistory(entries);

        // 4. 写回 Redis，并设置/刷新 TTL（至少 60 秒，防止配置误填 0）
        long ttl = Math.max(60L, aiChatProperties.getSessionTtlSeconds());
        redisTemplate.opsForValue().set(key, entries, ttl, TimeUnit.SECONDS);

        log.debug("AI 会话写入 Redis key={} size={} ttlSeconds={}", key, entries.size(), ttl);
    }

    /**
     * 删除指定会话在 Redis 中的全部历史（整 Key 删除）。
     * <p>
     * 预留能力：前端「新对话」时可调用；Key 不存在时 delete 不会报错。
     * </p>
     *
     * @param sessionId 要清空的会话 ID
     */
    @Override
    public void clear(String sessionId) {
        redisTemplate.delete(buildKey(sessionId));
    }

    /**
     * 构造 Redis Key。
     * <p>
     * 格式：{@link RedisConstants#AI_CHAT_SESSION_PREFIX} + sessionId<br>
     * 示例：{@code inventory:ai:chat:session:d492b4b29d1146edb1844953be481ee2}
     * </p>
     *
     * @param sessionId 会话 ID
     * @return 完整 Redis Key
     */
    private String buildKey(String sessionId) {
        return RedisConstants.AI_CHAT_SESSION_PREFIX + sessionId;
    }

    /**
     * 裁剪历史长度，逻辑与 {@link InMemoryChatSessionStore} 完全一致。
     * <p>
     * 计数单位是<strong>消息条数</strong>（User、Assistant 各算 1 条），不是「轮数」。
     * 从列表头部删除最早消息，保证 Prompt 不会无限变长、控制 Token 费用。
     * </p>
     *
     * @param entries 当前会话条目列表（可变，原地修改）
     */
    private void trimHistory(List<ChatHistoryEntry> entries) {
        int max = Math.max(2, aiChatProperties.getMaxHistoryMessages());
        while (entries.size() > max) {
            entries.remove(0);
        }
    }

    /**
     * 将可序列化 DTO {@link ChatHistoryEntry} 转回 Spring AI 的 {@link Message}，
     * 以便 {@link com.inventory.modules.ai.service.impl.AiChatServiceImpl#callModel} 直接 {@code addAll}。
     * <p>
     * 为何不直接存 Message？Message 是接口/实现类体系，Redis JSON 序列化不稳定；
     * 用 role + content 的 POJO 更清晰，也是生产常见做法。
     * </p>
     *
     * @param entries Redis 中读出的条目列表
     * @return UserMessage / AssistantMessage 列表（不含 SystemMessage）
     */
    private List<Message> toMessages(List<ChatHistoryEntry> entries) {
        List<Message> messages = new ArrayList<>(entries.size());
        for (ChatHistoryEntry entry : entries) {
            if ("assistant".equalsIgnoreCase(entry.getRole())) {
                messages.add(new AssistantMessage(entry.getContent()));
            } else {
                // 默认按 user 处理（当前仅存 user/assistant 两种）
                messages.add(new UserMessage(entry.getContent()));
            }
        }
        return messages;
    }

    /**
     * 将 Redis GET 得到的 Object 安全转为 {@link ChatHistoryEntry} 列表。
     * <p>
     * 项目 {@link com.inventory.config.redis.RedisConfig} 使用 GenericJackson2JsonRedisSerializer，
     * 反序列化后列表元素有时是 {@link ChatHistoryEntry}，有时是 {@code LinkedHashMap}（Jackson 默认行为），
     * 因此需要做 instanceof 分支，避免强转 ClassCastException。
     * </p>
     *
     * @param cached {@code redisTemplate.opsForValue().get(key)} 的返回值，可能为 null
     * @return 条目列表；null 或非 List 时返回空 ArrayList
     */
    private List<ChatHistoryEntry> readEntries(Object cached) {
        if (!(cached instanceof List<?> list)) {
            return new ArrayList<>();
        }
        List<ChatHistoryEntry> entries = new ArrayList<>(list.size());
        for (Object item : list) {
            if (item instanceof ChatHistoryEntry entry) {
                entries.add(entry);
            } else if (item instanceof Map<?, ?> map) {
                // Jackson 把 JSON 对象读成 Map 时的兜底转换
                entries.add(new ChatHistoryEntry(
                        String.valueOf(map.get("role")),
                        String.valueOf(map.get("content"))
                ));
            }
        }
        return entries;
    }
}
