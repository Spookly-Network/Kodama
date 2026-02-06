# Template System

The template system defines how servers are built using multiple versioned layers.

---

## 1. Storage

All templates are stored as `.tar` archives in an S3 bucket.  
The Brain stores only metadata:

- Template ID
- Version
- Checksum
- S3 key
- Optional metadata JSON

---

## 2. Template Layers

Instances can have any number of layers:

```md
Base Template
↳ Game Mode Template
↳ Map Template
↳ Custom Overrides
```


### Rules
- Order is defined by the Brain
- Node merges layers in sequence
- Last layer wins for file collisions
- Variables are replaced after merging

---

## 3. Local Cache

Each Node maintains a local cache:

- Cache keyed by Template ID + Version + Checksum
- Node only re-downloads if checksum differs
- Cache can be invalidated:
    - By Brain command
    - By Dev-Mode (always fetch fresh)

---

## 4. Variable Replacement

Variables use a simple `${VAR_NAME}` format.

Common variables:
- INSTANCE_ID
- INSTANCE_NAME
- PORT
- REGION
- CUSTOM values from Brain

---

## 5. Merge Strategy

Layer merge logic:

1. Extract each template layer in order
2. Overlay files onto the workspace
3. Last write wins
4. Apply variables
5. Pass final directory to Docker

Merge rules are platform-agnostic and work for any game.
