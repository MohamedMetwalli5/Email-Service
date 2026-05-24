package com.backendemailservice.backendemailservice.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
@EnableCaching(order = Ordered.LOWEST_PRECEDENCE - 1) // cache is the outer advice, transaction is the inner
public class CachingConfig {

    private static final Logger log = LoggerFactory.getLogger(CachingConfig.class);

    private final CacheManager cacheManager;

    public CachingConfig(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @PostConstruct
    public void clearStaleCache() {
        org.springframework.cache.Cache cache = cacheManager.getCache("inbox");
        if (cache != null) {
            cache.clear();
            log.info("Cleared stale 'inbox' cache entries on startup");
        }
    }
}
