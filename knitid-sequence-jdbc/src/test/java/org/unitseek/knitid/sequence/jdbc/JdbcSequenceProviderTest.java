package org.unitseek.knitid.sequence.jdbc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcSequenceProviderTest {

    @Test
    void allocatesBlocksAndContinuesAcrossBlocks() {
        JdbcDataSource dataSource = dataSource();
        JdbcSequenceProvider provider = provider(dataSource, 3);
        assertThat(provider.next("order")).isEqualTo(0);
        assertThat(provider.next("order")).isEqualTo(1);
        assertThat(provider.next("order")).isEqualTo(2);
        assertThat(provider.next("order")).isEqualTo(3);
        assertThat(provider.next("other")).isEqualTo(0);
    }

    @Test
    void isSafeUnderConcurrency() throws Exception {
        JdbcDataSource dataSource = dataSource();
        JdbcSequenceProvider provider = provider(dataSource, 10);
        List<Callable<List<Long>>> tasks = new ArrayList<>();
        for (int t = 0; t < 8; t++) {
            tasks.add(() -> {
                List<Long> values = new ArrayList<>();
                for (int i = 0; i < 100; i++) {
                    values.add(provider.next("concurrent"));
                }
                return values;
            });
        }
        Set<Long> values = new HashSet<>();
        try (var executor = Executors.newFixedThreadPool(8)) {
            for (Future<List<Long>> future : executor.invokeAll(tasks)) {
                values.addAll(future.get());
            }
        }
        assertThat(values).hasSize(800);
    }

    private static JdbcDataSource dataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:jdbcseq;DB_CLOSE_DELAY=-1");
        new JdbcTemplate(dataSource).execute(
                "CREATE TABLE IF NOT EXISTS knitid_sequence (scope VARCHAR(255) PRIMARY KEY, next_value BIGINT NOT NULL)");
        return dataSource;
    }

    private static JdbcSequenceProvider provider(JdbcDataSource dataSource, int blockSize) {
        return new JdbcSequenceProvider(new JdbcTemplate(dataSource),
                new DataSourceTransactionManager(dataSource), "knitid_sequence", blockSize);
    }
}
