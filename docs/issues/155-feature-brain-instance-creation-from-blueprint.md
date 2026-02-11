# #155 [Feature] Brain: instance creation from blueprint with overrides

## Summary
Extend instance creation to accept a blueprint reference and apply override semantics.

## Details
When blueprintId is provided, the Brain resolves effective fields from the blueprint, applies overrides from the request, and persists blueprintId plus override fields on the instance.

## Scope / Requirements
- Extend CreateInstanceRequest with:
  - blueprintId (optional)
  - overrides: permanent, slotsRequired, containerImage, installScript, startCommand, variables/variablesJson, portDefinitions, groupIds, templateLayers
- If blueprintId is present:
  - templateLayers is optional; if provided it replaces blueprint assignments
  - portDefinitions overrides replace blueprint port definitions
  - groupIds overrides replace blueprint group links; when absent, apply blueprint groups
  - variables overrides replace blueprint variables; variables and variablesJson remain mutually exclusive
- Reject creation if blueprint is deleted.
- Persist blueprintId and override fields on the instance.
- Resolve defaults:
  - slotsRequired defaults to 1 when not provided by blueprint or override
- Ensure existing non-blueprint flow remains unchanged.

## Acceptance Criteria
- Instance creation succeeds with blueprintId only and uses blueprint defaults.
- Providing overrides replaces blueprint values and persists on the instance.
- Deleted blueprint rejects new instance creation with a clear error.
- Tests cover override replacement, blueprint default usage, and backwards compatibility.
