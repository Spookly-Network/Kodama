package net.spookly.kodama.brain.service;

import java.util.UUID;

import net.spookly.kodama.brain.domain.instance.TemplateAssignmentSource;
import net.spookly.kodama.brain.domain.template.TemplateVersion;

public record ResolvedTemplateLayer(
    UUID assignmentId,
    UUID templateId,
    TemplateVersion templateVersion,
    int priority,
    int orderIndex,
    TemplateAssignmentSource source) {}
