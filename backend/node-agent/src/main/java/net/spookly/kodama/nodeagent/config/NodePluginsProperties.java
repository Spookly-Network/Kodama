package net.spookly.kodama.nodeagent.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "plugins")
public class NodePluginsProperties {

    private String dir = "./plugins";
    private List<String> enabled = new ArrayList<>();

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir == null || dir.isBlank() ? "./plugins" : dir;
    }

    public List<String> getEnabled() {
        return enabled;
    }

    public void setEnabled(List<String> enabled) {
        if (enabled == null) {
            this.enabled = new ArrayList<>();
            return;
        }
        List<String> filtered = new ArrayList<>();
        for (String entry : enabled) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            filtered.add(entry.trim());
        }
        this.enabled = filtered;
    }
}
