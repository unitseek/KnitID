package org.unitseek.knitid.worker.zookeeper.autoconfigure;

import org.apache.curator.framework.CuratorFramework;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import org.unitseek.knitid.core.spi.WorkerIdAssignerProvider;
import org.unitseek.knitid.worker.zookeeper.ZookeeperWorkerIdAssignerProvider;
import org.unitseek.knitid.worker.zookeeper.ZookeeperWorkerIdProperties;

/**
 * Auto-configures the ZooKeeper WorkerId assigner provider.
 */
@AutoConfiguration
@ConditionalOnClass({ CuratorFramework.class, WorkerIdAssignerProvider.class })
@ConditionalOnProperty(prefix = "knitid.worker.zookeeper", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(ZookeeperWorkerIdProperties.class)
public class ZookeeperWorkerIdAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "zookeeperWorkerIdAssignerProvider")
    public WorkerIdAssignerProvider zookeeperWorkerIdAssignerProvider(ZookeeperWorkerIdProperties properties) {
        return new ZookeeperWorkerIdAssignerProvider(properties);
    }
}
