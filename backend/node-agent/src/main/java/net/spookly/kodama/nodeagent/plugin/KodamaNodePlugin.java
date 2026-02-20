package net.spookly.kodama.nodeagent.plugin;

public interface KodamaNodePlugin {

  int API_VERSION = 1;

  String id();

  default int apiVersion() {
    return API_VERSION;
  }

  default String displayName() {
    return id();
  }

  default NodeInstanceStartMutation onBeforeInstanceStart(NodeInstanceStartContext context) {
    return NodeInstanceStartMutation.empty();
  }
}
