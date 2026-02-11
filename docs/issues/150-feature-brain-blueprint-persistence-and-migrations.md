# #150 [Feature] Brain: blueprint persistence and migrations

## Summary
Add database tables and entities for blueprints, blueprint attachments, and instance override fields.

## Details
Blueprints are stored as normal entities with soft delete. Instances store blueprintId plus override fields for runtime configuration.

## Scope / Requirements
- Flyway migration adds:
  - blueprints table with name, permanent, slotsRequired, containerImage, installScript, startCommandJson, variablesJson, deletedAt, createdAt, updatedAt.
  - blueprint_template_assignments table (blueprintId, templateId, templateVersionId, priority).
  - blueprint_port_definitions table (blueprintId, name, protocol, containerPort, hostRangeMin, hostRangeMax, hostRangeStep).
  - blueprint_group_links table (blueprintId, groupId).
  - instance columns: blueprint_id and override fields (permanent, slotsRequired, containerImage, installScript, startCommandJson, variablesJson, portDefinitionsJson).
- Unique constraint on blueprint name.
- Indexes on blueprintId in all attachment tables.
- JPA entities and repositories for new tables.
- Default slotsRequired to 1 when null during persistence.

## Acceptance Criteria
- Migration runs on MySQL and creates the expected schema and indexes.
- Entities can be persisted and queried without manual fixes.
- Soft delete field is available for API logic.
