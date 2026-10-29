package org.unitseek.knitid.worker.redis;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import org.unitseek.knitid.core.exception.IdConfigurationException;
import org.unitseek.knitid.core.exception.WorkerIdUnavailableException;
import org.unitseek.knitid.core.spi.WorkerId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class RedisWorkerIdAssignerTest {

    private static final Duration TTL = Duration.ofSeconds(60);

    private static final Duration RENEW = Duration.ofSeconds(20);

    @Test
    void assignsFirstFreeSlotAndIsIdempotent() {
        InMemoryLeaseStore store = new InMemoryLeaseStore();
        RedisWorkerIdAssigner assigner = assigner(store, 1, 15, "token-a");
        WorkerId first = assigner.assign();
        assertThat(first).isEqualTo(new WorkerId(1, 0));
        assertThat(assigner.assign()).isEqualTo(first);
        assertThat(store.leases).containsKey("knitid:worker:1:0");
    }

    @Test
    void skipsSlotsHeldByOtherAssigners() {
        InMemoryLeaseStore store = new InMemoryLeaseStore();
        RedisWorkerIdAssigner first = assigner(store, 0, 15, "token-a");
        RedisWorkerIdAssigner second = assigner(store, 0, 15, "token-b");
        assertThat(first.assign().workerId()).isZero();
        assertThat(second.assign().workerId()).isEqualTo(1);
    }

    @Test
    void failsWhenAllSlotsAreTaken() {
        InMemoryLeaseStore store = new InMemoryLeaseStore();
        assigner(store, 0, 1, "a").assign();
        assigner(store, 0, 1, "b").assign();
        assertThatThrownBy(() -> assigner(store, 0, 1, "c").assign())
                .isInstanceOf(WorkerIdUnavailableException.class)
                .hasMessageContaining("KNITID-2003");
    }

    @Test
    void renewFailsWhenLeaseIsLost() {
        InMemoryLeaseStore store = new InMemoryLeaseStore();
        RedisWorkerIdAssigner assigner = assigner(store, 0, 15, "token");
        WorkerId worker = assigner.assign();
        assigner.renew();
        store.renewResult = false;
        assertThatThrownBy(assigner::renew)
                .isInstanceOf(WorkerIdUnavailableException.class)
                .hasMessageContaining("KNITID-2007");
        assigner.release(worker);
        assertThat(store.leases).doesNotContainKey("knitid:worker:0:0");
    }

    @Test
    void releaseAllowsSlotReuse() {
        InMemoryLeaseStore store = new InMemoryLeaseStore();
        RedisWorkerIdAssigner assigner = assigner(store, 0, 15, "token");
        WorkerId worker = assigner.assign();
        assigner.release(worker);
        RedisWorkerIdAssigner next = assigner(store, 0, 15, "other");
        assertThat(next.assign().workerId()).isZero();
    }

    @Test
    void renewFailsWhenLeaseWasTakenOverByAnotherToken() {
        InMemoryLeaseStore store = new InMemoryLeaseStore();
        RedisWorkerIdAssigner assigner = assigner(store, 0, 15, "token-a");
        assigner.assign();
        store.leases.put("knitid:worker:0:0", "token-b");
        assertThatThrownBy(assigner::renew)
                .isInstanceOf(WorkerIdUnavailableException.class)
                .hasMessageContaining("KNITID-2007");
        assertThat(store.leases).containsEntry("knitid:worker:0:0", "token-b");
    }

    @Test
    void releaseDoesNotDeleteAnotherOwnersLease() {
        InMemoryLeaseStore store = new InMemoryLeaseStore();
        RedisWorkerIdAssigner assigner = assigner(store, 0, 15, "token-a");
        WorkerId worker = assigner.assign();
        store.leases.put("knitid:worker:0:0", "token-b");
        assigner.release(worker);
        assertThat(store.leases).containsEntry("knitid:worker:0:0", "token-b");
    }

    @Test
    void providerRequiresRedisTemplateAndValidProperties() {
        RedisWorkerIdProperties properties = new RedisWorkerIdProperties();
        RedisWorkerIdAssignerProvider withoutTemplate = new RedisWorkerIdAssignerProvider(null, properties);
        assertThat(withoutTemplate.name()).isEqualTo(RedisWorkerIdAssignerProvider.NAME);
        assertThatThrownBy(() -> withoutTemplate.create(0, 5))
                .isInstanceOf(IdConfigurationException.class)
                .hasMessageContaining("StringRedisTemplate");

        RedisWorkerIdAssignerProvider provider = new RedisWorkerIdAssignerProvider(
                mock(StringRedisTemplate.class), properties);
        assertThat(provider.create(0, 5)).isNotNull();
    }

    @Test
    void providerRejectsRenewIntervalNotSmallerThanTtl() {
        RedisWorkerIdProperties properties = new RedisWorkerIdProperties();
        properties.setLeaseTtl(Duration.ofSeconds(20));
        properties.setRenewInterval(Duration.ofSeconds(20));
        RedisWorkerIdAssignerProvider provider = new RedisWorkerIdAssignerProvider(
                mock(StringRedisTemplate.class), properties);
        assertThatThrownBy(() -> provider.create(0, 5))
                .isInstanceOf(IdConfigurationException.class)
                .hasMessageContaining("less than lease-ttl");
    }

    private static RedisWorkerIdAssigner assigner(InMemoryLeaseStore store, int datacenter, int maxWorker, String token) {
        return new RedisWorkerIdAssigner(store, "knitid:worker", TTL, RENEW, datacenter, maxWorker, token);
    }

    private static final class InMemoryLeaseStore implements RedisLeaseStore {

        private final Map<String, String> leases = new HashMap<>();

        private boolean renewResult = true;

        @Override
        public boolean tryAcquire(String key, String token, Duration ttl) {
            if (leases.containsKey(key)) {
                return false;
            }
            leases.put(key, token);
            return true;
        }

        @Override
        public boolean renew(String key, String token, Duration ttl) {
            return renewResult && token.equals(leases.get(key));
        }

        @Override
        public boolean release(String key, String token) {
            return token.equals(leases.get(key)) && leases.remove(key) != null;
        }
    }
}
