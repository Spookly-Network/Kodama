package net.spookly.kodama.nodeagent.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "node-agent")
public class NodeConfig {

    private String nodeId;
    private String nodeName;
    private String nodeVersion;
    private String region;
    private int capacitySlots;
    private boolean devMode;
    private String tags;
    private String baseUrl;
    private String brainBaseUrl;
    private String dockerHost;
    private String workspaceDir = "./data";
    private String cacheDir;
    private boolean registrationEnabled = true;
    private Auth auth = new Auth();
    private S3 s3 = new S3();

    public void validate() {
        List<String> errors = new ArrayList<>();
        addIfBlank(errors, nodeName, "node-agent.node-name is required");
        addIfBlank(errors, nodeVersion, "node-agent.node-version is required");
        addIfBlank(errors, region, "node-agent.region is required");
        addIfBlank(errors, brainBaseUrl, "node-agent.brain-base-url is required");
        addIfBlank(errors, cacheDir, "node-agent.cache-dir is required");
        if (capacitySlots < 1) {
            errors.add("node-agent.capacity-slots must be at least 1");
        }
        if (!errors.isEmpty()) {
            throw new IllegalStateException("Invalid node-agent configuration:\n- " + String.join("\n- ", errors));
        }
    }

    private void addIfBlank(List<String> errors, String value, String message) {
        if (value == null || value.isBlank()) {
            errors.add(message);
        }
    }

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

    public String getNodeVersion() {
        return nodeVersion;
    }

    public void setNodeVersion(String nodeVersion) {
        this.nodeVersion = nodeVersion;
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

    public String getCacheDir() {
        return cacheDir;
    }

    public void setCacheDir(String cacheDir) {
        this.cacheDir = cacheDir;
    }

    public boolean isRegistrationEnabled() {
        return registrationEnabled;
    }

    public void setRegistrationEnabled(boolean registrationEnabled) {
        this.registrationEnabled = registrationEnabled;
    }

    public Auth getAuth() {
        return auth;
    }

    public void setAuth(Auth auth) {
        this.auth = auth == null ? new Auth() : auth;
    }

    public S3 getS3() {
        return s3;
    }

    public void setS3(S3 s3) {
        this.s3 = s3 == null ? new S3() : s3;
    }

    public static class Auth {

        private String tokenPath;
        private String certPath;
        private String headerName = "X-Node-Token";

        public String getTokenPath() {
            return tokenPath;
        }

        public void setTokenPath(String tokenPath) {
            this.tokenPath = tokenPath;
        }

        public String getCertPath() {
            return certPath;
        }

        public void setCertPath(String certPath) {
            this.certPath = certPath;
        }

        public String getHeaderName() {
            return headerName;
        }

        public void setHeaderName(String headerName) {
            this.headerName = headerName;
        }
    }

    public static class S3 {

        private String endpoint;
        private String region;
        private String bucket;
        private String accessKey;
        private String secretKey;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }
    }
}
