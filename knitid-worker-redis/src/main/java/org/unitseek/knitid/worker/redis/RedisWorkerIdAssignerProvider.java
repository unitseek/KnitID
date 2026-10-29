package org.unitseek.knitid.worker.redis;

import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;

import org.unitseek.knitid.core.exception.IdConfigurationException;
import org.unitseek.knitid.core.exception.IdErrorCodes;
import org.unitseek.knitid.core.spi.WorkerIdAssigner;
import org.unitseek.knitid.core.spi.WorkerIdAssignerProvider;

/**
 * Exposes the Redis WorkerId assigner under the name {@code redis}.
 */
public final class RedisWorkerIdAssignerProvider implements WorkerIdAssignerProvider {

    public static final String NAME = "redis";

    private static final int MAX_SUPPORTED_WORKER_ID_BITS = 16;

    private final StringRedisTemplate template;

    private final RedisWorkerIdProperties properties;

    public RedisWorkerIdAssignerProvider(StringRedisTemplate template, RedisWorkerIdProperties properties) {
        this.template = template;
        this.properties = properties;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public WorkerIdAssigner create(int datacenterId, int workerIdBits) {
        if (template == null) {
            throw new IdConfigurationException(
                    IdErrorCodes.CONFIG_INVALID,
                    "Redis WorkerId assigner requires a StringRedisTemplate; "
                            + "add spring-boot-starter-data-redis and configure Redis");
        }
        if (datacenterId < 0) {
            throw new IdConfigurationException(
                    IdErrorCodes.CONFIG_INVALID, "ksid.datacenter-id must be >= 0");
        }
        if (workerIdBits < 1 || workerIdBits > MAX_SUPPORTED_WORKER_ID_BITS) {
            throw new IdConfigurationException(
                    IdErrorCodes.CONFIG_INVALID,
                    "Redis WorkerId assigner supports worker-id-bits between 1 and "
                            + MAX_SUPPORTED_WORKER_ID_BITS + ": " + workerIdBits);
        }
        if (properties.getKeyPrefix() == null || properties.getKeyPrefix().isBlank()) {
            throw new IdConfigurationException(
                    IdErrorCodes.CONFIG_INVALID, "knitid.worker.redis.key-prefix must not be blank");
        }
        if (properties.getLeaseTtl() == null || properties.getLeaseTtl().isNegative()
                || properties.getLeaseTtl().isZero()) {
            throw new IdConfigurationException(
                    IdErrorCodes.CONFIG_INVALID, "knitid.worker.redis.lease-ttl must be positive");
        }
        if (properties.getRenewInterval() == null || properties.getRenewInterval().isNegative()
                || properties.getRenewInterval().isZero()) {
            throw new IdConfigurationException(
                    IdErrorCodes.CONFIG_INVALID, "knitid.worker.redis.renew-interval must be positive");
        }
        if (properties.getRenewInterval().compareTo(properties.getLeaseTtl()) >= 0) {
            throw new IdConfigurationException(
                    IdErrorCodes.CONFIG_INVALID,
                    "knitid.worker.redis.renew-interval must be less than lease-ttl");
        }
        int maxWorkerId = (1 << workerIdBits) - 1;
        String token = UUID.randomUUID().toString();
        return new RedisWorkerIdAssigner(new StringRedisTemplateLeaseStore(template),
                properties.getKeyPrefix(), properties.getLeaseTtl(), properties.getRenewInterval(),
                datacenterId, maxWorkerId, token);
    }
}
