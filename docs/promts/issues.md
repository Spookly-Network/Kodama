# What should you do
Put the x development steps into small issues. use the templates you already created. 
Be very thorough when writing these. Keep them small and preferably indipendent from each other so 
it is easy to work on it.

- Use the labels to categorize the issues. Add types to the issues, as well as milestones.
- When adding blocks, use the issue number and add them only when it really blocks the development.
- Add epics as parents and depends as blockedby.
- When you are done, create a json file with all the issues and upload it to the issues folder.


**Start issue counting at #40.**

## Labels:
- area: Node
- area: Brain
- area: instance
- area: infrastructure
- area: panel
- area: template-system
- prio: low
- prio: medium
- prio: high

## Issue Types:
- Architecture
- Bug
- Documentation
- Epic
- Feature
- Research
- Task

# Example json
```json
{
  "id": "#73",
  "title": "[Feature] End-to-end integration test for node-agent lifecycle",
  "summary": "Create an integration test that exercises the full prepare/start/stop/destroy flow against a test Brain and Docker.",
  "details": "Use Testcontainers and a lightweight fake Brain or mocks to drive node-agent through a realistic lifecycle scenario.",
  "scope": "\u2022 Start Docker via Testcontainers\n\u2022 Start node-agent in test mode\n\u2022 Simulate Brain commands for one instance\n\u2022 Assert that workspace is created, container runs, callbacks are sent and cleanup happens.",
  "acceptance": "Integration test can be run locally/CI and passes, giving confidence that the node-agent lifecycle works end-to-end.",
  "notes": "",
  "labels": [
    "area: Node",
    "area: instance",
    "prio: medium"
  ],
  "type": "Task",
  "parent": "#45",
  "blockedby": [
    "#59",
    "#62",
    "#63",
    "#64",
    "#65",
    "#67"
  ],
  "milestone": "Node Agent - Phase 2 Stability & QoL"
}
```
**Important**
- Make shure to use the right structure for the issue type.
- When issue type is "Epic", use the fields from epic template and not from feature template.

## Output
Provide a downloadable zip and md files