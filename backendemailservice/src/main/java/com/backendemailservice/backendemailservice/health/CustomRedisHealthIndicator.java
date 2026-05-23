package com.backendemailservice.backendemailservice.health;

// Custom HealthIndicator for Redis (not auto-detected by Spring Boot)
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

@Component("redis")
public class CustomRedisHealthIndicator implements HealthIndicator {

    private final RedisConnectionFactory connectionFactory;

    public CustomRedisHealthIndicator(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public Health health() {
        try {
            connectionFactory.getConnection().ping();
            return Health.up().withDetail("cache", "Redis is reachable").build();
        } catch (Exception ex) {
            return Health.down(ex).withDetail("cache", "Redis unreachable").build();
        }
    }
}
