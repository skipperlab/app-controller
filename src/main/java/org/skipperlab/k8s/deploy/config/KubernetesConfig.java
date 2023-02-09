package org.skipperlab.k8s.deploy.config;

import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KubernetesConfig {
    @Value("${kubernetes.master-url}")
    private String masterUrl;

    @Bean("KubernetesClient")
    public KubernetesClient getKubernetesClient() {
        final ConfigBuilder configBuilder = new ConfigBuilder();
        if (masterUrl.length() > 0) {
            configBuilder.withMasterUrl(masterUrl);
        }
        return new KubernetesClientBuilder().withConfig(configBuilder.build()).build();
    }
}
