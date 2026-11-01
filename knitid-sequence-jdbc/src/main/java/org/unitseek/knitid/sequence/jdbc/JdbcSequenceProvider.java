package org.unitseek.knitid.sequence.jdbc;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import org.unitseek.knitid.core.exception.IdErrorCodes;
import org.unitseek.knitid.core.exception.SequenceUnavailableException;
import org.unitseek.knitid.core.spi.SequenceProvider;

/**
 * Database segment sequence provider.
 *
 * <p>Each process reserves a block of values in a transaction, then serves the
 * block locally. A row lock makes allocation safe across instances. A crashed
 * process can leave gaps in the sequence; KnitID does not promise gapless BIZ
 * sequences.</p>
 *
 * @since 1.0.0
 */
public final class JdbcSequenceProvider implements SequenceProvider {

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final String tableName;
    private final int blockSize;
    private final ConcurrentHashMap<String, Block> blocks = new ConcurrentHashMap<>();

    public JdbcSequenceProvider(JdbcTemplate jdbc, PlatformTransactionManager transactionManager,
            String tableName, int blockSize) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc must not be null");
        this.transactions = new TransactionTemplate(Objects.requireNonNull(transactionManager,
                "transactionManager must not be null"));
        if (tableName == null || !tableName.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("tableName must be a simple SQL identifier");
        }
        if (blockSize <= 0) {
            throw new IllegalArgumentException("blockSize must be > 0");
        }
        this.tableName = tableName;
        this.blockSize = blockSize;
    }

    @Override
    public long next(String scope) {
        return next(scope, false);
    }

    @Override
    public long next(String scope, boolean persistent) {
        if (scope == null || scope.isBlank()) {
            throw new IllegalArgumentException("scope must not be blank");
        }
        Block block = blocks.computeIfAbsent(scope, key -> new Block());
        synchronized (block) {
            long value = block.next.getAndIncrement();
            if (value < block.limit) {
                return value;
            }
            long start = reserveBlock(scope);
            block.next.set(start + 1);
            block.limit = start + blockSize;
            return start;
        }
    }

    private long reserveBlock(String scope) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                return transactions.execute(status -> {
                    List<Long> values = jdbc.query(
                            "SELECT next_value FROM " + tableName + " WHERE scope = ? FOR UPDATE",
                            (rs, rowNum) -> rs.getLong(1), scope);
                    long start;
                    if (values.isEmpty()) {
                        jdbc.update("INSERT INTO " + tableName + " (scope, next_value) VALUES (?, ?)", scope, 0L);
                        start = 0L;
                    }
                    else {
                        start = values.get(0);
                    }
                    jdbc.update("UPDATE " + tableName + " SET next_value = ? WHERE scope = ?",
                            start + blockSize, scope);
                    return start;
                });
            }
            catch (DuplicateKeyException ex) {
                if (attempt == 2) {
                    throw unavailable(scope, ex);
                }
            }
            catch (RuntimeException ex) {
                throw unavailable(scope, ex);
            }
        }
        throw unavailable(scope, null);
    }

    private SequenceUnavailableException unavailable(String scope, Throwable cause) {
        return new SequenceUnavailableException(IdErrorCodes.SEQUENCE_UNAVAILABLE,
                "JDBC sequence unavailable for scope " + scope, cause);
    }

    private static final class Block {
        private final AtomicLong next = new AtomicLong();
        private volatile long limit;
    }
}
