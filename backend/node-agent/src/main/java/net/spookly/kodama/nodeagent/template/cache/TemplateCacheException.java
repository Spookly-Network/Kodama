package net.spookly.kodama.nodeagent.template.cache;

public class TemplateCacheException extends RuntimeException {

    public TemplateCacheException(String message) {
        super(message);
    }

    public TemplateCacheException(String message, Throwable cause) {
        super(message, cause);
    }
}
