package net.spookly.kodama.nodeagent.template.controller;

import net.spookly.kodama.nodeagent.template.cache.TemplateCacheManager;
import net.spookly.kodama.nodeagent.template.cache.TemplateCachePurgeResult;
import net.spookly.kodama.nodeagent.template.dto.TemplateCachePurgeRequest;
import net.spookly.kodama.nodeagent.template.dto.TemplateCachePurgeResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/cache")
public class TemplateCacheController {

    private final TemplateCacheManager cacheManager;

    public TemplateCacheController(TemplateCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @PostMapping("/purge")
    @ResponseStatus(HttpStatus.OK)
    public TemplateCachePurgeResponse purge(@RequestBody(required = false) TemplateCachePurgeRequest request) {
        String templateId = request == null ? null : request.templateId();
        if (templateId != null && templateId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "templateId must not be blank");
        }
        try {
            if (templateId == null) {
                TemplateCachePurgeResult result = cacheManager.purgeAll();
                return TemplateCachePurgeResponse.forAll(result);
            }
            TemplateCachePurgeResult result = cacheManager.purgeTemplate(templateId);
            return TemplateCachePurgeResponse.forTemplate(templateId.trim(), result);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }
}
