package org.unitseek.knitid.worker.redis;

import java.time.Duration;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import org.unitseek.knitid.core.exception.WorkerIdUnavailableException;
import org.unitseek.knitid.core.spi.WorkerId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Redis SET NX / TTL / renew / release integration tests.
 *
 * <p>The image can be overridden with the {@code KNITID_TEST_REDIS_IMAGE}
 * environment variable; Docker Hub may require a mirror in some networks.</p>
 */
@Testcontainers(disabledWithoutDocker = true)
class RedisWorkerIdAssignerIntegrationTest {

    private static final String REDIS_IMAGE = System.getenv().getOrDefault(
            "KNITID_TEST_REDIS_IMAGE", "redis:8.10.3");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse(REDIS_IMAGE))
            .withExposedPorts(6379);

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
    void acquiresUniqueSlotsAndWritesTtl() {
        String prefix = prefix();
        RedisWorkerIdAssigner first = assigner(prefix, Duration.ofSeconds(60), 15, "token-a");
        RedisWorkerIdAssigner second = assigner(prefix, Duration.ofSeconds(60), 15, "token-b");

        WorkerId a = first.assign();
        WorkerId b = second.assign();

        assertThat(a).isEqualTo(new WorkerId(1, 0));
        assertThat(b).isEqualTo(new WorkerId(1, 1));
        assertThat(template.hasKey(prefix + ":1:0")).isTrue();
        assertThat(template.hasKey(prefix + ":1:1")).isTrue();
        assertThat(template.getExpire(prefix + ":1:0", TimeUnit.SECONDS)).isGreaterThan(0L);
    }

    @Test
    void renewExtendsTheLeaseTtl() throws Exception {
        String prefix = prefix();
        RedisWorkerIdAssigner assigner = assigner(prefix, Duration.ofSeconds(5), 15, "token");
        assigner.assign();
        String key = prefix + ":1:0";

        Thread.sleep(1500);
        Long beforeRenew = template.getExpire(key, TimeUnit.SECONDS);
        assertThat(beforeRenew).isNotNull().isLessThanOrEqualTo(5L);

        assigner.renew();
        Long afterRenew = template.getExpire(key, TimeUnit.SECONDS);
        assertThat(afterRenew).isNotNull().isGreaterThan(3L);
    }

    @Test
    void leaseExpiresAndRenewFails() throws Exception {
        String prefix = prefix();
        RedisWorkerIdAssigner assigner = assigner(prefix, Duration.ofSeconds(1), 15, "token");
        assigner.assign();
        String key = prefix + ":1:0";

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (template.hasKey(key) && System.nanoTime() < deadline) {
            Thread.sleep(50);
        }
        assertThat(template.hasKey(key)).isFalse();

        assertThatThrownBy(assigner::renew)
                .isInstanceOf(WorkerIdUnavailableException.class)
                .hasMessageContaining("KNITID-2007");

        RedisWorkerIdAssigner replacement = assigner(prefix, Duration.ofSeconds(1), 15, "other");
        assertThat(replacement.assign()).isEqualTo(new WorkerId(1, 0));
    }

    @Test
    void renewFailsWhenLeaseIsOwnedByAnotherToken() {
        String prefix = prefix();
        RedisWorkerIdAssigner assigner = assigner(prefix, Duration.ofSeconds(30), 15, "token-a");
        assigner.assign();
        String key = prefix + ":1:0";
        template.opsForValue().set(key, "token-b", Duration.ofSeconds(30));

        assertThatThrownBy(assigner::renew)
                .isInstanceOf(WorkerIdUnavailableException.class)
                .hasMessageContaining("KNITID-2007");
        assertThat(template.opsForValue().get(key)).isEqualTo("token-b");
    }

    @Test
    void releaseDeletesTheLeaseAndAllowsReuse() {
        String prefix = prefix();
        RedisWorkerIdAssigner first = assigner(prefix, Duration.ofSeconds(60), 15, "token");
        WorkerId worker = first.assign();
        String key = prefix + ":1:0";
        assertThat(template.hasKey(key)).isTrue();

        first.release(worker);
        assertThat(template.hasKey(key)).isFalse();

        RedisWorkerIdAssigner second = assigner(prefix, Duration.ofSeconds(60), 15, "other");
        assertThat(second.assign()).isEqualTo(new WorkerId(1, 0));
    }

    private static String prefix() {
        return "knitid:test:" + UUID.randomUUID();
    }

    private static RedisWorkerIdAssigner assigner(String prefix, Duration ttl, int maxWorkerId, String token) {
        return new RedisWorkerIdAssigner(new StringRedisTemplateLeaseStore(template), prefix,
                ttl, Duration.ofSeconds(1), 1, maxWorkerId, token);
    }
}
