package org.unitseek.knitid.sequence.jdbc.autoconfigure;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import org.unitseek.knitid.core.spi.SequenceProvider;
import org.unitseek.knitid.sequence.jdbc.JdbcSequenceProperties;
import org.unitseek.knitid.sequence.jdbc.JdbcSequenceProvider;

/**
 * Auto-configures the JDBC sequence provider as both {@code jdbc} and
 * {@code jdbcSequenceProvider}.
 *
 * @since 1.0.0
 */
@AutoConfiguration(afterName = "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration")
@ConditionalOnClass({ JdbcTemplate.class, SequenceProvider.class })
@ConditionalOnBean({ DataSource.class, PlatformTransactionManager.class })
@ConditionalOnSingleCandidate(DataSource.class)
@ConditionalOnProperty(prefix = "knitid.sequence.jdbc", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(JdbcSequenceProperties.class)
public class JdbcSequenceAutoConfiguration {

    @Bean(name = { "jdbcSequenceProvider", "jdbc" })
    @ConditionalOnMissingBean(name = { "jdbcSequenceProvider", "jdbc" })
    public SequenceProvider jdbcSequenceProvider(DataSource dataSource,
            PlatformTransactionManager transactionManager, JdbcSequenceProperties properties) {
        return new JdbcSequenceProvider(new JdbcTemplate(dataSource), transactionManager,
                properties.getTableName(), properties.getBlockSize());
    }
}
