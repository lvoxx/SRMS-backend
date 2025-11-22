package io.github.lvoxx.srms.warehouse.config;

import java.time.Duration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Default cache configuration
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10)) // Default TTL: 10 minutes
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()
                )
            )
            .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultCacheConfig)
            
            // ==================== COUNT SERVICE CACHES ====================
            .withCacheConfiguration("warehouse:count:all",
                defaultCacheConfig.entryTtl(Duration.ofMinutes(5)))
            .withCacheConfiguration("warehouse:count:below-minimum",
                defaultCacheConfig.entryTtl(Duration.ofMinutes(2)))
            .withCacheConfiguration("warehouse:count:out-of-stock",
                defaultCacheConfig.entryTtl(Duration.ofMinutes(2)))
            .withCacheConfiguration("warehouse:count:history:all",
                defaultCacheConfig.entryTtl(Duration.ofMinutes(15)))
            .withCacheConfiguration("warehouse:count:history:by-warehouse",
                defaultCacheConfig.entryTtl(Duration.ofMinutes(10)))
            .withCacheConfiguration("warehouse:count:history:by-type",
                defaultCacheConfig.entryTtl(Duration.ofMinutes(10)))
            .withCacheConfiguration("warehouse:count:history:by-warehouse-and-type",
                defaultCacheConfig.entryTtl(Duration.ofMinutes(10)))
            .withCacheConfiguration("warehouse:count:statistics",
                defaultCacheConfig.entryTtl(Duration.ofMinutes(5)))
            .withCacheConfiguration("warehouse:count:health",
                defaultCacheConfig.entryTtl(Duration.ofMinutes(3)))
            
            // ==================== STATISTIC SERVICE CACHES ====================
            // Import/Export statistics (update less frequently)
            .withCacheConfiguration("warehouse:stats:total-import",
                defaultCacheConfig.entryTtl(Duration.ofMinutes(10)))
            .withCacheConfiguration("warehouse:stats:total-export",
                defaultCacheConfig.entryTtl(Duration.ofMinutes(10)))
            .withCacheConfiguration("warehouse:stats:balance",
                defaultCacheConfig.entryTtl(Duration.ofMinutes(10)))
            .withCacheConfiguration("warehouse:stats:quantity-by-date-range",
                defaultCacheConfig.entryTtl(Duration.ofMinutes(15)))
            
            // Alert caches (update more frequently for real-time monitoring)
            .withCacheConfiguration("warehouse:stats:below-minimum",
                defaultCacheConfig.entryTtl(Duration.ofMinutes(2)))
            .withCacheConfiguration("warehouse:stats:out-of-stock",
                defaultCacheConfig.entryTtl(Duration.ofMinutes(2)))
            .withCacheConfiguration("warehouse:stats:all-alerts",
                defaultCacheConfig.entryTtl(Duration.ofMinutes(2)))
            
            // Dashboard caches (balance between freshness and performance)
            .withCacheConfiguration("warehouse:stats:dashboard",
                defaultCacheConfig.entryTtl(Duration.ofMinutes(3)))
            .withCacheConfiguration("warehouse:stats:details",
                defaultCacheConfig.entryTtl(Duration.ofMinutes(5)))
            .withCacheConfiguration("warehouse:stats:time-based",
                defaultCacheConfig.entryTtl(Duration.ofMinutes(10)))
            
            .transactionAware()
            .build();
    }
}