package org.skipperlab.k8s.deploy.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConfig {
    private final KafkaProperties properties;

    public KafkaConfig(KafkaProperties properties) {
        this.properties = properties;
    }

    @Bean("AdminClient")
    public AdminClient adminClient() {
        return KafkaAdminClient.create(this.getAdminProperties());
    }

    private Map<String, Object> getAdminProperties() {
        return new HashMap<String, Object>() {{
            put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
            put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 1000);
            put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 1000);
            put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 1000);
        }};
    }
}