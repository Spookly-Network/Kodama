package net.spookly.kodama.nodeagent.template.storage;

public interface TemplateStorageClient {

    TemplateTarball getTemplateTarball(String templateId, String version, String s3Key);
}
