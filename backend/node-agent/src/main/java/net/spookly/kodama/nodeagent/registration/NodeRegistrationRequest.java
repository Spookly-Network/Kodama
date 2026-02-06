package net.spookly.kodama.nodeagent.registration;

import com.fasterxml.jackson.annotation.JsonInclude;
import net.spookly.kodama.nodeagent.config.NodeConfig;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class NodeRegistrationRequest {

    private String name;
    private String region;
    private int capacitySlots;
    private String nodeVersion;
    private boolean devMode;
    private String tags;
    private String baseUrl;

    public NodeRegistrationRequest() {
    }

    public static NodeRegistrationRequest fromConfig(NodeConfig config) {
        NodeRegistrationRequest request = new NodeRegistrationRequest();
        request.setName(config.getNodeName());
        request.setRegion(config.getRegion());
        request.setCapacitySlots(config.getCapacitySlots());
        request.setNodeVersion(config.getNodeVersion());
        request.setDevMode(config.isDevMode());
        request.setTags(config.getTags());
        request.setBaseUrl(config.getBaseUrl());
        return request;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public int getCapacitySlots() {
        return capacitySlots;
    }

    public void setCapacitySlots(int capacitySlots) {
        this.capacitySlots = capacitySlots;
    }

    public String getNodeVersion() {
        return nodeVersion;
    }

    public void setNodeVersion(String nodeVersion) {
        this.nodeVersion = nodeVersion;
    }

    public boolean isDevMode() {
        return devMode;
    }

    public void setDevMode(boolean devMode) {
        this.devMode = devMode;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
