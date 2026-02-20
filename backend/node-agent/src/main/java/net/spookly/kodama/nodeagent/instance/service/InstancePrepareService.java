package net.spookly.kodama.nodeagent.instance.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import net.spookly.kodama.nodeagent.instance.callback.InstanceCallbackService;
import net.spookly.kodama.nodeagent.instance.dto.NodePrepareInstanceLayer;
import net.spookly.kodama.nodeagent.instance.dto.NodePrepareInstanceRequest;
import net.spookly.kodama.nodeagent.instance.registry.InstanceRegistryService;
import net.spookly.kodama.nodeagent.instance.workspace.InstanceWorkspaceManager;
import net.spookly.kodama.nodeagent.instance.workspace.InstanceWorkspacePaths;
import net.spookly.kodama.nodeagent.template.cache.TemplateCacheLookupResult;
import net.spookly.kodama.nodeagent.template.cache.TemplateCachePopulateService;
import net.spookly.kodama.nodeagent.template.merge.TemplateLayerMergeService;
import net.spookly.kodama.nodeagent.template.merge.TemplateLayerSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class InstancePrepareService {

    private static final Logger logger = LoggerFactory.getLogger(InstancePrepareService.class);

    private final TemplateCachePopulateService cachePopulateService;
    private final TemplateLayerMergeService mergeService;
    private final InstanceWorkspaceManager workspaceManager;
    private final InstanceVariablesResolver variablesResolver;
    private final InstanceCallbackService callbackService;
    private final InstanceRegistryService registryService;
    private final InstanceStartService instanceStartService;

    public InstancePrepareService(
            TemplateCachePopulateService cachePopulateService,
            TemplateLayerMergeService mergeService,
            InstanceWorkspaceManager workspaceManager,
            InstanceVariablesResolver variablesResolver,
            InstanceCallbackService callbackService,
            InstanceRegistryService registryService,
            InstanceStartService instanceStartService
    ) {
        this.cachePopulateService = Objects.requireNonNull(cachePopulateService, "cachePopulateService");
        this.mergeService = Objects.requireNonNull(mergeService, "mergeService");
        this.workspaceManager = Objects.requireNonNull(workspaceManager, "workspaceManager");
        this.variablesResolver = Objects.requireNonNull(variablesResolver, "variablesResolver");
        this.callbackService = Objects.requireNonNull(callbackService, "callbackService");
        this.registryService = Objects.requireNonNull(registryService, "registryService");
        this.instanceStartService = Objects.requireNonNull(instanceStartService, "instanceStartService");
    }

    public void prepare(NodePrepareInstanceRequest request) {
        if (request == null) {
            throw new InstancePrepareValidationException("prepare request is required");
        }
        UUID instanceId = requireInstanceId(request.instanceId());
        int layerCount;
        try {
            List<NodePrepareInstanceLayer> layers = requireLayers(request.layers());
            layerCount = layers.size();
            logger.info("Preparing instance workspace. instanceId={} layers={}", instanceId, layerCount);
            InstanceWorkspacePaths workspace = workspaceManager.prepareWorkspace(instanceId.toString());
            Map<String, String> variables = variablesResolver.resolve(request.variables(), request.variablesJson());

            List<TemplateLayerSource> sources = new ArrayList<>(layers.size());
            for (NodePrepareInstanceLayer layer : layers) {
                sources.add(resolveLayerSource(layer));
            }

            mergeService.mergeLayers(instanceId.toString(), workspace.mergedDir(), sources, variables);
            registryService.recordPrepared(workspace, request, layers, variables);
        } catch (InstancePrepareValidationException ex) {
            logger.warn("Instance preparation rejected. instanceId={}", instanceId, ex);
            try {
                callbackService.sendFailed(instanceId);
            } catch (RuntimeException callbackEx) {
                logger.warn("Failed to send prepare failure callback. instanceId={}", instanceId, callbackEx);
            }
            throw ex;
        } catch (RuntimeException ex) {
            logger.warn("Instance preparation failed. instanceId={}", instanceId, ex);
            try {
                callbackService.sendFailed(instanceId);
            } catch (RuntimeException callbackEx) {
                logger.warn("Failed to send prepare failure callback. instanceId={}", instanceId, callbackEx);
            }
            throw ex;
        }

        try {
            callbackService.sendPrepared(instanceId);
        } catch (RuntimeException ex) {
            logger.warn("Prepared callback failed. instanceId={}", instanceId, ex);
        }
        logger.info("Instance preparation complete. instanceId={} layers={}", instanceId, layerCount);
        instanceStartService.startInstance(instanceId, request.displayName());
    }

    private TemplateLayerSource resolveLayerSource(NodePrepareInstanceLayer layer) {
        if (layer == null) {
            throw new InstancePrepareValidationException("template layer is required");
        }
        requireTemplateVersionId(layer.templateVersionId());
        UUID templateId = requireTemplateId(layer.templateId());
        String version = requireValue("version", layer.version());
        String checksum = requireValue("checksum", layer.checksum());
        String s3Key = requireValue("s3Key", layer.s3Key());
        int orderIndex = layer.orderIndex();
        if (orderIndex < 0) {
            throw new InstancePrepareValidationException("layer orderIndex must be >= 0");
        }
        String templateKey = templateId.toString();
        TemplateCacheLookupResult cached = cachePopulateService.ensureCachedTemplate(
                templateKey,
                version,
                checksum,
                s3Key
        );
        return new TemplateLayerSource(templateKey, version, orderIndex, cached.contentsDir());
    }

    private UUID requireInstanceId(UUID instanceId) {
        if (instanceId == null) {
            throw new InstancePrepareValidationException("instanceId is required");
        }
        return instanceId;
    }

    private UUID requireTemplateVersionId(UUID templateVersionId) {
        if (templateVersionId == null) {
            throw new InstancePrepareValidationException("templateVersionId is required");
        }
        return templateVersionId;
    }

    private UUID requireTemplateId(UUID templateId) {
        if (templateId == null) {
            throw new InstancePrepareValidationException("templateId is required");
        }
        return templateId;
    }

    private List<NodePrepareInstanceLayer> requireLayers(List<NodePrepareInstanceLayer> layers) {
        if (layers == null || layers.isEmpty()) {
            throw new InstancePrepareValidationException("template layers are required");
        }
        return layers;
    }

    private String requireValue(String label, String value) {
        if (value == null || value.isBlank()) {
            throw new InstancePrepareValidationException(label + " is required");
        }
        return value.trim();
    }
}
