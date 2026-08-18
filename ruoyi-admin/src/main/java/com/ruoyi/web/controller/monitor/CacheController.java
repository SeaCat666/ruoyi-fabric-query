package com.ruoyi.web.controller.monitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.lang.management.ManagementFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.SysCache;
import com.alibaba.fastjson2.JSON;

/**
 * 缓存监控
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/monitor/cache")
public class CacheController
{
    @Autowired(required = false)
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RedisCache redisCache;

    private final static List<SysCache> caches = new ArrayList<SysCache>();
    {
        caches.add(new SysCache(CacheConstants.LOGIN_TOKEN_KEY, "用户信息"));
        caches.add(new SysCache(CacheConstants.SYS_CONFIG_KEY, "配置信息"));
        caches.add(new SysCache(CacheConstants.SYS_DICT_KEY, "数据字典"));
        caches.add(new SysCache(CacheConstants.CAPTCHA_CODE_KEY, "验证码"));
        caches.add(new SysCache(CacheConstants.REPEAT_SUBMIT_KEY, "防重提交"));
        caches.add(new SysCache(CacheConstants.RATE_LIMIT_KEY, "限流处理"));
        caches.add(new SysCache(CacheConstants.PWD_ERR_CNT_KEY, "密码错误次数"));
    }

    @SuppressWarnings("deprecation")
    @PreAuthorize("@ss.hasPermi('monitor:cache:list')")
    @GetMapping()
    public AjaxResult getInfo() throws Exception
    {
        if (!redisCache.isRedisEnabled())
        {
            return AjaxResult.success(getLocalCacheInfo());
        }

        Properties info = (Properties) redisTemplate.execute((RedisCallback<Object>) connection -> connection.info());
        Properties commandStats = (Properties) redisTemplate.execute((RedisCallback<Object>) connection -> connection.info("commandstats"));
        Object dbSize = redisTemplate.execute((RedisCallback<Object>) connection -> connection.dbSize());

        Map<String, Object> result = new HashMap<>(7);
        result.put("storageType", "redis");
        result.put("storageLabel", "Redis");
        result.put("info", info);
        result.put("dbSize", dbSize);
        result.put("memoryUsedBytes", parseLong(info.getProperty("used_memory")));
        result.put("memoryMaxBytes", parseLong(info.getProperty("maxmemory")));

        List<Map<String, String>> pieList = new ArrayList<>();
        if (commandStats != null)
        {
            commandStats.stringPropertyNames().forEach(key -> {
                Map<String, String> data = new HashMap<>(2);
                String property = commandStats.getProperty(key);
                data.put("name", StringUtils.removeStart(key, "cmdstat_"));
                data.put("value", StringUtils.substringBetween(property, "calls=", ",usec"));
                pieList.add(data);
            });
        }
        result.put("commandStats", pieList);
        return AjaxResult.success(result);
    }

    @PreAuthorize("@ss.hasPermi('monitor:cache:list')")
    @GetMapping("/getNames")
    public AjaxResult cache()
    {
        return AjaxResult.success(caches);
    }

    @PreAuthorize("@ss.hasPermi('monitor:cache:list')")
    @GetMapping("/getKeys/{cacheName}")
    public AjaxResult getCacheKeys(@PathVariable String cacheName)
    {
        Collection<String> cacheKeys = redisCache.keys(cacheName + "*");
        return AjaxResult.success(new TreeSet<>(cacheKeys));
    }

    @PreAuthorize("@ss.hasPermi('monitor:cache:list')")
    @GetMapping("/getValue/{cacheName}/{cacheKey}")
    public AjaxResult getCacheValue(@PathVariable String cacheName, @PathVariable String cacheKey)
    {
        Object cacheValue = redisCache.getCacheObject(cacheKey);
        SysCache sysCache = new SysCache(cacheName, cacheKey,
                cacheValue == null ? "" : JSON.toJSONString(cacheValue));
        return AjaxResult.success(sysCache);
    }

    @PreAuthorize("@ss.hasPermi('monitor:cache:list')")
    @DeleteMapping("/clearCacheName/{cacheName}")
    public AjaxResult clearCacheName(@PathVariable String cacheName)
    {
        Collection<String> cacheKeys = redisCache.keys(cacheName + "*");
        redisCache.deleteObject(cacheKeys);
        return AjaxResult.success();
    }

    @PreAuthorize("@ss.hasPermi('monitor:cache:list')")
    @DeleteMapping("/clearCacheKey/{cacheKey}")
    public AjaxResult clearCacheKey(@PathVariable String cacheKey)
    {
        redisCache.deleteObject(cacheKey);
        return AjaxResult.success();
    }

    @PreAuthorize("@ss.hasPermi('monitor:cache:list')")
    @DeleteMapping("/clearCacheAll")
    public AjaxResult clearCacheAll()
    {
        Collection<String> cacheKeys = redisCache.keys("*");
        redisCache.deleteObject(cacheKeys);
        return AjaxResult.success();
    }

    private Map<String, Object> getLocalCacheInfo()
    {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        Collection<String> keys = redisCache.keys("*");

        Map<String, Object> info = new HashMap<>();
        info.put("redis_version", "未启用");
        info.put("redis_mode", "local");
        info.put("tcp_port", "无");
        info.put("connected_clients", 1);
        info.put("uptime_in_days", ManagementFactory.getRuntimeMXBean().getUptime() / 86_400_000L);
        info.put("used_memory_human", formatBytes(usedMemory));
        info.put("maxmemory_human", formatBytes(maxMemory));
        info.put("aof_enabled", "0");
        info.put("rdb_last_bgsave_status", "不适用");
        info.put("instantaneous_input_kbps", "0");
        info.put("instantaneous_output_kbps", "0");

        List<Map<String, Object>> distribution = new ArrayList<>();
        for (SysCache cache : caches)
        {
            int count = redisCache.keys(cache.getCacheName() + "*").size();
            if (count > 0)
            {
                Map<String, Object> item = new HashMap<>(2);
                item.put("name", cache.getRemark());
                item.put("value", count);
                distribution.add(item);
            }
        }

        Map<String, Object> result = new HashMap<>(7);
        result.put("storageType", "local");
        result.put("storageLabel", "进程内缓存");
        result.put("info", info);
        result.put("dbSize", keys.size());
        result.put("memoryUsedBytes", usedMemory);
        result.put("memoryMaxBytes", maxMemory);
        result.put("commandStats", distribution);
        return result;
    }

    private static long parseLong(String value)
    {
        try
        {
            return value == null ? 0L : Long.parseLong(value);
        }
        catch (NumberFormatException ignored)
        {
            return 0L;
        }
    }

    private static String formatBytes(long bytes)
    {
        double megabytes = bytes / 1024D / 1024D;
        if (megabytes < 1024D)
        {
            return String.format("%.1f MB", megabytes);
        }
        return String.format("%.2f GB", megabytes / 1024D);
    }
}
