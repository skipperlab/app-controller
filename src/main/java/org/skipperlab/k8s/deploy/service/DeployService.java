package org.skipperlab.k8s.deploy.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.skipperlab.k8s.deploy.model.*;
import org.skipperlab.k8s.deploy.repository.CommandRepository;
import org.skipperlab.k8s.deploy.repository.PaletteRepository;
import org.skipperlab.k8s.deploy.repository.TopicRepository;
import org.skipperlab.k8s.deploy.repository.WorkloadRepository;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DeployService {
    private final KafkaService kafkaService;
    private final KubernetesService kubernetesService;
    private final WorkloadRepository workloadRepository;
    private final CommandRepository commandRepository;
    private final PaletteRepository paletteRepository;
    private final TopicRepository topicRepository;
    private final Configuration freeMakerCfg;
    private ObjectMapper objectMapper;
    private RestTemplate restTemplate;

    public DeployService(KafkaService kafkaService, KubernetesService kubernetesService, WorkloadRepository workloadRepository, CommandRepository commandRepository, PaletteRepository paletteRepository, TopicRepository topicRepository, Configuration freeMakerCfg) {
        this.kafkaService = kafkaService;
        this.kubernetesService = kubernetesService;
        this.workloadRepository = workloadRepository;
        this.commandRepository = commandRepository;
        this.paletteRepository = paletteRepository;
        this.topicRepository = topicRepository;
        this.freeMakerCfg = freeMakerCfg;
        this.objectMapper = new ObjectMapper();
        this.restTemplate = new RestTemplate();
    }

    public Workspace doDeploy(Workspace workspace) {
        Workspace existWorkspace = this.getWorkspaceStatus(workspace);
        if (Arrays.asList(new StatusType[]{StatusType.Define, StatusType.Deployed})
                .contains(existWorkspace.getStatus()))
        {
            List<Workload> workloads = this.workloadRepository.findByWorkspaceId(workspace.getId());
            List<Topic> topics = this.getTopics(workloads);
            topics.forEach(topic -> {
                try {
                    this.kafkaService.createTopic(topic);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            workloads.forEach(workload -> {
                try {
                    Workload newWorkload = this.kubernetesService.createDeployment(workload);
                    if(newWorkload != null) {
                        this.workloadRepository.save(newWorkload);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        return workspace;
    }

    private Workspace getWorkspaceStatus(Workspace workspace) {
        String namespace = this.kubernetesService.getNamespace(workspace.getName());
        if(namespace == null && workspace.getStatus() != StatusType.Define ) {
            workspace.setStatus(StatusType.Define);
        } else if (namespace != null && workspace.getStatus() == StatusType.Define) {
            workspace.setStatus(StatusType.Deployed);
        }
        return workspace;
    }

    private List<Topic> getTopics(List<Workload> workloads) {
        List<Topic> result = workloads.stream()
                .flatMap(workload -> {
                    try {
                        return Stream.concat(
                                Arrays.stream(objectMapper.readValue(workload.getInputTopics(), String[].class)),
                                Arrays.stream(objectMapper.readValue(workload.getOutputTopics(), String[].class))
                        );
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }).distinct()
                .map(topicName -> this.topicRepository.findByName(topicName))
                .collect(Collectors.toList());
        return result;
    }

    public Workspace undoDeploy(Workspace workspace) {
        Workspace existWorkspace = this.getWorkspaceStatus(workspace);
        if (Arrays.asList(new StatusType[]{StatusType.Deployed, StatusType.Running, StatusType.Stop, StatusType.Done, StatusType.Error})
                .contains(existWorkspace.getStatus()))
        {
            List<Workload> workloads = this.workloadRepository.findByWorkspaceId(workspace.getId());
            workloads.forEach(workload -> {
                try {
                    Workload oldWorkload = this.kubernetesService.deleteDeployment(workload);
                    if(oldWorkload != null) {
                        this.workloadRepository.save(oldWorkload);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
        return null;
    }

    public JsonNode setStatus(Workspace workspace, CommandType commandType) {
        JsonNode result = null;
        Workspace existWorkspace = this.getWorkspaceStatus(workspace);
        if (Arrays.asList(new StatusType[]{StatusType.Deployed, StatusType.Running, StatusType.Stop, StatusType.Done, StatusType.Error})
                .contains(existWorkspace.getStatus()))
        {
            result = this.objectMapper.valueToTree(existWorkspace);
            ArrayNode commandResults;
            try {
                commandResults = this.objectMapper.readValue("[]", ArrayNode.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            List<Workload> workloads = this.workloadRepository.findByWorkspaceId(workspace.getId());
            workloads.forEach(workload -> {
                Palette palette = workload.getPalette();
                if (Arrays.asList(new KafkaType[]{KafkaType.Consumer, KafkaType.Producer}).contains(palette.getKafkaType())
                    && ! Arrays.asList(new CommandType[]{CommandType.Create, CommandType.Update}).contains(commandType)) {
                    commandResults.add(this.callConnectCommand(commandType, workload));
                } else {
                    List<Command> commands = this.commandRepository.findByPaletteId(palette.getId()).stream()
                            .filter(command -> command.getCommandType() == commandType)
                            .collect(Collectors.toList());
                    commands.forEach(command -> {
                        commandResults.add(this.callHttpCommand(command, workload));
                    });
                }
            });
            ((ObjectNode)result).putArray("commandResults").addAll(commandResults);
        }
        return result;
    }

    private JsonNode callConnectCommand(CommandType commandType, Workload workload) {
        String host = "http://" + this.kubernetesService.getServiceUrl(workload);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpMethod method;
        String path;
        switch (commandType) {
            case Delete:
                method = HttpMethod.DELETE;
                path="/connectors/"+workload.getName();
                break;
            case Pause:
                method = HttpMethod.PUT;
                path="/connectors/"+workload.getName()+"/pause";
                break;
            case Resume:
                method = HttpMethod.PUT;
                path="/connectors/"+workload.getName()+"/resume";
                break;
            case Restart:
                method = HttpMethod.POST;
                path="/connectors/"+workload.getName()+"/restart";
                break;
            case List:
                method = HttpMethod.GET;
                path="/connectors";
                break;
            case Get:
                method = HttpMethod.GET;
                path="/connectors/"+workload.getName();
                break;
            case ConnectorPlugins:
                method = HttpMethod.GET;
                path="/connector-plugins";
                break;
            default:
                method = HttpMethod.GET;
                path="/connectors/"+workload.getName()+"/status";
                break;
        }
        HttpEntity<String> entity = new HttpEntity<>(null, headers);
        try {
            return this.restTemplate.exchange(host + path, method, entity, JsonNode.class).getBody();
        } catch (HttpClientErrorException exception) {
            return exception.getResponseBodyAs(JsonNode.class);
        }
    }

    private JsonNode callHttpCommand(Command command, Workload workload) {
        String url = "http://" + this.kubernetesService.getServiceUrl(workload) + command.getHttpPath().replace("${name}", workload.getName());
        HttpMethod method = HttpMethod.valueOf(command.getHttpMethod());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> root = this.kubernetesService.getRootFromWorkload(workload);
        this.kubernetesService.getConfig(workload.getWorkspace().getConfig(), root);
        this.kubernetesService.getConfig(workload.getConfig(), root);
        Reader in = new StringReader(command.getHttpBody());
        Writer out = new StringWriter();
        Template template = null;
        try {
            template = new Template("CMD_" + command.getId().toString(), in, this.freeMakerCfg);
            template.process(root, out);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String requestBody = out.toString();
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        return this.restTemplate.exchange(url, method, entity, JsonNode.class).getBody();
    }

    public JsonNode getStatus(Workspace workspace) {
        return this.setStatus(workspace, CommandType.Status);
    }
}
