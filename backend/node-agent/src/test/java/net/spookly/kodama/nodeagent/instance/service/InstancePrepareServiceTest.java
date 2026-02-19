package net.spookly.kodama.nodeagent.instance.service;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class InstancePrepareServiceTest {

    @Test
    void prepareDoesNotFailWhenPreparedCallbackFails() {
        TemplateCachePopulateService cachePopulateService = mock(TemplateCachePopulateService.class);
        TemplateLayerMergeService mergeService = mock(TemplateLayerMergeService.class);
        InstanceWorkspaceManager workspaceManager = mock(InstanceWorkspaceManager.class);
        InstanceVariablesResolver variablesResolver = mock(InstanceVariablesResolver.class);
        InstancePortAllocationService portAllocationService = mock(InstancePortAllocationService.class);
        InstanceCallbackService callbackService = mock(InstanceCallbackService.class);
        InstanceRegistryService registryService = mock(InstanceRegistryService.class);
        InstanceStartService instanceStartService = mock(InstanceStartService.class);
        InstancePrepareService service = new InstancePrepareService(
                cachePopulateService,
                mergeService,
                workspaceManager,
                variablesResolver,
                portAllocationService,
                callbackService,
                registryService,
                instanceStartService
        );

        UUID instanceId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        UUID templateVersionId = UUID.randomUUID();
        NodePrepareInstanceLayer layer = new NodePrepareInstanceLayer(
                templateVersionId,
                templateId,
                "1.0.0",
                "checksum",
                "templates/test.tgz",
                null,
                0
        );
        NodePrepareInstanceRequest request = new NodePrepareInstanceRequest(
                instanceId,
                "demo",
                "demo",
                null,
                Map.of(),
                null,
                List.of(layer)
        );

        InstanceWorkspacePaths workspace = new InstanceWorkspacePaths(
                instanceId.toString(),
                Path.of("/tmp/instances", instanceId.toString()),
                Path.of("/tmp/instances", instanceId.toString(), "merged"),
                Path.of("/tmp/instances", instanceId.toString(), "logs"),
                Path.of("/tmp/instances", instanceId.toString(), "temp")
        );

        when(workspaceManager.prepareWorkspace(instanceId.toString())).thenReturn(workspace);
        when(variablesResolver.resolve(any(), any())).thenReturn(Map.of());
        when(portAllocationService.allocate(any(), any()))
                .thenReturn(InstancePortAllocationService.PortAllocationResult.none());
        when(cachePopulateService.ensureCachedTemplate(any(), any(), any(), any()))
                .thenReturn(TemplateCacheLookupResult.hit(
                        templateId.toString(),
                        "1.0.0",
                        "checksum",
                        Path.of("/tmp/cache"),
                        "checksum"
                ));
        doNothing().when(mergeService).mergeLayers(any(), any(), any(), any());
        doNothing().when(registryService).recordPrepared(any(), any(), any(), any(), any());
        doThrow(new RuntimeException("callback boom"))
                .when(callbackService)
                .sendPrepared(instanceId, null);

        assertThatNoException().isThrownBy(() -> service.prepare(request));

        verify(callbackService).sendPrepared(instanceId, null);
    }

    @Test
    @SuppressWarnings("unchecked")
    void prepareInjectsAllocatedPortsIntoVariablesAndPreparedCallback() {
        TemplateCachePopulateService cachePopulateService = mock(TemplateCachePopulateService.class);
        TemplateLayerMergeService mergeService = mock(TemplateLayerMergeService.class);
        InstanceWorkspaceManager workspaceManager = mock(InstanceWorkspaceManager.class);
        InstanceVariablesResolver variablesResolver = mock(InstanceVariablesResolver.class);
        InstancePortAllocationService portAllocationService = mock(InstancePortAllocationService.class);
        InstanceCallbackService callbackService = mock(InstanceCallbackService.class);
        InstanceRegistryService registryService = mock(InstanceRegistryService.class);
        InstanceStartService instanceStartService = mock(InstanceStartService.class);
        InstancePrepareService service = new InstancePrepareService(
                cachePopulateService,
                mergeService,
                workspaceManager,
                variablesResolver,
                portAllocationService,
                callbackService,
                registryService,
                instanceStartService
        );

        UUID instanceId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        UUID templateVersionId = UUID.randomUUID();
        NodePrepareInstanceLayer layer = new NodePrepareInstanceLayer(
                templateVersionId,
                templateId,
                "1.0.0",
                "checksum",
                "templates/test.tgz",
                null,
                0
        );
        NodePrepareInstanceRequest request = new NodePrepareInstanceRequest(
                instanceId,
                "demo",
                "demo",
                null,
                Map.of("ENV", "prod", "PORT", "20000"),
                null,
                List.of(layer)
        );

        InstanceWorkspacePaths workspace = new InstanceWorkspacePaths(
                instanceId.toString(),
                Path.of("/tmp/instances", instanceId.toString()),
                Path.of("/tmp/instances", instanceId.toString(), "merged"),
                Path.of("/tmp/instances", instanceId.toString(), "logs"),
                Path.of("/tmp/instances", instanceId.toString(), "temp")
        );

        String allocatedPortsJson = "[{\"name\":\"game\",\"protocol\":\"udp\",\"containerPort\":25565,\"hostPort\":30000}]";
        when(workspaceManager.prepareWorkspace(instanceId.toString())).thenReturn(workspace);
        when(variablesResolver.resolve(any(), any())).thenReturn(Map.of("ENV", "prod", "PORT", "20000"));
        when(portAllocationService.allocate(eq(instanceId), any())).thenReturn(
                new InstancePortAllocationService.PortAllocationResult(
                        allocatedPortsJson,
                        Map.of("PORT", "30000", "PORT_GAME", "30000")
                )
        );
        when(cachePopulateService.ensureCachedTemplate(any(), any(), any(), any()))
                .thenReturn(TemplateCacheLookupResult.hit(
                        templateId.toString(),
                        "1.0.0",
                        "checksum",
                        Path.of("/tmp/cache"),
                        "checksum"
                ));

        assertThatNoException().isThrownBy(() -> service.prepare(request));

        ArgumentCaptor<Map<String, String>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mergeService).mergeLayers(eq(instanceId.toString()), eq(workspace.mergedDir()), any(), variablesCaptor.capture());
        assertThat(variablesCaptor.getValue())
                .containsEntry("ENV", "prod")
                .containsEntry("PORT", "30000")
                .containsEntry("PORT_GAME", "30000");
        verify(registryService).recordPrepared(eq(workspace), eq(request), eq(List.of(layer)), eq(variablesCaptor.getValue()), eq(allocatedPortsJson));
        verify(callbackService).sendPrepared(instanceId, allocatedPortsJson);
    }
}
