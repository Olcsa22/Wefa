//package hu.lanoga.toolbox.config;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.data.redis.connection.RedisPassword;
//import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
//import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
//import org.springframework.data.redis.core.RedisTemplate;
//
////@ConditionalOnProperty(name = "tools.redis.enabled", matchIfMissing = true)
////@Configuration
//public class RedisConfig {
//
//	@Value("${tools.redis.host}")
//	private String redisHost;
//	@Value("${tools.redis.port}")
//	private Integer redisPort;
//	@Value("${tools.redis.password}")
//	private String redisPassword;
//
//	private JedisConnectionFactory jedisConnectionFactory() {
//		final RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(this.redisHost, this.redisPort);
//		redisStandaloneConfiguration.setPassword(RedisPassword.of(this.redisPassword));
//		return new JedisConnectionFactory(redisStandaloneConfiguration);
//	}
//
//	@Bean
//	public RedisTemplate<String, Object> redisTemplate() {
//		final RedisTemplate<String, Object> template = new RedisTemplate<>();
//		template.setConnectionFactory(this.jedisConnectionFactory());
//		return template;
//	}
//}
