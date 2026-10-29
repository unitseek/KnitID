package org.unitseek.knitid.worker.redis;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import org.unitseek.knitid.core.exception.WorkerIdUnavailableException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies failure behaviour when Redis becomes unreachable.
 */
@Testcontainers(disabledWithoutDocker = true)
class RedisWorkerIdAssignerDisconnectIntegrationTest {

    private static final String REDIS_IMAGE = System.getenv().getOrDefault(
            "KNITID_TEST_REDIS_IMAGE", "redis:7.4-alpine");

    @Test
    void renewAndAssignFailWhenRedisStops() {
        GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse(REDIS_IMAGE))
                .withExposedPorts(6379);
        redis.start();
        LettuceConnectionFactory connectionFactory = null;
        try {
            RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration(
                    redis.getHost(), redis.getMappedPort(6379));
            LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                    .commandTimeout(Duration.ofSeconds(2))
                    .build();
            connectionFactory = new LettuceConnectionFactory(standalone, clientConfig);
            connectionFactory.afterPropertiesSet();
            StringRedisTemplate template = new StringRedisTemplate(connectionFactory);
            template.afterPropertiesSet();

            RedisWorkerIdAssigner assigner = new RedisWorkerIdAssigner(
                    new StringRedisTemplateLeaseStore(template), "knitid:test:disconnect",
                    Duration.ofSeconds(60), Duration.ofSeconds(1), 0, 15, "token");
            assigner.assign();

            redis.stop();

            assertThatThrownBy(assigner::renew)
                    .isInstanceOf(WorkerIdUnavailableException.class)
                    .hasMessageContaining("KNITID-2007");

            RedisWorkerIdAssigner other = new RedisWorkerIdAssigner(
                    new StringRedisTemplateLeaseStore(template), "knitid:test:disconnect",
                    Duration.ofSeconds(60), Duration.ofSeconds(1), 0, 15, "other");
            assertThatThrownBy(other::assign)
                    .isInstanceOf(WorkerIdUnavailableException.class)
                    .hasMessageContaining("KNITID-2003");
        }
        finally {
            if (connectionFactory != null) {
                connectionFactory.destroy();
            }
            if (redis.isRunning()) {
                redis.stop();
            }
        }
    }
}
