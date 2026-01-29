import type { Node } from "./Node"

export interface Instance {
    id: string
    name: string
    displayName: string
    state: InstanceState
    requestedByUserId: string
    node?: Node
    region: string
    tags: string
    devModeAllowed: boolean
    portsJson: string
    variablesJson: string
    createdAt: Date
    updatedAt: Date
    startedAt?: Date
    stoppedAt?: Date
    failureReason?: string
}

export interface InstanceDto {
    id: string
    name: string
    displayName: string
    state: InstanceState
    requestedByUserId: string
    nodeId?: string
    region: string
    tags: string
    devModeAllowed: boolean
    portsJson: string
    variablesJson: string
    createdAt: string
    updatedAt: string
    startedAt?: string
    stoppedAt?: string
    failureReason?: string
}

export enum InstanceState {
    REQUESTED,
    PREPARING,
    PREPARED,
    STARTING,
    RUNNING,
    STOPPING,
    STOPPED,
    DESTROYED,
    FAILED
}

