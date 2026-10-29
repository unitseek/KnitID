package org.unitseek.knitid.worker.kubernetes.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import org.unitseek.knitid.core.spi.WorkerIdAssignerProvider;
import org.unitseek.knitid.worker.kubernetes.KubernetesWorkerIdAssignerProvider;
import org.unitseek.knitid.worker.kubernetes.KubernetesWorkerIdProperties;

/**
 * Auto-configures the Kubernetes WorkerId assigner provider.
 */
@AutoConfiguration
@ConditionalOnClass(WorkerIdAssignerProvider.class)
@ConditionalOnProperty(prefix = "knitid.worker.kubernetes", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(KubernetesWorkerIdProperties.class)
public class KubernetesWorkerIdAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "kubernetesWorkerIdAssignerProvider")
    public WorkerIdAssignerProvider kubernetesWorkerIdAssignerProvider(KubernetesWorkerIdProperties properties) {
        return new KubernetesWorkerIdAssignerProvider(properties);
    }
}
