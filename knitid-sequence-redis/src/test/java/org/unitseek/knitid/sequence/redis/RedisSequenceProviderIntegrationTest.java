package org.unitseek.knitid.sequence.redis;

import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import org.unitseek.knitid.core.api.IdGenerator;
import org.unitseek.knitid.core.api.IdType;
import org.unitseek.knitid.core.spi.GeneratorConfig;
import org.unitseek.knitid.core.spi.SequenceProvider;
import org.unitseek.knitid.generator.biz.BizIdGeneratorFactory;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class RedisSequenceProviderIntegrationTest {

    private static final String REDIS_IMAGE = System.getenv().getOrDefault(
            "KNITID_TEST_REDIS_IMAGE", "redis:8.10.3");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse(REDIS_IMAGE))
            .withExposedPorts(6379)
            .waitingFor(Wait.forListeningPort());

    private static LettuceConnectionFactory connectionFactory;

    private static StringRedisTemplate template;

    @BeforeAll
    static void connect() {
        RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration(
                REDIS.getHost(), REDIS.getMappedPort(6379));
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofSeconds(2))
                .build();
        connectionFactory = new LettuceConnectionFactory(standalone, clientConfig);
        connectionFactory.afterPropertiesSet();
        template = new StringRedisTemplate(connectionFactory);
        template.afterPropertiesSet();
    }

    @AfterAll
    static void disconnect() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    void sequenceIsSharedAcrossProviderInstances() {
        String prefix = prefix();
        RedisSequenceProvider first = new RedisSequenceProvider(template, prefix, Duration.ofMinutes(5));
        RedisSequenceProvider second = new RedisSequenceProvider(template, prefix, Duration.ofMinutes(5));
        Set<Long> values = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            values.add((i % 2 == 0 ? first : second).next("ORD"));
        }
        assertThat(values).hasSize(20).containsExactlyInAnyOrderElementsOf(
                java.util.stream.LongStream.rangeClosed(0, 19).boxed().toList());
    }

    @Test
    void batchIsAtomicAndContiguous() {
        RedisSequenceProvider provider = new RedisSequenceProvider(template, prefix(), Duration.ofMinutes(5));
        assertThat(provider.next("ORD", 5)).containsExactly(0, 1, 2, 3, 4);
        assertThat(provider.next("ORD", 3)).containsExactly(5, 6, 7);
    }

    @Test
    void persistentScopeHasNoTtl() {
        String prefix = prefix();
        RedisSequenceProvider provider = new RedisSequenceProvider(template, prefix, Duration.ofSeconds(30));
        assertThat(provider.next("ORD", true)).isZero();
        assertThat(template.getExpire(prefix + ":ORD", TimeUnit.SECONDS)).isEqualTo(-1L);
    }

    @Test
    void firstIncrementSetsTtlAndScopesAreIndependent() {
        String prefix = prefix();
        RedisSequenceProvider provider = new RedisSequenceProvider(template, prefix, Duration.ofSeconds(30));
        assertThat(provider.next("ORD:20260101")).isZero();
        assertThat(provider.next("ORD:20260102")).isZero();

        Long ttl = template.getExpire(prefix + ":ORD:20260101", TimeUnit.SECONDS);
        assertThat(ttl).isNotNull().isPositive().isLessThanOrEqualTo(30L);
    }

    @Test
    void bizGeneratorsShareSequenceAcrossInstances() {
        String redisPrefix = prefix();
        RedisSequenceProvider firstProvider = new RedisSequenceProvider(template, redisPrefix, Duration.ofMinutes(5));
        RedisSequenceProvider secondProvider = new RedisSequenceProvider(template, redisPrefix, Duration.ofMinutes(5));
        IdGenerator first = bizFactory(firstProvider).create(bizConfig(redisPrefix));
        IdGenerator second = bizFactory(secondProvider).create(bizConfig(redisPrefix));

        Set<String> values = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            values.add((i % 2 == 0 ? first : second).generate());
        }
        assertThat(values).hasSize(100);
    }

    private static BizIdGeneratorFactory bizFactory(SequenceProvider sequenceProvider) {
        return new BizIdGeneratorFactory(
                java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC),
                name -> sequenceProvider);
    }

    private static GeneratorConfig bizConfig(String redisPrefix) {
        Map<String, Object> values = new java.util.LinkedHashMap<>();
        values.put("biz.template", "{seq:6}");
        values.put("biz.zone-id", "UTC");
        values.put("biz.sequence-provider", "redis");
        values.put("biz.check-algorithm", "NONE");
        return new GeneratorConfig() {
            @Override
            public String name() {
                return "order-id";
            }

            @Override
            public IdType type() {
                return IdType.BIZ;
            }

            @Override
            public java.util.Optional<Object> get(String key) {
                return java.util.Optional.ofNullable(values.get(key));
            }
        };
    }

    private static String prefix() {
        return "knitid:test:seq:" + UUID.randomUUID();
    }
}
