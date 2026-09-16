package io.tcbs.template.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

@Configuration
public class RedisConfiguration {

    /**
     * RedisTemplate dùng chung cho mọi thao tác thủ công với Redis (khác với @Cacheable).
     * - key / hash-key: String thuần để dễ đọc khi debug bằng redis-cli
     * - value / hash-value: JSON có kèm thông tin kiểu (@class) nên đọc ra được đúng object ban đầu
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        // Chỉ cho phép ghi/đọc @class của các kiểu an toàn, tránh lỗ hổng deserialize tuỳ ý
        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType("io.tcbs.template.")
            .allowIfSubType("java.util.")
            .allowIfSubType("java.time.")
            .allowIfSubTypeIsArray()
            .build();

        RedisSerializer<String> keySerializer = new StringRedisSerializer();
        RedisSerializer<Object> valueSerializer = GenericJacksonJsonRedisSerializer.builder().enableDefaultTyping(typeValidator).build();

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);
        template.afterPropertiesSet();
        return template;
    }
}
