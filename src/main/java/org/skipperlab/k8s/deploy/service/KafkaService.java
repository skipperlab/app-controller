package org.skipperlab.k8s.deploy.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.config.ConfigResource;
import org.skipperlab.k8s.deploy.model.StatusType;
import org.skipperlab.k8s.deploy.model.Topic;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Service
public class KafkaService {

    private final AdminClient adminClient;
    private final ObjectMapper mapper;

    public KafkaService(AdminClient adminClient) {
        this.adminClient = adminClient;
        this.mapper = new ObjectMapper();
    }

    public List<Topic> allTopics() throws ExecutionException, InterruptedException {
        return this.adminClient.listTopics(new ListTopicsOptions().listInternal(true))
                .listings()
                .get()
                .stream()
                .map(topic -> {
                    return new Topic(null, topic.topicId().toString(), topic.name(), topic.isInternal(), null, null, StatusType.Deployed,null);
                })
                .collect(Collectors.toList());
    }

    public Topic getTopic(String name) {
        DescribeTopicsResult topicsResult = this.adminClient.describeTopics(Arrays.asList(name));
        TopicDescription topicDescription = null;
        try {
            topicDescription = topicsResult.allTopicNames().get().get(name);
        } catch (Exception e) {
            return null;
        }
        if(topicDescription != null) {
            return this.fromTopicDescription(topicDescription);
        } else {
            return null;
        }
    }

    public Topic getTopicAndConfig(String name) {
        DescribeTopicsResult topicsResult = this.adminClient.describeTopics(Arrays.asList(name));
        TopicDescription topicDescription = null;
        try {
            topicDescription = topicsResult.allTopicNames().get().get(name);
        } catch (Exception e) {
            return null;
        }
        if(topicDescription != null) {
            Collection<ConfigResource> configResources =  Collections.singleton( new ConfigResource(ConfigResource.Type.TOPIC, name));
            DescribeConfigsResult configsResult = this.adminClient.describeConfigs(configResources);
            Config configs = null;
            try {
                configs = (Config)configsResult.all().get().values().toArray()[0];
            } catch (Exception e) {
                return null;
            }
            if(configs != null) {
                return this.fromTopicDescriptionAndConfig(topicDescription, configs);
            } else {
                return this.fromTopicDescription(topicDescription);
            }
        } else {
            return null;
        }
    }

    public Topic createTopic(Topic topic) throws ExecutionException, InterruptedException, JsonProcessingException, TimeoutException {
        Map<String, String> topicConfig;
        if(topic.getConfig() != null && !topic.getConfig().isEmpty()) {
            Collection<ConfigEntry> configs = mapper.readValue(topic.getConfig(), new TypeReference<Collection<ConfigEntry>>(){});
            topicConfig = configs.stream().collect(Collectors.toMap(ConfigEntry::name, ConfigEntry::value));
        } else {
            topicConfig = null;
        }

        Topic existTopic = this.getTopic(topic.getName());
        if(existTopic != null) {
            if (topic.getPartitions() > existTopic.getPartitions()) {
                this.adminClient.createPartitions(
                        Collections.singletonMap(topic.getName(), NewPartitions.increaseTo(topic.getPartitions()))
                        ).all().get();
            }
            if(topicConfig != null) {
                ConfigResource configResource = new ConfigResource(ConfigResource.Type.TOPIC, topic.getName());
                DescribeConfigsResult configsResult = this.adminClient.describeConfigs(Collections.singleton(configResource));
                Config configs = configsResult.all().get().get(configResource);
                Collection<AlterConfigOp> configOp = configs.entries()
                        .stream()
                        .map(entry -> {
                            if(topicConfig.containsKey(entry.name())) {
                                return new AlterConfigOp(
                                        new ConfigEntry(entry.name(), topicConfig.get(entry.name())),
                                        AlterConfigOp.OpType.SET
                                );
                            } else if(!entry.isDefault()) {
                                return new AlterConfigOp(entry, AlterConfigOp.OpType.DELETE);
                            } else {
                                return null;
                            }
                        })
                        .collect(Collectors.toList());

                Map<ConfigResource, Collection<AlterConfigOp>> alterConfigs = new HashMap<>();
                alterConfigs.put(configResource, configOp);

                this.adminClient.incrementalAlterConfigs(alterConfigs).all().get();
            }
        } else {
            NewTopic newTopic = new NewTopic(topic.getName(), topic.getPartitions(), (short) topic.getReplicas().intValue());
            if(topicConfig != null) newTopic.configs(topicConfig);
            CreateTopicsResult topicsResult = this.adminClient.createTopics(Arrays.asList(newTopic));
            topicsResult.all().get();

            topic.setTopicId(topicsResult.topicId(topic.getName()).get().toString());
        }
        return topic;
    }

    public Topic deleteTopic(String name) throws ExecutionException, InterruptedException {
        Topic existTopic = this.getTopic(name);
        if(existTopic != null) {
            this.adminClient.deleteTopics(Arrays.asList(name)).all().get();
            return existTopic;
        } else {
            return null;
        }
    }

    protected Topic fromTopicDescription(TopicDescription topicDescription) {
        return new Topic(null,
                topicDescription.topicId().toString(),
                topicDescription.name(),
                topicDescription.isInternal(),
                topicDescription.partitions().size(),
                topicDescription.partitions().get(0).replicas().size(),
                StatusType.Deployed,
                null
        );
    }

    protected Topic fromTopicDescriptionAndConfig(TopicDescription topicDescription, Config configs) {
        Topic topic = this.fromTopicDescription(topicDescription);
        String json = configs.entries().stream()
                .filter(entry -> !entry.isDefault())
                .map(entry -> "{\"name\": \"" + entry.name() + "\", \"value\": \"" + entry.value() + "}")
                .collect(Collectors.joining(","));
        json = "[" + json + "]";
        topic.setConfig(json);
        return topic;
    }
}
