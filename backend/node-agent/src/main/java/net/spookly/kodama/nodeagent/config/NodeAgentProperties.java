package net.spookly.kodama.nodeagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "node-agent")
public class NodeAgentProperties {

    private String nodeId = "local";
    private String nodeName = "local-node";
    private String brainBaseUrl = "";
    private String dockerHost = "";
    private String workspaceDir = "./data";

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getBrainBaseUrl() {
        return brainBaseUrl;
    }

    public void setBrainBaseUrl(String brainBaseUrl) {
        this.brainBaseUrl = brainBaseUrl;
    }

    public String getDockerHost() {
        return dockerHost;
    }

    public void setDockerHost(String dockerHost) {
        this.dockerHost = dockerHost;
    }

    public String getWorkspaceDir() {
        return workspaceDir;
    }

    public void setWorkspaceDir(String workspaceDir) {
        this.workspaceDir = workspaceDir;
    }
}
