export type InstanceState =
    | 'REQUESTED'
    | 'PREPARING'
    | 'PREPARED'
    | 'STARTING'
    | 'RUNNING'
    | 'STOPPING'
    | 'STOPPED'
    | 'DESTROYED'
    | 'FAILED'

export type TemplateAssignmentSource = 'INSTANCE' | 'GROUP'

export interface InstanceTemplateLayer {
    id: string
    templateId: string
    templateVersionId: string
    priority: number
    orderIndex: number
    source: TemplateAssignmentSource
}

export interface Instance {
    id: string
    name: string
    displayName: string
    state: InstanceState
    nodeId: string | null
    requestedBy: string | null
    region: string | null
    tags: string | null
    devModeAllowed: boolean | null
    portsJson: string | null
    variablesJson: string | null
    createdAt: string
    updatedAt: string
    startedAt: string | null
    stoppedAt: string | null
    failureReason: string | null
    templateLayers: InstanceTemplateLayer[]
}

export type InstanceDto = Instance
