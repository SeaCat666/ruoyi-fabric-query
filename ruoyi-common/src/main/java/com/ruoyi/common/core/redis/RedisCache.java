package com.ruoyi.common.core.redis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

/**
 * spring redis 工具类
 *
 * @author ruoyi
 **/
@SuppressWarnings(value = { "unchecked", "rawtypes" })
@Component
public class RedisCache
{
    private static final Logger log = LoggerFactory.getLogger(RedisCache.class);

    @Value("${ruoyi.redis.enabled:true}")
    private boolean redisEnabled;

    @Autowired(required = false)
    public RedisTemplate redisTemplate;

    /**
     * 开发环境的进程内缓存。关闭 Redis 时用于验证码、令牌和系统配置，
     * 应用重启后缓存会自动清空。
     */
    private final Map<String, LocalCacheValue> localCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init()
    {
        if (!useRedis())
        {
            log.info("Redis已禁用，当前使用进程内缓存");
        }
    }

    /**
     * 缓存基本的对象，Integer、String、实体类等
     *
     * @param key 缓存的键值
     * @param value 缓存的值
     */
    public <T> void setCacheObject(final String key, final T value)
    {
        if (useRedis())
        {
            redisTemplate.opsForValue().set(key, value);
            return;
        }
        putLocal(key, value, 0L);
    }

    /**
     * 缓存基本的对象，Integer、String、实体类等
     *
     * @param key 缓存的键值
     * @param value 缓存的值
     * @param timeout 时间
     * @param timeUnit 时间颗粒度
     */
    public <T> void setCacheObject(final String key, final T value, final Integer timeout, final TimeUnit timeUnit)
    {
        if (useRedis())
        {
            redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
            return;
        }
        putLocal(key, value, System.currentTimeMillis() + timeUnit.toMillis(timeout));
    }

    /**
     * 设置有效时间
     *
     * @param key Redis键
     * @param timeout 超时时间
     * @return true=设置成功；false=设置失败
     */
    public boolean expire(final String key, final long timeout)
    {
        return expire(key, timeout, TimeUnit.SECONDS);
    }

    /**
     * 设置有效时间
     *
     * @param key Redis键
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return true=设置成功；false=设置失败
     */
    public boolean expire(final String key, final long timeout, final TimeUnit unit)
    {
        if (useRedis())
        {
            return redisTemplate.expire(key, timeout, unit);
        }
        LocalCacheValue current = getLocalEntry(key);
        if (current == null)
        {
            return false;
        }
        putLocal(key, current.value, System.currentTimeMillis() + unit.toMillis(timeout));
        return true;
    }

    /**
     * 获取有效时间
     *
     * @param key Redis键
     * @return 有效时间
     */
    public long getExpire(final String key)
    {
        if (useRedis())
        {
            return redisTemplate.getExpire(key);
        }
        LocalCacheValue current = getLocalEntry(key);
        if (current == null)
        {
            return -2L;
        }
        if (current.expireAtMillis == 0L)
        {
            return -1L;
        }
        return Math.max(0L, TimeUnit.MILLISECONDS.toSeconds(current.expireAtMillis - System.currentTimeMillis()));
    }

    /**
     * 判断 key是否存在
     *
     * @param key 键
     * @return true 存在 false不存在
     */
    public Boolean hasKey(String key)
    {
        if (useRedis())
        {
            return redisTemplate.hasKey(key);
        }
        return getLocalEntry(key) != null;
    }

    /**
     * 获得缓存的基本对象。
     *
     * @param key 缓存键值
     * @return 缓存键值对应的数据
     */
    public <T> T getCacheObject(final String key)
    {
        if (useRedis())
        {
            ValueOperations<String, T> operation = redisTemplate.opsForValue();
            return operation.get(key);
        }
        LocalCacheValue current = getLocalEntry(key);
        return current == null ? null : (T) current.value;
    }

    /**
     * 删除单个对象
     *
     * @param key 缓存键值
     */
    public boolean deleteObject(final String key)
    {
        if (useRedis())
        {
            return redisTemplate.delete(key);
        }
        return localCache.remove(key) != null;
    }

    /**
     * 删除集合对象
     *
     * @param collection 多个对象
     * @return 删除是否成功
     */
    public boolean deleteObject(final Collection collection)
    {
        if (useRedis())
        {
            return redisTemplate.delete(collection) > 0;
        }
        boolean removed = false;
        for (Object key : collection)
        {
            removed |= localCache.remove(String.valueOf(key)) != null;
        }
        return removed;
    }

    /**
     * 缓存List数据
     *
     * @param key 缓存的键值
     * @param dataList 待缓存的List数据
     * @return 缓存的对象数量
     */
    public <T> long setCacheList(final String key, final List<T> dataList)
    {
        if (useRedis())
        {
            Long count = redisTemplate.opsForList().rightPushAll(key, dataList);
            return count == null ? 0 : count;
        }
        putLocal(key, new ArrayList<>(dataList), 0L);
        return dataList.size();
    }

    /**
     * 获得缓存的list对象
     *
     * @param key 缓存的键值
     * @return 缓存列表
     */
    public <T> List<T> getCacheList(final String key)
    {
        if (useRedis())
        {
            return redisTemplate.opsForList().range(key, 0, -1);
        }
        LocalCacheValue current = getLocalEntry(key);
        return current == null ? new ArrayList<>() : new ArrayList<>((List<T>) current.value);
    }

    /**
     * 缓存Set
     *
     * @param key 缓存键值
     * @param dataSet 缓存的数据
     * @return Redis模式下返回绑定操作，本地模式返回null
     */
    public <T> BoundSetOperations<String, T> setCacheSet(final String key, final Set<T> dataSet)
    {
        if (!useRedis())
        {
            putLocal(key, new HashSet<>(dataSet), 0L);
            return null;
        }
        BoundSetOperations<String, T> setOperation = redisTemplate.boundSetOps(key);
        Iterator<T> it = dataSet.iterator();
        while (it.hasNext())
        {
            setOperation.add(it.next());
        }
        return setOperation;
    }

    /**
     * 获得缓存的set
     *
     * @param key 缓存键值
     * @return 缓存集合
     */
    public <T> Set<T> getCacheSet(final String key)
    {
        if (useRedis())
        {
            return redisTemplate.opsForSet().members(key);
        }
        LocalCacheValue current = getLocalEntry(key);
        return current == null ? new HashSet<>() : new HashSet<>((Set<T>) current.value);
    }

    /**
     * 缓存Map
     *
     * @param key 缓存键值
     * @param dataMap 待缓存Map
     */
    public <T> void setCacheMap(final String key, final Map<String, T> dataMap)
    {
        if (dataMap == null)
        {
            return;
        }
        if (useRedis())
        {
            redisTemplate.opsForHash().putAll(key, dataMap);
            return;
        }
        putLocal(key, new HashMap<>(dataMap), 0L);
    }

    /**
     * 获得缓存的Map
     *
     * @param key 缓存键值
     * @return 缓存Map
     */
    public <T> Map<String, T> getCacheMap(final String key)
    {
        if (useRedis())
        {
            return redisTemplate.opsForHash().entries(key);
        }
        LocalCacheValue current = getLocalEntry(key);
        return current == null ? new HashMap<>() : new HashMap<>((Map<String, T>) current.value);
    }

    /**
     * 往Hash中存入数据
     *
     * @param key Redis键
     * @param hKey Hash键
     * @param value 值
     */
    public <T> void setCacheMapValue(final String key, final String hKey, final T value)
    {
        if (useRedis())
        {
            redisTemplate.opsForHash().put(key, hKey, value);
            return;
        }
        synchronized (localCache)
        {
            Map<String, T> map = getCacheMap(key);
            map.put(hKey, value);
            putLocal(key, map, 0L);
        }
    }

    /**
     * 获取Hash中的数据
     *
     * @param key Redis键
     * @param hKey Hash键
     * @return Hash中的对象
     */
    public <T> T getCacheMapValue(final String key, final String hKey)
    {
        if (useRedis())
        {
            HashOperations<String, String, T> opsForHash = redisTemplate.opsForHash();
            return opsForHash.get(key, hKey);
        }
        return this.<T>getCacheMap(key).get(hKey);
    }

    /**
     * 获取多个Hash中的数据
     *
     * @param key Redis键
     * @param hKeys Hash键集合
     * @return Hash对象集合
     */
    public <T> List<T> getMultiCacheMapValue(final String key, final Collection<Object> hKeys)
    {
        if (useRedis())
        {
            return redisTemplate.opsForHash().multiGet(key, hKeys);
        }
        Map<String, T> map = getCacheMap(key);
        List<T> values = new ArrayList<>(hKeys.size());
        for (Object hKey : hKeys)
        {
            values.add(map.get(String.valueOf(hKey)));
        }
        return values;
    }

    /**
     * 删除Hash中的某条数据
     *
     * @param key Redis键
     * @param hKey Hash键
     * @return 是否成功
     */
    public boolean deleteCacheMapValue(final String key, final String hKey)
    {
        if (useRedis())
        {
            return redisTemplate.opsForHash().delete(key, hKey) > 0;
        }
        synchronized (localCache)
        {
            Map<String, Object> map = getCacheMap(key);
            boolean removed = map.remove(hKey) != null;
            if (removed)
            {
                putLocal(key, map, 0L);
            }
            return removed;
        }
    }

    /**
     * 获得缓存的基本对象列表
     *
     * @param pattern Redis glob格式
     * @return 对象列表
     */
    public Collection<String> keys(final String pattern)
    {
        if (useRedis())
        {
            return redisTemplate.keys(pattern);
        }
        Pattern keyPattern = compileGlob(pattern);
        List<String> keys = new ArrayList<>();
        for (String key : localCache.keySet())
        {
            if (getLocalEntry(key) != null && keyPattern.matcher(key).matches())
            {
                keys.add(key);
            }
        }
        return keys;
    }

    /**
     * 当前是否实际使用 Redis。
     *
     * @return Redis 已启用且连接模板可用时返回 true
     */
    public boolean isRedisEnabled()
    {
        return useRedis();
    }

    private boolean useRedis()
    {
        return redisEnabled && redisTemplate != null;
    }

    private void putLocal(String key, Object value, long expireAtMillis)
    {
        localCache.put(key, new LocalCacheValue(value, expireAtMillis));
    }

    private LocalCacheValue getLocalEntry(String key)
    {
        LocalCacheValue current = localCache.get(key);
        if (current != null && current.isExpired())
        {
            localCache.remove(key, current);
            return null;
        }
        return current;
    }

    private Pattern compileGlob(String glob)
    {
        StringBuilder regex = new StringBuilder("^");
        for (int i = 0; i < glob.length(); i++)
        {
            char current = glob.charAt(i);
            if (current == '*')
            {
                regex.append(".*");
            }
            else if (current == '?')
            {
                regex.append('.');
            }
            else
            {
                regex.append(Pattern.quote(String.valueOf(current)));
            }
        }
        regex.append('$');
        return Pattern.compile(regex.toString());
    }

    private static final class LocalCacheValue
    {
        private final Object value;
        private final long expireAtMillis;

        private LocalCacheValue(Object value, long expireAtMillis)
        {
            this.value = value;
            this.expireAtMillis = expireAtMillis;
        }

        private boolean isExpired()
        {
            return expireAtMillis > 0L && expireAtMillis <= System.currentTimeMillis();
        }
    }
}
