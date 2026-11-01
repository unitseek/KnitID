package org.unitseek.knitid.sequence.jdbc.autoconfigure;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import org.unitseek.knitid.core.spi.SequenceProvider;
import org.unitseek.knitid.sequence.jdbc.JdbcSequenceProvider;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcSequenceAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JdbcSequenceAutoConfiguration.class))
            .withUserConfiguration(DataSourceConfig.class)
            .withPropertyValues("knitid.sequence.jdbc.block-size=7");

    @Test
    void registersJdbcSequenceProvider() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(SequenceProvider.class);
            assertThat(context.getBean(SequenceProvider.class)).isInstanceOf(JdbcSequenceProvider.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class DataSourceConfig {
        @Bean
        DataSource dataSource() {
            JdbcDataSource dataSource = new JdbcDataSource();
            dataSource.setURL("jdbc:h2:mem:jdbcseq-auto;DB_CLOSE_DELAY=-1");
            return dataSource;
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
    }
}
