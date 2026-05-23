package com.backendemailservice.backendemailservice.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
@EnableCaching(order = Ordered.LOWEST_PRECEDENCE - 1) // cache is the outer advice, transaction is the inner
public class CachingConfig {
}
